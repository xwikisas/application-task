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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xwiki.task.TaskException;
import com.xwiki.task.TaskReferenceGenerator;

/**
 * The default implementation of {@link com.xwiki.task.TaskReferenceGenerator}.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Singleton
public class DefaultTaskReferenceGenerator implements TaskReferenceGenerator
{
    private static final String TASK_PAGE_NAME_PREFIX = "Task_";

    private static final String TASK_MANAGER_SPACE = "TaskManager";

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private final Map<SpaceReference, Integer> nameOccurences = new HashMap<>();

    @Override
    public synchronized DocumentReference generate(DocumentReference parent) throws TaskException
    {
        SpaceReference parentSpaceRef = parent.getLastSpaceReference();
        if (!authorizationManager.hasAccess(Right.EDIT, parentSpaceRef)) {
            parentSpaceRef = new SpaceReference(parent.getWikiReference().getName(), TASK_MANAGER_SPACE);
            if (!authorizationManager.hasAccess(Right.EDIT, parentSpaceRef)) {
                throw new TaskException(String.format(
                    "The current user does not have rights over [%s] or [%s] thus the task page could not be created.",
                    parent.getLastSpaceReference(), parentSpaceRef));
            }
        }
        return getUniqueName(parentSpaceRef);
    }

    private DocumentReference getUniqueName(SpaceReference spaceRef)
    {

        int i = nameOccurences.getOrDefault(spaceRef, 0);
        DocumentReference docRef = new DocumentReference(TASK_PAGE_NAME_PREFIX + i, spaceRef);

        while (documentAccessBridge.exists(docRef)) {
            i++;
            docRef = new DocumentReference(TASK_PAGE_NAME_PREFIX + i, spaceRef);
            nameOccurences.put(spaceRef, i);
        }
        nameOccurences.put(spaceRef, ++i);
        return docRef;
    }
}
