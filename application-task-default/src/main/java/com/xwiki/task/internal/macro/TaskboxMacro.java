package com.xwiki.task.internal.macro;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xwiki.task.MacroUtils;
import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.macro.TaskMacroParameters;
import com.xwiki.task.macro.TaskboxMacroParameters;
import com.xwiki.task.model.Task;

/**
 * This macro displays a checkbox inside the page that is not linked with any other entity other than the content.
 *
 * @version $Id$
 * @since 3.8.0
 */
@Component
@Named("taskbox")
@Singleton
public class TaskboxMacro extends AbstractMacro<TaskboxMacroParameters>
{
    @Inject
    private MacroUtils macroUtils;

    @Inject
    private TaskConfiguration taskConfiguration;

    /**
     * Default constructor.
     */
    public TaskboxMacro()
    {
        super("name", "description", new DefaultContentDescriptor("Content of the task.", false, Block.LIST_BLOCK_TYPE),
            TaskboxMacroParameters.class);
        setDefaultCategories(Collections.singleton(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(TaskboxMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Map<String, String> taskParams = new HashMap<>();

        taskParams.put(Task.STATUS,
            parameters.isChecked() ? Task.STATUS_DONE : taskConfiguration.getDefaultInlineStatus());
        taskParams.put("idDisplayed", TaskMacroParameters.IdDisplay.FALSE.name());
        taskParams.put("reference", parameters.getId());
        taskParams.put("className", "taskbox");

        MacroBlock taskMacroBlock = new MacroBlock("task", taskParams, content, false);
        return Collections.singletonList(taskMacroBlock);
    }
}
