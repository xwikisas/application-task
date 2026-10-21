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
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the task picker displayed for a macro parameter in the macro editing modal. It can accept one or more
 * tasks, depending on the parameter.
 *
 * @version $Id$
 * @since 3.12.0
 */
public class TaskPickerElement extends BaseElement
{
    private static final String PARAMETER_FIELD_SELECTOR =
        "[class*=-editor-modal] .macro-parameter-field select[name='%s']";

    private final String parameterName;

    /**
     * @param parameterName the name of the macro parameter that uses the task picker
     */
    public TaskPickerElement(String parameterName)
    {
        this.parameterName = parameterName;
    }

    /**
     * Searches for a task and selects it. When the picker accepts a single task, the selected task is replaced.
     *
     * @param searchText the text to type in the picker
     * @param value the value of the task to select among the suggestions
     * @return this picker
     */
    public TaskPickerElement selectTask(String searchText, String value)
    {
        getSuggestInput().sendKeys(searchText).waitForSuggestions().selectByValue(value);
        return this;
    }

    /**
     * @return the values of the selected tasks
     */
    public List<String> getSelectedTasks()
    {
        return getSuggestInput().getValues();
    }

    /**
     * @return the labels of the selected tasks, as displayed to the user
     */
    public List<String> getSelectedTaskLabels()
    {
        waitUntilReady();
        return getSuggestInput().getSelectedSuggestions().stream()
            .map(SuggestInputElement.SuggestionElement::getLabel).collect(Collectors.toList());
    }

    /**
     * Waits for the picker to be created and to load the labels of the selected tasks.
     *
     * @return this picker
     */
    public TaskPickerElement waitUntilReady()
    {
        // Selectize inserts its control right after the select it enhances.
        By controlSelector =
            By.cssSelector(String.format(PARAMETER_FIELD_SELECTOR, this.parameterName) + " + .selectize-control");
        getDriver().waitUntilElementIsVisible(controlSelector);
        getDriver().waitUntilCondition(driver -> !getDriver().findElementWithoutWaitingWithoutScrolling(controlSelector)
            .getAttribute("class").contains("loading"));
        return this;
    }

    private SuggestInputElement getSuggestInput()
    {
        // MacroDialogEditModal#getMacroParameterInput supports only inputs, while the task picker is a select.
        return new SuggestInputElement(getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(String.format(PARAMETER_FIELD_SELECTOR, this.parameterName))));
    }
}
