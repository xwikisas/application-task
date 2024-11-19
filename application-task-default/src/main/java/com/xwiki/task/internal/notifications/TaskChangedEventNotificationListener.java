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

package com.xwiki.task.internal.notifications;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xwiki.task.model.Task;

/**
 * Listener which fires when adding/modifying a task in order to notify users of the changes.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.TaskChangedEventNotificationListener")
public class TaskChangedEventNotificationListener extends AbstractEventListener
{
    private static final LocalDocumentReference TASK_CLASS =
        new LocalDocumentReference("TaskManager", "TaskManagerClass");

    private static final EntityReference CLASS_MATCHER = BaseObjectReference.any("TaskManager.TaskManagerClass");

    /**
     * The fields of the Task class which are watched for changes.
     */
    private static final List<String> WATCHED_FIELDS =
        Arrays.asList(Task.ASSIGNEE, Task.DUE_DATE, Task.PROJECT, Task.STATUS, Task.SEVERITY);

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    /**
     * Initialize the listener.
     */
    public TaskChangedEventNotificationListener()
    {
        super(TaskChangedEventNotificationListener.class.getName(),
            Arrays.asList(new XObjectUpdatedEvent(CLASS_MATCHER), new XObjectAddedEvent(CLASS_MATCHER)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof XObjectUpdatedEvent) {
            onEvent((XObjectUpdatedEvent) event, (XWikiDocument) source, (XWikiContext) data);
        } else if (event instanceof XObjectAddedEvent) {
            onEvent((XObjectAddedEvent) event, (XWikiDocument) source, (XWikiContext) data);
        }
    }

    /**
     * Helper to get the value of the properties of an XObject.
     *
     * @param obj the base object
     * @param propertyName the property to retrieve
     * @return the value of the desired property, or null if the property doesn't exist or has unsupported type.
     */
    private static Object getPropertyValue(BaseObject obj, String propertyName)
    {
        if (null == obj) {
            return null;
        }
        // Method getValue() always returns null from the Property interface implementation, so this if chain is needed.
        PropertyInterface property = obj.safeget(propertyName);
        if (property instanceof StringProperty) {
            return ((StringProperty) property).getValue();
        } else if (property instanceof LargeStringProperty) {
            return ((LargeStringProperty) property).getValue();
        } else if (property instanceof DateProperty) {
            return ((DateProperty) property).getValue();
        } else {
            return null;
        }
    }

    /**
     * Get a {@link TaskChangedEvent} reflecting the changes made to a certain property, if a change happened.
     *
     * @param currentObject the current version of the object
     * @param previousObject the previous version of the object
     * @param propertyName the property to check for changes
     * @param baseEvent a skeleton event used as a template when calling multiple times for the same object
     * @return the event describing the changes done to the specified field, null when no change occurred
     */
    private TaskChangedEvent getFieldChangedEvent(BaseObject currentObject, BaseObject previousObject,
        String propertyName, TaskChangedEvent baseEvent)
    {
        Object currentValue = getPropertyValue(currentObject, propertyName);
        Object previousValue = getPropertyValue(previousObject, propertyName);

        if (currentValue == null) {
            // Don't send an event when a field is unset.
            return null;
        }

        boolean valuesAreEqual = currentValue.equals(previousValue);

        String translationKeySuffix = propertyName;

        if (valuesAreEqual) {
            return null;
        }

        Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("currentValue", currentValue);
        eventInfo.put("previousValue", previousValue);
        eventInfo.put("type", translationKeySuffix);

        TaskChangedEvent event =
            new TaskChangedEvent(baseEvent.getDocumentReference(), baseEvent.getDocumentVersion(), eventInfo);
        event.setEventInfo(eventInfo);

        return event;
    }

    private void onEvent(XObjectUpdatedEvent event, XWikiDocument sourceDoc, XWikiContext context)
    {
        XWikiDocument previousDoc = sourceDoc.getOriginalDocument();

        BaseObject currentObject = sourceDoc.getXObject(TASK_CLASS);
        BaseObject previousObject = previousDoc.getXObject(TASK_CLASS);

        TaskChangedEvent baseTaskChangedEvent =
            new TaskChangedEvent(sourceDoc.getDocumentReference(), sourceDoc.getVersion());

        WATCHED_FIELDS.forEach((String field) -> {
            TaskChangedEvent taskChangedEvent =
                getFieldChangedEvent(currentObject, previousObject, field, baseTaskChangedEvent);
            if (null != taskChangedEvent) {
                observationManagerProvider.get().notify(taskChangedEvent, sourceDoc.toString(), context);
            }
        });
    }

    private void onEvent(XObjectAddedEvent event, XWikiDocument sourceDoc, XWikiContext context)
    {
        BaseObject currentObject = sourceDoc.getXObject(TASK_CLASS);

        TaskChangedEvent baseTaskChangedEvent =
            new TaskChangedEvent(sourceDoc.getDocumentReference(), sourceDoc.getVersion());

        // Only send notification for assignee if the task was just created.
        TaskChangedEvent taskChangedEvent =
            getFieldChangedEvent(currentObject, null, Task.ASSIGNEE, baseTaskChangedEvent);

        observationManagerProvider.get().notify(taskChangedEvent, sourceDoc.toString(), context);
    }
}
