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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
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

    private static final String MENTION = "mention";

    private static final String REFERENCE = "reference";

    private static final String VALUE = "value";

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
     * @param assignees the list of strings that will be used to generate a mention macros.
     * @param duedate the date that will be formatted and used to generate a date macro.
     * @param text the message of the task that will precede the assignee and due date.
     * @param storageFormat the format desired for the date.
     * @return a list of blocks that represent the content of a macro.
     * @throws TaskException if the text parameter failed to be parsed.
     */
    public List<Block> generateTaskContentBlocks(List<String> assignees, Date duedate, String text,
        SimpleDateFormat storageFormat) throws TaskException
    {
        XDOM newTaskContentXDOM = null;
        try {
            newTaskContentXDOM = macroUtils.getMacroContentXDOM(
                new MacroBlock("temporaryMacro", new HashMap<>(), text == null ? "" : text, false), Syntax.XWIKI_2_1);
        } catch (MacroExecutionException e) {
            throw new TaskException(String.format("Failed to generate the XDOM for the given content [%s].", text), e);
        }

        List<Block> mentions =
            newTaskContentXDOM.getBlocks(new MacroBlockMatcher(MENTION), Block.Axes.DESCENDANT_OR_SELF);
        Block deadline = newTaskContentXDOM.getFirstBlock(new MacroBlockMatcher(DATE), Block.Axes.DESCENDANT_OR_SELF);

        boolean changed = false;
        changed |= handleMentions(mentions, newTaskContentXDOM, assignees);

        changed |= handleDeadline(deadline, newTaskContentXDOM, duedate, storageFormat);
        return newTaskContentXDOM.getChildren();
    }

    private boolean handleMentions(List<Block> mentions, XDOM newTaskContentXDOM, List<String> assignees)
    {
        if (mentions.isEmpty() && assignees == null) {
            // Nothing changed.
            return false;
        }
        if (assignees == null || assignees.isEmpty()) {
            // Task obj assignees were removed -> remove mentions from content.
            for (Block mention : mentions) {
                mention.getParent().removeBlock(mention);
            }
            return true;
        }

        if (mentions.stream().map(block -> block.getParameter(REFERENCE)).collect(Collectors.toList())
            .equals(assignees))
        {
            // If the mentions and assignees are equal, nothing changed.
            return false;
        }

        int i = 0;
        for (Block mention : mentions) {
            // Replace the existing mentions with the updated values coming from the task obj.
            if (i >= assignees.size()) {
                // If there are more mentions than assignees coming from the task obj, it means that they were removed.
                mention.getParent().removeBlock(mention);
                continue;
            }
            mention.setParameter(REFERENCE, assignees.get(i++));
        }
        // If there are more assignees than mentions, we need to create the said mentions.
        for (int j = i; j < assignees.size(); j++) {
            Map<String, String> mentionParams = new HashMap<>();
            mentionParams.put("style", "FULL_NAME");
            mentionParams.put(REFERENCE, assignees.get(j));
            mentionParams.put("anchor",
                assignees.get(j).replace('.', '-') + '-' + RandomStringUtils.random(5, true, false));
            MacroBlock mentionBlock = new MacroBlock(MENTION, mentionParams, true);
            newTaskContentXDOM.addChild(mentionBlock);
        }
        return true;
    }

    private boolean handleDeadline(Block dateMacro, XDOM newTaskContentXDOM, Date deadlineProp,
        SimpleDateFormat storageFormat)
    {
        if (deadlineProp == null && dateMacro == null) {
            // Nothing changed.
            return false;
        }
        if (deadlineProp == null) {
            // Task obj deadline was removed -> remove date macro from content.
            dateMacro.getParent().removeBlock(dateMacro);
            return true;
        }

        if (dateMacro == null) {
            String serializedDeadlineProp = storageFormat.format(deadlineProp);
            Map<String, String> dateParams = new HashMap<>();
            dateParams.put(VALUE, serializedDeadlineProp);
            MacroBlock dateBlock = new MacroBlock(DATE, dateParams, true);
            newTaskContentXDOM.addChild(dateBlock);
            // Task obj deadline was added -> add date macro to content.
            return true;
        }

        String serializedDeadlineProp = storageFormat.format(deadlineProp);
        if (serializedDeadlineProp.equals(dateMacro.getParameter(VALUE))) {
            // No change. Nothing to do.
            return false;
        } else {
            dateMacro.setParameter(VALUE, serializedDeadlineProp);
            return true;
        }
    }
}
