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

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
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
    private static final String ASSIGNEE_FIELD_NAME = "assignee";
    private static final String REPORTER_FIELD_NAME = "reporter";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    private ObservationManager observationManager;

    public static final List<String> WATCHED_FIELDS = Arrays.asList(
        "duedate",
        "assignee",
        "project",
        "status",
        "severity"
    );

    /**
     * Initialize the listener.
     */
    public TaskChangedEventNotificationListener()
    {
        super(TaskChangedEventNotificationListener.class.getName(), Arrays.asList(new DocumentUpdatedEvent()));
    }

    private Object getPropertyValue(BaseObject obj, String propertyName)
    {
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

    private TaskChangedEvent getFieldChangedEvent(
        BaseObject currentObject,
        BaseObject previousObject,
        String propertyName,
        TaskChangedEvent event)
    {
        Object currentValue = getPropertyValue(currentObject, propertyName);
        Object previousValue = getPropertyValue(previousObject, propertyName);

        if (currentValue == null || previousValue == null) {
            System.out.println("New/Deleted " + propertyName + ", you get a pass.");
            return null;
        }

        boolean valuesAreEqual = currentValue.equals(previousValue);

        if (currentValue instanceof Date) {
            long currentDate = ((Date) currentValue).getTime();
            long previousDate = ((Date) previousValue).getTime();

            valuesAreEqual = (currentDate == previousDate);
            if (currentDate > previousDate) {
                propertyName += ".later";
            } else {
                propertyName += ".earlier";
            }
        }
        
        
        if (!valuesAreEqual) {
            Map<String, Object> localizationParams = new HashMap<String, Object>();
            localizationParams.put("new", currentValue);
            localizationParams.put("old", previousValue);
            localizationParams.put("type", propertyName);

            event = event.clone();
            event.setLocalizationParams(localizationParams);
            System.out.println(propertyName + ": " + currentValue + " <- " + previousValue);
            return event;
        } else {
            return null;
        }
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
            System.out.println("Keine Task Objekt.");
            return;
        }

        XWikiDocumentArchive versionArchive = document.getDocumentArchive();
        Version version = document.getRCSVersion(); //versionArchive.getLatestNode().getVersion();

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
            e.printStackTrace();
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
            System.out.println("Keine Task, haben Sie jetzt vielleicht einen Task adden oder deleten?");
            return;
        }

        Set<String> targetedUsers = new HashSet<String>(Arrays.asList(
            (String) getPropertyValue(currentObject, REPORTER_FIELD_NAME),
            (String) getPropertyValue(currentObject, ASSIGNEE_FIELD_NAME),
            (String) getPropertyValue(previousObject, REPORTER_FIELD_NAME),
            (String) getPropertyValue(previousObject, ASSIGNEE_FIELD_NAME)
        ));

        System.out.println("Targeted: " + targetedUsers.toString());

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
                System.out.println("Done sending the notification for " + field);
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
