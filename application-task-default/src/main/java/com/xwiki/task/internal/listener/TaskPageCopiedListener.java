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
package com.xwiki.task.internal.listener;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentCopyingEvent;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.TaskCounter;
import com.xwiki.task.TaskException;
import com.xwiki.task.model.Task;

import static com.xwiki.task.internal.AbstractTaskEventListener.TASK_CLASS_REFERENCE;

/**
 * This listener handles the case when a page containing a task object is copied or a page containing task macros was
 * copied. The id of the task object should be updated to a new one and the owner should be updated.
 *
 * @version $Id$
 * @since 3.10.0
 */
@Component
@Named("TaskPageCopiedListener")
@Singleton
public class TaskPageCopiedListener extends AbstractEventListener
{
    private static final String WEBHOME = "WebHome";

    private static final String TASKS = "Tasks";

    @Inject
    protected ObservationContext observationContext;

    @Inject
    protected TaskCounter taskCounter;

    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /**
     * Default constructor.
     */
    public TaskPageCopiedListener()
    {
        super("TaskPageCopiedListener", Collections.singletonList(new DocumentCreatingEvent()));
    }

    /**
     * Default constructor.
     *
     * @param name the name of the listener.
     * @param events the list of events that this listener watches.
     */
    public TaskPageCopiedListener(String name, List<? extends Event> events)
    {
        super(name, events);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;
        XWikiDocument document = ((XWikiDocument) source);
        logger.debug("Processing [{}].", document.getDocumentReference());

        // This listener handles only the copying of Task pages.
        if (!observationContext.isIn(otherEvent -> otherEvent instanceof DocumentCopyingEvent)) {
            logger.debug("Document [{}] was not created in a copying or renaming event. Returning.",
                document.getDocumentReference());
            return;
        }
        BaseObject taskObj = document.getXObject(TASK_CLASS_REFERENCE);
        if (taskObj == null) {
            logger.debug("Created document [{}] does not contain a task object.", document.getDocumentReference());
            return;
        }

        maybeSetNewId(taskObj, context, document);

        maybeSetNewOwner(taskObj, context);
    }

    protected boolean maybeSetNewId(BaseObject taskObj, XWikiContext context, XWikiDocument document)
    {
        boolean changed = false;
        try {
            int newNumber = taskCounter.getNextNumber();
            logger.debug("Task doc [{}] was copied. Changing the id of the copied instance from [{}] to [{}].",
                taskObj.getDocumentReference(), taskObj.getIntValue(Task.NUMBER), newNumber);
            taskObj.set(Task.NUMBER, newNumber, context);
            changed = true;
        } catch (TaskException e) {
            logger.warn("Failed to set a new id to the copied task document [{}]. Cause: [{}].",
                document.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
        }
        return changed;
    }

    protected boolean maybeSetNewOwner(BaseObject taskObj, XWikiContext context)
    {
        // We are not interested in task pages that don't have owners.
        if (taskObj.getLargeStringValue(Task.OWNER).isEmpty()) {
            logger.debug("Task doc [{}] doesn't have an owner.", taskObj.getDocumentReference());
            return false;
        }
        // If the Task Page was copied/moved together with its owner, the owner ref was changed so we should update
        // the object field that stores that information.
        Optional<EntityReference> movedParentPage = getMovedOrCopiedEntity();
        if (!movedParentPage.isPresent()) {
            logger.debug("Task page [{}] was not moved as part of a move/copy job.", taskObj.getDocumentReference());
            return false;
        }
        // If a task page is moved through the UI, we shouldn't update the owner.
        // If a "Tasks" space is moved, we don't need to update the owner.
        if (isTaskPageMoved(taskObj, movedParentPage)) {
            logger.debug(
                "Task page [{}] was created as a result of moving/copying either a task page or Tasks subspace [{}].",
                taskObj.getDocumentReference(), movedParentPage.get());
            return false;
        }

        try {
            EntityReference ownerName = taskObj.getDocumentReference().getName().equals(WEBHOME)
                ? taskObj.getDocumentReference().getParent() : taskObj.getDocumentReference();

            // Update the owner if the created task was a child of a Tasks subspace.
            if (ownerName.getParent() != null && ownerName.getParent().getName().equals(TASKS)) {
                String newOwner = serializer.serialize(
                    new DocumentReference(WEBHOME, (SpaceReference) ownerName.getParent().getParent()));
                logger.debug("Setting new owner for [{}] as [{}].", taskObj.getDocumentReference(), newOwner);
                taskObj.set(Task.OWNER, newOwner, context);
                return true;
            }
            logger.debug(
                "The moved/copied task page [{}] is not part of the Tasks subspace. The owner will not be updated.",
                taskObj.getDocumentReference());
        } catch (Exception e) {
            logger.error("Failed to set the new owner for the task page [{}].", taskObj.getDocumentReference(), e);
        }
        return false;
    }

    private boolean isTaskPageMoved(BaseObject taskObj, Optional<EntityReference> movedParentPage)
    {
        if (movedParentPage.get().equals(taskObj.getDocumentReference())) {
            return true;
        }
        if (movedParentPage.get().getName().equals(TASKS)
            || (movedParentPage.get().getParent() != null && movedParentPage.get().getParent().getName().equals(TASKS)))
        {
            return true;
        }
        return false;
    }

    private Optional<EntityReference> getMovedOrCopiedEntity()
    {
        AtomicReference<JobStartedEvent> jobStartedEvent = new AtomicReference<>();
        if (!observationContext.isIn(otherEvent -> {
            if (otherEvent instanceof JobStartedEvent) {
                jobStartedEvent.set((JobStartedEvent) otherEvent);
                return true;
            }
            return false;
        }))
        {
            return Optional.empty();
        }

        if (!(jobStartedEvent.get().getRequest() instanceof AbstractCopyOrMoveRequest)) {
            return Optional.empty();
        }
        AbstractCopyOrMoveRequest copyOrMoveRequest = ((AbstractCopyOrMoveRequest) jobStartedEvent.get().getRequest());
        return copyOrMoveRequest.getEntityReferences().stream().findFirst();
    }
}
