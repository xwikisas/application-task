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
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xwiki.task.PaginatedReferences;
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

    @Inject
    private QueryManager queryManager;

    @Inject
    private Logger logger;

    /**
     * Runs a process that finds all the macros that have an absolute reference (i.e. xwiki:Space.Tasks.Task1 or
     * Space.Tasks.Task1) and turns them in a relative page reference. (i.e. /Tasks/Task1). This prettifies the
     * reference parameter of the task macro and allows the copying and moving of xwiki pages run correctly.
     *
     * @since 3.10.2
     */
    public void relativizeMacroReferences()
    {
        if (!authorization.hasAccess(Right.ADMIN)) {
            return;
        }
        try {
            taskMissingDataManager.relativizeReferences();
        } catch (TaskException ignored) {
        }
    }

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
        return getPagesWithIncompleteTaskMacros(0, 15);
    }

    /**
     * @param offset the offset that will be used in returning the subset of pages with incomplete data.
     * @param limit the limit imposed on the returned list.
     * @return a list pages that contain task macros with incomplete data.
     */
    public List<DocumentReference> getPagesWithIncompleteTaskMacros(int offset, int limit)
    {
        try {
            return taskMissingDataManager.getMissingDataTaskOwners(offset, limit);
        } catch (TaskException e) {
            return Collections.emptyList();
        }
    }

    /**
     * @param offset the offset that will be used in returning the subset of pages with incomplete data.
     * @param limit the limit imposed on the returned list.
     * @return a paginated list of pages that contain task macros with incomplete data.
     */
    public PaginatedReferences getPaginatedPagesWithIncompleteTaskMacros(int offset, int limit)
    {
        try {
            return taskMissingDataManager.getPaginatedMissingDataTaskOwners(offset, limit);
        } catch (TaskException e) {
            return new PaginatedReferences(Collections.emptyList());
        }
    }

    /**
     * @return a list of statuses sorted by their order property.
     */
    public List<String> getSortedStatuses()
    {

        try {
            Query query = queryManager.createQuery("select prop1.value from BaseObject as obj, "
                + "StringProperty as prop1, IntegerProperty as prop2 where obj.className='TaskManager.StatusClass' and "
                + "obj.id=prop1.id.id and prop1.id.name='status' and obj.id=prop2.id.id and prop2.id.name='order' "
                + "order by prop2.value", Query.HQL);
            List<String> results = query.execute().stream().map(Objects::toString).collect(Collectors.toList());
            return results;
        } catch (QueryException e) {
            logger.error("Failed to retrieve the statuses.", e);
            throw new RuntimeException(e);
        }
    }
}
