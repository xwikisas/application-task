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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.task.model.Task;

/**
 * Tool to transform the reference parameter of the task macro into a relative reference.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Component(roles = TaskMacroReferenceMigrator.class)
@Singleton
public class TaskMacroReferenceMigrator
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private MacroBlockFinder blockFinder;

    @Inject
    @Named("supercompact")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Checks the given documents for task macros and replaces the reference parameter of the said macros with a
     * relative value.
     *
     * @param referenceList a list of document references that will be searched for task macros.
     */
    public void relativizeReference(List<DocumentReference> referenceList)
    {
        for (DocumentReference documentReference : referenceList) {
            // Resolve as a page ref and serialize it.
            String compactRef = serializer.serialize(pageReferenceResolver.resolve(documentReference)) + '/';
            try {
                logger.info("Searching for tasks inside [{}].", compactRef);
                XWikiContext context = contextProvider.get();
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                XDOM updatedXDOM =
                    blockFinder.find(document.getXDOM(), document.getSyntax(), (macroBlock) -> {
                        if (Task.MACRO_NAME.equals(macroBlock.getId()) && macroBlock.getParameters().containsKey(
                            "reference"))
                        {
                            String referenceParam = macroBlock.getParameter(Task.REFERENCE);
                            if (referenceParam.startsWith(compactRef)) {
                                referenceParam = referenceParam.substring(compactRef.length() - 1);
                                logger.info("Replaced the reference of a task macro from [{}] to [{}].",
                                    macroBlock.getParameter(Task.REFERENCE), referenceParam);
                                macroBlock.setParameter(Task.REFERENCE, referenceParam);
                            }
                            return MacroBlockFinder.Lookup.SKIP;
                        }
                        return MacroBlockFinder.Lookup.CONTINUE;
                    });
                if (!document.getXDOM().equals(updatedXDOM)) {
                    DocumentReference currentUser = context.getUserReference();
                    context.setUserReference(document.getAuthorReference());
                    document.setContent(updatedXDOM);
                    context.getWiki().saveDocument(document, "Updated the reference of the task macros to be relative.",
                        true, context);
                    context.setUserReference(currentUser);
                }
            } catch (XWikiException e) {
                logger.warn("Failed to retrieve the document [{}]. Cause [{}].", documentReference,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
