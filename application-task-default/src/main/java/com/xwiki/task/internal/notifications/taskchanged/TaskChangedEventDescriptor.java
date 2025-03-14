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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * Descriptor for the event {@link TaskChangedEvent}.
 *
 * @version $Id$
 * @since 3.8.0
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.taskchanged.TaskChangedEventDescriptor")
public class TaskChangedEventDescriptor implements RecordableEventDescriptor
{
    @Override
    public String getEventType()
    {
        return TaskChangedEvent.class.getCanonicalName();
    }

    @Override
    public String getApplicationName()
    {
        return "taskmanager.extension.name";
    }

    @Override
    public String getDescription()
    {
        return "taskmanager.events.taskChangedEvent.description";
    }

    @Override
    public String getApplicationIcon()
    {
        return "list";
    }

    @Override
    public String getEventTitle()
    {
        return "taskmanager.events.taskChangedEvent.title";
    }
}
