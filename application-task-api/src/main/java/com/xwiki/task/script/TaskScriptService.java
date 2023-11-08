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
package com.xwiki.task.script;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.TaskException;
import com.xwiki.task.TaskMissingDataManager;
import com.xwiki.task.job.TaskDataInferringJobRequest;

/**
 * Script service for retrieving information about the Task Manager Application.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Named("task")
@Singleton
@Unstable
public class TaskScriptService implements ScriptService
{
    @Inject
    private TaskConfiguration configuration;

    @Inject
    private TaskMissingDataManager taskMissingDataManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private JobExecutor jobExecutor;

    /**
     * @return the configuration of the application.
     */
    public TaskConfiguration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * @return a job created as a result of {@link TaskDataInferringJobRequest}.
     */
    public Job inferTaskData()
    {
        if (!authorization.hasAccess(Right.ADMIN)) {
            return null;
        }

        TaskDataInferringJobRequest jobRequest = new TaskDataInferringJobRequest(new WikiReference("xwiki"));
        try {
            return jobExecutor.execute("taskmanager.infertaskdata", jobRequest);
        } catch (JobException ignored) {

        }
        return null;
    }

    /**
     * Infer data for the tasks of some document.
     *
     * @param documentReference the document reference of the document.
     */
    public void inferTaskData(DocumentReference documentReference)
    {
        if (!authorization.hasAccess(Right.ADMIN)) {
            return;
        }
        try {
            taskMissingDataManager.inferMissingTaskData(documentReference);
        } catch (TaskException ignored) {
        }
    }

    /**
     * @return a list pages that contain task macros with incomplete data.
     */
    public List<DocumentReference> getPagesWithIncompleteTaskMacros()
    {
        try {
            return taskMissingDataManager.getMissingDataTaskOwners();
        } catch (TaskException e) {
            return Collections.emptyList();
        }
    }
}
