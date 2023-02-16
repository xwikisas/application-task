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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xwiki.task.TaskException;

/**
 * Class that will handle processing of Task blocks.
 *
 * @version $Id$
 * @since 3.0
 */
@Component(roles = TaskBlockProcessor.class)
@Singleton
public class TaskBlockProcessor
{
    private static final String DATE = "date";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private MacroContentParser contentParser;

    /**
     * Render the task content in xwiki/2.1 syntax.
     *
     * @param taskBlocks the content of a task macro.
     * @param syntax the syntax in which the taskBlocks need to be rendered.
     * @return the result of rendering the content in xwiki/2.1 syntax.
     */
    public String renderTaskContent(List<Block> taskBlocks, String syntax) throws TaskException
    {
        try {
            // Store the macro as wiki syntax inside the task description, so we can display the rendered macro
            // inside the live table.
            BlockRenderer renderer = componentManager.getInstance(BlockRenderer.class, syntax);
            WikiPrinter printer = new DefaultWikiPrinter(new StringBuffer());
            renderer.render(taskBlocks, printer);
            return printer.toString();
        } catch (ComponentLookupException e) {
            throw new TaskException(String.format("Failed to render the task blocks into the syntax [%s].", syntax), e);
        }
    }

    /**
     * Get the XDOM of the contentt of a task macro.
     *
     * @param taskBlock the block whose content we want to retrieve.
     * @param syntax The syntax in which the content is encoded.
     * @return the XDOM of the task content.
     * @throws TaskException if parsing fails.
     */
    public XDOM getTaskContentXDOM(MacroBlock taskBlock, Syntax syntax) throws TaskException
    {
        try {
            MacroTransformationContext macroContext = new MacroTransformationContext();
            macroContext.setCurrentMacroBlock(taskBlock);
            macroContext.setSyntax(syntax);
            return this.contentParser.parse(taskBlock.getContent(), macroContext, true, false);
        } catch (MacroExecutionException e) {
            throw new TaskException(String.format("Failed to parse the task block with id [%s].", taskBlock.getId()),
                e);
        }
    }

    /**
     * Create a link block for a task.
     * @param reference the reference to the task page.
     * @param taskNumber the number of the task, that uniquely identifies it.
     * @return a {@link LinkBlock} that points to the reference and the number of the task as content.
     */
    public Block createTaskLinkBlock(String reference, int taskNumber)
    {
        return new LinkBlock(
            Arrays.asList(new SpecialSymbolBlock('#'), new WordBlock(String.valueOf(taskNumber))),
            new ResourceReference(reference, ResourceType.DOCUMENT),
            false);
    }

    /**
     * Generate the content of a Task macro as a list of blocks. This list can be rendered in different syntaxes i.e.
     * xwiki/2.1.
     *
     * @param assignee the string that will be used to generate a mention macro.
     * @param duedate the date that will be formatted and used to generate a date macro.
     * @param text the message of the task that will precede the assignee and due date.
     * @param storageFormat the format desired for the date.
     * @return a list of blocks that represent the content of a macro.
     * @throws TaskException if the text parameter failed to be parsed.
     */
    public List<Block> generateTaskContentBlocks(String assignee, Date duedate, String text,
        SimpleDateFormat storageFormat) throws TaskException
    {
        Map<String, String> mentionParams = new HashMap<>();
        Map<String, String> dateParams = new HashMap<>();

        mentionParams.put("style", "FULL_NAME");
        mentionParams.put("reference", assignee);
        mentionParams.put("anchor", assignee.replace('.', '-') + '-' + RandomStringUtils.random(5, true, false));

        dateParams.put(DATE, storageFormat.format(duedate));

        MacroBlock mentionBlock = new MacroBlock("mention", mentionParams, true);
        MacroBlock dateBlock = new MacroBlock(DATE, dateParams, true);

        XDOM newTaskContentXDOM =
            getTaskContentXDOM(new MacroBlock("temporaryMacro", new HashMap<>(), text, false),
                Syntax.PLAIN_1_0);

        Block insertionPoint = newTaskContentXDOM.getFirstBlock(new ClassBlockMatcher(ParagraphBlock.class),
            Block.Axes.DESCENDANT_OR_SELF);
        if (insertionPoint == null) {
            insertionPoint = newTaskContentXDOM;
        }
        insertionPoint.addChild(mentionBlock);
        insertionPoint.addChild(dateBlock);

        return newTaskContentXDOM.getChildren();
    }
}
