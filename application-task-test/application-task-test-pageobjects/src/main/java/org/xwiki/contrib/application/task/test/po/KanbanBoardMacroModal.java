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
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;

/**
 * Represents the macro editing modal for the Kanban board macro, where the columns are added, removed, reordered and
 * colored in a list, instead of being typed in the columns and colors parameters.
 *
 * @version $Id$
 * @since 3.12.0
 */
public class KanbanBoardMacroModal extends MacroDialogEditModal
{
    private static final String MODAL_SELECTOR = "[class*=-editor-modal] ";

    private static final String PARAMETER_SELECTOR = MODAL_SELECTOR + ".macro-parameter[data-id='%s']";

    private static final String EDITOR_SELECTOR =
        String.format(PARAMETER_SELECTOR, "columns") + " .kanban-columns-editor";

    private static final String COLUMNS_SELECTOR = EDITOR_SELECTOR + " .kanban-column";

    private static final String COLUMN_SELECTOR = COLUMNS_SELECTOR + "[data-value='%s']";

    private static final String ADD_DROPDOWN_SELECTOR = EDITOR_SELECTOR + " .kanban-columns-add";

    private static final String SUGGESTIONS_SELECTOR = ADD_DROPDOWN_SELECTOR + " .kanban-column-suggestion";

    private static final String DATA_VALUE = "data-value";

    @Override
    public KanbanBoardMacroModal waitUntilReady()
    {
        super.waitUntilReady();
        // The list of columns is displayed once the columns picker has restored the order of the columns.
        getDriver().waitUntilElementIsVisible(By.cssSelector(EDITOR_SELECTOR));
        return this;
    }

    /**
     * @return the selected columns, in the order they are displayed on the board
     */
    public List<String> getColumns()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(COLUMNS_SELECTOR)).stream()
            .map(column -> column.getAttribute(DATA_VALUE)).collect(Collectors.toList());
    }

    /**
     * Searches the statuses that can be added as columns.
     *
     * @param query the text to search in the status names
     * @return the matching statuses, excluding the ones already displayed as columns
     */
    public List<String> searchColumns(String query)
    {
        openAddDropdown();
        WebElement searchInput = getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(ADD_DROPDOWN_SELECTOR + " .kanban-columns-search input"));
        searchInput.clear();
        searchInput.sendKeys(query);
        List<String> suggestions = getDriver().findElementsWithoutWaiting(By.cssSelector(SUGGESTIONS_SELECTOR))
            .stream().map(suggestion -> suggestion.getAttribute(DATA_VALUE)).collect(Collectors.toList());
        closeAddDropdown();
        return suggestions;
    }

    /**
     * Adds a column at the end of the board.
     *
     * @param column the status displayed by the added column
     * @return this modal
     */
    public KanbanBoardMacroModal addColumn(String column)
    {
        openAddDropdown();
        getDriver().findElementWithoutWaiting(
            By.cssSelector(String.format(SUGGESTIONS_SELECTOR + "[data-value='%s'] a", column))).click();
        getDriver().waitUntilElementIsVisible(By.cssSelector(String.format(COLUMN_SELECTOR, column)));
        closeAddDropdown();
        return this;
    }

    /**
     * @param column a selected column
     * @return this modal
     */
    public KanbanBoardMacroModal removeColumn(String column)
    {
        getColumn(column).findElement(By.className("kanban-column-remove")).click();
        getDriver().waitUntilCondition(driver -> !getColumns().contains(column));
        return this;
    }

    /**
     * Moves a column one position up, using the keyboard on its drag handle.
     *
     * @param column a selected column, that is not the first one
     * @return this modal
     */
    public KanbanBoardMacroModal moveColumnUp(String column)
    {
        return moveColumn(column, Keys.ARROW_UP, -1);
    }

    /**
     * Moves a column one position down, using the keyboard on its drag handle.
     *
     * @param column a selected column, that is not the last one
     * @return this modal
     */
    public KanbanBoardMacroModal moveColumnDown(String column)
    {
        return moveColumn(column, Keys.ARROW_DOWN, 1);
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
     *     in the list of columns
     */
    public boolean isColorsParameterDisplayed()
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(String.format(PARAMETER_SELECTOR, "colors"))).isDisplayed();
    }

    private KanbanBoardMacroModal moveColumn(String column, Keys key, int offset)
    {
        int expectedIndex = getColumns().indexOf(column) + offset;
        getColumn(column).findElement(By.className("kanban-column-handle")).sendKeys(key);
        getDriver().waitUntilCondition(driver -> getColumns().indexOf(column) == expectedIndex);
        return this;
    }

    private void openAddDropdown()
    {
        getDriver().findElementWithoutWaiting(By.cssSelector(ADD_DROPDOWN_SELECTOR + " .dropdown-toggle")).click();
        getDriver().waitUntilElementIsVisible(By.cssSelector(ADD_DROPDOWN_SELECTOR + ".open .dropdown-menu"));
    }

    private void closeAddDropdown()
    {
        if (getDriver().findElementsWithoutWaiting(By.cssSelector(ADD_DROPDOWN_SELECTOR + ".open")).isEmpty()) {
            // Adding the last status closes the dropdown, and pressing Escape outside of it would close the modal.
            return;
        }
        // Pressing Escape in the dropdown closes only the dropdown, not the modal.
        getDriver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
        getDriver().waitUntilCondition(
            driver -> driver.findElements(By.cssSelector(ADD_DROPDOWN_SELECTOR + ".open")).isEmpty());
    }

    private WebElement getColumn(String column)
    {
        return getDriver().findElementWithoutWaiting(By.cssSelector(String.format(COLUMN_SELECTOR, column)));
    }

    private WebElement getColorPicker(String column)
    {
        return getColumn(column).findElement(By.className("kanban-column-color"));
    }
}
