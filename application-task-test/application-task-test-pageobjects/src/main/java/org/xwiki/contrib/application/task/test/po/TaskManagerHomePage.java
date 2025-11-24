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
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the TaskManager.WebHome page.
 *
 * @version $Id: b3d6b45d382bdfae77b31ceeb16805fefdbfcd21 $
 * @since 4.3M2
 */
public class TaskManagerHomePage extends ViewPage
{
    @FindBy(xpath = "//a[@class = 'action add' and . = 'Add new entry']")
    private WebElement addTaskButton;

    /**
     * Opens the home page.
     */
    public static TaskManagerHomePage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage());
        return new TaskManagerHomePage();
    }

    public static String getSpace()
    {
        return "TaskManager";
    }

    public static String getPage()
    {
        return "WebHome";
    }

    /**
     * Click on the "Add New Entry" which is a link, part of the AppWithinMinutes application
     */
    public void clickAddNewEntry()
    {
        addTaskButton.click();
    }

    /**
     * Set the name of the task to be created in the popup
     *
     * @param taskName Name that will be used to create the task
     */
    public void setEntryName(String taskName)
    {
        WebElement nameInput =
            getDriver().findElementWithoutWaiting(By.xpath("//div[@id = 'entryNamePopup']//input[@type = 'text']"));
        nameInput.clear();
        nameInput.sendKeys(taskName);
    }

    /**
     * Validate the name in the popup and create the task
     *
     * @return Return the inline page to edit the task
     */
    public TaskManagerInlinePage clickAddEntry()
    {
        WebElement addButton =
            getDriver().findElementWithoutWaiting(By.xpath("//div[@id = 'entryNamePopup']//input[@type = 'image']"));
        addButton.click();
        return new TaskManagerInlinePage();
    }

    /**
     * Click the button which leads to the list of task templates, to redirect to the templates page.
     *
     * @since 3.11.0
     */
    public void clickTaskTemplateListButton()
    {
        getUtil().getDriver().findElement(By.id("task-manager-view-task-template-list")).click();
    }

    /**
     * @return the FAQ livetable element
     * @deprecated since 3.10.0, use {@link #getTaskLiveDataTable()} instead.
     */
    @Deprecated(since = "3.10.0")
    public LiveTableElement getTaskLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("taskmanager");
        lt.waitUntilReady();
        return lt;
    }

    /**
     * @return the table layout of the livedata present on the homepage.
     * @since 3.10.0
     */
    public TableLayoutElement getTaskLiveDataTable()
    {
        LiveDataElement liveDataElement = new LiveDataElement("taskmanager");
        TableLayoutElement tableLayoutElement = liveDataElement.getTableLayout();
        tableLayoutElement.waitUntilReady();
        return tableLayoutElement;
    }
}
