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
 * Represents the macro editing modal for the Taskbox (checkbox) macro.
 *
 * @version $Id$
 * @since 3.11.5
 */
public class TaskboxMacroModal extends MacroDialogEditModal
{
    private static final String PARAMETER_ID = "id";

    @Override
    public TaskboxMacroModal waitUntilReady()
    {
        super.waitUntilReady();
        return this;
    }

    /**
     * @return the value of the id parameter, waiting for it to be generated when inserting a new macro
     */
    public String getId()
    {
        getDriver().waitUntilCondition(driver -> !getMacroParameter(PARAMETER_ID).isEmpty());
        return getMacroParameter(PARAMETER_ID);
    }
}
