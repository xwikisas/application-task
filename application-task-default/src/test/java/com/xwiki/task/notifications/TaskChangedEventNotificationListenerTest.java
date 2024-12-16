
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

package com.xwiki.task.notifications;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xwiki.task.internal.notifications.TaskChangedEvent;
import com.xwiki.task.internal.notifications.TaskChangedEventFactory;
import com.xwiki.task.internal.notifications.TaskChangedEventNotificationListener;
import com.xwiki.task.model.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class TaskChangedEventNotificationListenerTest
{
    private static final List<String> WATCHED_FIELDS = TaskChangedEventNotificationListener.WATCHED_FIELDS;

    private final XWikiDocument taskPage = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "Task"));

    // For cases when a modification is initiated in a script.
    private final XWikiDocument scriptPage = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "Script"));

    @Mock
    private XWikiContext context;

    @InjectMockComponents
    private TaskChangedEventNotificationListener eventListener;

    @MockComponent
    private TaskChangedEventFactory taskChangedEventFactory;

    @MockComponent
    private Provider<ObservationManager> observationManagerProvider;

    @Mock
    private ObservationManager mockObservationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @BeforeEach
    void setup()
    {
        when(taskChangedEventFactory.getEvents(any(), anyList())).thenAnswer((input) -> {
            List<String> fieldList = input.getArgument(1);
            return fieldList.stream().map(fieldName -> {
                TaskChangedEvent ev = new TaskChangedEvent();
                ev.setType(fieldName);
                return ev;
            }).collect(Collectors.toList());
        });
        when(this.context.getDoc()).thenReturn(this.scriptPage);
        when(contextProvider.get()).thenReturn(this.context);
        when(observationManagerProvider.get()).thenReturn(mockObservationManager);
    }

    @Test
    void onXObjectUpdatedEvent()
    {
        this.eventListener.onEvent(new XObjectUpdatedEvent(), this.taskPage, this.context);
        verify(mockObservationManager, times(WATCHED_FIELDS.size())).notify(
            argThat((TaskChangedEvent event) -> WATCHED_FIELDS.contains(event.getType())),
            eq(this.taskPage.toString()), any());
    }

    @Test
    void onXObjectAddedEvent()
    {
        // Only one event for a new task.
        this.eventListener.onEvent(new XObjectAddedEvent(), this.taskPage, this.context);
        verify(mockObservationManager, times(1)).notify(
            argThat((TaskChangedEvent event) -> event.getType().equals(Task.ASSIGNEE)),
            eq(this.taskPage.toString()), any());
    }

    @Test
    void onOtherEvent()
    {
        this.eventListener.onEvent(new DocumentCreatedEvent(), this.taskPage, this.context);
        verify(mockObservationManager, times(0)).notify(any(), any(), any());
    }
}
