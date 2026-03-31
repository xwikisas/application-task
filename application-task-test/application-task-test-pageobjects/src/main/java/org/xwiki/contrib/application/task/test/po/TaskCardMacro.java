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

/**
 * Represents a TaskCard macro and provides access to its attributes.
 *
 * @version $Id$
 * @since 3.11.0
 */
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

    public boolean isDone()
    {
        return taskCard.getAttribute("class").contains("task-card-status-Done");
    }

    public String getTitle()
    {
        return taskCard.findElement(By.cssSelector(".task-card-title a")).getText().trim();
    }

    public String getDueDate()
    {
        return taskCard.findElement(By.cssSelector(".task-card-duedate")).getText().replace("Due Date", "").trim();
    }

    public void goToTaskPage()
    {
        taskCard.findElement(By.cssSelector(".task-card-title a")).click();
    }

    public boolean hasDependencies()
    {
        return !taskCard.findElements(By.cssSelector(".task-card-dependencies")).isEmpty();
    }

    public int getDependenciesCount()
    {
        List<WebElement> container = taskCard.findElements(By.cssSelector(".task-card-dependencies"));

        if (container.isEmpty()) {
            return 0;
        }

        return container.get(0).findElements(By.cssSelector(".task-card")).size();
    }

    public List<TaskCardMacro> getDependencies()
    {
        List<WebElement> container = taskCard.findElements(By.cssSelector(".task-card-dependencies"));

        if (container.isEmpty()) {
            return List.of();
        }

        return container.get(0).findElements(By.cssSelector(".task-card")).stream().map(TaskCardMacro::new)
            .collect(Collectors.toList());
    }

    public TaskCardMacro getDependency(int index)
    {
        return getDependencies().get(index);
    }

    public String getDependencyTitle()
    {
        WebElement link = taskCard.findElement(By.cssSelector(".task-card-title a"));
        String href = link.getAttribute("href");
        if (href != null && !href.isEmpty()) {
            String[] parts = href.split("/");
            return parts[parts.length - 1];
        }

        return link.getText().trim();
    }

    public String getDependencyStatus()
    {
        String classes = taskCard.getAttribute("class");

        if (classes.contains("task-card-status-Done")) {
            return "Done";
        } else if (classes.contains("task-card-status-InProgress")) {
            return "InProgress";
        } else if (classes.contains("task-card-status-Late")) {
            return "Late";
        }
        return "Unknown";
    }
}
