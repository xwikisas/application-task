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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.MacroUtils;
import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.TaskException;
import com.xwiki.task.model.Task;

/**
 * Class that will handle the management of tasks from different mediums.
 *
 * @version $Id$
 * @since 3.0
 */
@Component(roles = TaskXDOMProcessor.class)
@Singleton
public class TaskXDOMProcessor
{
    private static final String DATE_MACRO_ID = "date";

    private static final String MENTION_MACRO_ID = "mention";

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private TaskConfiguration configuration;

    @Inject
    private Logger logger;

    @Inject
    private TaskBlockProcessor taskBlockProcessor;

    @Inject
    private MacroBlockFinder blockFinder;

    @Inject
    private MacroUtils macroUtils;

    /**
     * Extracts the existing Tasks that have a reference from a given XDOM.
     *
     * @param content the XDOM from which one desires to extract or check for the existence of Tasks.
     * @param contentSource the source of the document.
     * @return a list of found Tasks or an empty list if the XDOM didn't contain any valid task. Where a valid task is
     *     one that has an id.
     */
    public List<Task> extract(XDOM content, DocumentReference contentSource)
    {
        List<Task> tasks = new ArrayList<>();
        Syntax syntax =
            (Syntax) content.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);
        blockFinder.find(content, syntax, (macro) -> {
            if (Task.MACRO_NAME.equals(macro.getId())) {
                Task task = initTask(syntax, contentSource, macro);
                if (task == null) {
                    return MacroBlockFinder.Lookup.SKIP;
                }
                tasks.add(task);
            }
            return MacroBlockFinder.Lookup.CONTINUE;
        });
        return tasks;
    }

    /**
     * Parse the content of a document and sync the task macro with a given task object.
     *
     * @param documentReference the reference to the document that contains the task macro that needs updating.
     * @param taskObject the task object that will be used to update task macro.
     * @param content the content of the document that needs parsing.
     * @param syntax the syntax of the document content.
     * @return the modified content.
     */
    public XDOM updateTaskMacroCall(DocumentReference documentReference, BaseObject taskObject, XDOM content,
        Syntax syntax)
    {
        DocumentReference taskDocRef = taskObject.getDocumentReference();
        SimpleDateFormat storageFormat = new SimpleDateFormat(configuration.getStorageDateFormat());
        blockFinder.find(content, syntax, (macro) -> {
            if (Task.MACRO_NAME.equals(macro.getId())) {
                if (maybeUpdateTaskMacroCall(documentReference, taskObject, taskDocRef, content, storageFormat,
                    macro))
                {
                    return MacroBlockFinder.Lookup.BREAK;
                }
                return MacroBlockFinder.Lookup.SKIP;
            }
            return MacroBlockFinder.Lookup.CONTINUE;
        });
        return content;
    }

    /**
     * Remove the task macro call that has the given reference.
     *
     * @param taskReference the reference that identifies the task macro.
     * @param ownerReference the reference that identifies the document in which the task reference resides.
     * @param docContent the XDOM of the document in which the task reference resides.
     * @param syntax the syntax of the document that contain the task macro.
     * @return the modified XDOM on the document.
     */
    public XDOM removeTaskMacroCall(DocumentReference taskReference, DocumentReference ownerReference, XDOM docContent,
        Syntax syntax)
    {
        this.blockFinder.find(docContent, syntax, (macro) -> {
            if (Task.MACRO_NAME.equals(macro.getId())) {
                DocumentReference macroRef =
                    resolver.resolve(macro.getParameters().getOrDefault(Task.REFERENCE, ""), ownerReference);
                if (macroRef.equals(taskReference)) {
                    List<Block> siblings = macro.getParent().getChildren();
                    siblings.remove(macro);
                    return MacroBlockFinder.Lookup.BREAK;
                }
                return MacroBlockFinder.Lookup.SKIP;
            }
            return MacroBlockFinder.Lookup.CONTINUE;
        });
        return docContent;
    }

    private Task initTask(Syntax syntax, DocumentReference contentSource, MacroBlock macro)
    {
        Map<String, String> macroParams = macro.getParameters();
        String taskReference = macroParams.get(Task.REFERENCE);
        Task task = new Task();

        if (StringUtils.isEmpty(taskReference)) {
            return null;
        }
        task.setReference(resolver.resolve(taskReference, contentSource));
        extractBasicProperties(macroParams, task);

        try {

            XDOM macroContent = macroUtils.getMacroContentXDOM(macro, syntax);
            task.setName(macroUtils.renderMacroContent(macroContent.getChildren(), Syntax.PLAIN_1_0));
            task.setAssignee(extractAssignedUser(macroContent));

            Date deadline = extractDeadlineDate(macroContent);

            task.setDuedate(deadline);
        } catch (MacroExecutionException | ComponentLookupException e) {
            logger.warn("Failed to extract the task with reference [{}] from the content of the page [{}]: [{}].",
                task.getReference(), contentSource, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
        return task;
    }

    private boolean maybeUpdateTaskMacroCall(DocumentReference documentReference, BaseObject taskObject,
        DocumentReference taskDocRef, XDOM content, SimpleDateFormat storageFormat, MacroBlock macro)
    {
        DocumentReference taskRef =
            resolver.resolve(macro.getParameters().getOrDefault(Task.REFERENCE, ""), documentReference);
        if (taskRef.equals(taskDocRef)) {

            setBasicMacroParameters(taskObject, storageFormat, macro);

            try {
                Syntax syntax =
                    (Syntax) content.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);

                List<Block> newTaskContentBlocks = taskBlockProcessor.generateTaskContentBlocks(
                    taskObject.getLargeStringValue(Task.ASSIGNEE),
                    taskObject.getDateValue(Task.DUE_DATE),
                    taskObject.getStringValue(Task.NAME), storageFormat
                );

                String newContent = macroUtils.renderMacroContent(newTaskContentBlocks, syntax);

                macroUtils.updateMacroContent(macro, newContent);
            } catch (ComponentLookupException | TaskException e) {
                logger.warn("Failed to update the task macro call for the task with reference [{}]: [{}].",
                    taskDocRef, ExceptionUtils.getRootCauseMessage(e));
            }
            return true;
        }
        return false;
    }

    private void setBasicMacroParameters(BaseObject taskObject, SimpleDateFormat storageFormat, MacroBlock macro)
    {
        String taskStatus = taskObject.getStringValue(Task.STATUS);
        if (taskStatus.equals(Task.STATUS_DONE)) {
            Date completeDate = taskObject.getDateValue(Task.COMPLETE_DATE);
            macro.setParameter(Task.COMPLETE_DATE,
                storageFormat.format(completeDate != null ? completeDate : new Date()));
        } else {
            taskObject.setDateValue(Task.COMPLETE_DATE, null);
            macro.setParameter(Task.COMPLETE_DATE, "");
        }
        Date createDate = taskObject.getDateValue(Task.CREATE_DATE);
        if (createDate == null) {
            createDate = new Date();
            taskObject.setDateValue(Task.CREATE_DATE, createDate);
        }
        macro.setParameter(Task.CREATE_DATE, storageFormat.format(createDate));
        macro.setParameter(Task.STATUS, taskObject.getStringValue(Task.STATUS));
        macro.setParameter(Task.REPORTER, taskObject.getLargeStringValue(Task.REPORTER));
    }

    private void extractBasicProperties(Map<String, String> macroParams, Task task)
    {
        String reporter = macroParams.getOrDefault(Task.REPORTER, "");
        if (!reporter.isEmpty()) {
            task.setReporter(resolver.resolve(reporter));
        }

        String taskStatus = macroParams.getOrDefault(Task.STATUS, Task.STATUS_IN_PROGRESS);
        task.setStatus(taskStatus);

        String strCreateDate = macroParams.getOrDefault(Task.CREATE_DATE, "");
        String strCompletedDate = macroParams.getOrDefault(Task.COMPLETE_DATE, "");

        SimpleDateFormat dateFormat = new SimpleDateFormat(configuration.getStorageDateFormat());

        Date createDate;
        try {
            createDate = dateFormat.parse(strCreateDate);
        } catch (ParseException e) {
            if (!strCreateDate.isEmpty()) {
                logger.warn("Failed to parse the createDate macro parameter [{}]. Expected format is [{}]",
                    strCreateDate, configuration.getStorageDateFormat());
            }
            createDate = new Date();
        }
        task.setCreateDate(createDate);

        Date completeDate = null;
        if (taskStatus.equals(Task.STATUS_DONE)) {
            try {
                completeDate = dateFormat.parse(strCompletedDate);
            } catch (ParseException e) {
                if (!strCompletedDate.isEmpty()) {
                    logger.warn("Failed to parse the completeDate macro parameter [{}]. Expected format is [{}]",
                        strCreateDate, configuration.getStorageDateFormat());
                }
                completeDate = new Date();
            }
        }
        task.setCompleteDate(completeDate);
    }

    private DocumentReference extractAssignedUser(XDOM taskContent)
    {
        MacroBlock macro =
            taskContent.getFirstBlock(new MacroBlockMatcher(MENTION_MACRO_ID), Block.Axes.DESCENDANT);

        if (macro == null) {
            return null;
        }
        return resolver.resolve(macro.getParameters().get(Task.REFERENCE));
    }

    private Date extractDeadlineDate(XDOM taskContent)
    {
        Date deadline = null;

        MacroBlock macro =
            taskContent.getFirstBlock(new MacroBlockMatcher(DATE_MACRO_ID), Block.Axes.DESCENDANT);

        if (macro == null) {
            return deadline;
        }

        String dateValue = macro.getParameters().get("value");
        try {
            String formatParam = macro.getParameters().get("format");
            deadline = new SimpleDateFormat(formatParam != null && !formatParam.isEmpty() ? formatParam
                : configuration.getStorageDateFormat()).parse(dateValue);
        } catch (ParseException e) {
            logger.warn("Failed to parse the deadline date [{}] of the Task macro! Expected format is [{}]",
                dateValue, configuration.getStorageDateFormat());
        }
        return deadline;
    }
}
