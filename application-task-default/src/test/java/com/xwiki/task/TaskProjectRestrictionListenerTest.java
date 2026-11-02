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
package com.xwiki.task;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.AbstractTaskEventListener;
import com.xwiki.task.internal.TaskProjectRestrictionListener;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskProjectRestrictionListener}.
 *
 * @version $Id$
 */
@ComponentTest
class TaskProjectRestrictionListenerTest
{
    private static final String PROJECT = "Project";

    private static final String NOT_ALLOWED = "Not allowed";

    private static final DocumentReference TASK_REFERENCE = new DocumentReference("xwiki", "Space", "Task");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    @InjectMockComponents
    private TaskProjectRestrictionListener listener;

    @MockComponent
    private ProjectManager projectManager;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private ObservationContext observationContext;

    @MockComponent
    private TaskConfiguration configuration;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocument originalDocument;

    private final BaseObject taskObj = new BaseObject();

    private final BaseObject originalTaskObj = new BaseObject();

    private final DocumentUpdatingEvent event = new DocumentUpdatingEvent();

    @BeforeEach
    void setup()
    {
        when(this.document.getDocumentReference()).thenReturn(TASK_REFERENCE);
        when(this.document.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE)).thenReturn(this.taskObj);
        when(this.document.getOriginalDocument()).thenReturn(this.originalDocument);
        when(this.originalDocument.getXObject(AbstractTaskEventListener.TASK_CLASS_REFERENCE))
            .thenReturn(this.originalTaskObj);
        when(this.context.getUserReference()).thenReturn(USER_REFERENCE);
        when(this.localizationManager.getTranslationPlain("taskmanager.project.notAllowed", PROJECT))
            .thenReturn(NOT_ALLOWED);
    }

    @Test
    void projectNotChanged() throws TaskException
    {
        this.originalTaskObj.setStringValue(Task.PROJECT, PROJECT);
        this.taskObj.setStringValue(Task.PROJECT, PROJECT);

        this.listener.onEvent(this.event, this.document, this.context);

        assertFalse(this.event.isCanceled());
        verify(this.projectManager, never()).isProjectAllowed(any(), any(), any());
    }

    @Test
    void projectRemoved() throws TaskException
    {
        this.originalTaskObj.setStringValue(Task.PROJECT, PROJECT);
        this.taskObj.setStringValue(Task.PROJECT, "");

        this.listener.onEvent(this.event, this.document, this.context);

        assertFalse(this.event.isCanceled());
        verify(this.projectManager, never()).isProjectAllowed(any(), any(), any());
    }

    @Test
    void allowedProject() throws TaskException
    {
        this.taskObj.setStringValue(Task.PROJECT, PROJECT);
        when(this.projectManager.isProjectAllowed(PROJECT, TASK_REFERENCE, USER_REFERENCE)).thenReturn(true);

        this.listener.onEvent(this.event, this.document, this.context);

        assertFalse(this.event.isCanceled());
    }

    @Test
    void notAllowedProject() throws TaskException
    {
        this.taskObj.setStringValue(Task.PROJECT, PROJECT);
        when(this.projectManager.isProjectAllowed(PROJECT, TASK_REFERENCE, USER_REFERENCE)).thenReturn(false);

        this.listener.onEvent(this.event, this.document, this.context);

        assertTrue(this.event.isCanceled());
        assertEquals(NOT_ALLOWED, this.event.getReason());
    }

    @Test
    void template() throws TaskException
    {
        when(this.document.getDocumentReference())
            .thenReturn(new DocumentReference("xwiki", List.of("TaskManager", "TaskManagerTemplates"),
                "TaskManagerTemplate"));
        this.taskObj.setStringValue(Task.PROJECT, PROJECT);

        this.listener.onEvent(this.event, this.document, this.context);

        assertFalse(this.event.isCanceled());
        verify(this.projectManager, never()).isProjectAllowed(any(), any(), any());
    }
}
