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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.task.TaskException;
import com.xwiki.task.TaskMissingDataManager;

/**
 * Default implementation of {@link TaskMissingDataManager}.
 *
 * @version $Id$
 * @since 3.3
 */
@Component
@Singleton
public class DefaultTaskMissingDataManager implements TaskMissingDataManager
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private TaskDatesInitializer taskDatesInitializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Logger logger;

    @Override
    public List<DocumentReference> getMissingDataTaskOwners() throws TaskException
    {
        List<DocumentReference> missingDataTaskOwners = new ArrayList<>();
        processTasksWithOwner(documentReference -> {
            try {
                XWikiContext context = contextProvider.get();
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                if (taskDatesInitializer.doesDocumentContainIncompleteTasks(document.getXDOM())) {
                    missingDataTaskOwners.add(documentReference);
                }
            } catch (XWikiException e) {
                logger.warn("Some msg");
            }
        });
        return missingDataTaskOwners;
    }

    @Override
    public void inferMissingTaskData() throws TaskException
    {
        processTasksWithOwner(documentReference -> {
            try {
                inferMissingTaskData(documentReference);
            } catch (TaskException e) {
                logger.warn("Failed to infer the missing task data for [{}]. Cause: [{}].", documentReference,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        });
    }

    @Override
    public void inferMissingTaskData(DocumentReference documentReference) throws TaskException
    {

        try {
            XWikiContext context = contextProvider.get();
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            XDOM xdom = document.getXDOM();
            if (taskDatesInitializer.processDocument(document, xdom, context)) {
                document.setContent(xdom);
                context.getWiki().saveDocument(document, "Updated data of tasks.", context);
            }
        } catch (XWikiException e) {
            throw new TaskException(String.format("Failed to retrieve the document for [%s].", documentReference), e);
        }
    }

    private void processTasksWithOwner(Consumer<DocumentReference> consumer) throws TaskException
    {
        try {
            String statement =
                "SELECT DISTINCT task.owner "
                    + "FROM Document AS doc, doc.object(TaskManager.TaskManagerClass) AS task "
                    + "WHERE task.owner <> ''";
            Query query = queryManager.createQuery(statement, Query.XWQL);
            List<String> results = query.execute();
            for (String result : results) {
                consumer.accept(resolver.resolve(result));
            }
        } catch (QueryException e) {
            throw new TaskException("Failed to infer the missing task data.", e);
        }
    }
}
