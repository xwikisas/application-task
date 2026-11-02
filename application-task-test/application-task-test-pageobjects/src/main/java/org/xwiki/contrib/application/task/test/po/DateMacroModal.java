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

import org.openqa.selenium.By;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;

/**
 * The macro editor modal of the date macro.
 *
 * @version $Id$
 * @since 3.12.0
 */
public class DateMacroModal extends MacroDialogEditModal
{
    @Override
    public DateMacroModal waitUntilReady()
    {
        super.waitUntilReady();
        // The date inputs are converted to the current storage format before the date picker is initialized.
        getDriver().waitUntilCondition(
            driver -> driver.findElements(By.cssSelector(".macro-editor input.stored-date")).isEmpty());
        return this;
    }

    /**
     * @return the date value, as written in the macro parameter
     */
    public String getValue()
    {
        return getMacroParameter("value");
    }
}
