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

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.AbstractTaskEventListener;
import com.xwiki.task.internal.TaskStatusSyncListener;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskStatusSyncListener}.
 *
 * @version $Id$
 */
@ComponentTest
class TaskStatusSyncListenerTest
{
    private static final String STATUS_TODO = "ToDo";

    private static final Date OLD_DATE = new Date(1000L);

    @InjectMockComponents
    private TaskStatusSyncListener listener;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocument originalDocument;

    private final BaseObject taskObj = new BaseObject();

    private final BaseObject originalTaskObj = new BaseObject();

    @BeforeEach
    void setup()
    {
        when(this.document.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Tasks", "Task"));
        when(this.document.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(this.taskObj);
        when(this.document.getOriginalDocument()).thenReturn(this.originalDocument);
        when(this.originalDocument.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE))
            .thenReturn(this.originalTaskObj);
    }

    @Test
    void movedToDone()
    {
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        setTask(this.taskObj, Task.STATUS_DONE, 40);

        update();

        assertEquals(100, this.taskObj.getIntValue(Task.PROGRESS));
        assertNotNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void movedToDoneKeepsExistingCompleteDate()
    {
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        this.originalTaskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);
        setTask(this.taskObj, Task.STATUS_DONE, 40);
        this.taskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);

        update();

        assertEquals(OLD_DATE, this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void movedOutOfDone()
    {
        setTask(this.originalTaskObj, Task.STATUS_DONE, 100);
        this.originalTaskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);
        setTask(this.taskObj, STATUS_TODO, 100);
        this.taskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);

        update();

        assertEquals(0, this.taskObj.getIntValue(Task.PROGRESS));
        assertNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
        assertNull(this.taskObj.getDateValue(Task.START_DATE));
    }

    @Test
    void movedBackToToDoResetsProgress()
    {
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        this.originalTaskObj.setDateValue(Task.START_DATE, OLD_DATE);
        setTask(this.taskObj, STATUS_TODO, 40);
        this.taskObj.setDateValue(Task.START_DATE, OLD_DATE);

        update();

        assertEquals(0, this.taskObj.getIntValue(Task.PROGRESS));
        assertEquals(OLD_DATE, this.taskObj.getDateValue(Task.START_DATE));
    }

    @Test
    void movedToToDoWithExplicitProgressKeepsProgress()
    {
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        setTask(this.taskObj, STATUS_TODO, 20);

        update();

        assertEquals(20, this.taskObj.getIntValue(Task.PROGRESS));
        assertEquals(STATUS_TODO, this.taskObj.getStringValue(Task.STATUS));
    }

    @Test
    void movedToInProgressSetsMissingStartDate()
    {
        setTask(this.originalTaskObj, STATUS_TODO, 30);
        setTask(this.taskObj, Task.STATUS_IN_PROGRESS, 30);

        update();

        assertEquals(30, this.taskObj.getIntValue(Task.PROGRESS));
        assertNotNull(this.taskObj.getDateValue(Task.START_DATE));
        assertNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void movedToInProgressKeepsExistingStartDate()
    {
        setTask(this.originalTaskObj, STATUS_TODO, 30);
        this.originalTaskObj.setDateValue(Task.START_DATE, new Timestamp(OLD_DATE.getTime()));
        setTask(this.taskObj, Task.STATUS_IN_PROGRESS, 30);
        this.taskObj.setDateValue(Task.START_DATE, OLD_DATE);

        update();

        assertEquals(OLD_DATE, this.taskObj.getDateValue(Task.START_DATE));
    }

    @Test
    void progressSetTo100MovesToDone()
    {
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        setTask(this.taskObj, Task.STATUS_IN_PROGRESS, 100);

        update();

        assertEquals(Task.STATUS_DONE, this.taskObj.getStringValue(Task.STATUS));
        assertEquals(100, this.taskObj.getIntValue(Task.PROGRESS));
        assertNotNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void progressLoweredMovesDoneTaskToInProgress()
    {
        setTask(this.originalTaskObj, Task.STATUS_DONE, 100);
        this.originalTaskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);
        setTask(this.taskObj, Task.STATUS_DONE, 60);
        this.taskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);

        update();

        assertEquals(Task.STATUS_IN_PROGRESS, this.taskObj.getStringValue(Task.STATUS));
        assertEquals(60, this.taskObj.getIntValue(Task.PROGRESS));
        assertNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
        assertNotNull(this.taskObj.getDateValue(Task.START_DATE));
    }

