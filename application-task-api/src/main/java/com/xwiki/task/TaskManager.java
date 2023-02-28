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
package com.xwiki.task;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xwiki.task.model.Task;

/**
 * This class provides access to different methods that will ease the handling of tasks.
 *
 * @version $Id$
 * @since 3.0
 */
@Role
@Unstable
public interface TaskManager
{
    /**
     * @param reference the reference of a page that contains a Task Object.
     * @return the Task Model of the object inside the page.
     * @throws TaskException if the page does not have a task.
     */
    Task getTask(DocumentReference reference) throws TaskException;

    /**
     * @param id the ID of the task.
     * @return the Task Model of the object inside the page that has the same ID as the one specified.
     * @throws TaskException if there is no task with the given id.
     */
    Task getTask(int id) throws TaskException;

    /**
     * Delete the tasks that have a certain page as an owner.
     * @param documentReference the value by which we want to remove a task.
     * @throws TaskException when there was an error in retrieving or deleting certain task documents.
     */
    void deleteTasksByOwner(DocumentReference documentReference) throws TaskException;
}
