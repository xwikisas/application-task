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
 * Represents the macro editing modal for the Task macro.
 *
 * @version $Id$
 * @since 3.11.10
 */
public class TaskMacroModal extends MacroDialogEditModal
{
    /**
     * The name of the reference parameter.
     */
    public static final String PARAMETER_REFERENCE = "reference";

    /**
     * @param parameter the input `name` attribute of the parameter we are interested in.
     * @return true if the input is visible; false otherwise. Some parameters, such as `reference`, can be hidden the
     *     first time the macro modal is opened.
     */
    public boolean isVisible(String parameter)
    {
        return getMacroParameterInput(parameter).isDisplayed();
    }

    /**
     * Clicks the button next to the reference parameter that will generate a new reference value.
     *
     * @return the newly generated value.
     */
    public String generateNewReference()
    {
        this.getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(".task-reference-group .btn-default")).click();
        this.getDriver().waitUntilElementDisappears(By.xpath("//div[contains(@class,'xnotification-inprogress')]"));
        return getMacroParameter(PARAMETER_REFERENCE);
    }
}
