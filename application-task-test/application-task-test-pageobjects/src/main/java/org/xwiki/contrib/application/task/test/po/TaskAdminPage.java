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

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object of the Task Manager administration section.
 *
 * @since 3.7.0
 */
public class TaskAdminPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "Task Manager";

    @FindBy(id = "TaskManager.TaskManagerClass_0_defaultInlineStatus")
    private WebElement defaultInlineStatusElement;

    @FindBy(id = "project")
    private WebElement addProjectTextInput;

    @FindBy(id = "severity")
    private WebElement addSeverityTextInput;

    @FindBy(id = "status")
    private WebElement addStatusTextInput;

    private Select defaultInlineStatusSelect;

    public TaskAdminPage()
    {
        super(SECTION_ID);
    }

    public static TaskAdminPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new TaskAdminPage();
    }

    public String getDefaultInlineStatusValue()
    {
        return getDefaultInlineStatusSelect().getFirstSelectedOption().getText();
    }

    public void setDefaultInlineStatusValue(String value)
    {
        getDefaultInlineStatusSelect().selectByVisibleText(value);
    }

    /**
     * Create a new project for tasks.
     *
     * @param projectName Name that will be used for the new project
     * @since 3.8.0
     */
    public void addNewProject(String projectName)
    {
        addProjectTextInput.click();
        addProjectTextInput.clear();
        addProjectTextInput.sendKeys(projectName);
        addProjectTextInput.sendKeys(Keys.ENTER);
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * Create a new severity class for tasks (e.g. High priority).
     *
     * @param severityName Name that will be used for the new severity class
     * @since 3.8.0
     */
    public void addNewSeverity(String severityName)
    {
        addSeverityTextInput.click();
        addSeverityTextInput.clear();
        addSeverityTextInput.sendKeys(severityName);
        addSeverityTextInput.sendKeys(Keys.ENTER);
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * Create a new status for tasks (e.g. ToDo, Done).
     *
     * @param statusName Name that will be used for the new status
     * @since 3.8.0
     */
    public void addNewStatus(String statusName)
    {
        addStatusTextInput.click();
        addStatusTextInput.clear();
        addStatusTextInput.sendKeys(statusName);
        addStatusTextInput.sendKeys(Keys.ENTER);
        getDriver().waitUntilPageIsReloaded();
    }

    private Select getDefaultInlineStatusSelect()
    {
        if (defaultInlineStatusSelect == null) {
            defaultInlineStatusSelect = new Select(defaultInlineStatusElement);
        }
        return defaultInlineStatusSelect;
    }
}
