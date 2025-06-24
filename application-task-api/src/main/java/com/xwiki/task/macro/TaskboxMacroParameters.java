package com.xwiki.task.macro;

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

import org.xwiki.properties.annotation.PropertyDisplayType;

import com.xwiki.task.TaskboxId;

/**
 * The parameters used by the taskbox macro.
 *
 * @version $Id$
 * @since 3.8.0
 */
public class TaskboxMacroParameters
{
    private String id;

    private boolean checked;

    /**
     * @return unique identifier on the page.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id see {@link #getId()}.
     */
    @PropertyDisplayType(TaskboxId.class)
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return whether the macro is checked or not.
     */
    public boolean isChecked()
    {
        return checked;
    }

    /**
     * @param checked see {@link #isChecked()}.
     */
    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }
}
