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

package com.xwiki.task.internal.notifications.taskchanged;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xwiki.task.model.Task;

/**
 * Listener which fires when adding/modifying a task in order to notify users of the changes.
 *
 * @version $Id$
 * @since 3.8.0
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventNotificationListener")
public class TaskChangedEventNotificationListener extends AbstractEventListener
{
    /**
     * The fields of the Task class which are watched for changes.
     */
    public static final List<String> WATCHED_FIELDS =
        Arrays.asList(Task.ASSIGNEE, Task.DUE_DATE, Task.PROJECT, Task.STATUS, Task.SEVERITY);

    private static final EntityReference CLASS_MATCHER = BaseObjectReference.any("TaskManager.TaskManagerClass");

    private static final BeginEvent FOLD_EVENT_MATCHER = event -> event instanceof BeginFoldEvent;

    @Inject
    private TaskChangedEventFactory taskChangedEventFactory;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ObservationContext observationContext;

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
        if (observationContext.isIn(FOLD_EVENT_MATCHER)) {
            // If the page is renamed, don't send notifications like it was a newly created task.
            return;
        }

        List<String> events;
        if (event instanceof XObjectUpdatedEvent) {
            events = WATCHED_FIELDS;
        } else if (event instanceof XObjectAddedEvent) {
            // When a task is created, only generate one notification, for the assignee.
            events = List.of(Task.ASSIGNEE);
        } else {
            return;
        }

        XWikiDocument sourceDoc = (XWikiDocument) source;
        taskChangedEventFactory.getEvents(sourceDoc, events).forEach((TaskChangedEvent taskEvent) -> {
            observationManagerProvider.get().notify(taskEvent, sourceDoc.toString(), contextProvider.get());
        });
    }
}