    @Test
    void progressAddedMovesToDoTaskToInProgress()
    {
        setTask(this.originalTaskObj, STATUS_TODO, 0);
        setTask(this.taskObj, STATUS_TODO, 10);

        update();

        assertEquals(Task.STATUS_IN_PROGRESS, this.taskObj.getStringValue(Task.STATUS));
    }

    @Test
    void progressChangedOnCustomStatusKeepsStatus()
    {
        setTask(this.originalTaskObj, "Blocked", 10);
        setTask(this.taskObj, "Blocked", 20);

        update();

        assertEquals("Blocked", this.taskObj.getStringValue(Task.STATUS));
    }

    @Test
    void explicitChangesTakePrecedence()
    {
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        setTask(this.taskObj, Task.STATUS_DONE, 70);
        this.taskObj.setDateValue(Task.COMPLETE_DATE, OLD_DATE);

        update();

        assertEquals(Task.STATUS_DONE, this.taskObj.getStringValue(Task.STATUS));
        assertEquals(70, this.taskObj.getIntValue(Task.PROGRESS));
        assertEquals(OLD_DATE, this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void unchangedTaskIsNotModified()
    {
        setTask(this.originalTaskObj, Task.STATUS_DONE, 50);
        setTask(this.taskObj, Task.STATUS_DONE, 50);

        update();

        assertEquals(50, this.taskObj.getIntValue(Task.PROGRESS));
        assertNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void createdAsDone()
    {
        when(this.originalDocument.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(null);
        this.taskObj.setStringValue(Task.STATUS, Task.STATUS_DONE);

        this.listener.onEvent(new DocumentCreatingEvent(), this.document, this.context);

        assertEquals(100, this.taskObj.getIntValue(Task.PROGRESS));
        assertNotNull(this.taskObj.getDateValue(Task.COMPLETE_DATE));
    }

    @Test
    void createdInProgressHasNoStartDate()
    {
        when(this.originalDocument.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(null);
        this.taskObj.setStringValue(Task.STATUS, Task.STATUS_IN_PROGRESS);

        this.listener.onEvent(new DocumentCreatingEvent(), this.document, this.context);

        assertNull(this.taskObj.getDateValue(Task.START_DATE));
        assertNull(this.taskObj.getField(Task.PROGRESS));
    }

    @Test
    void templatesAreIgnored()
    {
        when(this.document.getDocumentReference())
            .thenReturn(new DocumentReference("xwiki", List.of("TaskManager", "TaskManagerTemplates"), "Template"));
        setTask(this.originalTaskObj, Task.STATUS_IN_PROGRESS, 40);
        setTask(this.taskObj, Task.STATUS_DONE, 40);

        update();

        assertEquals(40, this.taskObj.getIntValue(Task.PROGRESS));
    }

    @Test
    void documentWithoutTaskObjectIsIgnored()
    {
        when(this.document.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(null);

        update();

        assertNull(this.taskObj.getField(Task.PROGRESS));
    }

    private void update()
    {
        this.listener.onEvent(new DocumentUpdatingEvent(), this.document, this.context);
    }

    private void setTask(BaseObject object, String status, int progress)
    {
        object.setStringValue(Task.STATUS, status);
        object.setIntValue(Task.PROGRESS, progress);
    }
}
