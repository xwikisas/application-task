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
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.model.reference.EntityReference;

import com.xwiki.task.model.Task;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.DateProperty;

/**
 * Listener which fires when adding/modifying a task in order to notify users of the changes.
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.TaskChangedEventNotificationListener")
public class TaskChangedEventNotificationListener extends AbstractEventListener
{
    private static final String TASK_MANAGER_CLASS_NAME = "TaskManager.TaskManagerClass";
    private static final EntityReference CLASS_MATCHER = BaseObjectReference.any(TASK_MANAGER_CLASS_NAME);

    /**
     * The fields of the Task class which are watched for changes.
     */
    private static final List<String> WATCHED_FIELDS = Arrays.asList(
        Task.ASSIGNEE,
        Task.DUE_DATE,
        Task.PROJECT,
        Task.STATUS,
        Task.SEVERITY
    );

    @Inject
    private ComponentManager componentManager;

    private ObservationManager observationManager;

    /**
     * Initialize the listener.
     */
    public TaskChangedEventNotificationListener()
    {
        super(TaskChangedEventNotificationListener.class.getName(), Arrays.asList(
            new XObjectUpdatedEvent(CLASS_MATCHER),
            new XObjectAddedEvent(CLASS_MATCHER)
        ));
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
            return (Date) ((DateProperty) property).getValue();
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
     * @return the event describing the changes done to the specified field, null when no change occured
     */
    private TaskChangedEvent getFieldChangedEvent(
        BaseObject currentObject,
        BaseObject previousObject,
        String propertyName,
        TaskChangedEvent baseEvent)
    {
        Object currentValue = getPropertyValue(currentObject, propertyName);
        Object previousValue = getPropertyValue(previousObject, propertyName);

        if (currentValue == null) {
            return null;
        }

        boolean valuesAreEqual = currentValue.equals(previousValue);

        String localizationSuffix = propertyName;

        if (currentValue instanceof Date) {
            long currentDate = ((Date) currentValue).getTime();
            long previousDate = ((Date) previousValue).getTime();

            valuesAreEqual = (currentDate == previousDate);
            if (currentDate > previousDate) {
                localizationSuffix += ".later";
            } else {
                localizationSuffix += ".earlier";
            }
        }

        if (valuesAreEqual) {
            return null;
        }

        Map<String, Object> localizationParams = new HashMap<String, Object>();
        localizationParams.put("new", currentValue);
        localizationParams.put("old", previousValue);
        localizationParams.put("type", localizationSuffix);

        TaskChangedEvent event = baseEvent.clone();
        event.setLocalizationParams(localizationParams);

        return event;
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

    private void onEvent(XObjectUpdatedEvent event, XWikiDocument source, XWikiContext data)
    {
        XWikiContext context = (XWikiContext) data;

        BaseObject currentObject =
            context.getDoc().getXObject(source.resolveClassReference(TASK_MANAGER_CLASS_NAME));
        BaseObject previousObject =
            source.getXObject(source.resolveClassReference(TASK_MANAGER_CLASS_NAME));

        TaskChangedEvent baseTaskChangedEvent =
            new TaskChangedEvent(source.getDocumentReference(), source.getVersion());

        WATCHED_FIELDS.forEach((String field) -> {
            TaskChangedEvent taskChangedEvent = getFieldChangedEvent(
                currentObject,
                previousObject,
                field,
                baseTaskChangedEvent
            );
            if (null != taskChangedEvent) {
                getObservationManager().notify(taskChangedEvent, source.toString(), data);
            }
        });
    }

    private void onEvent(XObjectAddedEvent event, XWikiDocument source, XWikiContext data)
    {
        BaseObject currentObject =
            source.getXObject(source.resolveClassReference(TASK_MANAGER_CLASS_NAME));

        TaskChangedEvent baseTaskChangedEvent = 
            new TaskChangedEvent(source.getDocumentReference(), source.getVersion());

        // Only send notification for assignee if the task was just created.
        TaskChangedEvent taskChangedEvent = getFieldChangedEvent(
            currentObject,
            null,
            Task.ASSIGNEE,
            baseTaskChangedEvent
        );

        getObservationManager().notify(taskChangedEvent, source.toString(), data);
    }

    private ObservationManager getObservationManager()
    {
        if (this.observationManager == null) {
            try {
                this.observationManager = componentManager.getInstance(ObservationManager.class);
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Cound not retrieve an Observation Manager against the component manager");
            }
        }
        return this.observationManager;
    }
}
