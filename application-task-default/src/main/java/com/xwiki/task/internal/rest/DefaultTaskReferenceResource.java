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
package com.xwiki.task.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xwiki.task.TaskException;
import com.xwiki.task.TaskReferenceGenerator;
import com.xwiki.task.rest.TaskReferenceResource;

/**
 * Default implementation of {@link TaskReferenceResource}.
 *
 * @version $Id$
 * @since 3.1
 */
@Component
@Named("com.xwiki.task.internal.rest.DefaultTaskReferenceResource")
@Singleton
public class DefaultTaskReferenceResource extends XWikiResource implements TaskReferenceResource
{
    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private TaskReferenceGenerator taskReferenceGenerator;

    @Inject
    @Named("supercompact")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @Override
    public String generateId(String wikiName, String spaces, String pageName) throws XWikiRestException
    {
        DocumentReference ownerRef = new DocumentReference(pageName, getSpaceReference(spaces, wikiName));
        DocumentReference docRef =
            new DocumentReference(pageName, new SpaceReference("Tasks", getSpaceReference(spaces, wikiName)));
        if (!contextualAuthorizationManager.hasAccess(Right.EDIT, docRef)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
            return serializer.serialize(pageReferenceResolver.resolve(taskReferenceGenerator.generate(docRef)),
                ownerRef);
        } catch (TaskException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
