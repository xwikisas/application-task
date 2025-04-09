package com.xwiki.task.internal.rest;

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

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.model.Task;
import com.xwiki.task.rest.TaskResource;

/**
 * Default implementation of {@link TaskResource}.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Named("com.xwiki.task.internal.rest.DefaultTaskResource")
@Singleton
public class DefaultTaskResource extends ModifiablePageResource implements TaskResource
{
    private static final LocalDocumentReference TASK_CLASS_REFERENCE =
        new LocalDocumentReference(Collections.singletonList("TaskManager"), "TaskManagerClass");

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private TaskConfiguration taskConfiguration;

    @Override
    public Response changeTaskStatus(String wikiName, String spaces, String pageName, String status)
        throws XWikiRestException
    {
        DocumentReference docRef = new DocumentReference(pageName, getSpaceReference(spaces, wikiName));

        if (!contextualAuthorizationManager.hasAccess(Right.EDIT, docRef)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            XWikiDocument document = getXWikiContext().getWiki().getDocument(docRef, getXWikiContext()).clone();
            BaseObject taskObject = document.getXObject(TASK_CLASS_REFERENCE);

            if (taskObject == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String setStatus = taskObject.getStringValue(Task.STATUS);
            String newStatus = status;

            if ("toggle".equals(status)) {
                newStatus =
                    setStatus.equals(Task.STATUS_DONE) ? taskConfiguration.getDefaultInlineStatus() : Task.STATUS_DONE;
            }

            taskObject.set(Task.STATUS, newStatus, getXWikiContext());

            Date completeDate = null;
            int progress = 0;
            if (newStatus.equals(Task.STATUS_DONE)) {
                completeDate = new Date();
                progress = 100;
            }
            taskObject.set(Task.PROGRESS, progress, getXWikiContext());
            taskObject.set(Task.COMPLETE_DATE, completeDate, getXWikiContext());

            getXWikiContext().getWiki().saveDocument(document, "Task status was updated.", getXWikiContext());

            return Response.ok().build();
        } catch (XWikiException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
