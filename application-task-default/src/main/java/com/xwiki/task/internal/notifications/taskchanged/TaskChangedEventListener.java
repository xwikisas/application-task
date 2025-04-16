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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.task.model.Task;

/**
 * Class which handles auto-watching tasks for users who are assigned to it.
 *
 * @version $Id$
 * @since 3.8.0
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventListener")
public class TaskChangedEventListener extends AbstractEventListener
{
    private static final String SEPARATOR = ",";

    @Inject
    private WatchedEntityFactory watchedEntityFactory;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private Logger logger;

    /**
     * Initialize the listener.
     */
    public TaskChangedEventListener()
    {
        super(TaskChangedEventListener.class.getName(), List.of(new TaskChangedEvent()));
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
        String oldValue = (String) taskChangedEvent.getPreviousValue();
        String currentValue = (String) taskChangedEvent.getCurrentValue();

        Set<String> previousAssignees = new HashSet<>(List.of(oldValue.split(SEPARATOR)));
        Set<String> currentAssignees = new HashSet<>(List.of(currentValue.split(SEPARATOR)));

        List<String> unassignees = identifyUnassignedUsers(currentAssignees, previousAssignees);
        List<String> assignees = identifyNewAssigneesUsers(currentAssignees, previousAssignees);

        // In order to receive notifications, watch the task page for the newly assigned user.
        watchTask(docRef, assignees);
        // In order to stop receiving notifications, unwatch the task page for the unassigned user.
        unwatchTask(docRef, (unassignees));
    }

    private List<String> identifyNewAssigneesUsers(Set<String> currentAssignees, Set<String> previousAssignees)
    {
        List<String> newAssignees = new ArrayList<>();
        for (String currentAssignee : currentAssignees) {
            if (!previousAssignees.contains(currentAssignee)) {
                newAssignees.add(currentAssignee);
            }
        }
        return newAssignees;
    }

    private List<String> identifyUnassignedUsers(Set<String> currentAssignees, Set<String> previousAssignees)
    {
        List<String> unassignedUsers = new ArrayList<>();
        for (String oldAssignee : previousAssignees) {
            if (!currentAssignees.contains(oldAssignee)) {
                unassignedUsers.add(oldAssignee);
            }
        }
        return unassignedUsers;
    }

    private void watchTask(WatchedLocationReference docRef, List<String> users)
    {
        if (users != null && !users.isEmpty()) {
            try {
                for (String userName : users) {
                    DocumentReference user = documentReferenceResolver.resolve(userName);
                    bindUserToTask(docRef, user);
                }
            } catch (NotificationException e) {
                logger.error("Failed to watch task page [{}] for user [{}] after assignee changes. Root cause: [{}]",
                    docRef, users, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private void bindUserToTask(WatchedLocationReference docRef, DocumentReference user) throws NotificationException
    {
        List<NotificationPreference> preferences = notificationPreferenceManager.getAllPreferences(user);
        watchedEntitiesManager.watchEntity(docRef, user);
        // Workaround for watchEntity unintentionally altering preferences.
        // TODO: Remove after upgrading the XWiki parent to a version >= 15.5, in order to include a fix for
        // XWIKI-19070: Change the notifications locations inclusion default: don't consider that the whole wiki
        // is watched when nothing is watched
        notificationPreferenceManager.savePreferences(preferences);
    }

    private void unbindUserToTask(WatchedLocationReference docRef, DocumentReference user) throws NotificationException
    {
        List<NotificationPreference> preferences = notificationPreferenceManager.getAllPreferences(user);
        watchedEntitiesManager.unwatchEntity(docRef, user);
        // Workaround for watchEntity unintentionally altering preferences.
        // TODO: Remove after upgrading the XWiki parent to a version >= 15.5, in order to include a fix for
        // XWIKI-19070: Change the notifications locations inclusion default: don't consider that the whole
        // wiki is watched when nothing is watched
        notificationPreferenceManager.savePreferences(preferences);
    }

    private void unwatchTask(WatchedLocationReference docRef, List<String> users)
    {
        if (users != null && !users.isEmpty()) {
            for (String userName : users) {
                DocumentReference user = documentReferenceResolver.resolve(userName);
                try {
                    unbindUserToTask(docRef, user);
                } catch (NotificationException e) {
                    logger.error(
                        "Failed to unwatch task page [{}] for user [{}] after assignee changes. Root cause: [{}]",
                        docRef, user, ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
    }
}
