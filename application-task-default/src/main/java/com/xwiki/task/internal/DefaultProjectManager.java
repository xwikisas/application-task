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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xwiki.task.ProjectManager;
import com.xwiki.task.TaskException;

/**
 * Default implementation of {@link ProjectManager}, based on the {@code spaces} and {@code users} properties of the
 * {@code TaskManager.ProjectClass} objects.
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component
@Singleton
public class DefaultProjectManager implements ProjectManager
{
    /**
     * The reference of the class that defines a project.
     */
    public static final LocalDocumentReference PROJECT_CLASS_REFERENCE =
        new LocalDocumentReference(AbstractTaskEventListener.TASK_MANAGER_SPACE, "ProjectClass");

    /**
     * The name of the property that holds the name of the project.
     */
    public static final String PROJECT = "project";

    /**
     * The name of the property that holds the spaces in which the project can be assigned.
     */
    public static final String SPACES = "spaces";

    /**
     * The name of the property that holds the users that can assign the project anywhere.
     */
    public static final String USERS = "users";

    private static final String LIST_SEPARATORS = "|,";

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public Set<String> getAllowedProjects(DocumentReference taskReference, DocumentReference userReference)
        throws TaskException
    {
        Set<String> allowedProjects = new LinkedHashSet<>();
        for (BaseObject projectObj : getProjectObjects()) {
            if (isAllowed(projectObj, taskReference, userReference)) {
                allowedProjects.add(projectObj.getStringValue(PROJECT));
            }
        }
        return allowedProjects;
    }

    @Override
    public boolean isProjectAllowed(String project, DocumentReference taskReference, DocumentReference userReference)
        throws TaskException
    {
        for (BaseObject projectObj : getProjectObjects()) {
            if (Objects.equals(project, projectObj.getStringValue(PROJECT))
                && isAllowed(projectObj, taskReference, userReference))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowed(BaseObject projectObj, DocumentReference taskReference,
        DocumentReference userReference)
    {
        DocumentReference projectReference = projectObj.getDocumentReference();
        List<String> spaces = getList(projectObj, SPACES);
        List<String> users = getList(projectObj, USERS);
        if (spaces.isEmpty() && users.isEmpty()) {
            return true;
        }
        // The users of the project can assign it to any task, whatever its location.
        if (userReference != null && users.stream()
            .anyMatch(user -> userReference.equals(this.resolver.resolve(user, projectReference))))
        {
            return true;
        }
        return spaces.stream().map(space -> this.resolver.resolve(space, projectReference))
            .map(DocumentReference::getLastSpaceReference).anyMatch(space -> isInSpace(taskReference, space));
    }

    private boolean isInSpace(DocumentReference taskReference, SpaceReference spaceReference)
    {
        return taskReference.hasParent(spaceReference);
    }

    private List<String> getList(BaseObject object, String propertyName)
    {
        BaseProperty<?> property = (BaseProperty<?>) object.getField(propertyName);
        if (property == null) {
            return Collections.emptyList();
        }
        if (property instanceof ListProperty) {
            return ((ListProperty) property).getList();
        }
        String value = property.toText();
        return StringUtils.isBlank(value) ? Collections.emptyList()
            : ListClass.getListFromString(value, LIST_SEPARATORS, false);
    }

    private List<BaseObject> getProjectObjects() throws TaskException
    {
        XWikiContext context = this.contextProvider.get();
        List<String> projectDocuments;
        try {
            projectDocuments = this.queryManager
                .createQuery("from doc.object(TaskManager.ProjectClass) as project", Query.XWQL).execute();
        } catch (QueryException e) {
            throw new TaskException("Failed to retrieve the projects.", e);
        }
        List<BaseObject> projectObjects = new ArrayList<>();
        for (String projectDocument : projectDocuments) {
            try {
                XWikiDocument document =
                    context.getWiki().getDocument(this.resolver.resolve(projectDocument), context);
                projectObjects.addAll(document.getXObjects(PROJECT_CLASS_REFERENCE).stream().filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            } catch (XWikiException e) {
                throw new TaskException(String.format("Failed to load the project document [%s].", projectDocument),
                    e);
            }
        }
        return projectObjects;
    }
}
