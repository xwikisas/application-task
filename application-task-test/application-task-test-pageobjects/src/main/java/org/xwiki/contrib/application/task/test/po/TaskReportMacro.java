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

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xwiki.test.ui.po.BaseElement;

public class TaskReportMacro extends BaseElement
{
    private final WebElement report;

    public TaskReportMacro(String id)
    {
        this.report = getDriver().findElement(By.id(id));
    }

    public List<TaskElement> getTasks()
    {
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(30));

        wait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(report, By.cssSelector("div.task-macro")));

        List<WebElement> tasks = report.findElements(By.cssSelector("div.task-macro"));

        if (tasks.isEmpty()) {
            throw new NoSuchElementException("No task-macro found inside report " + report.getAttribute("id"));
        }

        return tasks.stream().map(TaskElement::new).collect(Collectors.toList());
    }

    public TaskElement getTask(int index)
    {
        List<TaskElement> tasks = getTasks();
        if (index < 0 || index >= tasks.size()) {
            throw new IndexOutOfBoundsException("Task index " + index + " out of bounds, total tasks: " + tasks.size());
        }
        return tasks.get(index);
    }
}
