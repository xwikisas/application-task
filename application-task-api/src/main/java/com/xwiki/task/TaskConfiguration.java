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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * The configuration of the Task Manager Application for the current wiki.
 *
 * @version $Id$
 * @since 3.0
 */
@Role
@Unstable
public interface TaskConfiguration
{
    /**
     * @return the date format that should be used for storage.
     */
    @Deprecated
    String getStorageDateFormat();

    /**
     * @return a list of fold events during which the task listeners should execute. By default, the listeners do not
     *     execute during fold events.
     * @since 3.1.1
     */
    List<String> getNotSkippedFoldEvents();

    /**
     * @return the status that will be set by default when creating a task macro.
     * @since 3.7.0
     */
    String getDefaultInlineStatus();

    /**
     * @return the date format that should be used for displaying purposes.
     */
    @Deprecated
    String getDisplayDateFormat();

    /**
     * @return whether the id (and link) of the task macro should be displayed or not.
     */
    boolean isIdDisplayed();
}
