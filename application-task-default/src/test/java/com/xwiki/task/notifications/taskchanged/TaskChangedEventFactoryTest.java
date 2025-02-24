
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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xwiki.task.internal.notifications.taskchanged.TaskChangedEvent;
import com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventFactory;
import com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventNotificationListener;
import com.xwiki.task.model.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ComponentTest
class TaskChangedEventFactoryTest
{
    private static final List<String> WATCHED_FIELDS = TaskChangedEventNotificationListener.WATCHED_FIELDS;

    private final Map<String, List<Object>> testValueMap = Map.of(
            Task.ASSIGNEE, List.of("XWiki.Assignee1", "XWiki.Assignee2"),
            Task.REPORTER, List.of("XWiki.Reporter1", "XWiki.Reporter2"),
            Task.DUE_DATE, List.of(new Date(1000), new Date(1200)),
            Task.PROJECT, List.of("Other", "XWiki"),
            Task.SEVERITY, List.of("Low", "High"),
            Task.STATUS, List.of(Task.STATUS_IN_PROGRESS, Task.STATUS_DONE)
    );

    @InjectMockComponents
    private TaskChangedEventFactory taskChangedEventFactory;

    @Mock
    private XWikiDocument currTaskPage;

    @Mock
    private XWikiDocument prevTaskPage;

    @Mock
    private BaseObject currTaskObject;

    @Mock
    private BaseObject prevTaskObject;

    @BeforeEach
    void setup()
    {
        when(this.currTaskPage.getXObject(any(EntityReference.class))).thenReturn(this.currTaskObject);
        when(this.prevTaskPage.getXObject(any(EntityReference.class))).thenReturn(this.prevTaskObject);
        when(this.currTaskPage.getOriginalDocument()).thenReturn(this.prevTaskPage);

        changeFields(currTaskObject, List.of());
        changeFields(prevTaskObject, List.of());
    }

    @Test
    void getAllFieldsChangedEvents()
    {
        List<String> changedFields = WATCHED_FIELDS;
        changeFields(currTaskObject, changedFields);

        List<TaskChangedEvent> events = taskChangedEventFactory.getEvents(currTaskPage, WATCHED_FIELDS);
        Assertions.assertEquals(changedFields.size(), events.size());

        for (TaskChangedEvent event : events) {
            String type = event.getType();
            Assertions.assertTrue(changedFields.contains(type));
            Assertions.assertEquals(testValueMap.get(type).get(0), event.getPreviousValue());
            Assertions.assertEquals(testValueMap.get(type).get(1), event.getCurrentValue());
        }
    }

    @Test
    void getSomeFieldsChangedEvents()
    {
        List<String> changedFields = List.of(Task.ASSIGNEE, Task.PROJECT, Task.DUE_DATE);
        changeFields(currTaskObject, changedFields);

        List<TaskChangedEvent> events = taskChangedEventFactory.getEvents(currTaskPage, WATCHED_FIELDS);
        Assertions.assertEquals(changedFields.size(), events.size());

        for (TaskChangedEvent event : events) {
            String type = event.getType();
            Assertions.assertTrue(changedFields.contains(type));
            Assertions.assertEquals(testValueMap.get(type).get(0), event.getPreviousValue());
            Assertions.assertEquals(testValueMap.get(type).get(1), event.getCurrentValue());
        }
    }

    @Test
    void getNoFieldsChanged()
    {
        List<TaskChangedEvent> events = taskChangedEventFactory.getEvents(currTaskPage, WATCHED_FIELDS);
        Assertions.assertTrue(events.isEmpty());
    }

    @Test
    void nullCurrentValue()
    {
        DateProperty dateProperty = new DateProperty();
        dateProperty.setName(Task.DUE_DATE);
        dateProperty.setValue(null);
        when(this.currTaskObject.safeget(Task.DUE_DATE)).thenReturn(dateProperty);

        List<TaskChangedEvent> events = taskChangedEventFactory.getEvents(currTaskPage, WATCHED_FIELDS);
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(Task.DUE_DATE, events.get(0).getType());
        Assertions.assertNull(events.get(0).getCurrentValue());
    }

    @Test
    void nullPreviousValue()
    {
        DateProperty dateProperty = new DateProperty();
        dateProperty.setName(Task.DUE_DATE);
        dateProperty.setValue(null);
        when(this.prevTaskObject.safeget(Task.DUE_DATE)).thenReturn(dateProperty);

        List<TaskChangedEvent> events = taskChangedEventFactory.getEvents(currTaskPage, WATCHED_FIELDS);
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(Task.DUE_DATE, events.get(0).getType());
        Assertions.assertNull(events.get(0).getPreviousValue());
    }

    private void changeFields(BaseObject taskObject, List<String> changedFields)
    {
        testValueMap.forEach((field, testValues) -> {
            Object value;
            if (changedFields.contains(field)) {
                value = testValues.get(1);
            } else {
                value = testValues.get(0);
            }
            PropertyInterface valueProperty;
            if (field.equals(Task.DUE_DATE)) {
                DateProperty dateProperty = new DateProperty();
                dateProperty.setName(field);
                dateProperty.setValue(value);
                valueProperty = dateProperty;
            } else if (field.equals(Task.ASSIGNEE)) {
                LargeStringProperty stringProperty = new LargeStringProperty();
                stringProperty.setName(field);
                stringProperty.setValue(value);
                valueProperty = stringProperty;
            } else {
                StringProperty stringProperty = new StringProperty();
                stringProperty.setName(field);
                stringProperty.setValue(value);
                valueProperty = stringProperty;
            }
            when(taskObject.safeget(field)).thenReturn(valueProperty);
        });
    }
}
