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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;

import com.xwiki.task.TaskException;
import com.xwiki.task.TaskMissingDataManager;
import com.xwiki.task.job.TaskDataInferringJobRequest;
import com.xwiki.task.job.TaskDataInferringJobStatus;

/**
 * This job will run {@link TaskMissingDataManager#inferMissingTaskData()} async.
 *
 * @version $Id$
 * @since 3.3
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(TaskDataInferringJob.JOBTYPE)
public class TaskDataInferringJob
    extends AbstractJob<TaskDataInferringJobRequest, TaskDataInferringJobStatus>
{
    /**
     * The identifier for the job.
     */
    public static final String JOBTYPE = "taskmanager.infertaskdata";

    @Inject
    private TaskMissingDataManager taskMissingDataManager;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected TaskDataInferringJobStatus createNewStatus(TaskDataInferringJobRequest request)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return new TaskDataInferringJobStatus(request, currentJobStatus, observationManager, loggerManager);
    }

    @Override
    protected void runInternal()
    {
        logger.info("Starting data inferring job.");
        List<DocumentReference> owners = null;
        progressManager.pushLevelProgress(2, this);
        progressManager.startStep(this);
        logger.info("Looking for pages that contain task macros with missing data.");
        try {
            owners = taskMissingDataManager.getMissingDataTaskOwners();
        } catch (TaskException taskException) {
            logger.warn(
                "Failed to retrieve the pages holding tasks with incomplete data on the wiki [{}]. Cause: [{}].",
                request.getProperty(TaskDataInferringJobRequest.PROPERTY_WIKI),
                ExceptionUtils.getRootCauseMessage(taskException));
            return;
        }
        logger.info("Found [{}] pages that contain such macros.", owners.size());
        progressManager.startStep(this);
        progressManager.pushLevelProgress(owners.size(), this);
        for (DocumentReference owner : owners) {
            progressManager.startStep(this);
            try {
                taskMissingDataManager.inferMissingTaskData(owner);
                logger.info("Inferred data for the task macros inside [{}].", owner);
            } catch (TaskException e) {
                logger.warn("Failed to infer the missing data for the task macros in page [{}]. Cause [{}].", owner,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        logger.info("Done.");
    }
}
