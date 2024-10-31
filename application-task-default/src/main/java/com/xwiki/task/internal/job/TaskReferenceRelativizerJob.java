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
package com.xwiki.task.internal.job;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xwiki.task.event.TaskRelativizedEvent;
import com.xwiki.task.event.TaskRelativizingEvent;
import com.xwiki.task.internal.TaskMacroReferenceMigrator;
import com.xwiki.task.job.TaskReferenceRelativizerJobRequest;
import com.xwiki.task.job.TaskReferenceRelativizerJobStatus;

/**
 * A job that will retrieve all the pages containing task macros and that will change the reference parameters of the
 * macros into a relative version.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(TaskReferenceRelativizerJob.JOBTYPE)
public class TaskReferenceRelativizerJob
    extends AbstractJob<TaskReferenceRelativizerJobRequest, TaskReferenceRelativizerJobStatus>
{
    /**
     * The identifier of the job.
     */
    public static final String JOBTYPE = "taskmanager.relativizetaskreference";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private TaskMacroReferenceMigrator referenceMigrator;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected TaskReferenceRelativizerJobStatus createNewStatus(TaskReferenceRelativizerJobRequest request)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return new TaskReferenceRelativizerJobStatus(request, currentJobStatus, observationManager, loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        logger.info("Starting task reference relativizing job.");
        logger.info("Looking for pages that contain task macros.");
        String statement =
            "SELECT DISTINCT task.owner "
                + "FROM Document AS doc, doc.object(TaskManager.TaskManagerClass) AS task "
                + "WHERE task.owner <> ''";
        Query query = queryManager.createQuery(statement, Query.XWQL);
        List<String> results = query.execute();
        logger.info("Found [{}] pages that contain such macros.", results.size());
        List<DocumentReference> docRefs = results.stream()
            .map(result -> referenceResolver.resolve(result))
            .collect(Collectors.toList());

        observationManager.notify(new TaskRelativizingEvent(), this, docRefs);
        referenceMigrator.relativizeReference(docRefs);
        observationManager.notify(new TaskRelativizedEvent(), this, docRefs);
        logger.info("Done.");
    }
}
