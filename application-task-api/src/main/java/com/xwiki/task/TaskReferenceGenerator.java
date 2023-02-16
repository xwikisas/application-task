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

/**
 * Counter that handles the id generation for the tasks.
 *
 * @version $Id$
 * @since 3.0
 */
@Role
@Unstable
public interface TaskReferenceGenerator
{
    /**
     * Generate a reference for a task, relative to its parent.
     *
     * @param parent the parent of the task.
     * @return a reference to the task. It can be either a child of the parent, a sibling or it can be a child to the
     * default TaskManager home, depending on the rights of the current user.
     */
    DocumentReference generate(DocumentReference parent);
}
