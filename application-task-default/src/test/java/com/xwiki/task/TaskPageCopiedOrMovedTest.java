package com.xwiki.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.refactoring.event.DocumentCopyingEvent;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.TaskPageCopiedOrMovedEventListener;
import com.xwiki.task.model.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class TaskPageCopiedOrMovedTest
{
    @InjectMockComponents
    private TaskPageCopiedOrMovedEventListener listener;

    @MockComponent
    private ObservationContext observationContext;

    @MockComponent
    private TaskCounter taskCounter;

    @MockComponent
    private Logger logger;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> serializer;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private BaseObject baseObject;

    @Mock
    private XWiki xWiki;

    private XWikiContext context;

    private EntityReference copiedEntityRef = new LocalDocumentReference(Arrays.asList("Smth", "Tasks", "Page"),
        "WebHome");

    private EntityReference taskOwner = new DocumentReference("xwiki", "Smth", "WebHome");

    private ArgumentCaptor<BeginEvent> matcher = ArgumentCaptor.forClass(BeginEvent.class);

    @BeforeEach
    void setup() throws TaskException
    {
        context = new XWikiContext();
        context.setWiki(xWiki);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);

        DocumentCopyingEvent copyingEvent = new DocumentCopyingEvent();

        CopyRequest copyRequest = new CopyRequest();
        copyRequest.setEntityReferences(Collections.singletonList(copiedEntityRef));
        JobStartedEvent jobStartedEvent = new JobStartedEvent("type", "", copyRequest);

        setJobsThatEventIsIn(copyingEvent, jobStartedEvent);

        when(xWikiDocument.getXObject(any(LocalDocumentReference.class))).thenReturn(baseObject);
        when(taskCounter.getNextNumber()).thenReturn(1);

        when(serializer.serialize(eq(taskOwner))).thenReturn(taskOwner.toString());

        when(baseObject.getLargeStringValue(Task.OWNER)).thenReturn("owner");
        when(baseObject.getDocumentReference()).thenReturn(
            new DocumentReference((LocalDocumentReference) copiedEntityRef, new WikiReference(
                "xwiki")));
    }

    private void setJobsThatEventIsIn(BeginEvent copyingEvent, BeginEvent jobStartedEvent)
    {
        reset(observationContext);
        AtomicInteger callCount = new AtomicInteger(0);
        when(observationContext.isIn(matcher.capture())).thenAnswer(invocation -> {
            BeginEvent event = invocation.getArgument(0);
            int callNr = callCount.getAndIncrement();
            if (callNr == 0) {
                return event.matches(copyingEvent);
            } else {
                return event.matches(jobStartedEvent);
            }
        });
    }

    @Test
    void changeNumberAndOwnerOfCopiedTaskPageTest() throws XWikiException
    {
        // Check recursion.
        doAnswer(invocation -> {
            listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);
            return null;
        }).when(xWiki).saveDocument(xWikiDocument, context);

        listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);

        verify(baseObject).set(Task.NUMBER, 1, context);
        verify(baseObject).set(eq(Task.OWNER), eq(new DocumentReference("xwiki", "Smth", "WebHome").toString()),
            eq(context));
        verify(xWiki, atMostOnce()).saveDocument(xWikiDocument, context);
    }

    @Test
    void doNothingIfNotInsideARefactoringJobTest() throws XWikiException
    {
        reset(observationContext);
        when(observationContext.isIn(any())).thenAnswer(invocation -> {
            return false;
        });
        listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);

        verify(baseObject, never()).set(any(), any(), any());
        verify(xWiki, never()).saveDocument(xWikiDocument, context);
    }

    @Test
    void doNothingIfNotATaskPageTest() throws XWikiException
    {
        when(xWikiDocument.getXObject(any(EntityReference.class))).thenReturn(null);

        listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);

        verify(baseObject, never()).set(any(), any(), any());
        verify(xWiki, never()).saveDocument(xWikiDocument, context);
    }

    @Test
    void doNotChangeIdIfMovingPageTest() throws XWikiException
    {
        DocumentRenamingEvent docRenamingEvent = new DocumentRenamingEvent();

        CopyRequest copyRequest = new CopyRequest();
        copyRequest.setEntityReferences(Collections.singletonList(copiedEntityRef));
        JobStartedEvent jobStartedEvent = new JobStartedEvent("type", "", copyRequest);
        setJobsThatEventIsIn(docRenamingEvent, jobStartedEvent);

        listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);

        verify(baseObject, never()).set(Task.NUMBER, 1, context);
        verify(baseObject).set(eq(Task.OWNER), eq(new DocumentReference("xwiki", "Smth", "WebHome").toString()),
            eq(context));
        verify(xWiki, atMostOnce()).saveDocument(xWikiDocument, context);
    }

    @Test
    void doNotSetOwnerIfItsATaskPageWithoutOneTest() throws XWikiException
    {
        setJobsThatEventIsIn(new DocumentRenamingEvent(), null);
        when(baseObject.getLargeStringValue(Task.OWNER)).thenReturn("");
        listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);

        verify(baseObject, never()).set(eq(Task.OWNER), any(EntityReference.class),
            eq(context));
        verify(xWiki, never()).saveDocument(xWikiDocument, context);
    }

    @Test
    void doNotSetOwnerIfNotInARefactorJob() throws XWikiException
    {
        JobStartedEvent jobStartedEvent = new JobStartedEvent("a", "b", new DeleteRequest());
        setJobsThatEventIsIn(new DocumentRenamingEvent(), jobStartedEvent);
        listener.onEvent(new DocumentCreatedEvent(), xWikiDocument, context);

        verify(baseObject, never()).set(eq(Task.OWNER), any(EntityReference.class),
            eq(context));
        verify(xWiki, never()).saveDocument(xWikiDocument, context);
    }
}
