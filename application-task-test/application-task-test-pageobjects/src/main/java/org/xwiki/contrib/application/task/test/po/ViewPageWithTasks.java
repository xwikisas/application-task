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
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on a page that contains task macros..
 *
 * @version $Id$
 * @since 3.2.1
 */
public class ViewPageWithTasks extends ViewPage
{
    @FindBy(className = "task-macro")
    private List<WebElement> taskMacros;

    public List<WebElement> getTaskMacros()
    {
        return taskMacros;
    }

    public String getTaskMacroContent(int index)
    {
        return taskMacros.get(index).findElement(By.className("task-content")).getText();
    }

    public WebElement getTaskMacroLink(int index)
    {
        return taskMacros.get(index).findElement(By.tagName("a"));
    }

    public boolean isTaskMacroCheckboxChecked(int index)
    {
        return Objects.equals(taskMacros.get(index).findElement(By.className("task-status")).getAttribute("checked"),
            "true");
    }

    public void clickTaskMacroCheckbox(int index)
    {
        taskMacros.get(index).findElement(By.className("task-status")).click();
        this.waitForNotificationSuccessMessage("Task status changed successfully!");
    }

    public LiveTableElement getTaskReportLiveTable() {
        WebElement liveTableElement = getDriver().findElement(By.className("xwiki-livetable"));
        return new LiveTableElement(liveTableElement.getAttribute("id"));
    }
}
