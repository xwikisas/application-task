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
 * Represents the macro editing modal for the Kanban board macro, where the color of each column is picked next to the
 * column, instead of being typed in the colors parameter.
 *
 * @version $Id$
 * @since 3.12.0
 */
public class KanbanBoardMacroModal extends MacroDialogEditModal
{
    private static final String MODAL_SELECTOR = "[class*=-editor-modal] ";

    private static final String PARAMETER_SELECTOR = MODAL_SELECTOR + ".macro-parameter[data-id='%s']";

    private static final String COLUMNS_SELECT_SELECTOR =
        MODAL_SELECTOR + ".macro-parameter-field select[name='columns']";

    private static final String COLOR_PICKER_SELECTOR =
        String.format(PARAMETER_SELECTOR, "columns") + " .selectize-input > [data-value='%s'] .kanban-column-color";

    @Override
    public KanbanBoardMacroModal waitUntilReady()
    {
        super.waitUntilReady();
        waitUntilColorPickersAreDisplayed();
        return this;
    }

    /**
     * @return the selected columns, in the order they are displayed on the board
     */
    public List<String> getColumns()
    {
        return getColumnsInput().getValues();
    }

    /**
     * Adds a column at the end of the board.
     *
     * @param column the status displayed by the added column
     * @return this modal
     */
    public KanbanBoardMacroModal addColumn(String column)
    {
        getColumnsInput().click().waitForSuggestions().selectByValue(column);
        // SuggestInputElement#hideSuggestions presses Escape, which would close the modal. Move the focus out of the
        // columns picker instead, which closes its suggestions.
        getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(String.format(PARAMETER_SELECTOR, "columns") + " .macro-parameter-name")).click();
        getDriver().waitUntilCondition(
            driver -> driver.findElements(By.cssSelector(".selectize-dropdown.active")).isEmpty());
        waitUntilColorPickersAreDisplayed();
        return this;
    }

    /**
     * @param column a selected column
     * @return the color of the column, in the #rrggbb format
     */
    public String getColumnColor(String column)
    {
        return getColorPicker(column).getAttribute("value");
    }

    /**
     * @param column a selected column
     * @param color the new color of the column, in the #rrggbb format
     * @return this modal
     */
    public KanbanBoardMacroModal setColumnColor(String column, String color)
    {
        // The browsers don't support typing in color inputs, they open a native color picker instead.
        getDriver().executeScript("arguments[0].value = arguments[1];"
            + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", getColorPicker(column), color);
        return this;
    }

    /**
     * @return {@code true} if the colors parameter is displayed as a text input, {@code false} if the colors are picked
     *     next to the columns
     */
    public boolean isColorsParameterDisplayed()
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(String.format(PARAMETER_SELECTOR, "colors"))).isDisplayed();
    }

    private SuggestInputElement getColumnsInput()
    {
        return new SuggestInputElement(
            getDriver().findElementWithoutWaitingWithoutScrolling(By.cssSelector(COLUMNS_SELECT_SELECTOR)));
    }

    private WebElement getColorPicker(String column)
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(String.format(COLOR_PICKER_SELECTOR, column)));
    }

    private void waitUntilColorPickersAreDisplayed()
    {
        // The color pickers are added once the columns picker is created and has restored the order of the columns.
        getDriver().waitUntilElementIsVisible(By.cssSelector(COLUMNS_SELECT_SELECTOR + " + .selectize-control"));
        getDriver().waitUntilCondition(driver -> getColumns().stream().allMatch(
            column -> !driver.findElements(By.cssSelector(String.format(COLOR_PICKER_SELECTOR, column))).isEmpty()));
    }
}
