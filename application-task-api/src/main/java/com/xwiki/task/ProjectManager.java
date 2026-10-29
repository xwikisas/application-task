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

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Decides which Task Manager projects can be assigned to a task. A project can restrict its usage to a list of spaces
 * and to a list of users:
 * <ul>
 * <li>a project without spaces and without users can be assigned to any task;</li>
 * <li>a project can be assigned to a task located in one of the project spaces or in one of their subspaces;</li>
 * <li>a project can be assigned by one of the project users, wherever the task is located.</li>
 * </ul>
 *
 * @version $Id$
 * @since 3.12.0
 */
@Role
@Unstable
public interface ProjectManager
{
    /**
     * @param taskReference the reference of the task document
     * @param userReference the user that assigns the project, can be {@code null} for guest
     * @return the names of the projects that can be assigned to the given task by the given user
     * @throws TaskException if the projects could not be retrieved
     */
    Set<String> getAllowedProjects(DocumentReference taskReference, DocumentReference userReference)
        throws TaskException;

    /**
     * @param project the name of the project
     * @param taskReference the reference of the task document
     * @param userReference the user that assigns the project, can be {@code null} for guest
     * @return {@code true} if the given project can be assigned to the given task by the given user, {@code false}
     *     otherwise
     * @throws TaskException if the projects could not be retrieved
     */
    boolean isProjectAllowed(String project, DocumentReference taskReference, DocumentReference userReference)
        throws TaskException;
}
