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

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
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
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.task.MacroUtils;
import com.xwiki.task.TaskConfiguration;
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

    private static final String PARAM_REFERENCE = "reference";

    private static final String PARAM_VALUE = "value";

    private static final String PARAM_FORMAT = "format";

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private TaskReferenceUtils taskReferenceUtils;

    @Inject
    private DateMacroConfiguration configuration;

    @Inject
    private Logger logger;

    @Inject
    private MacroBlockFinder blockFinder;

    @Inject
    private MacroUtils macroUtils;

    @Inject
    private TaskConfiguration taskConfiguration;

    @Inject
    @Named("xwiki/2.1")
    private Parser xwikiParser;

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
        return extract(content, contentSource, false);
    }

    /**
     * Extracts the existing Tasks that have a reference from a given XDOM.
     *
     * @param content the XDOM from which one desires to extract or check for the existence of Tasks.
     * @param contentSource the source of the document.
     * @param onlyReferences whether the returned tasks should contain only the references or all the details.
     * @return a list of found Tasks or an empty list if the XDOM didn't contain any valid task. Where a valid task is
     *     one that has an id.
     * @since 3.7.0
     */
    public List<Task> extract(XDOM content, DocumentReference contentSource, boolean onlyReferences)
    {
        List<Task> tasks = new ArrayList<>();
        Syntax syntax = (Syntax) content.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);
        blockFinder.find(content, syntax, (macro) -> {
            if (Task.MACRO_NAME.equals(macro.getId())) {
                String serializedRef = macro.getParameters().get(Task.REFERENCE);
                if (StringUtils.isEmpty(serializedRef)) {
                    return MacroBlockFinder.Lookup.SKIP;
                }
                DocumentReference taskRef = taskReferenceUtils.resolveAsDocumentReference(serializedRef, contentSource);
                if (onlyReferences) {
                    tasks.add(new Task(taskRef));
                    return MacroBlockFinder.Lookup.CONTINUE;
                }
                Task task = initTask(syntax, contentSource, macro);
                if (task == null) {
                    return MacroBlockFinder.Lookup.SKIP;
                }
                task.setReference(taskRef);
                task.setName(macro.getContent());
                tasks.add(task);
            }
            return MacroBlockFinder.Lookup.CONTINUE;
        });
        return tasks;
    }

    /**
     * Parse the content of a document and sync the task macro with a given task object.
     *
     * @param taskObject the task object that will be used to update task macro.
     * @param ownerDoc the document that maybe contains a task macro call that needs updating.
     * @param taskDocument the document that contains the task object.
     * @return the modified content.
     */
    public XDOM updateTaskMacroCall(BaseObject taskObject, XWikiDocument ownerDoc, XWikiDocument taskDocument)
    {
        XDOM content = ownerDoc.getXDOM();
        Syntax syntax = ownerDoc.getSyntax();
        SimpleDateFormat storageFormat = new SimpleDateFormat(configuration.getStorageDateFormat());
        blockFinder.find(content, syntax, (macro) -> {
            if (Task.MACRO_NAME.equals(macro.getId())) {
                if (maybeUpdateTaskMacroCall(ownerDoc, taskObject, taskDocument, storageFormat, macro)) {
                    return MacroBlockFinder.Lookup.BREAK;
                }
                return MacroBlockFinder.Lookup.CONTINUE;
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
                DocumentReference macroRef = taskReferenceUtils.resolveAsDocumentReference(
                    macro.getParameters().getOrDefault(Task.REFERENCE, ""), ownerReference);
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
        Task task = new Task();

        extractBasicProperties(macroParams, task);

        try {

            XDOM macroContent = macroUtils.getMacroContentXDOM(macro, syntax);
            task.setAssignee(extractAssignedUser(macroContent));

            Date deadline = extractDeadlineDate(macroContent);

            task.setDuedate(deadline);
        } catch (MacroExecutionException e) {
            logger.warn("Failed to extract the task with reference [{}] from the content of the page [{}]: [{}].",
                task.getReference(), contentSource, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
        return task;
    }

    private boolean maybeUpdateTaskMacroCall(XWikiDocument ownerDoc, BaseObject taskObject,
        XWikiDocument taskDoc, SimpleDateFormat storageFormat, MacroBlock macro)
    {
        DocumentReference taskRef =
            taskReferenceUtils.resolveAsDocumentReference(macro.getParameters().getOrDefault(Task.REFERENCE, ""),
                ownerDoc.getDocumentReference());
        if (taskRef.equals(taskDoc.getDocumentReference())) {

            setBasicMacroParameters(taskObject, storageFormat, macro);

            XDOM parsedTaskName;
            try {
                parsedTaskName = xwikiParser.parse(new StringReader(taskObject.getStringValue(Task.NAME)));
            } catch (org.xwiki.rendering.parser.ParseException e) {
                logger.warn("Failed to update the task macro identified by [{}]. Cause: [{}].", taskRef,
                    ExceptionUtils.getRootCauseMessage(e));
                return true;
            }

            maybeSetAssignee(taskObject, parsedTaskName);
            maybeSetDeadline(taskObject, parsedTaskName, storageFormat);

            macroUtils.updateMacroContent(macro, taskObject.getStringValue(Task.NAME));

            return true;
        }
        return false;
    }

    private void maybeSetDeadline(BaseObject taskObject, XDOM parsedName, SimpleDateFormat storageFormat)
    {
        Date deadline = taskObject.getDateValue(Task.DUE_DATE);
        MacroBlock dateBlock = parsedName.getFirstBlock(new MacroBlockMatcher(DATE_MACRO_ID),
            Block.Axes.DESCENDANT);
        boolean domChanged = false;
        if (deadline != null) {
            if (dateBlock == null) {
                if (parsedName.getChildren().size() != 1) {
                    dateBlock = new MacroBlock(DATE_MACRO_ID, Collections.emptyMap(), false);
                    parsedName.addChild(dateBlock);
                } else {
                    dateBlock = new MacroBlock(DATE_MACRO_ID, Collections.emptyMap(), true);
                    parsedName.getChildren().get(0).addChild(dateBlock);
                }
            }
            String formattedDeadline = storageFormat.format(deadline);

            String formatParam = dateBlock.getParameter(PARAM_FORMAT);
            if (formatParam != null && !formatParam.isEmpty()) {
                formattedDeadline = new SimpleDateFormat(formatParam).format(deadline);
            }

            String valueParam = dateBlock.getParameter(PARAM_VALUE);
            if (!Objects.equals(valueParam, formattedDeadline)) {
                domChanged = true;
                dateBlock.setParameter(PARAM_VALUE, formattedDeadline);
            }
        } else {
            if (dateBlock != null) {
                domChanged = true;
                dateBlock.getParent().removeBlock(dateBlock);
            }
        }
        if (domChanged) {
            try {
                taskObject.setLargeStringValue(Task.NAME,
                    macroUtils.renderMacroContent(parsedName.getChildren(), Syntax.XWIKI_2_1));
            } catch (ComponentLookupException e) {
                logger.warn("Failed to add date macro with value [{}] in the task name [{}]. Cause: [{}].",
                    deadline, taskObject.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private void maybeSetAssignee(BaseObject taskObject, XDOM parsedName)
    {
        String assignee = taskObject.getLargeStringValue(Task.ASSIGNEE);
        MacroBlock mentionBlock = parsedName.getFirstBlock(new MacroBlockMatcher(MENTION_MACRO_ID),
            Block.Axes.DESCENDANT);
        boolean domChanged = false;
        if (assignee != null && !assignee.isEmpty()) {
            if (mentionBlock == null) {
                if (parsedName.getChildren().size() != 1) {
                    mentionBlock = new MacroBlock(MENTION_MACRO_ID, Collections.emptyMap(), false);
                    parsedName.addChild(mentionBlock);
                } else {
                    mentionBlock = new MacroBlock(MENTION_MACRO_ID, Collections.emptyMap(), true);
                    parsedName.getChildren().get(0).addChild(mentionBlock);
                }
            }
            String parameterAssignee = mentionBlock.getParameter(PARAM_REFERENCE);
            if (!Objects.equals(parameterAssignee, assignee)) {
                domChanged = true;
                mentionBlock.setParameter(PARAM_REFERENCE, assignee);
                mentionBlock.setParameter("anchor",
                    assignee.replace('.', '-') + '-' + RandomStringUtils.random(5, true, false));
            }
        } else {
            if (mentionBlock != null) {
                domChanged = true;
                mentionBlock.getParent().removeBlock(mentionBlock);
            }
        }
        if (domChanged) {
            try {
                taskObject.setLargeStringValue(Task.NAME,
                    macroUtils.renderMacroContent(parsedName.getChildren(), Syntax.XWIKI_2_1));
            } catch (ComponentLookupException e) {
                logger.warn("Failed to add mention to user [{}] in the description of the task [{}]. Cause: [{}].",
                    assignee, taskObject.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
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

        String taskStatus = macroParams.getOrDefault(Task.STATUS, taskConfiguration.getDefaultInlineStatus());
        task.setStatus(taskStatus);

        String strCreateDate = macroParams.getOrDefault(Task.CREATE_DATE, "");
        String strCompletedDate = macroParams.getOrDefault(Task.COMPLETE_DATE, "");

        SimpleDateFormat dateFormat = new SimpleDateFormat(configuration.getStorageDateFormat());

        try {
            Date createDate = dateFormat.parse(strCreateDate);
            task.setCreateDate(createDate);
        } catch (ParseException e) {
            if (!strCreateDate.isEmpty()) {
                logger.warn("Failed to parse the createDate macro parameter [{}]. Expected format is [{}]",
                    strCreateDate, configuration.getStorageDateFormat());
            }
        }

        if (taskStatus.equals(Task.STATUS_DONE)) {
            try {
                Date completeDate = dateFormat.parse(strCompletedDate);
                task.setCompleteDate(completeDate);
            } catch (ParseException e) {
                if (!strCompletedDate.isEmpty()) {
                    logger.warn("Failed to parse the completeDate macro parameter [{}]. Expected format is [{}]",
                        strCreateDate, configuration.getStorageDateFormat());
                }
            }
        }
    }

    private DocumentReference extractAssignedUser(XDOM taskContent)
    {
        MacroBlock macro = taskContent.getFirstBlock(new MacroBlockMatcher(MENTION_MACRO_ID), Block.Axes.DESCENDANT);

        if (macro == null) {
            return null;
        }
        return resolver.resolve(macro.getParameters().get(Task.REFERENCE));
    }

    private Date extractDeadlineDate(XDOM taskContent)
    {
        Date deadline = null;

        MacroBlock macro = taskContent.getFirstBlock(new MacroBlockMatcher(DATE_MACRO_ID), Block.Axes.DESCENDANT);

        if (macro == null) {
            return deadline;
        }

        String dateValue = macro.getParameters().get(PARAM_VALUE);
        try {
            String formatParam = macro.getParameters().get(PARAM_FORMAT);
            deadline = new SimpleDateFormat(formatParam != null && !formatParam.isEmpty() ? formatParam
                : configuration.getStorageDateFormat()).parse(dateValue);
        } catch (ParseException e) {
            logger.warn("Failed to parse the deadline date [{}] of the Task macro! Expected format is [{}]", dateValue,
                configuration.getStorageDateFormat());
        }
        return deadline;
    }
}
