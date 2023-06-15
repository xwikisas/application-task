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
package com.xwiki.task.internal;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.syntax.Syntax;

import com.xwiki.task.TaskException;
import com.xwiki.task.model.Task;

/**
 * Class that is used to recursively traverse a {@link XDOM} and look for task macros.
 *
 * @since 3.0.3
 * @version $Id$
 */
@Component(roles = MacroBlockVisitor.class)
@Singleton
public class MacroBlockVisitor
{
    @Inject
    private MacroDescriptorProcessor macroDescriptorProcessor;

    @Inject
    private TaskBlockProcessor taskBlockProcessor;

    @Inject
    private Logger logger;

    /**
     * @param content the content that will be searched recursively for macro tasks macros.
     * @param syntax the syntax of the content.
     * @param function the function that will be executed once a task will be found. It received a
     *     {@link MacroBlock} and returns true if the traversal should stop there or false if it should continue.
     * @return the modified content.
     */
    public XDOM visit(XDOM content, Syntax syntax, Function<MacroBlock, Boolean> function)
    {
        List<MacroBlock> macros = content.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT);
        for (MacroBlock macro : macros) {
            if (Task.MACRO_NAME.equals(macro.getId())) {
                if (function.apply(macro)) {
                    break;
                }
            } else if (macro.getContent() != null && !macro.getContent().isEmpty()
                && this.macroDescriptorProcessor.shouldMacroContentBeParsed(macro.getId()))
            {
                try {
                    XDOM updatedContent =
                        visit(this.taskBlockProcessor.getTaskContentXDOM(macro, syntax), syntax, function);
                    updateMacroContent(macro,
                        taskBlockProcessor.renderTaskContent(updatedContent.getChildren(), syntax));
                } catch (TaskException e) {
                    logger.warn("Failed to update the content of the macro identified by [{}]. Cause: [{}]",
                        macro.getId(), ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        return content;
    }

    /**
     * Update the content of a macro replacing it with a clone that has the desired content.
     *
     * @param macro the macro that needs to be updated. It has to have a parent.
     * @param newContent the new content that will replace the current content of the macro.
     */
    public static void updateMacroContent(MacroBlock macro, String newContent)
    {
        if (macro.getParent() == null) {
            return;
        }
        List<Block> siblings = macro.getParent().getChildren();
        int macroIndex = siblings.indexOf(macro);
        siblings.remove(macroIndex);
        MacroBlock newMacroBlock =
            new MacroBlock(macro.getId(), macro.getParameters(), newContent, macro.isInline());
        siblings.add(macroIndex, newMacroBlock);
    }
}
