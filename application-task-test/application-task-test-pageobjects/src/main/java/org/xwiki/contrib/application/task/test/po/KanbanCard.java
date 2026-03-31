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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a task card from a KanbanBoard macro column and provides access to its attributes.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class KanbanCard extends BaseElement
{
    private final WebElement card;

    public KanbanCard(WebElement card)
    {
        this.card = card;
    }

    public String getTask()
    {
        return card.findElement(By.cssSelector(".kanban-item-title")).getText();
    }

    public String getLink()
    {
        return card.findElement(By.cssSelector(".kanban-item-title")).getAttribute("href");
    }

    public Map<String, String> getFields()
    {
        Map<String, String> fields = new HashMap<>();

        List<WebElement> items = card.findElements(By.cssSelector(".card-field"));

        for (WebElement item : items) {
            String title = item.findElement(By.cssSelector(".card-field-title")).getText();
            String value = item.findElement(By.cssSelector(".card-field-value")).getText();

            fields.put(title, value);
        }

        return fields;
    }

    public String getFieldValue(String fieldName)
    {
        return getFields().get(fieldName);
    }

    public int getProgressPercentage()
    {
        String text = card.findElement(By.cssSelector(".progress-bar span")).getText().trim();

        return Integer.parseInt(text.replace("%", ""));
    }
}
