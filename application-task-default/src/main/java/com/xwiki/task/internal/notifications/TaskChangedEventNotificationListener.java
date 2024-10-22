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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import javax.inject.Inject;

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
// import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
// import org.xwiki.model.reference.DocumentReference;
// import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
// import org.xwiki.user.CurrentUserReference;
// import org.xwiki.user.UserReference;
// import org.xwiki.notifications.NotificationException;
// import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
// import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
// import org.xwiki.notifications.filters.watch.WatchedEntityReference;
// import org.xwiki.user.UserReferenceResolver;
// import org.xwiki.user.internal.document.DocumentUserReference;

import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
// import com.xpn.xwiki.user.api.XWikiUser;
import com.xwiki.task.model.Task;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.DateProperty;

/**
 * Listener which fires when modifying a task in order to notify users of the changes.
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.TaskChangedEventNotificationListener")
public class TaskChangedEventNotificationListener extends AbstractEventListener
{
    private static final String TASK_MANAGER_CLASS_NAME = "TaskManager.TaskManagerClass";

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

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Logger logger;

    private ObservationManager observationManager;

    /**
     * Initialize the listener.
     */
    public TaskChangedEventNotificationListener()
    {
        super(TaskChangedEventNotificationListener.class.getName(), Arrays.asList(new DocumentUpdatedEvent()));
    }

    /**
     * Helper to get the value of the properties of an XObject.
     * @param obj the base object
     * @param propertyName the property to retrieve
     * @return the value of the desired property, or null if the property doesn't exist or has unsupported typ.
     */
    private static Object getPropertyValue(BaseObject obj, String propertyName)
    {
        // Method getValue() is not defined for the Property interface.
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
     * Get a {@link TaskChangedEvent} reflecting the changes made on a certain property, if a change happened.
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
            // TODO: Maybe add localization string for 'field value was just added/removed'?
            // (only possible for assignee and duedate)
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

    /**
     * Register and process the event by saving relevant information in the body field of the event.
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        if (null == document.getXObject(document.resolveClassReference(TASK_MANAGER_CLASS_NAME))) {
            // Stop if the document has no TaskManager tasks attached.
            return;
        }

        XWikiDocumentArchive versionArchive = document.getDocumentArchive();
        Version version = document.getRCSVersion();

        XWikiDocument currentDoc = null;
        XWikiDocument previousDoc = null;

        try {
            currentDoc = documentRevisionProvider
                .getRevision(document, version.toString());
            previousDoc = documentRevisionProvider.getRevision(
                document,
                versionArchive.getNode(
                    versionArchive.getPrevVersion(version)
                ).getVersion().toString()
            );
        } catch (Exception e) {
            logger.warn(
                "Error while getting TaskManager document versions of task [{}]: [{}].",
                document.getDocumentReference(),
                e
            );
            return;
        }

        BaseObject currentObject = currentDoc.getXObject(
            currentDoc.resolveClassReference(TASK_MANAGER_CLASS_NAME)
        );
        BaseObject previousObject = previousDoc.getXObject(
            previousDoc.resolveClassReference(TASK_MANAGER_CLASS_NAME)
        );

        if (null == previousObject || null == currentObject) {
            // Stop if the task was just added or deleted.
            return;
        }

        // The reporter shouldn't be changing between versions.
        Set<String> targetedUsers = new HashSet<String>(Arrays.asList(
            (String) getPropertyValue(currentObject, Task.REPORTER),
            (String) getPropertyValue(currentObject, Task.ASSIGNEE),
            (String) getPropertyValue(previousObject, Task.ASSIGNEE)
        ));

        TaskChangedEvent baseTaskChangedEvent =
            new TaskChangedEvent(document.getDocumentReference(), version.toString(), targetedUsers);

        WATCHED_FIELDS.forEach((String field) -> {
            TaskChangedEvent taskChangedEvent = getFieldChangedEvent(
                currentObject,
                previousObject,
                field,
                baseTaskChangedEvent
            );
            if (null != taskChangedEvent) {
                getObservationManager().notify(taskChangedEvent, document.toString(), data);
            }
        });
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
