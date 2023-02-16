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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.TaskException;
import com.xwiki.task.TaskManager;
import com.xwiki.task.model.Task;

/**
 * Default implementation of {@link TaskManager}.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Singleton
public class DefaultTaskManager implements TaskManager
{
    private static final LocalDocumentReference TASK_CLASS_REFERENCE =
        new LocalDocumentReference(Collections.singletonList("TaskManager"), "TaskManagerClass");

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    @Override
    public Task getTaskByReference(String reference)
    {
        XWikiContext context = contextProvider.get();
        DocumentReference docRef = resolver.resolve(reference);
        try {
            XWikiDocument doc = context.getWiki().getDocument(docRef, context);
            Task task = new Task();
            BaseObject obj = doc.getXObject(TASK_CLASS_REFERENCE);
            if (obj == null) {
                logger.warn("The page [{}] does not have a Task Object.", docRef);
                return null;
            }
            task.setReference(docRef);
            task.setName(obj.getStringValue(Task.NAME));
            task.setNumber(obj.getIntValue(Task.NUMBER));
            task.setOwner(resolver.resolve(obj.getLargeStringValue(Task.OWNER)));
            task.setAssignee(resolver.resolve(obj.getLargeStringValue(Task.ASSIGNEE)));
            task.setStatus(obj.getStringValue(Task.STATUS));
            task.setReporter(resolver.resolve(obj.getLargeStringValue(Task.REPORTER)));
            task.setRender(obj.getLargeStringValue(Task.RENDER));
            task.setDuedate(obj.getDateValue(Task.DUE_DATE));
            task.setCreateDate(obj.getDateValue(Task.CREATE_DATE));
            task.setCompleteDate(obj.getDateValue(Task.COMPLETE_DATE));
            return task;
        } catch (XWikiException e) {
            logger.error("Failed to retrieve the task from the page [{}]", reference);
            return null;
        }
    }

    @Override
    public void deleteTasksByOwner(DocumentReference documentReference) throws TaskException
    {
        try {
            XWikiContext context = contextProvider.get();
            String statement = ", BaseObject as taskObj, StringProperty as ownerProp "
                + "WHERE taskObj.name = doc.fullName AND taskObj.className = 'TaskManager.TaskManagerClass' "
                + "AND taskObj.id = ownerProp.id.id AND ownerProp.id.name = 'owner' "
                + "AND ownerProp.value like :docRef";
            Query query = queryManager.createQuery(statement, Query.HQL);
            query.bindValue("docRef").anyChars().literal(serializer.serialize(documentReference));
            List<String> results = query.execute();
            for (String result : results) {
                DocumentReference taskRef = resolver.resolve(result);
                XWikiDocument document = context.getWiki().getDocument(taskRef, context);
                context.getWiki().deleteDocument(document, context);
            }
        } catch (QueryException | XWikiException e) {
            throw new TaskException(String.format("Failed to delete the task documents that had [%s] as owner.",
                documentReference), e);
        }
    }
}
