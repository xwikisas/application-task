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
public class TaskChangedEvent implements RecordableEvent, Cloneable
{
    private DocumentReference documentReference;
    private String documentVersion;
    private Map<String, Object> localizationParams;

    /**
     * Event which represents a property change in a task. This is a dummy constructor.
     */
    public TaskChangedEvent()
    {
    }

    /**
     * Event which represents a property change in a task.
     * 
     * @param documentReference  the document reference of the changed task.
     * @param documentVersion    the version of the task document which contains the changes made.
     * @param localizationParams the localization parameters used to format the localization string of the notification.
     */
    public TaskChangedEvent(
        DocumentReference documentReference,
        String documentVersion,
        Map<String, Object> localizationParams
    )
    {
        this.documentReference = documentReference;
        this.documentVersion = documentVersion;
        this.localizationParams = localizationParams;
    }

    /**
     * Event which represents a property change in a task.
     * 
     * @param documentReference the document reference of the changed task.
     * @param documentVersion   the version of the task document which contains the changes made.
     */
    public TaskChangedEvent(
        DocumentReference documentReference,
        String documentVersion
    )
    {
        this(documentReference, documentVersion, null);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof TaskChangedEvent;
    }

    @Override
    public TaskChangedEvent clone()
    {
        // Create a new class instance, sharing the same objects as property values.
        return new TaskChangedEvent(documentReference, documentVersion, localizationParams);
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
     * @return the localization parameters of this notification.
     */
    public Map<String, Object> getLocalizationParams()
    {
        return localizationParams;
    }

    /**
     * @param localizationParams a dictionary of parameters for use in the localization strings.
     * The dictionary must contain:
     *   * `new` field, containing the new value of the changed property.
     *   * `old` field, containing the old value of the changed property.
     *   * `type` field, containing the suffix of the localization string to use when displaying the notification.
     */
    public void setLocalizationParams(Map<String, Object> localizationParams)
    {
        this.localizationParams = localizationParams;
    }

}
