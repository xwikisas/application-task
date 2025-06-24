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

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.task.internal.MacroBlockFinder;
import com.xwiki.task.rest.TaskboxResource;

/**
 * The default implementation for the {@link TaskboxResource} that recursively looks through the content of a given page
 * and the content of its macros, finds the taskbox macro, updates the status and saves the page.
 *
 * @version $Id$
 * @since 3.8.0
 */
@Component
@Named("com.xwiki.task.internal.rest.DefaultTaskboxResource")
@Singleton
public class DefaultTaskboxResource extends XWikiResource implements TaskboxResource
{
    private static final Set<String> TRUE_VALUES = Set.of("true", "1");

    private static final Set<String> FALSE_VALUES = Set.of("false", "0");

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private MacroBlockFinder macroBlockFinder;

    @Override
    public Response changeTaskStatus(String wikiName, String spaces, String pageName, String id, String checked)
        throws XWikiRestException
    {
        DocumentReference docRef = new DocumentReference(pageName, getSpaceReference(spaces, wikiName));

        if (!TRUE_VALUES.contains(checked) && !FALSE_VALUES.contains(checked)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!contextualAuthorizationManager.hasAccess(Right.EDIT, docRef)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        XWikiContext context = getXWikiContext();
        XWikiDocument document = null;
        try {
            document = context.getWiki().getDocument(docRef, context);
        } catch (XWikiException e) {
            throw new XWikiRestException("Could not retrieve document.", e);
        }
        XDOM docDOM = document.getXDOM();
        AtomicReference<Boolean> contentChanged =
            maybeUpdateTaskbox(id, checked, docDOM, document);

        if (contentChanged.get()) {
            try {
                document.setContent(docDOM);
                context.getWiki()
                    .saveDocument(document, String.format("Updated the taskbox with id [%s].", id), context);
                return Response.ok().build();
            } catch (XWikiException e) {
                throw new XWikiRestException("Failed to update the content of the page.", e);
            }
        }
        return Response.ok().build();
    }

    private AtomicReference<Boolean> maybeUpdateTaskbox(String id, String checked, XDOM docDOM,
        XWikiDocument document)
    {
        AtomicReference<Boolean> contentChanged = new AtomicReference<>(false);
        macroBlockFinder.find(docDOM, document.getSyntax(), (macroBlock -> {
            if (!"taskbox".equals(macroBlock.getId())) {
                return MacroBlockFinder.Lookup.CONTINUE;
            }
            String macroId = macroBlock.getParameters().getOrDefault("id", "");
            if (!id.equals(macroId)) {
                return MacroBlockFinder.Lookup.CONTINUE;
            }
            macroBlock.setParameter("checked", checked);
            contentChanged.set(true);

            return MacroBlockFinder.Lookup.BREAK;
        }));
        return contentChanged;
    }
}
