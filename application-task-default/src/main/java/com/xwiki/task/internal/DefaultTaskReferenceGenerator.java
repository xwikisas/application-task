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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
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
    private AuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private LocalizationManager localizationManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private final Map<SpaceReference, Integer> nameOccurences = new HashMap<>();

    @Override
    public DocumentReference generate(DocumentReference parent)
    {
        XWikiContext context = contextProvider.get();
        SpaceReference parentSpaceRef = parent.getLastSpaceReference();
        if (!authorizationManager.hasAccess(Right.EDIT, context.getUserReference(), parentSpaceRef)) {
            parentSpaceRef = new SpaceReference(parent.getWikiReference().getName(), TASK_MANAGER_SPACE);
        }
        return getUniqueName(parentSpaceRef, context);
    }

    private DocumentReference getUniqueName(SpaceReference spaceRef, XWikiContext context)
    {

        int i = nameOccurences.getOrDefault(spaceRef, 0);
        DocumentReference docRef = new DocumentReference(TASK_PAGE_NAME_PREFIX + i, spaceRef);

        while (context.getWiki().exists(docRef, context)) {
            i++;
            docRef = new DocumentReference(TASK_PAGE_NAME_PREFIX + i, spaceRef);
            nameOccurences.put(spaceRef, i);
        }
        nameOccurences.put(spaceRef, ++i);
        return docRef;
    }
}
