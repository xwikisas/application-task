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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.task.event.TaskRelativizedEvent;
import com.xwiki.task.event.TaskRelativizingEvent;

/**
 * Listener responsible with migrating the task macro "reference" parameters to a relative value.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Singleton
@Named("TaskReferenceMigratorListener")
public class TaskReferenceMigratorListener extends AbstractEventListener implements Initializable
{
    private static final LocalDocumentReference EXECUTED_DOC_FLAG = new LocalDocumentReference("TaskManager",
        "ReferenceRelitivizerExecuted");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    private Provider<QueryManager> queryManagerProvider;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Provider<TaskMacroReferenceMigrator> referenceMigratorProvider;

    @Inject
    private Provider<ObservationManager> observationManagerProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    /**
     * Default constructor.
     */
    public TaskReferenceMigratorListener()
    {
        super("TaskReferenceMigratorListener", Collections.emptyList());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Do nothing.
    }

    @Override
    public void initialize() throws InitializationException
    {
        XWikiContext context = contextProvider.get();
        if (context == null) {
            return;
        }
        DocumentReference flagDocRef = new DocumentReference(EXECUTED_DOC_FLAG, context.getWikiReference());
        // If the flag document exists, it means that the relativizer has been executed in the past.
        try {
            if (context.getWiki().exists(flagDocRef, context)) {
                return;
            }
        } catch (XWikiException e) {
            logger.warn("Failed to check whether [{}] exists or not. Cause: [{}].", EXECUTED_DOC_FLAG,
                ExceptionUtils.getRootCauseMessage(e));
            return;
        }

        try {
            logger.info("Starting task reference relativizing job.");
            String statement =
                "SELECT DISTINCT task.owner "
                    + "FROM Document AS doc, doc.object(TaskManager.TaskManagerClass) AS task "
                    + "WHERE task.owner <> ''";
            Query query = queryManagerProvider.get().createQuery(statement, Query.XWQL);
            List<String> results = query.execute();
            logger.info("Found [{}] pages that contain task macros.", results.size());
            List<DocumentReference> docRefs = results.stream()
                .map(result -> referenceResolver.resolve(result))
                .collect(Collectors.toList());

            ObservationManager observationManager = observationManagerProvider.get();
            observationManager.notify(new TaskRelativizingEvent(), this, docRefs);
            referenceMigratorProvider.get().relativizeReference(docRefs);
            observationManager.notify(new TaskRelativizedEvent(), this, docRefs);
            logger.info("Done.");

            saveFlagDocument(context, flagDocRef);
        } catch (XWikiException e) {
            logger.warn(
                "An error was encountered while trying to relitivize the references of the task macros. Cause: [{}].",
                ExceptionUtils.getRootCauseMessage(e));
        } catch (QueryException e) {
            // The query fails if the application is installed for the first time as there is no TaskManagerClass.
            try {
                saveFlagDocument(context, flagDocRef);
            } catch (XWikiException ex) {
                logger.warn(
                    "Failed to create the flag document on the first installation of the Task Manager Application."
                        + " Cause: [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private void saveFlagDocument(XWikiContext context, DocumentReference flagDocRef) throws XWikiException
    {
        XWikiDocument flagDoc = context.getWiki().getDocument(flagDocRef, context);
        flagDoc.setHidden(true);
        flagDoc.getAuthors().setCreator(userReferenceResolver.resolve(context.getUserReference()));
        context.getWiki().saveDocument(flagDoc, context);
    }
}
