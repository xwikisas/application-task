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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.TaskException;
import com.xwiki.task.TaskReferenceGenerator;
import com.xwiki.task.model.Task;

/**
 * Class that will handle the retrieval of tasks from different mediums.
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
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private TaskConfiguration configuration;

    @Inject
    private Logger logger;

    @Inject
    private TaskReferenceGenerator taskRefGenerator;

    @Inject
    private TaskBlockProcessor taskBlockProcessor;

    /**
     * Extracts the existing Tasks from a given XDOM.
     *
     * @param content the XDOM from which one desires to extract or check for the existence of Tasks.
     * @param contentSource the source of the document.
     * @return a list of found Tasks or an empty list if the XDOM didn't contain any valid task. Where a valid task is
     *     one that has an id.
     */
    public List<Task> extract(XDOM content, DocumentReference contentSource)
    {
        List<MacroBlock> macros = content.getBlocks(new MacroBlockMatcher(Task.MACRO_NAME), Block.Axes.DESCENDANT);
        List<Task> tasks = new ArrayList<>();

        for (MacroBlock macro : macros) {
            Map<String, String> macroParams = macro.getParameters();
            String macroId = macroParams.get(Task.REFERENCE);
            DocumentReference taskReference;
            Task task = new Task();

            if (macroId == null) {
                try {
                    taskReference = taskRefGenerator.generate(contentSource);
                } catch (TaskException e) {
                    logger.warn("Failed to extract a task from the page [{}]. Cause: [{}].", contentSource,
                        ExceptionUtils.getRootCauseMessage(e));
                    continue;
                }
                macro.setParameter(Task.REFERENCE, serializer.serialize(taskReference, contentSource));
                task.setOwner(contentSource);
            } else {
                taskReference = resolver.resolve(macroId, contentSource);
            }

            extractBasicProperties(macroParams, taskReference, task);

            Syntax syntax =
                (Syntax) content.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);

            try {

                XDOM macroContent = taskBlockProcessor.getTaskContentXDOM(macro, syntax);
                task.setName(
                    taskBlockProcessor.renderTaskContent(macroContent.getChildren(), Syntax.PLAIN_1_0));
                task.setAssignee(extractAssignedUser(macroContent));

                Date deadline = extractDeadlineDate(macroContent);

                task.setDuedate(deadline);
            } catch (TaskException e) {
                logger.warn("Failed to extract the task with reference [{}] from the content of the page [{}]: [{}].",
                    taskReference, contentSource, ExceptionUtils.getRootCauseMessage(e));
                continue;
            }

            tasks.add(task);
        }
        return tasks;
    }

    /**
     * Parse the content of a document and sync the task macro with a given task object.
     *
     * @param documentReference the reference to the document that contains the task macro that needs updating.
     * @param taskObject the task object that will be used to update task macro.
     * @param context the current context.
     */
    public void updateTaskMacroCall(DocumentReference documentReference, BaseObject taskObject, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument ownerDocument = context.getWiki().getDocument(documentReference, context).clone();
        if (ownerDocument.isNew()) {
            return;
        }
        DocumentReference taskDocRef = taskObject.getDocumentReference();
        XDOM content = ownerDocument.getXDOM();
        List<MacroBlock> macros = content.getBlocks(new MacroBlockMatcher(Task.MACRO_NAME), Block.Axes.DESCENDANT);
        SimpleDateFormat storageFormat = new SimpleDateFormat(configuration.getStorageDateFormat());
        for (MacroBlock macro : macros) {
            DocumentReference taskRef =
                resolver.resolve(macro.getParameters().getOrDefault(Task.REFERENCE, ""), documentReference);
            if (taskRef.equals(taskDocRef)) {

                setBasicMacroParameters(taskObject, storageFormat, macro);

                try {
                    List<Block> siblings = macro.getParent().getChildren();
                    int macroIndex = siblings.indexOf(macro);
                    siblings.remove(macroIndex);
                    Syntax syntax =
                        (Syntax) content.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);

                    List<Block> newTaskContentBlocks = taskBlockProcessor.generateTaskContentBlocks(
                        taskObject.getLargeStringValue(Task.ASSIGNEE),
                        taskObject.getDateValue(Task.DUE_DATE),
                        taskObject.getStringValue(Task.NAME), storageFormat
                    );

                    String newContent = taskBlockProcessor.renderTaskContent(newTaskContentBlocks, syntax);

                    MacroBlock newMacroBlock =
                        new MacroBlock(macro.getId(), macro.getParameters(), newContent, macro.isInline());
                    siblings.add(macroIndex, newMacroBlock);
                } catch (TaskException e) {
                    logger.warn("Failed to update the task macro call for the task with reference [{}]: [{}].",
                        taskDocRef, ExceptionUtils.getRootCauseMessage(e));
                }
                break;
            }
        }
        ownerDocument.setContent(content);
        context.getWiki()
            .saveDocument(ownerDocument, String.format("Task [%s] has been updated!", taskDocRef), context);
    }

    /**
     * Remove the task macro call that has the given reference.
     *
     * @param taskReference the reference that identifies the task macro.
     * @param location the location of the task macro.
     * @param context the current context.
     */
    public void removeTaskMacroCall(DocumentReference taskReference, DocumentReference location,
        XWikiContext context)
    {
        try {
            XWikiDocument hostDocument = context.getWiki().getDocument(location, context);
            XDOM docContent = hostDocument.getXDOM();
            List<MacroBlock> macros =
                docContent.getBlocks(new MacroBlockMatcher(Task.MACRO_NAME), Block.Axes.DESCENDANT);
            for (MacroBlock macro : macros) {
                DocumentReference macroRef = resolver.resolve(macro.getParameters().getOrDefault(Task.REFERENCE, ""));
                if (macroRef.equals(taskReference)) {
                    List<Block> siblings = macro.getParent().getChildren();
                    siblings.remove(macro);
                    hostDocument.setContent(docContent);
                    context.getWiki().saveDocument(hostDocument,
                        String.format("Removed the task with the reference of [%s]", taskReference), context);
                    break;
                }
            }
        } catch (XWikiException e) {
            logger.warn("Failed to remove macro calls from the document [{}]: [{}].", location,
                ExceptionUtils.getRootCauseMessage(e));
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
        }
        Date createDate = taskObject.getDateValue(Task.CREATE_DATE);
        if (createDate == null) {
            createDate = new Date();
            taskObject.setDateValue(Task.CREATE_DATE, createDate);
        }
        macro.setParameter(Task.CREATE_DATE, storageFormat.format(createDate));
        macro.setParameter(Task.STATUS, taskObject.getStringValue(Task.STATUS));
        macro.setParameter(Task.REPORTER, taskObject.getStringValue(Task.REPORTER));
    }

    private void extractBasicProperties(Map<String, String> macroParams, DocumentReference macroId, Task task)
    {
        task.setReference(macroId);

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
            logger.warn("Failed to parse the createDate macro parameter [{}]. Expected format is [{}]",
                strCreateDate, configuration.getStorageDateFormat());
            createDate = new Date();
        }
        task.setCreateDate(createDate);

        Date completeDate = null;
        if (taskStatus.equals(Task.STATUS_DONE)) {
            try {
                completeDate = strCompletedDate.isEmpty() ? new Date() : dateFormat.parse(strCompletedDate);
            } catch (ParseException e) {
                logger.warn("Failed to parse the completeDate macro parameter [{}]. Expected format is [{}]",
                    strCreateDate, configuration.getStorageDateFormat());
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
        Date deadline = new Date();

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
