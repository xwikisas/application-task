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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
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
    private static final String EXCEPTION_DOCUMENT_RETRIEVAL = "Could not retrieve the document [{}]. Cause:";

    private static final String EXCEPTION_TASK_PAGES_UPDATE =
        "Something went wrong during the updating of the task pages bound to the task macros present in [{}].";

    /**
     * The execution context property holding the last document saved during the current fold event, which still has
     * to be processed. It is kept in the execution context (and not in a field of this singleton) so that concurrent
     * fold events, executed by different jobs, don't process each other's documents.
     */
    private static final String PENDING_FOLD_DOCUMENT = "taskMacroUpdate.pendingFoldDocument";

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private TaskManager taskManager;

    @Inject
    private Execution execution;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Default constructor.
     */
    public TaskMacroUpdateEventListener()
    {
        super(TaskMacroUpdateEventListener.class.getName(), Arrays.asList(new DocumentUpdatingEvent(),
            new DocumentCreatingEvent(), new DocumentDeletingEvent(), new JobFinishedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Fold events such as the filter conversion (used by imports and migrations) are executed inside jobs. When
        // the job finishes, process the last document that was saved during the fold event.
        if (event instanceof JobFinishedEvent) {
            processPendingFoldDocument(contextProvider.get());
            return;
        }
        super.onEvent(event, source, data);
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context, Event event, boolean inFoldEvent)
    {
        // Skip task pages. They will be handled by TaskObjectUpdateEventListener.
        if (document.getXObject(TASK_CLASS_REFERENCE) != null) {
            return;
        }
        // If the flag is set, it means that the listener was triggered as a result of a save made by
        // TaskObjectUpdateEventListener which updated some macro calls. Skip the execution.
        if (context.get(TASK_UPDATE_FLAG) != null) {
            return;
        }

        // The handling of the fold event needs not be wrapped in a recursion check so it will get executed.
        if (maybeHandleFoldEvent(document, context, inFoldEvent)) {
            return;
        }

        context.put(TASK_UPDATE_FLAG, true);
        try {
            if (event instanceof DocumentDeletingEvent) {
                taskManager.deleteTasksByOwner(document.getDocumentReference());
                return;
            }
            updateTaskPages(document, context);
        } catch (TaskException e) {
            logger.error("Failed to delete the tasks that have the current document as owner:", e);
        } catch (Exception e) {
            logger.error(EXCEPTION_TASK_PAGES_UPDATE, document.getDocumentReference(), e);
        } finally {
            context.put(TASK_UPDATE_FLAG, null);
        }
    }

    private boolean maybeHandleFoldEvent(XWikiDocument document, XWikiContext context, boolean inFoldEvent)
    {
        // If inside a fold event, ignore the various versions of the same document and process only the last revision.
        // A document is processed when a different document is saved or when the job that executes the fold event
        // finishes.
        if (inFoldEvent) {
            AtomicReference<DocumentReference> pendingDocument = getPendingFoldDocument();
            if (pendingDocument != null) {
                DocumentReference previousDocument = pendingDocument.getAndSet(document.getDocumentReference());
                if (previousDocument != null && !previousDocument.equals(document.getDocumentReference())) {
                    processFoldDocument(previousDocument, context);
                }
            } else {
                // No execution context to remember the document in, so process it right away.
                processFoldDocument(document, context);
            }
            return true;
        }
        return false;
    }

    private void processPendingFoldDocument(XWikiContext context)
    {
        ExecutionContext executionContext = execution.getContext();
        if (executionContext == null || context == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        AtomicReference<DocumentReference> pendingDocument =
            (AtomicReference<DocumentReference>) executionContext.getProperty(PENDING_FOLD_DOCUMENT);
        if (pendingDocument == null) {
            return;
        }
        DocumentReference documentReference = pendingDocument.getAndSet(null);
        if (documentReference != null) {
            processFoldDocument(documentReference, context);
        }
    }

    @SuppressWarnings("unchecked")
    private AtomicReference<DocumentReference> getPendingFoldDocument()
    {
        ExecutionContext executionContext = execution.getContext();
        if (executionContext == null) {
            return null;
        }
        if (!executionContext.hasProperty(PENDING_FOLD_DOCUMENT)) {
            // Inherited, so that the contexts pushed during the fold event share the same pending document.
            executionContext.newProperty(PENDING_FOLD_DOCUMENT).inherited()
                .initial(new AtomicReference<DocumentReference>()).declare();
        }
        return (AtomicReference<DocumentReference>) executionContext.getProperty(PENDING_FOLD_DOCUMENT);
    }

    private void processFoldDocument(DocumentReference documentReference, XWikiContext context)
    {
        try {
            processFoldDocument(context.getWiki().getDocument(documentReference, context), context);
        } catch (XWikiException e) {
            logger.warn(EXCEPTION_DOCUMENT_RETRIEVAL, documentReference, e);
        }
    }

    private void processFoldDocument(XWikiDocument document, XWikiContext context)
    {
        // Set the flag so that the saves of the task pages don't trigger this listener again.
        context.put(TASK_UPDATE_FLAG, true);
        try {
            updateTaskPages(document, context);
        } catch (Exception e) {
            logger.error(EXCEPTION_TASK_PAGES_UPDATE, document.getDocumentReference(), e);
        } finally {
            context.put(TASK_UPDATE_FLAG, null);
        }
    }

    private void updateTaskPages(XWikiDocument document, XWikiContext context)
    {
        XDOM documentContent = document.getXDOM();

        List<Task> tasks = this.taskXDOMProcessor.extract(documentContent, document.getDocumentReference());

        List<Task> previousDocTasks = Collections.emptyList();

        if (document.getOriginalDocument() != null) {
            XWikiDocument previousVersionDoc = document.getOriginalDocument();
            XDOM previousContent = previousVersionDoc.getXDOM();
            previousDocTasks = this.taskXDOMProcessor.extract(previousContent, document.getDocumentReference(), true);
            List<DocumentReference> currentTasksIds =
                tasks.stream().map(Task::getReference).collect(Collectors.toList());
            previousDocTasks.removeIf(task -> currentTasksIds.contains(task.getReference()));
        }
        if (!tasks.isEmpty() || !previousDocTasks.isEmpty()) {
            deleteTaskPages(document, context, previousDocTasks);
            createOrUpdateTaskPages(document, context, tasks);
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
                logger.error("Failed to remove the Task Document with id [{}]:",
                    previousDocTask.getReference(), e);
            }
        }
    }

    private void createOrUpdateTaskPages(XWikiDocument document, XWikiContext context, List<Task> tasks)
    {
        for (Task task : tasks) {
            DocumentReference taskReference = task.getReference();
            try {
                // Create/Update the task page only if it's a child of `Parent.Tasks` or if the user has edit rights
                // over it.
                if (!isChildOfTasksSubspace(taskReference, document.getDocumentReference())
                    && !authorizationManager.hasAccess(Right.EDIT, taskReference))
                {
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
                boolean docChanged = maybeUpdateTaskDoc(document, context, task, taskObj, taskDoc, taskReference);

                if (docChanged) {
                    context.getWiki().saveDocument(taskDoc, "Task updated!", context);
                }
            } catch (XWikiException e) {
                logger.error("Failed to retrieve the document that contains the Task Object with id [{}]:",
                    taskReference, e);
            }
        }
    }

    private boolean maybeUpdateTaskDoc(XWikiDocument document, XWikiContext context, Task task, BaseObject taskObj,
        XWikiDocument taskDoc, DocumentReference taskReference)
    {
        BaseObject clonedObj = taskObj.clone();
        UserReference currentUser = document.getAuthors().getContentAuthor();
        taskDoc.getAuthors().setOriginalMetadataAuthor(currentUser);
        clonedObj.set(Task.OWNER, serializer.serialize(document.getDocumentReference(), taskReference),
            context);
        populateObjectWithMacroParams(context, task, clonedObj);
        boolean docChanged = !clonedObj.getDiff(taskObj, context).isEmpty();
        if (docChanged) {
            taskDoc.setXObject(taskObj.getNumber(), clonedObj);
        }
        if (taskDoc.isNew()) {
            taskDoc.setHidden(true);
            taskDoc.getAuthors().setCreator(currentUser);
            taskDoc.getAuthors().setEffectiveMetadataAuthor(currentUser);
        }
        return docChanged;
    }

    private boolean isChildOfTasksSubspace(EntityReference possibleChild, DocumentReference possibleParent)
    {
        SpaceReference expectedParent = new SpaceReference("Tasks", possibleParent.getLastSpaceReference());
        return possibleChild.hasParent(expectedParent);
    }

    private void populateObjectWithMacroParams(XWikiContext context, Task task, BaseObject object)
    {
        object.set(Task.NAME, task.getName(), context);

        object.set(Task.DESCRIPTION, task.getDescription(), context);

        object.set(Task.REPORTER, serializer.serialize(task.getReporter()), context);

        object.set(Task.STATUS, task.getStatus(), context);

        object.set(Task.CREATE_DATE, task.getCreateDate(), context);

        String assignees =
            task.getAssignees().stream().map(user -> serializer.serialize(user))
                .collect(Collectors.joining(","));
        object.set(Task.ASSIGNEE, assignees, context);

        object.set(Task.DUE_DATE, task.getDueDate(), context);

        object.set(Task.COMPLETE_DATE, task.getCompleteDate(), context);
    }
}
