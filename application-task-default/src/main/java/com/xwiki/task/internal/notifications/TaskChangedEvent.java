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

import java.util.Map;

import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReference;

/**
 * Event sent when a task has been changed.
 *
 * @version $Id$
 * @since 3.5.2
 */
public class TaskChangedEvent implements RecordableEvent
{
    private DocumentReference documentReference;

    private String documentVersion;

    private Map<String, Object> eventInfo;

    /**
     * Event which represents a property change in a task. This is a dummy constructor.
     */
    public TaskChangedEvent()
    {
    }

    /**
     * Event which represents a property change in a task.
     *
     * @param documentReference the document reference of the changed task.
     * @param documentVersion the version of the task document which contains the changes made.
     * @param eventInfo additional event info used to format the localization string of the notification.
     */
    public TaskChangedEvent(DocumentReference documentReference, String documentVersion, Map<String, Object> eventInfo)
    {
        this.documentReference = documentReference;
        this.documentVersion = documentVersion;
        this.eventInfo = eventInfo;
    }

    /**
     * Event which represents a property change in a task.
     *
     * @param documentReference the document reference of the changed task.
     * @param documentVersion the version of the task document which contains the changes made.
     */
    public TaskChangedEvent(DocumentReference documentReference, String documentVersion)
    {
        this(documentReference, documentVersion, null);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof TaskChangedEvent;
    }

    /**
     * @return the document reference of the task which generated this event.
     */
    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    /**
     * @return the version of the task document which generated this event.
     */
    public String getDocumentVersion()
    {
        return documentVersion;
    }

    /**
     * @return the event info of this notification (contains modified field values).
     */
    public Map<String, Object> getEventInfo()
    {
        return eventInfo;
    }

    /**
     * @param eventInfo a dictionary of parameters for use in the localization strings. It must contain a
     *     `currentValue` and `previousValue` field, containing the new and old values of the changed property, and a
     *     `type` field containing the suffix of the localization string to use when displaying the notification.
     */
    public void setEventInfo(Map<String, Object> eventInfo)
    {
        this.eventInfo = eventInfo;
    }
}
