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
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.event.EntitiesRenamingEvent;
import org.xwiki.refactoring.internal.job.MoveJob;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.question.EntitySelection;

/**
 * Listener responsible with making sure that the task pages are always moved together with their owners.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Singleton
@Named("TaskPageMovingEventListener")
public class TaskPageMovingEventListener extends AbstractEventListener
{
    private static final String MOVE_FLAG = "shouldMoveTasks";

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public TaskPageMovingEventListener()
    {
        super("TaskPageMovingEventListener", List.of(new EntitiesRenamingEvent(), new DocumentsDeletingEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof EntitiesRenamingEvent) {
            handleMoveEvent((EntitiesRenamingEvent) event, (MoveJob) source, (MoveRequest) data);
        } else {
            handleDocumentsDeletingEvent(source, (Map<EntityReference, EntitySelection>) data);
        }
    }

    private static void handleDocumentsDeletingEvent(Object source,
        Map<EntityReference, EntitySelection> concernedEntities)
    {
        // If the move job was previously processed by this listener (is a not deep job for a page that is owner of
        // some task pages) we need to only move the initially moved page and the Tasks subspace.
        if (!(source instanceof MoveJob)) {
            return;
        }
        MoveJob moveJob = (MoveJob) source;

        if (!moveJob.getRequest().containsProperty(MOVE_FLAG)) {
            return;
        }
        Optional<EntityReference> movedParentPage = moveJob.getRequest().getEntityReferences().stream().findFirst();
        if (movedParentPage.isEmpty()) {
            return;
        }
        SpaceReference tasksSpace = new SpaceReference("Tasks", movedParentPage.get().getParent());

        for (Map.Entry<EntityReference, EntitySelection> entry : concernedEntities.entrySet()) {
            if (entry.getKey().hasParent(tasksSpace)) {
                entry.getValue().setSelected(true);
            } else {
                entry.getValue().setSelected(false);
            }
        }

        concernedEntities.get(movedParentPage.get()).setSelected(true);
    }

    private void handleMoveEvent(EntitiesRenamingEvent event, MoveJob source, MoveRequest moveRequest)
    {
        // Detect if the created document is inside a Renaming Job that is NOT deep (the children are not moved).
        // If that's the case, set the job as deep and move only the children under the Tasks space.
        if (moveRequest.isDeep()) {
            return;
        }
        Optional<EntityReference> movedParentPage = moveRequest.getEntityReferences().stream().findFirst();
        if (movedParentPage.isEmpty()) {
            return;
        }
        String statement = "from doc.object(TaskManager.TaskManagerClass) AS task WHERE task.owner like :page";
        try {
            List<String> childPages = queryManager.createQuery(statement, Query.XWQL)
                .bindValue("page", serializer.serialize(movedParentPage.get())).setLimit(1).execute();

            if (!childPages.isEmpty()) {
                // If the request was not initially deep but there are task pages that should be moved, set the
                // request to deep and, during the DocumentsDeletingEvent, exclude all the pages and include the
                // Tasks space.
                moveRequest.setProperty(MOVE_FLAG, true);
                moveRequest.setDeep(true);
            }
        } catch (QueryException e) {
            logger.warn("Failed to retrieve the task pages that have [{}] as owner. Cause: [{}].",
                movedParentPage.get(), ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
