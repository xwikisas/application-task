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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the TaskManager.TaskTemplateList page.
 *
 * @version $Id$
 * @since 4.0.0
 */
public class TaskTemplateListPage extends ViewPage
{
    private static final String LIVE_DATA_ID = "taskManagerTemplates";

    @FindBy(id = "button-submit-template")
    private WebElement submitTemplateButton;

    @FindBy(name = "templateName")
    private WebElement templateNameField;

    @FindBy(className = "task-template-list-description")
    private WebElement descriptionElement;

    public static String getURL()
    {
        return getUtil().getURL("TaskManager", "TaskTemplateList");
    }

    /**
     * Opens the home page.
     */
    public static TaskTemplateListPage gotoPage()
    {
        getUtil().gotoPage("TaskManager", "TaskTemplateList");
        return new TaskTemplateListPage();
    }

    public void submit()
    {
        submitTemplateButton.click();
    }

    public void setTemplateName(String name)
    {
        templateNameField.sendKeys(name);
    }

    /**
     * @return the text explaining what task templates are
     * @since 3.11.5
     */
    public String getDescription()
    {
        return descriptionElement.getText();
    }

    /**
     * @return a list of urls to /bin/view/..TemplateProvider
     */
    public List<String> getListedTemplates()
    {
        // Wait for the live data to load its entries.
        new LiveDataElement(LIVE_DATA_ID).getTableLayout();
        return getDriver().findElement(By.id(LIVE_DATA_ID)).findElements(
            By.cssSelector("a[href*=\"/bin/view/\"][href*=\"TemplateProvider\"]")).stream()
            .map(we -> we.getAttribute("href")).collect(Collectors.toList());
    }
}
