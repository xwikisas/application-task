package com.xwiki.task;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.internal.document.DocumentUserReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xwiki.task.internal.AbstractTaskEventListener;
import com.xwiki.task.internal.TaskMacroUpdateEventListener;
import com.xwiki.task.internal.TaskXDOMProcessor;
import com.xwiki.task.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class TaskMacroUpdateEventListenerTest
{
    private static final String DOC_PREV_VERSION = "1";

    private static final String TASK_NAME = "Hello there";

    @InjectMockComponents
    private TaskMacroUpdateEventListener eventListener;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private TaskXDOMProcessor taskXDOMProcessor;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private EntityReferenceProvider referenceProvider;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument docWithTasks;

    @Mock
    private XWikiDocument taskDoc;

    @Mock
    private XWikiDocument task_1Doc;

    @Mock
    private XWikiDocument prevVersionDoc;

    @Mock
    private XDOM docXDOM;

    @Mock
    private XDOM prevVersionDocXDOM;

    @Mock
    private MacroBlock macro1;

    @Mock
    private BaseObject taskObj;

    @Mock
    private BaseObject task_1Obj;

    @Mock
    private DocumentAuthors documentAuthors;

    private final DocumentReference adminRef = new DocumentReference("xwiki", "XWiki", "Admin");

    private final DocumentReference pageWithMacro = new DocumentReference("xwiki", "XWiki", "Home");

    private final DocumentReference taskPage = new DocumentReference("xwiki", "XWiki", "Task");

    private final DocumentReference taskPage_1 = new DocumentReference("xwiki", "XWiki", "Task_1");

    private final DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User1");

    private final UserReference userRef = new DocumentUserReference(userDocRef, true);

    private Task task = new Task();

    private Task task_1 = new Task();

    private final Date date1 = new Date(1000);

    @BeforeEach
    void setup() throws XWikiException
    {
        when(this.referenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(new DocumentReference(
            "xwiki", "XWiki", "WebHome"));
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.docWithTasks.getDocumentReference()).thenReturn(this.pageWithMacro);
        when(this.docWithTasks.getXDOM()).thenReturn(this.docXDOM);
        when(this.docWithTasks.getOriginalDocument()).thenReturn(this.prevVersionDoc);
        when(this.prevVersionDoc.getXDOM()).thenReturn(this.prevVersionDocXDOM);
        when(this.docWithTasks.clone()).thenReturn(this.docWithTasks);
        when(this.prevVersionDoc.clone()).thenReturn(this.prevVersionDoc);
        when(this.taskDoc.clone()).thenReturn(this.taskDoc);
        when(this.task_1Doc.clone()).thenReturn(this.task_1Doc);
        when(this.wiki.getDocument(this.pageWithMacro, this.context)).thenReturn(this.docWithTasks);
        when(this.wiki.getDocument(this.taskPage, this.context)).thenReturn(this.taskDoc);
        when(this.wiki.getDocument(this.taskPage_1, this.context)).thenReturn(this.task_1Doc);
        when(this.taskDoc.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE, true, this.context)).thenReturn(
            this.taskObj);
        when(this.taskDoc.getDocumentReference()).thenReturn(this.taskPage);
        when(this.task_1Doc.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(this.task_1Obj);
        when(this.task_1Doc.getDocumentReference()).thenReturn(this.taskPage_1);
        when(this.taskObj.getLargeStringValue(Task.OWNER)).thenReturn(this.pageWithMacro.toString());
        when(this.taskObj.getStringValue(Task.NAME)).thenReturn("");
        when(this.task_1Obj.getLargeStringValue(Task.OWNER)).thenReturn(this.pageWithMacro.toString());
        when(this.resolver.resolve(this.pageWithMacro.toString(), this.taskPage)).thenReturn(this.pageWithMacro);
        when(this.resolver.resolve(this.pageWithMacro.toString(), this.taskPage_1)).thenReturn(this.pageWithMacro);
        when(this.serializer.serialize(this.pageWithMacro, this.taskPage)).thenReturn(this.pageWithMacro.toString());
        when(this.serializer.serialize(this.pageWithMacro, this.taskPage_1)).thenReturn(this.pageWithMacro.toString());
        when(this.taskDoc.getAuthors()).thenReturn(this.documentAuthors);
        when(this.taskDoc.getContent()).thenReturn("");
        when(this.context.getUserReference()).thenReturn(this.userDocRef);
        when(this.userReferenceResolver.resolve(this.userDocRef)).thenReturn(this.userRef);
        when(this.taskObj.clone()).thenReturn(this.taskObj);
        when(this.taskObj.getDiff(this.taskObj, this.context)).thenReturn(
            Collections.singletonList(mock(ObjectDiff.class)));

        task_1.setReference(taskPage_1);
        task_1.setReporter(adminRef);
        task_1.setDuedate(date1);
        task_1.setAssignee(adminRef);
        task_1.setName(TASK_NAME);
        task_1.setCompleteDate(date1);
        task_1.setStatus(Task.STATUS_DONE);
        task_1.setNumber(1);
        task_1.setOwner(pageWithMacro);
        task_1.setCreateDate(date1);
        task_1.setDescription(TASK_NAME);

        task.setReference(taskPage);
        task.setReporter(adminRef);
        task.setDuedate(date1);
        task.setAssignee(adminRef);
        task.setName(TASK_NAME);
        task.setCompleteDate(date1);
        task.setStatus(Task.STATUS_DONE);
        task.setNumber(2);
        task.setOwner(pageWithMacro);
        task.setCreateDate(date1);
        task.setDescription(TASK_NAME);
    }

    @Test
    void onDeletingEvent() throws TaskException
    {
        this.eventListener.onEvent(new DocumentDeletingEvent(), this.docWithTasks, this.context);

        verify(this.taskManager).deleteTasksByOwner(this.pageWithMacro);
    }

    @Test
    void onUpdatingWithRemovedTaskEvent() throws XWikiException
    {
        when(this.taskXDOMProcessor.extract(this.docXDOM, this.pageWithMacro)).thenReturn(
            new ArrayList<>(Collections.singletonList(task)));
        when(this.taskXDOMProcessor.extract(this.prevVersionDocXDOM, this.pageWithMacro, true)).thenReturn(
            new ArrayList<>(Collections.singletonList(task_1)));
        when(this.taskDoc.isNew()).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.EDIT, taskPage)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.DELETE, taskPage_1)).thenReturn(true);

        this.eventListener.onEvent(new DocumentUpdatingEvent(), this.docWithTasks, this.context);

        verify(this.taskObj).set(Task.OWNER, this.pageWithMacro.toString(), this.context);
        verify(this.documentAuthors).setEffectiveMetadataAuthor(this.userRef);
        verify(this.taskObj).set(Task.NAME, "Hello there", this.context);
        verify(this.wiki).saveDocument(this.taskDoc, "Task updated!", this.context);
        verify(this.wiki).deleteDocument(this.task_1Doc, this.context);
    }
}
