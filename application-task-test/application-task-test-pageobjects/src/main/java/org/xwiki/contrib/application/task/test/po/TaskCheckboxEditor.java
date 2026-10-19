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
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the tasks displayed by the task and checkbox macros inside the WYSIWYG editor.
 *
 * @version $Id$
 * @since 3.11.8
 */
public class TaskCheckboxEditor extends BaseElement
{
    private final CKEditor editor;

    /**
     * @param editor the editor that contains the task checkboxes
     */
    public TaskCheckboxEditor(CKEditor editor)
    {
        this.editor = editor;
    }

    /**
     * Click the task checkbox at the given index and wait for the macro that displays it to be updated.
     *
     * @param index the index of the task checkbox in the edited content, starting from 0
     */
    public void toggleTaskCheckbox(int index)
    {
        boolean checked = isTaskCheckboxChecked(index);
        this.editor.getRichTextArea().click(getCheckboxSelector(index));
        this.editor.executeOnEditedContent(() -> getDriver().waitUntilCondition(driver -> {
            WebElement checkbox = driver.findElement(getCheckboxSelector(index));
            // The macro is updated when the checked attribute matches the checkbox state.
            return checkbox.isSelected() != checked && (checkbox.getDomAttribute("checked") != null) != checked;
        }));
    }

    /**
     * @param index the index of the task checkbox in the edited content, starting from 0
     * @return {@code true} if the task checkbox is checked, {@code false} otherwise
     */
    public boolean isTaskCheckboxChecked(int index)
    {
        boolean[] checked = new boolean[1];
        this.editor.executeOnEditedContent(
            () -> checked[0] = getDriver().findElement(getCheckboxSelector(index)).isSelected());
        return checked[0];
    }

    /**
     * Type text at the end of the content of the task at the given index.
     *
     * @param index the index of the task in the edited content, starting from 0
     * @param text the text to type
     */
    public void appendToTaskContent(int index, String text)
    {
        // Move the caret at the end of the nested editable that holds the content of the task macro.
        Object found = getDriver().executeScript(String.join("\n",
            "const editor = CKEDITOR.instances[arguments[0]];",
            "const editables = Object.values(editor.widgets.instances)",
            "  .filter(widget => widget.name === 'xwiki-macro' && ['task', 'checkbox'].includes(widget.data.name))",
            "  .map(widget => widget.editables.$content).filter(editable => editable);",
            "const editable = editables[arguments[1]];",
            "if (!editable) {",
            "  return false;",
            "}",
            "editable.focus();",
            "const range = editor.createRange();",
            "range.moveToElementEditEnd(editable);",
            "editor.getSelection().selectRanges([range]);",
            "return true;"), this.editor.getName(), index);
        if (!Boolean.TRUE.equals(found)) {
            throw new IllegalStateException(String.format("The content of the task [%d] is not editable.", index));
        }
        RichTextAreaElement richTextArea = this.editor.getRichTextArea();
        richTextArea.sendKeys(text);
        richTextArea.waitUntilTextContains(text);
    }

    private By getCheckboxSelector(int index)
    {
        return By.xpath(String.format("(//input[contains(@class, 'task-status')])[%d]", index + 1));
    }
}
