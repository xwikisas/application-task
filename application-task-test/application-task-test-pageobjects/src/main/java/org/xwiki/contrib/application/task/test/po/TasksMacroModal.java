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
package org.xwiki.contrib.application.task.test.po;

import org.xwiki.ckeditor.test.po.MacroDialogEditModal;

/**
 * Represents the macro editing modal for the Tasks macro.
 *
 * @version $Id$
 * @since 3.12.0
 */
public class TasksMacroModal extends MacroDialogEditModal
{
    @Override
    public TasksMacroModal waitUntilReady()
    {
        super.waitUntilReady();
        getTaskPicker().waitUntilReady();
        return this;
    }

    /**
     * @return the picker of the tasks to display, which accepts multiple tasks identified by their numbers
     */
    public TaskPickerElement getTaskPicker()
    {
        return new TaskPickerElement("ids");
    }
}
