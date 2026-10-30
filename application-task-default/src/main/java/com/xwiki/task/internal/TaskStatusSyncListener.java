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
package com.xwiki.task.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.model.Task;

/**
 * Keeps the properties of a task that depend on its status consistent when only one side is changed, whatever made
 * the change (Kanban board, Live Data, REST, scripts):
 * <ul>
 * <li>a status change updates the progress: 100 when moved to {@code Done}, 0 when moved to {@code ToDo} or out of
 * {@code Done};</li>
 * <li>a progress change updates the status: 100 means {@code Done}, less than 100 moves a {@code Done} task back to
 * {@code InProgress} and more than 0 moves a {@code ToDo} task to {@code InProgress};</li>
 * <li>the completion date is set when the task becomes {@code Done} and cleared when it stops being {@code Done};</li>
 * <li>the start date is set, if missing, when the task is moved to {@code InProgress}.</li>
 * </ul>
 * A property explicitly changed in the same save is never overwritten. The task document is modified before being
 * saved, so no additional save is needed.
 *
 * @version $Id$
 * @since 3.11.5
 */
@Component
@Singleton
@Named(TaskStatusSyncListener.NAME)
public class TaskStatusSyncListener extends AbstractTaskEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "com.xwiki.task.internal.TaskStatusSyncListener";

    private static final String STATUS_TODO = "ToDo";

    private static final int DONE_PROGRESS = 100;

    /**
     * Constructor.
     */
    public TaskStatusSyncListener()
    {
        super(NAME, Arrays.asList(new DocumentUpdatingEvent(), new DocumentCreatingEvent()));
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context, Event event, boolean inFoldEvent)
    {
        BaseObject taskObj = document.getXObject(TASK_CLASS_REFERENCE);
        if (taskObj == null || isTemplate(document)) {
            return;
        }

        XWikiDocument originalDocument = document.getOriginalDocument();
        BaseObject originalTaskObj =
            originalDocument != null ? originalDocument.getXObject(TASK_CLASS_REFERENCE) : null;
        String originalStatus = originalTaskObj != null ? originalTaskObj.getStringValue(Task.STATUS) : null;

        boolean statusChanged = !Objects.equals(taskObj.getStringValue(Task.STATUS), originalStatus);
        boolean progressChanged = !Objects.equals(getProgress(taskObj), getProgress(originalTaskObj));
        if (statusChanged && !progressChanged) {
            syncProgressWithStatus(taskObj, originalStatus);
        } else if (!statusChanged && progressChanged) {
            syncStatusWithProgress(taskObj);
        }

        syncDates(taskObj, originalTaskObj, originalStatus);
    }

    private void syncDates(BaseObject taskObj, BaseObject originalTaskObj, String originalStatus)
    {
        String status = taskObj.getStringValue(Task.STATUS);
        if (!Objects.equals(status, originalStatus)) {
            syncCompleteDate(taskObj, originalTaskObj, status);
            // Only on an actual transition, so that tasks created as in progress don't get a start date.
            if (originalTaskObj != null) {
                syncStartDate(taskObj, originalTaskObj, status);
            }
        }
    }

    private void syncProgressWithStatus(BaseObject taskObj, String originalStatus)
    {
        String status = taskObj.getStringValue(Task.STATUS);
        if (Task.STATUS_DONE.equals(status)) {
            taskObj.setIntValue(Task.PROGRESS, DONE_PROGRESS);
        } else if (Task.STATUS_DONE.equals(originalStatus) || STATUS_TODO.equals(status)) {
            taskObj.setIntValue(Task.PROGRESS, 0);
        }
    }

    private void syncStatusWithProgress(BaseObject taskObj)
    {
        Integer progress = getProgress(taskObj);
        if (progress == null) {
            return;
        }
        String status = taskObj.getStringValue(Task.STATUS);
        if (progress >= DONE_PROGRESS && !Task.STATUS_DONE.equals(status)) {
            taskObj.setStringValue(Task.STATUS, Task.STATUS_DONE);
        } else if (progress < DONE_PROGRESS && Task.STATUS_DONE.equals(status)
            || progress > 0 && STATUS_TODO.equals(status))
        {
            taskObj.setStringValue(Task.STATUS, Task.STATUS_IN_PROGRESS);
        }
    }

    private void syncCompleteDate(BaseObject taskObj, BaseObject originalTaskObj, String status)
    {
        if (isDateChanged(taskObj, originalTaskObj, Task.COMPLETE_DATE)) {
            return;
        }
        if (Task.STATUS_DONE.equals(status)) {
            if (taskObj.getDateValue(Task.COMPLETE_DATE) == null) {
                taskObj.setDateValue(Task.COMPLETE_DATE, new Date());
            }
        } else if (taskObj.getDateValue(Task.COMPLETE_DATE) != null) {
            taskObj.setDateValue(Task.COMPLETE_DATE, null);
        }
    }

    private void syncStartDate(BaseObject taskObj, BaseObject originalTaskObj, String status)
    {
        if (Task.STATUS_IN_PROGRESS.equals(status) && taskObj.getDateValue(Task.START_DATE) == null
            && !isDateChanged(taskObj, originalTaskObj, Task.START_DATE))
        {
            taskObj.setDateValue(Task.START_DATE, new Date());
        }
    }

    private Integer getProgress(BaseObject taskObj)
    {
        if (taskObj == null || taskObj.getField(Task.PROGRESS) == null) {
            return null;
        }
        return taskObj.getIntValue(Task.PROGRESS);
    }

    private boolean isDateChanged(BaseObject taskObj, BaseObject originalTaskObj, String property)
    {
        Date originalDate = originalTaskObj != null ? originalTaskObj.getDateValue(property) : null;
        // Compare the time values since the stored dates can be Timestamp instances, which don't equal plain Dates.
        return !Objects.equals(getTime(taskObj.getDateValue(property)), getTime(originalDate));
    }

    private Long getTime(Date date)
    {
        return date != null ? date.getTime() : null;
    }
}
