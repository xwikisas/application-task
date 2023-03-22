package com.xwiki.task.internal;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.internal.job.FilterStreamConverterJob;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.TaskException;
import com.xwiki.task.TaskManager;
import com.xwiki.task.model.Task;

/**
 * Listener that will create/modify the Task pages associated with the TaskMacros inside a page.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Named("com.xwiki.taskmanager.internal.TaskMacroUpdateEventListener")
@Singleton
public class TaskMacroUpdateEventListener extends AbstractTaskEventListener
{
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private JobExecutor executor;

    @Inject
    private DocumentAccessBridge accessBridge;

    @Inject
    private TaskManager taskManager;

    /**
     * Default constructor.
     */
    public TaskMacroUpdateEventListener()
    {
        super(TaskMacroUpdateEventListener.class.getName(), Arrays.asList(new DocumentUpdatingEvent(),
            new DocumentCreatingEvent(), new DocumentDeletingEvent()));
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context, Event event)
    {
        // Skip when inside filter-job because it generates a lot of save events for each version of the imported doc.
        if (executor.getCurrentJob(FilterStreamConverterJob.ROOT_GROUP) != null) {
            return;
        }

        if (event instanceof DocumentDeletingEvent) {
            try {
                taskManager.deleteTasksByOwner(document.getDocumentReference());
            } catch (TaskException e) {
                logger.warn("Failed to delete the tasks that have the current document as owner: [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
            return;
        }
        if (context.get(TASK_UPDATE_FLAG) != null) {
            return;
        }
        XDOM documentContent = document.getXDOM();

        List<Task> tasks = this.taskXDOMProcessor.extract(documentContent, document.getDocumentReference());

        String previousVersion = document.getPreviousVersion();
        List<Task> previousDocTasks = new ArrayList<>();

        if (previousVersion != null) {
            try {
                XWikiDocument previousVersionDoc = revisionProvider.getRevision(document, previousVersion);
                XDOM previousContent = previousVersionDoc.getXDOM();
                previousDocTasks = this.taskXDOMProcessor.extract(previousContent, document.getDocumentReference());
                List<DocumentReference> currentTasksIds =
                    tasks.stream().map(Task::getReference).collect(Collectors.toList());
                previousDocTasks.removeIf(task -> currentTasksIds.contains(task.getReference()));
            } catch (XWikiException e) {
                logger.warn("There was an exception when attempting to remove the task pages associated to the task "
                        + "macros present in the previous version of the document: [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        if (tasks.size() > 0 || previousDocTasks.size() > 0) {
            // TaskExtractor will add new IDs to the tasks if they don't have one, so we need to update the content.
            try {
                document.setContent(documentContent);
            } catch (XWikiException e) {
                logger.warn("Could not update the content of the document [{}]: [{}].",
                    document.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
            }
            context.put(TASK_UPDATE_FLAG, true);
            deleteTaskPages(document, context, previousDocTasks);
            createOrUpdateTaskPages(document, context, tasks);
            context.put(TASK_UPDATE_FLAG, null);
        }
    }

    private void deleteTaskPages(XWikiDocument document, XWikiContext context, List<Task> previousDocTasks)
    {
        for (Task previousDocTask : previousDocTasks) {
            try {
                XWikiDocument taskDoc = context.getWiki().getDocument(previousDocTask.getReference(), context);
                BaseObject taskObj = taskDoc.getXObject(TASK_CLASS_REFERENCE);
                if (taskObj == null || !document.getDocumentReference()
                    .equals(resolver.resolve(taskObj.getLargeStringValue(Task.OWNER), previousDocTask.getReference())))
                {
                    continue;
                }
                if (authorizationManager.hasAccess(Right.DELETE, previousDocTask.getReference())) {
                    context.getWiki().deleteDocument(taskDoc, context);
                } else if (authorizationManager.hasAccess(Right.EDIT, previousDocTask.getReference())) {
                    taskObj.set(Task.OWNER, "", context);
                    context.getWiki().saveDocument(taskDoc, context);
                } else {
                    logger.warn(
                        "The task macro with id [{}] was removed but the associated page could not be deleted or "
                            + "modified because the current user does not have the rights to do so.",
                        previousDocTask.getReference());
                }
            } catch (XWikiException e) {
                logger.warn("Failed to remove the Task Document with id [{}]: [{}].", previousDocTask.getReference(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private void createOrUpdateTaskPages(XWikiDocument document, XWikiContext context, List<Task> tasks)
    {
        for (Task task : tasks) {
            DocumentReference taskReference = task.getReference();
            try {
                if (!authorizationManager.hasAccess(Right.EDIT, taskReference)) {
                    logger.warn(
                        "The user [{}] edited the macro with id [{}] but does not have edit rights over it's "
                            + "corresponding page.",
                        context.getUserReference(), taskReference);
                    continue;
                }

                XWikiDocument taskDoc = context.getWiki().getDocument(taskReference, context).clone();

                BaseObject taskObj = taskDoc.getXObject(TASK_CLASS_REFERENCE, true, context);

                if (!taskDoc.isNew() && !document.getDocumentReference()
                    .equals(resolver.resolve(taskObj.getLargeStringValue(Task.OWNER), taskReference)))
                {
                    continue;
                }

                taskObj.set(Task.OWNER, document.getDocumentReference(), context);

                populateObjectWithMacroParams(context, task, taskObj);

                context.getWiki().saveDocument(taskDoc, "Task updated!", context);
            } catch (XWikiException e) {
                logger.warn("Failed to retrieve the document that contains the Task Object with id [{}]: [{}].",
                    taskReference, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private void populateObjectWithMacroParams(XWikiContext context, Task task, BaseObject object)
    {
        object.set(Task.NAME, task.getName(), context);

        object.set(Task.REPORTER,
            serializer.serialize(task.getReporter() != null ? task.getReporter() : context.getUserReference()),
            context);

        object.set(Task.STATUS, task.getStatus(), context);

        object.set(Task.CREATE_DATE, task.getCreateDate(), context);

        object.set(Task.ASSIGNEE, serializer.serialize(task.getAssignee()), context);

        object.set(Task.DUE_DATE, task.getDueDate(), context);

        object.set(Task.COMPLETE_DATE, task.getCompleteDate(), context);
    }
}
