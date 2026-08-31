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
package com.xwiki.task.rest;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.task.TaskException;
import com.xwiki.task.TaskReferenceGenerator;
import com.xwiki.task.internal.rest.DefaultTaskReferenceResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultTaskReferenceResourceTest
{
    private static int TASK_COUNTER = 0;

    @InjectMockComponents
    private DefaultTaskReferenceResource resource;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private TaskReferenceGenerator taskReferenceGenerator;

    @MockComponent
    @Named("supercompact")
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @MockComponent
    private PageReferenceResolver<String> stringPageReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    private static final String WIKI = "xwiki";

    private static final String SPACE = "Test/spaces/Test2";

    private static final String NAME = "WebHome";

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws TaskException, ComponentLookupException
    {
        Provider<XWikiContext> contextProvider = componentManager.getInstance(new DefaultParameterizedType(null,
            Provider.class, XWikiContext.class));
        when(contextProvider.get()).thenReturn(this.context);
        when(this.context.getUserReference()).thenReturn(new DocumentReference(WIKI, "space", NAME));
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.serializer.serialize(any(), any())).then((a) -> {
            EntityReference entityReference = a.getArgument(0);
            StringBuilder sb = new StringBuilder();
            entityReference.getReversedReferenceChain().forEach(e -> {
                if (e.getName().equals("Tasks") || sb.length() > 0) {
                    sb.append("/").append(e.getName());
                }
            });
            return sb.toString();
        });
        when(this.contextualAuthorizationManager.hasAccess(any(), any())).thenReturn(true);
        when(this.taskReferenceGenerator.generate(any())).then(a -> {
            EntityReference entityReference = a.getArgument(0);
            return new DocumentReference(String.format("Task_%d", TASK_COUNTER++), entityReference.getParent(),
                Collections.emptyMap());
        });

        when(this.pageReferenceResolver.resolve(any())).then((a) -> {
            DocumentReference documentReference = a.getArgument(0);

            return new PageReference(
                documentReference.getWikiReference().getName(),
                documentReference.getReversedReferenceChain()
                    .stream()
                    .filter(e -> !(e instanceof WikiReference) && !"WebHome".equals(e.getName()))
                    .map(EntityReference::getName)
                    .collect(Collectors.toList()));
        });
    }

    @Test
    void generateId() throws XWikiRestException
    {
        String id = resource.generateId(WIKI, SPACE, NAME);
        assertEquals("/Tasks/Task_0", id);
    }

    @Test
    void generateIdWithNoRights()
    {
        when(contextualAuthorizationManager.hasAccess(any(), any())).thenReturn(false);
        assertThrows(WebApplicationException.class, () -> {
            String id = resource.generateId(WIKI, SPACE, NAME);
        });
    }

    @Test
    void generateIdThrowsError() throws XWikiRestException, TaskException
    {
        reset(taskReferenceGenerator);
        when(taskReferenceGenerator.generate(any())).thenThrow(TaskException.class);
        assertThrows(WebApplicationException.class, () -> {
            String id = resource.generateId(WIKI, SPACE, NAME);
        });
    }

    @Test
    void validateValidId() throws XWikiException, XWikiRestException
    {
        PageReference pageReference = mock(PageReference.class);
        when(stringPageReferenceResolver.resolve(any(), any())).thenReturn(pageReference);
        when(this.wiki.exists(pageReference, context)).thenReturn(false);
        Response response = resource.validateId(WIKI, SPACE, NAME, "/Task/Task_1");
        assertEquals(200, response.getStatus());
    }

    @Test
    void validateIdWithNoViewRights() throws XWikiRestException
    {
        when(this.contextualAuthorizationManager.hasAccess(any(), any())).thenReturn(false);
        Response response = resource.validateId(WIKI, SPACE, NAME, "/Task/Task_1");
        assertEquals(401, response.getStatus());
    }

    @Test
    void validateInvalidId() throws XWikiException, XWikiRestException
    {
        PageReference pageReference = mock(PageReference.class);
        when(stringPageReferenceResolver.resolve(any(), any())).thenReturn(pageReference);
        when(this.wiki.exists(pageReference, context)).thenReturn(true);
        Response response = resource.validateId(WIKI, SPACE, NAME, "/Task/Task_1");
        assertEquals(409, response.getStatus());
    }

    @Test
    void validateIdThrowsError() throws XWikiException
    {
        PageReference pageReference = mock(PageReference.class);
        when(stringPageReferenceResolver.resolve(any(), any())).thenReturn(pageReference);
        when(this.wiki.exists(pageReference, context)).thenThrow(XWikiException.class);
        assertThrows(XWikiRestException.class, () -> {
            Response response = resource.validateId(WIKI, SPACE, NAME, "/Task/Task_1");
        });
    }
}
