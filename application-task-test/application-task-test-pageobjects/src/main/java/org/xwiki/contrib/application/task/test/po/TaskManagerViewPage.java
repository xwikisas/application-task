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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class TaskManagerViewPage extends ViewPage
{
    private static final String CLASS_PREFIX = "TaskManager.TaskManagerClass_0_";
    
    @FindBy(xpath = "//div[contains(@id, \"document-title\")]/h1")
    private WebElement nameElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "severity\"]/../../dd/p")
    private WebElement severityElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "assignee\"]/../../dd/ul/li/div[contains(@class, \"user-name\")]")
    private WebElement assigneeElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "status\"]/../../dd/p")
    private WebElement statusElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "progress\"]/../../dd/div/div/span")
    private WebElement progressElement;
    
    /**
     * Opens the home page.
     */
    public static TaskManagerViewPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page);
        return new TaskManagerViewPage();
    }
    
    public String getName() {
    	return nameElement.getText();
    }
    
    public String getSeverity() {
    	return severityElement.getText();
    }
    
    public String getAssignee() {
    	return assigneeElement.getText();
    }
    
    public String getStatus() {
    	return statusElement.getText();
    }
    
    public String getProgress() {
    	return progressElement.getText();
    }
}
