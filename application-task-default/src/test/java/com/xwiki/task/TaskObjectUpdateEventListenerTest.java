/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.task;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationContext;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.AbstractTaskEventListener;
import com.xwiki.task.internal.TaskObjectUpdateEventListener;
import com.xwiki.task.internal.TaskXDOMProcessor;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskObjectUpdateEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class TaskObjectUpdateEventListenerTest
{
    private static final String OWNER = "Space.Owner";

    private static final String OLD_CONTENT = "old content";

    private static final String NEW_CONTENT = "new content";

    @InjectMockComponents
    private TaskObjectUpdateEventListener eventListener;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private TaskCounter taskCounter;

    @MockComponent
    private TaskXDOMProcessor taskXDOMProcessor;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private ObservationContext observationContext;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private TaskConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument taskDoc;

    @Mock
    private XWikiDocument ownerDoc;

    @Mock
    private BaseObject taskObj;

    @Mock
    private XDOM ownerXDOM;

    @Mock
    private DocumentAuthors ownerAuthors;

    @Mock
    private UserReference userReference;

    private final DocumentReference taskRef = new DocumentReference("xwiki", "Space", "Task");

    private final DocumentReference ownerRef = new DocumentReference("xwiki", "Space", "Owner");

    private final DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User1");

    @BeforeEach
    void setup() throws XWikiException
    {
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getUserReference()).thenReturn(this.userDocRef);
        when(this.userReferenceResolver.resolve(this.userDocRef)).thenReturn(this.userReference);
        when(this.taskDoc.getDocumentReference()).thenReturn(this.taskRef);
        when(this.taskDoc.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(this.taskObj);
        when(this.taskObj.getStringValue(Task.OWNER)).thenReturn(OWNER);
        when(this.taskObj.getIntValue(Task.NUMBER, -1)).thenReturn(1);
        when(this.taskObj.getDocumentReference()).thenReturn(this.taskRef);
        when(this.resolver.resolve(OWNER, this.taskRef)).thenReturn(this.ownerRef);
        when(this.wiki.getDocument(this.taskRef, this.context)).thenReturn(this.taskDoc);
        when(this.wiki.getDocument(this.ownerRef, this.context)).thenReturn(this.ownerDoc);
        when(this.ownerDoc.clone()).thenReturn(this.ownerDoc);
        when(this.ownerDoc.getDocumentReference()).thenReturn(this.ownerRef);
        when(this.ownerDoc.getXDOM()).thenReturn(this.ownerXDOM);
        when(this.ownerDoc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(this.ownerDoc.getAuthors()).thenReturn(this.ownerAuthors);
        when(this.taskXDOMProcessor.updateTaskMacroCall(this.ownerRef, this.taskObj, this.ownerXDOM,
            Syntax.XWIKI_2_1)).thenReturn(this.ownerXDOM);
        when(this.taskXDOMProcessor.removeTaskMacroCall(this.taskRef, this.ownerRef, this.ownerXDOM,
            Syntax.XWIKI_2_1)).thenReturn(this.ownerXDOM);
        when(this.authorizationManager.hasAccess(Right.EDIT, this.ownerRef)).thenReturn(true);
    }

    @Test
    void updateTaskSavesOwnerWhenMacroCallChanged() throws XWikiException
    {
        when(this.ownerDoc.getContent()).thenReturn(OLD_CONTENT, NEW_CONTENT);

        this.eventListener.onEvent(new DocumentUpdatingEvent(), this.taskDoc, this.context);

        verify(this.ownerAuthors).setOriginalMetadataAuthor(this.userReference);
        verify(this.wiki).saveDocument(this.ownerDoc, "Task [xwiki:Space.Task] has been updated!", this.context);
    }

    @Test
    void updateTaskDoesNotSaveOwnerWhenContentUnchanged() throws XWikiException
    {
        when(this.ownerDoc.getContent()).thenReturn(OLD_CONTENT);

        this.eventListener.onEvent(new DocumentUpdatingEvent(), this.taskDoc, this.context);

        verify(this.wiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any(XWikiContext.class));
    }

    @Test
    void updateTaskDoesNotSaveOwnerWithoutEditRight() throws XWikiException
    {
        when(this.authorizationManager.hasAccess(Right.EDIT, this.ownerRef)).thenReturn(false);
        when(this.ownerDoc.getContent()).thenReturn(OLD_CONTENT, NEW_CONTENT);

        this.eventListener.onEvent(new DocumentUpdatingEvent(), this.taskDoc, this.context);

        verify(this.wiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any(XWikiContext.class));
        assertEquals("The task macro of the task [xwiki:Space.Task] was not updated in the page "
            + "[xwiki:Space.Owner] because the user [xwiki:XWiki.User1] cannot edit that page.",
            this.logCapture.getMessage(0));
    }

    @Test
    void deleteTaskSavesOwnerWhenMacroCallRemoved() throws XWikiException
    {
        when(this.ownerDoc.getContent()).thenReturn(OLD_CONTENT, NEW_CONTENT);

        this.eventListener.onEvent(new DocumentDeletingEvent(), this.taskDoc, this.context);

        verify(this.wiki).saveDocument(this.ownerDoc, "Removed the task with the reference of [xwiki:Space.Task]",
            this.context);
    }

    @Test
    void deleteTaskDoesNotSaveOwnerWhenContentUnchanged() throws XWikiException
    {
        when(this.ownerDoc.getContent()).thenReturn(OLD_CONTENT);

        this.eventListener.onEvent(new DocumentDeletingEvent(), this.taskDoc, this.context);

        verify(this.wiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any(XWikiContext.class));
    }

    @Test
    void deleteTaskDoesNotSaveOwnerWithoutEditRight() throws XWikiException
    {
        when(this.authorizationManager.hasAccess(Right.EDIT, this.ownerRef)).thenReturn(false);
        when(this.ownerDoc.getContent()).thenReturn(OLD_CONTENT, NEW_CONTENT);

        this.eventListener.onEvent(new DocumentDeletingEvent(), this.taskDoc, this.context);

        verify(this.wiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any(XWikiContext.class));
        assertEquals("The task macro of the task [xwiki:Space.Task] was not updated in the page "
            + "[xwiki:Space.Owner] because the user [xwiki:XWiki.User1] cannot edit that page.",
            this.logCapture.getMessage(0));
    }
}
