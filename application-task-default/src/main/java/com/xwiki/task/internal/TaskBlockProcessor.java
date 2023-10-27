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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
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
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;

import com.xwiki.task.MacroUtils;
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
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private MacroUtils macroUtils;

    /**
     * Create a link block for a task.
     *
     * @param reference the reference to the task page.
     * @param taskNumber the number of the task, that uniquely identifies it.
     * @return a {@link LinkBlock} that points to the reference and the number of the task as content.
     */
    public Block createTaskLinkBlock(String reference, int taskNumber)
    {
        return createTaskLinkBlock(reference, taskNumber, ResourceType.DOCUMENT);
    }

    /**
     * Create a link block for a task.
     *
     * @param reference the reference to the task page.
     * @param taskNumber the number of the task, that uniquely identifies it.
     * @param resourceType the resource type of the reference.
     * @return a {@link LinkBlock} that points to the reference and the number of the task as content.
     */
    public Block createTaskLinkBlock(String reference, int taskNumber, ResourceType resourceType)
    {
        return new LinkBlock(
            Arrays.asList(new SpecialSymbolBlock('#'), new WordBlock(String.valueOf(taskNumber))),
            new ResourceReference(reference, resourceType),
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
        XDOM newTaskContentXDOM = null;
        try {
            newTaskContentXDOM = macroUtils.getMacroContentXDOM(
                new MacroBlock("temporaryMacro", new HashMap<>(), text == null ? "" : text, false), Syntax.PLAIN_1_0);
        } catch (MacroExecutionException e) {
            throw new TaskException(String.format("Failed to generate the XDOM for the given content [%s].", text), e);
        }

        Block insertionPoint = newTaskContentXDOM.getFirstBlock(new ClassBlockMatcher(ParagraphBlock.class),
            Block.Axes.DESCENDANT_OR_SELF);
        if (insertionPoint == null) {
            insertionPoint = newTaskContentXDOM;
        }

        if (!StringUtils.isEmpty(assignee)) {
            Map<String, String> mentionParams = new HashMap<>();
            mentionParams.put("style", "FULL_NAME");
            mentionParams.put("reference", assignee);
            // TODO: Possible improvement: use the IdGenerator from the XDOM of the document.
            mentionParams.put("anchor", assignee.replace('.', '-') + '-' + RandomStringUtils.random(5, true, false));
            MacroBlock mentionBlock = new MacroBlock("mention", mentionParams, true);
            insertionPoint.addChild(mentionBlock);
        }

        if (duedate != null) {
            Map<String, String> dateParams = new HashMap<>();
            dateParams.put("value", storageFormat.format(duedate));
            MacroBlock dateBlock = new MacroBlock(DATE, dateParams, true);
            insertionPoint.addChild(dateBlock);
        }
        return newTaskContentXDOM.getChildren();
    }
}
