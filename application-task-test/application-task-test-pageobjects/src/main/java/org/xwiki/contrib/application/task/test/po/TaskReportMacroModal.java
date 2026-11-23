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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the macro editing modal for the Task report macro.
 *
 * @version $Id$
 * @since 3.12.1
 */
public class TaskReportMacroModal extends MacroDialogEditModal
{
    private static final String COLUMNS_SELECTOR =
        "[class*=-editor-modal] .macro-parameter-field select[name='columns']";

    private static final String MODAL_HEADER_SELECTOR = "[class*=-editor-modal] .modal-header";

    @Override
    public TaskReportMacroModal waitUntilReady()
    {
        super.waitUntilReady();
        // The columns picker is ready once it has restored the order of the columns from the macro call.
        getDriver().waitUntilCondition(driver -> Boolean.TRUE.equals(getDriver().executeScript(
            "return !!jQuery(arguments[0]).data('taskStatusPickerReady')", getColumnsSelect())));
        return this;
    }

    /**
     * @return the selected columns, in the order in which they are displayed by the macro
     */
    public List<String> getColumns()
    {
        return getColumnsPicker().getValues();
    }

    /**
     * Searches for a column and adds it after the selected columns.
     *
     * @param searchText the text to type in the columns picker
     * @param column the column to add among the suggestions
     * @return this modal
     */
    public TaskReportMacroModal addColumn(String searchText, String column)
    {
        SuggestInputElement picker = getColumnsPicker();
        picker.sendKeys(searchText).waitForSuggestions().selectByValue(column);
        // Close the suggestions, which can cover the modal buttons, by moving the focus out of the picker, since
        // Escape would also close the modal.
        getDriver().findElementWithoutWaitingWithoutScrolling(By.cssSelector(MODAL_HEADER_SELECTOR)).click();
        picker.waitForSuggestionsClearance();
        return this;
    }

    private WebElement getColumnsSelect()
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(By.cssSelector(COLUMNS_SELECTOR));
    }

    private SuggestInputElement getColumnsPicker()
    {
        // MacroDialogEditModal#getMacroParameterInput supports only inputs, while the columns picker is a select.
        return new SuggestInputElement(getColumnsSelect());
    }
}
