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
 * Represents a KanbanBoard macro column and provides access to its attributes.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class KanbanColumn extends BaseElement
{
    private final WebElement column;

    public KanbanColumn(WebElement column)
    {
        this.column = column;
    }

    public String getType()
    {
        return column.getAttribute("data-id");
    }

    public List<KanbanCard> getTaskCards()
    {
        return column.findElements(By.cssSelector(".kanban-item")).stream().map(KanbanCard::new)
            .collect(Collectors.toList());
    }

    public String getHeaderColor()
    {
        return column.findElement(By.cssSelector(".kanban-board-header")).getCssValue("background-color");
    }

    public String getWidth()
    {
        String style = column.getAttribute("style");

        for (String part : style.split(";")) {
            part = part.trim();
            if (part.startsWith("width")) {
                return part.split(":")[1].trim();
            }
        }
        return "";
    }
}
