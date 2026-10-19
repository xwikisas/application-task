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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

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
@Named("checkbox")
@Singleton
public class TaskboxMacro extends AbstractTaskMacro<TaskboxMacroParameters>
{
    /**
     * Default constructor.
     */
    public TaskboxMacro()
    {
        super(TaskboxMacroParameters.class);
    }

    @Override
    public List<Block> execute(TaskboxMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        TaskMacroParameters taskParameters = new TaskMacroParameters();
        taskParameters.setStatus(
            parameters.isChecked() ? Task.STATUS_DONE : getTaskConfiguration().getDefaultInlineStatus());
        taskParameters.setIdDisplayed(TaskMacroParameters.IdDisplay.FALSE);
        taskParameters.setReference(parameters.getId());
        taskParameters.setClassName("taskbox");

        // Render the task directly instead of calling the task macro, so that the content of this macro is editable in
        // place in the WYSIWYG editor.
        return renderTask(taskParameters, content, context);
    }
}
