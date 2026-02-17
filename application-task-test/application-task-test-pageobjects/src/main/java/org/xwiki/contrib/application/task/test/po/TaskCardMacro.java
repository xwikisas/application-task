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

public class TaskCardMacro extends BaseElement
{
    private final WebElement taskCard;

    public TaskCardMacro(WebElement taskCard)
    {
        this.taskCard = taskCard;
    }

    public String getStatus()
    {
        String statusText = taskCard.findElement(By.cssSelector(".task-card-status")).getText();

        return statusText.replace("[", "").replace("]", "").trim();
    }

    public List<String> getAssignees()
    {
        return taskCard.findElements(By.cssSelector(".task-card-assignee img")).stream()
            .map(e -> e.getAttribute("title").replace("[", "").replace("]", "")).collect(Collectors.toList());
    }

    public boolean isDone()
    {
        return taskCard.getAttribute("class").contains("task-card-status-Done");
    }

    public boolean isLate()
    {
        return !taskCard.findElements(By.cssSelector(".task-card-late")).isEmpty();
    }

    public String getTitle()
    {
        return taskCard.findElement(By.cssSelector(".task-card-title a")).getText().trim();
    }

    public String getTitleLink()
    {
        return taskCard.findElement(By.cssSelector(".task-card-title a")).getAttribute("href");
    }

    public String getDueDate()
    {
        return taskCard.findElement(By.cssSelector(".task-card-duedate")).getText().replace("Due Date", "").trim();
    }
}
