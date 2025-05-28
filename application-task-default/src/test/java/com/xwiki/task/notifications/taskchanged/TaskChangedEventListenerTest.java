
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

package com.xwiki.task.notifications.taskchanged;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.internal.DefaultTargetableNotificationPreferenceBuilder;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.notifications.taskchanged.TaskChangedEvent;
import com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventListener;
import com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventNotificationListener;
import com.xwiki.task.model.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class TaskChangedEventListenerTest
{
    private final DocumentReference adminRef = new DocumentReference("xwiki", "XWiki", "Admin");

    private final DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "User");

    private final XWikiDocument taskPage = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "Task"));

    private final WatchedLocationReference taskWatchedLocationReference =
        new WatchedLocationReference(this.taskPage.getDocumentReference(), null, null, null, null);

    // Component under test and component mocks.
    @InjectMockComponents
    private TaskChangedEventListener eventListener;

    @MockComponent
    private WatchedEntitiesManager watchedEntitiesManager;

    @MockComponent
    private XWikiContext context;

    @MockComponent
    private XWiki xwiki;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private WatchedEntityFactory watchedEntityFactory;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private XWikiDocument userDocument;

    private TaskChangedEvent event;

    @BeforeEach
    void setup() throws Exception
    {
        this.event = new TaskChangedEvent(this.taskPage);
        this.event.setPreviousValue(adminRef.toString());
        this.event.setCurrentValue(userRef.toString());
        when(documentReferenceResolver.resolve(anyString())).thenAnswer(i -> {
            if (i.getArgument(0).equals(this.adminRef.toString())) {
                return this.adminRef;
            } else if (i.getArgument(0).equals(this.userRef.toString())) {
                return this.userRef;
            } else {
                return null;
            }
        });
        when(watchedEntityFactory.createWatchedLocationReference(any())).thenAnswer(i -> {
            if (i.getArgument(0).equals(this.taskPage.getDocumentReference())) {
                return this.taskWatchedLocationReference;
            } else {
                return new WatchedLocationReference(i.getArgument(0), null, null, null, null);
            }
        });

        // Notification Preferences
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument((DocumentReference) any(), any())).thenReturn(this.userDocument);
        when(
            this.userDocument.resolveClassReference("XWiki.Notifications.Code.NotificationPreferenceClass")).thenReturn(
            new DocumentReference("XWiki", "Notifications.Code", "NotificationPreferenceClass"));

        BaseObject taskChangedEventNotificationPreference = new BaseObject();
        taskChangedEventNotificationPreference.setStringValue("eventType",
            "com.xwiki.task.internal.notifications.taskchanged.TaskChangedEvent");
        taskChangedEventNotificationPreference.setIntValue("notificationEnabled", 1);
        // User is watching for task changed notifications.
        when(this.userDocument.getXObjects(any())).thenReturn(List.of(taskChangedEventNotificationPreference));
    }

    @ParameterizedTest
    @MethodSource("getOtherFields")
    void noActionOnOtherFields(String changedField) throws NotificationException
    {
        this.event.setType(changedField);

        this.eventListener.onEvent(event, this.taskPage, this.context);

        verify(this.watchedEntitiesManager, never()).unwatchEntity(any(), any());
        verify(this.watchedEntitiesManager, never()).watchEntity(any(), any());
    }

    @Test
    void noActionOnOtherEvent() throws NotificationException
    {
        this.eventListener.onEvent(new XObjectAddedEvent(), this.taskPage, this.context);
        this.eventListener.onEvent(new XObjectUpdatedEvent(), this.taskPage, this.context);
        this.eventListener.onEvent(new DocumentRenamedEvent(), this.taskPage, this.context);

        verify(this.watchedEntitiesManager, never()).unwatchEntity(any(), any());
        verify(this.watchedEntitiesManager, never()).watchEntity(any(), any());
    }

    @Test
    void watchUnwatchOnAssigneeChanged() throws NotificationException
    {
        this.event.setType(Task.ASSIGNEE);
        List<NotificationPreference> userPreferences = List.of(
            new DefaultTargetableNotificationPreferenceBuilder().prepare().setEnabled(true)
                .setProperties(Map.of(NotificationPreferenceProperty.EVENT_TYPE, TaskChangedEvent.class.getName()))
                .build());
        when(this.notificationPreferenceManager.getAllPreferences(any(DocumentReference.class))).thenReturn(
            userPreferences);

        this.eventListener.onEvent(event, this.taskPage, this.context);

        verify(this.watchedEntitiesManager).unwatchEntity(this.taskWatchedLocationReference, adminRef);
        verify(this.watchedEntitiesManager).watchEntity(this.taskWatchedLocationReference, userRef);
    }

    @Test
    void watchUnwatchOnAssigneeChangedNoPreference() throws NotificationException
    {
        // The user is not subscribed to receive task notifications.
        when(this.userDocument.getXObjects(any())).thenReturn(List.of());
        this.event.setType(Task.ASSIGNEE);
        when(this.notificationPreferenceManager.getAllPreferences(any(DocumentReference.class))).thenReturn(List.of());

        this.eventListener.onEvent(event, this.taskPage, this.context);

        verify(this.watchedEntitiesManager, never()).unwatchEntity(this.taskWatchedLocationReference, adminRef);
        verify(this.watchedEntitiesManager, never()).watchEntity(this.taskWatchedLocationReference, userRef);
    }

    private static Stream<Arguments> getOtherFields()
    {
        return TaskChangedEventNotificationListener.WATCHED_FIELDS.stream()
            .filter(f -> !(f.equals(Task.ASSIGNEE) || f.equals(Task.REPORTER))).map(Arguments::of);
    }
}
