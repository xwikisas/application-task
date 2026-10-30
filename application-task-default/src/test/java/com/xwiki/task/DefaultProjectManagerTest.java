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
package com.xwiki.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.DefaultProjectManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultProjectManager}, following the examples of the specification of the issue #353.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultProjectManagerTest
{
    private static final String WIKI = "xwiki";

    private static final String PROJECT_1 = "Project1";

    private static final String PROJECT_2 = "Project2";

    private static final String PROJECT_3 = "Project3";

    private static final String SPACE_1 = "Space1.WebHome";

    private static final String SPACE_2 = "Space2.WebHome";

    private static final String USER_1 = "XWiki.User1";

    private static final String USER_2 = "XWiki.User2";

    private static final DocumentReference TASK_IN_SPACE_1 = new DocumentReference(WIKI, "Space1", "Task");

    private static final DocumentReference TASK_IN_SPACE_2 = new DocumentReference(WIKI, "Space2", "Task");

    private static final DocumentReference TASK_IN_SPACE_3 = new DocumentReference(WIKI, "Space3", "Task");

    private static final DocumentReference TASK_IN_SPACE_2_SUBSPACE =
        new DocumentReference(WIKI, Arrays.asList("Space2", "Sub", "Tasks"), "Task");

    private static final DocumentReference TASK_IN_SPACE_22 = new DocumentReference(WIKI, "Space22", "Task");

    @InjectMockComponents
    private DefaultProjectManager projectManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private Query query;

    @Mock
    private Query countQuery;

    private final List<String> projectDocuments = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiId()).thenReturn(WIKI);
        when(this.queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(this.query);
        when(this.query.<String>execute()).thenReturn(this.projectDocuments);
        when(this.resolver.resolve(anyString(), any(Object[].class)))
            .thenAnswer(invocation -> resolve(invocation.getArgument(0)));
        when(this.localSerializer.serialize(any(EntityReference.class)))
            .thenAnswer(invocation -> invocation.<EntityReference>getArgument(0).getName());
        when(this.queryManager.createQuery(startsWith("select count"), eq(Query.XWQL))).thenReturn(this.countQuery);
        when(this.countQuery.bindValue(anyString(), any())).thenReturn(this.countQuery);

        addProject(PROJECT_2, Collections.emptyList(), Collections.emptyList());
        addProject(PROJECT_3, List.of(SPACE_2), List.of(USER_1));
        addProject(PROJECT_1, List.of(SPACE_1, SPACE_2), List.of(USER_1, USER_2));
    }

    @Test
    void userOfAllProjectsInUnrelatedSpace() throws TaskException
    {
        assertEquals(Set.of(PROJECT_1, PROJECT_2, PROJECT_3),
            this.projectManager.getAllowedProjects(TASK_IN_SPACE_3, user(1)));
    }

    @Test
    void userInSpaceOfAllProjects() throws TaskException
    {
        assertEquals(Set.of(PROJECT_1, PROJECT_2, PROJECT_3),
            this.projectManager.getAllowedProjects(TASK_IN_SPACE_2, user(2)));
    }

    @Test
    void otherUserInUnrelatedSpace() throws TaskException
    {
        assertEquals(Set.of(PROJECT_2), this.projectManager.getAllowedProjects(TASK_IN_SPACE_3, user(3)));
    }

    @Test
    void otherUserInSpaceOfAllProjects() throws TaskException
    {
        assertEquals(Set.of(PROJECT_1, PROJECT_2, PROJECT_3),
            this.projectManager.getAllowedProjects(TASK_IN_SPACE_2, user(3)));
    }

    @Test
    void userInSpaceOfOneProject() throws TaskException
    {
        assertEquals(Set.of(PROJECT_1, PROJECT_2), this.projectManager.getAllowedProjects(TASK_IN_SPACE_1, user(2)));
    }

    @Test
    void subspace() throws TaskException
    {
        assertEquals(Set.of(PROJECT_1, PROJECT_2, PROJECT_3),
            this.projectManager.getAllowedProjects(TASK_IN_SPACE_2_SUBSPACE, user(3)));
        // A space whose name only starts with the name of a project space is not a subspace.
        assertEquals(Set.of(PROJECT_2), this.projectManager.getAllowedProjects(TASK_IN_SPACE_22, user(3)));
    }

    @Test
    void guest() throws TaskException
    {
        assertEquals(Set.of(PROJECT_2), this.projectManager.getAllowedProjects(TASK_IN_SPACE_3, null));
    }

    @Test
    void isProjectAllowed() throws TaskException
    {
        assertTrue(this.projectManager.isProjectAllowed(PROJECT_2, TASK_IN_SPACE_3, user(3)));
        assertTrue(this.projectManager.isProjectAllowed(PROJECT_3, TASK_IN_SPACE_3, user(1)));
        assertFalse(this.projectManager.isProjectAllowed(PROJECT_3, TASK_IN_SPACE_1, user(2)));
        assertFalse(this.projectManager.isProjectAllowed("Unknown", TASK_IN_SPACE_3, user(1)));
    }

    @Test
    void countTasksOutsideSpacesWithoutSpaces() throws Exception
    {
        assertEquals(0, this.projectManager.countTasksOutsideSpaces(PROJECT_2));
        assertEquals(0, this.projectManager.countTasksOutsideSpaces("Unknown"));
        verify(this.queryManager, never()).createQuery(startsWith("select count"), eq(Query.XWQL));
    }

    @Test
    void countTasksOutsideSpaces() throws Exception
    {
        addProject("Project_4", List.of("Space_4%!.WebHome"), Collections.emptyList());
        when(this.countQuery.<Long>execute()).thenReturn(List.of(7L));

        assertEquals(7, this.projectManager.countTasksOutsideSpaces("Project_4"));
        verify(this.queryManager).createQuery("select count(doc.fullName) from Document doc, "
            + "doc.object(TaskManager.TaskManagerClass) as task where task.project = :project "
            + "and doc.space <> :templateSpace and doc.space <> :space0 and doc.space not like :spacePrefix0 "
            + "escape '!'", Query.XWQL);
        verify(this.countQuery).bindValue("project", "Project_4");
        verify(this.countQuery).bindValue("templateSpace", "TaskManager.TaskManagerTemplates");
        verify(this.countQuery).bindValue("space0", "Space_4%!");
        verify(this.countQuery).bindValue("spacePrefix0", "Space!_4!%!!.%");
    }

    @Test
    void countTasksOutsideSpacesWithSeveralSpaces() throws Exception
    {
        when(this.countQuery.<Long>execute()).thenReturn(List.of(3L));

        assertEquals(3, this.projectManager.countTasksOutsideSpaces(PROJECT_1));
        verify(this.countQuery).bindValue("space0", "Space1");
        verify(this.countQuery).bindValue("spacePrefix0", "Space1.%");
        verify(this.countQuery).bindValue("space1", "Space2");
        verify(this.countQuery).bindValue("spacePrefix1", "Space2.%");
    }

    private DocumentReference user(int number)
    {
        return new DocumentReference(WIKI, "XWiki", "User" + number);
    }

    private void addProject(String name, List<String> spaces, List<String> users) throws Exception
    {
        String projectDocument = "TaskManager.Project_" + name;
        DocumentReference projectReference = resolve(projectDocument);
        BaseObject projectObj = new BaseObject();
        projectObj.setDocumentReference(projectReference);
        projectObj.setStringValue(DefaultProjectManager.PROJECT, name);
        projectObj.setDBStringListValue(DefaultProjectManager.SPACES, spaces);
        projectObj.setDBStringListValue(DefaultProjectManager.USERS, users);

        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getXObjects(DefaultProjectManager.PROJECT_CLASS_REFERENCE)).thenReturn(List.of(projectObj));
        when(this.wiki.getDocument(projectReference, this.context)).thenReturn(document);
        this.projectDocuments.add(projectDocument);
    }

    private DocumentReference resolve(String reference)
    {
        int separator = reference.lastIndexOf('.');
        return new DocumentReference(WIKI, Arrays.asList(reference.substring(0, separator).split("\\.")),
            reference.substring(separator + 1));
    }
}
