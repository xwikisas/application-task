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

package com.xwiki.task.internal.macro;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xwiki.task.macro.TaskMacroParameters;

/**
 * Task macro that will facilitate the creation of content-stored tasks inside a xwiki document. Upon saving the
 * document, a task page will be created. The macro and the page will be synced.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Named("task")
@Singleton
public class TaskMacro extends AbstractTaskMacro<TaskMacroParameters>
{
    /**
     * Default constructor.
     */
    public TaskMacro()
    {
        super(TaskMacroParameters.class);
    }

    @Override
    public List<Block> execute(TaskMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return renderTask(parameters, content, context);
    }
}
