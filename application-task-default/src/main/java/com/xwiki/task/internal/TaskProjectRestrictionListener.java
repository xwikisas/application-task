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

import java.util.Arrays;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.ProjectManager;
import com.xwiki.task.TaskException;
import com.xwiki.task.model.Task;

/**
 * Prevents the save of a task whose project was changed to a project that the current user is not allowed to assign
 * to it. See {@link ProjectManager} for the rules. Tasks whose project is not changed are not checked, so that the
 * existing tasks can still be saved after the restrictions of their project are changed.
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component
@Singleton
@Named(TaskProjectRestrictionListener.NAME)
public class TaskProjectRestrictionListener extends AbstractTaskEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "com.xwiki.task.internal.TaskProjectRestrictionListener";

    @Inject
    private ProjectManager projectManager;

    @Inject
    private ContextualLocalizationManager localizationManager;

    /**
     * Constructor.
     */
    public TaskProjectRestrictionListener()
    {
        super(NAME, Arrays.asList(new DocumentUpdatingEvent(), new DocumentCreatingEvent()));
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context, Event event, boolean inFoldEvent)
    {
        BaseObject taskObj = document.getXObject(TASK_CLASS_REFERENCE);
        if (taskObj == null || isTemplate(document)) {
            return;
        }
        String project = taskObj.getStringValue(Task.PROJECT);
        if (StringUtils.isEmpty(project) || Objects.equals(project, getOriginalProject(document))) {
            return;
        }
        try {
            if (!this.projectManager.isProjectAllowed(project, document.getDocumentReference(),
                context.getUserReference()))
            {
                this.logger.debug("Project [{}] is not allowed for the task [{}] and the user [{}].", project,
                    document.getDocumentReference(), context.getUserReference());
                ((CancelableEvent) event).cancel(
                    this.localizationManager.getTranslationPlain("taskmanager.project.notAllowed", project));
            }
        } catch (TaskException e) {
            this.logger.error("Failed to check if the project [{}] can be assigned to the task [{}].", project,
                document.getDocumentReference(), e);
        }
    }

    private String getOriginalProject(XWikiDocument document)
    {
        XWikiDocument originalDocument = document.getOriginalDocument();
        BaseObject originalTaskObj =
            originalDocument != null ? originalDocument.getXObject(TASK_CLASS_REFERENCE) : null;
        return originalTaskObj != null ? originalTaskObj.getStringValue(Task.PROJECT) : null;
    }
}
