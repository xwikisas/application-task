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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.TaskXDOMProcessor;
import com.xwiki.task.internal.TaskMacroUpdateEventListener;
import com.xwiki.task.model.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@RunWith(MockitoJUnitRunner.class)
public class TaskMacroUpdateEventListenerTests
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(TaskMacroUpdateEventListener.class);

    private EventListener listener;

    private DocumentReferenceResolver<String> resolver;

    private TaskXDOMProcessor taskXDOMProcessor;

    private EntityReferenceSerializer<String> serializer;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument document;

    @Mock
    private XDOM docXDOM;

    @Mock
    private MacroBlock macro1;

    @Mock
    private BaseObject obj1;

    private final DocumentReference adminRef = new DocumentReference("xwiki", "XWiki", "Admin");

    private final DocumentReference docRef = new DocumentReference("xwiki", "XWiki", "Task");

    public final DateFormat deadlineDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");

    private Task task1 = new Task();

    private Date date1 = new Date();
    @Before
    public void setup() throws ComponentLookupException
    {
        this.listener = mocker.getComponentUnderTest();
//        this.resolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        this.taskXDOMProcessor = mocker.getInstance(TaskXDOMProcessor.class);
        this.serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);

        when(this.document.getXDOM()).thenReturn(this.docXDOM);
        when(this.document.getDocumentReference()).thenReturn(this.docRef);

        task1.setReference(new DocumentReference("wiki", "Space", "WebHome"));
        task1.setReporter(adminRef);
        task1.setCreateDate(date1);
        task1.setStatus("false");
        task1.setAssignee(adminRef);
        task1.setRender("Some description");
        task1.setDuedate(date1);
        task1.setCompleteDate(date1);

        when(this.taskXDOMProcessor.extract(this.docXDOM, this.docRef)).thenReturn(new ArrayList<>(Collections.singletonList(task1)));
//        when(this.document.getXObjects(TaskMacroUpdateEventListener.TASK_CLASS_REFERENCE)).thenReturn(Collections.singletonList(obj1));
//        when(this.docXDOM.getBlocks(any(), any())).thenReturn(Collections.singletonList(macro1));
//        when(this.serializer.serialize(adminRef)).thenReturn("XWiki.Admin");
//        when(this.obj1.getStringValue("id")).thenReturn("someid1");
//        when(this.resolver.resolve("XWiki.Admin")).thenReturn(adminRef);
//        when(this.macro1.getContent()).thenReturn("Important task! {{mention reference=\"XWiki.Admin\" "
//            + "style=\"FULL_NAME\" anchor=\"XWiki-Admin-zmrgl3\"/}}{{date date=\"2022/10/12 18:16\"/}} lesgooo it works");

    }

    @Test
    public void updateExistingMacroObjectTest() throws ParseException, XWikiException
    {
        this.listener.onEvent(null, document, context);

//        verify(obj1).set(Task.CREATOR, "XWiki.Admin", context);
//        verify(obj1).set(Task.STATUS, task1.isCompleted(), context);
//        verify(obj1).set(Task.CREATE_DATE, task1.getCreateDate(), context);
//        verify(obj1).set(Task.DESCRIPTION, task1.getDescription(), context);
//
//        verify(obj1).set(Task.ASSIGNEES, Collections.singletonList("XWiki.Admin"), context);
//        verify(obj1).set(Task.DEADLINE, task1.getDeadline(), context);
//        verify(obj1).set(Task.DESCRIPTION, task1.getDescription(), context);

        verify(document, never()).removeXObject(obj1);
    }

    @Test
    public void updateExistingMacroObjectWithMultipleAssignees() throws XWikiException, ParseException
    {
        DocumentReference assignee2ref = new DocumentReference("xwiki", "XWiki", "User1");
        task1.setAssignee(adminRef);
//        when(serializer.serialize(assignee2ref)).thenReturn("XWiki.User1");

        this.listener.onEvent(null, document, context);

        verify(document, never()).newXObject(TaskMacroUpdateEventListener.TASK_CLASS_REFERENCE, context);
//        verify(obj1).set(Task.ASSIGNEES, Arrays.asList("XWiki.Admin", "XWiki.User1"), context);
    }

    @Test
    public void createMacroObjectTest() throws XWikiException, ParseException
    {

//        when(this.document.getXObjects(TaskMacroUpdateEventListener.TASK_CLASS_REFERENCE)).thenReturn(Collections.emptyList());
//        when(this.document.newXObject(TaskMacroUpdateEventListener.TASK_CLASS_REFERENCE, context)).thenReturn(obj1);

        this.listener.onEvent(null, document, context);

//        verify(document).newXObject(TaskMacroUpdateEventListener.TASK_CLASS_REFERENCE, context);
//
//        verify(obj1).set(Task.CREATOR, "XWiki.Admin", context);
//        verify(obj1).set(Task.STATUS, task1.isCompleted(), context);
//        verify(obj1).set(Task.CREATE_DATE, task1.getCreateDate(), context);
//        verify(obj1).set(Task.DESCRIPTION, task1.getDescription(), context);
//
//        verify(obj1).set(Task.ASSIGNEES, Collections.singletonList("XWiki.Admin"), context);
//        verify(obj1).set(Task.DEADLINE, task1.getDeadline(), context);
//        verify(obj1).set(Task.DESCRIPTION, task1.getDescription(), context);
    }

    @Test
    public void removeMacroObjectWhenThereIsNoCorrespondingMacroOnPageTest() {
        when(this.taskXDOMProcessor.extract(any(), any())).thenReturn(Collections.emptyList());

        this.listener.onEvent(null, document, context);

//        verify(this.document).removeXObject(obj1);
        verify(this.document, never()).setTitle(any());
    }
}
