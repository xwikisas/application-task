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
        return report.findElements(By.className("task-macro")).stream().map(TaskElement::new)
            .collect(Collectors.toList());
    }

    public TaskElement getTask(int index)
    {
        return getTasks().get(index);
    }

    public int getColumnCount()
    {
        List<WebElement> headers =
            report.findElements(By.cssSelector("tr.column-header-names th:not([style*='display: none'])"));
        return headers.size();
    }

    public List<String> getColumnNames()
    {
        return report.findElements(
                By.cssSelector("tr.column-header-names th:not([style*='display: none']) .property-name")).stream()
            .map(WebElement::getText).collect(Collectors.toList());
    }
}
