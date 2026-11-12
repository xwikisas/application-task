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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.TaskMissingDataManager;
import com.xwiki.task.job.TaskDataInferringJobRequest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class TaskScriptServiceTest
{
    private static final WikiReference CURRENT_WIKI = new WikiReference("subwiki");

    private static final DocumentReference DOCUMENT = new DocumentReference("subwiki", "Space", "Page");

    @InjectMockComponents
    private TaskScriptService scriptService;

    @MockComponent
    private TaskConfiguration configuration;

    @MockComponent
    private TaskMissingDataManager taskMissingDataManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private QueryManager queryManager;

    @BeforeEach
    void setup()
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(CURRENT_WIKI);
        // Having the right on the current document is not enough for any of the methods.
        when(this.authorization.hasAccess(Right.ADMIN)).thenReturn(true);
        when(this.authorization.hasAccess(Right.EDIT)).thenReturn(true);
    }

    @Test
    void inferTaskDataOfDocumentWithEditRight() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, DOCUMENT)).thenReturn(true);

        this.scriptService.inferTaskData(DOCUMENT);

        verify(this.taskMissingDataManager).inferMissingTaskData(DOCUMENT);
    }

    @Test
    void inferTaskDataOfDocumentWithoutEditRight()
    {
        this.scriptService.inferTaskData(DOCUMENT);

        verifyNoInteractions(this.taskMissingDataManager);
    }

    @Test
    void inferTaskDataWithWikiAdminRight() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, CURRENT_WIKI)).thenReturn(true);
        Job job = mock(Job.class);
        when(this.jobExecutor.execute(eq("taskmanager.infertaskdata"), any(TaskDataInferringJobRequest.class)))
            .thenReturn(job);

        assertSame(job, this.scriptService.inferTaskData());
    }

    @Test
    void inferTaskDataWithoutWikiAdminRight()
    {
        assertNull(this.scriptService.inferTaskData());

        verifyNoInteractions(this.jobExecutor);
    }

    @Test
    void relativizeMacroReferencesWithWikiAdminRight() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, CURRENT_WIKI)).thenReturn(true);

        this.scriptService.relativizeMacroReferences();

        verify(this.taskMissingDataManager).relativizeReferences();
    }

    @Test
    void relativizeMacroReferencesWithoutWikiAdminRight() throws Exception
    {
        this.scriptService.relativizeMacroReferences();

        verify(this.taskMissingDataManager, never()).relativizeReferences();
    }
}
