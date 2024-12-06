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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.task.model.Task;

/**
 * Class which handles auto-watching tasks for users who are assigned to it.
 *
 * @version $Id$
 * @since 3.7
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.TaskChangedEventPageWatcher")
public class TaskChangedEventPageWatcher extends AbstractEventListener
{

    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private WatchedEntityFactory watchedEntityFactory;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Initialize the listener.
     */
    public TaskChangedEventPageWatcher()
    {
        super(TaskChangedEventPageWatcher.class.getName(), Arrays.asList(new TaskChangedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!(event instanceof TaskChangedEvent)) {
            return;
        }
        TaskChangedEvent taskChangedEvent = (TaskChangedEvent) event;
        if (!taskChangedEvent.getType().equals(Task.ASSIGNEE)) {
            return;
        }
        WatchedLocationReference docRef =
            watchedEntityFactory.createWatchedLocationReference(taskChangedEvent.getDocument().getDocumentReference());
        watchTask(docRef, (String) taskChangedEvent.getCurrentValue());
        unwatchTask(docRef, (String) taskChangedEvent.getPreviousValue());
    }

    private void watchTask(WatchedLocationReference docRef, String userFullName)
    {
        if (!userFullName.isEmpty()) {
            try {
                watchedEntitiesManager.watchEntity(docRef, documentReferenceResolver.resolve(userFullName));
            } catch (NotificationException e) {
                logger.error("Failed to watch task page [{}] for user [{}]. Cause:", docRef, userFullName, e);
            }
        }
    }

    private void unwatchTask(WatchedLocationReference docRef, String userFullName)
    {
        if (!userFullName.isEmpty()) {
            try {
                watchedEntitiesManager.unwatchEntity(docRef, documentReferenceResolver.resolve(userFullName));
            } catch (NotificationException e) {
                logger.error("Failed to unwatch task page [{}] for user [{}]. Cause:", docRef, userFullName, e);
            }
        }
    }
}
