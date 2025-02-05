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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.eventstream.RecordableEvent;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Event sent when a task has been changed.
 *
 * @version $Id$
 * @since 3.7
 */
public class TaskChangedEvent implements RecordableEvent
{
    protected static final String CURRENT_VALUE_KEY = "currentValue";

    protected static final String PREVIOUS_VALUE_KEY = "previousValue";

    protected static final String TYPE_KEY = "type";

    private XWikiDocument document;

    private Map<String, Object> eventInfo;

    /**
     * Event which represents a property change in a task. This is a dummy constructor.
     */
    public TaskChangedEvent()
    {
        this.eventInfo = new HashMap<>();
    }

    /**
     * Event which represents a property change in a task.
     *
     * @param document the document of the changed task.
     */
    public TaskChangedEvent(XWikiDocument document)
    {
        this.document = document;
        this.eventInfo = new HashMap<>();
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof TaskChangedEvent;
    }

    /**
     * @return the document of the task which generated this event.
     */
    public XWikiDocument getDocument()
    {
        return document;
    }

    /**
     * @return the event info of this notification (contains modified field values). It should contain a `currentValue`
     *     and `previousValue` field (optional), containing the new and old values of the changed property, and a `type`
     *     field containing the suffix of the localization string to use when displaying the notification.
     */
    public Map<String, Object> getEventInfo()
    {
        return eventInfo;
    }

    /**
     * @param eventInfo a dictionary of parameters for use in the localization strings. It should contain a
     *     `currentValue` and `previousValue` field, containing the new and old values of the changed property, and a
     *     `type` field containing the suffix of the localization string to use when displaying the notification.
     */
    public void setEventInfo(Map<String, Object> eventInfo)
    {
        this.eventInfo = eventInfo;
    }

    /**
     * Get the name of the changed property.
     *
     * @return the name of the property which was changed.
     */
    public String getType()
    {
        return (String) eventInfo.get(TaskChangedEvent.TYPE_KEY);
    }

    /**
     * Set the name of the changed property.
     *
     * @param type the name of the changed property.
     */
    public void setType(String type)
    {
        eventInfo.put(TaskChangedEvent.TYPE_KEY, type);
    }

    /**
     * Get the previous value of the changed property.
     *
     * @return the previous value of the property which was changed.
     */
    public Object getPreviousValue()
    {
        return eventInfo.get(TaskChangedEvent.PREVIOUS_VALUE_KEY);
    }

    /**
     * Set the previous value of the changed property.
     *
     * @param previousValue the previous value of the changed property.
     */
    public void setPreviousValue(Object previousValue)
    {
        eventInfo.put(TaskChangedEvent.PREVIOUS_VALUE_KEY, previousValue);
    }

    /**
     * Get the current value of the changed property.
     *
     * @return the current value of the property which was changed.
     */
    public Object getCurrentValue()
    {
        return eventInfo.get(TaskChangedEvent.CURRENT_VALUE_KEY);
    }

    /**
     * Set the current value of the changed property.
     *
     * @param currentValue the current value of the changed property.
     */
    public void setCurrentValue(Object currentValue)
    {
        eventInfo.put(TaskChangedEvent.CURRENT_VALUE_KEY, currentValue);
    }
}
