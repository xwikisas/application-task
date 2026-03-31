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
 * Represents a KanbanBoard macro and provides access to its columns.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class KanbanBoardMacro extends BaseElement
{
    private final WebElement kanbanBoard;

    public KanbanBoardMacro(WebElement kanbanBoard)
    {
        this.kanbanBoard = kanbanBoard;
    }

    public List<KanbanColumn> getColumns()
    {
        return kanbanBoard.findElements(By.cssSelector(".kanban-board")).stream().map(KanbanColumn::new)
            .collect(Collectors.toList());
    }

    public KanbanColumn getColumn(int index)
    {
        return getColumns().get(index);
    }
}
