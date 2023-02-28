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

import java.util.Collections;
import java.util.Date;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.DefaultTaskManager;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultTaskManagerTests
{
    private static final String TASK_0_NAME = "Task";
    private static final String TASK_0_STATUS = "Done";
    private static final int TASK_0_NUMBER = 1;
    private static final Date TASK_0_DATE = new Date(1000);
    @InjectMockComponents
    private DefaultTaskManager taskManager;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;
    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject taskObject;

    @Mock
    private Query query;

    private final DocumentReference documentReference = new DocumentReference("wiki", "XWiki", "Doc");

    private final DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User1");

    @BeforeEach
    public void setup() throws XWikiException
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(this.documentReference, this.context)).thenReturn(this.document);
        when(this.document.getXObject(any(EntityReference.class))).thenReturn(this.taskObject);
        when(this.resolver.resolve(documentReference.toString())).thenReturn(documentReference);
        when(this.resolver.resolve(userReference.toString())).thenReturn(userReference);
        when(this.serializer.serialize(documentReference)).thenReturn(documentReference.toString());
        when(this.serializer.serialize(userReference)).thenReturn(userReference.toString());

        when(taskObject.getStringValue(Task.NAME)).thenReturn(TASK_0_NAME);
        when(taskObject.getIntValue(Task.NUMBER)).thenReturn(TASK_0_NUMBER);
        when(taskObject.getLargeStringValue(Task.OWNER)).thenReturn(documentReference.toString());
        when(taskObject.getLargeStringValue(Task.ASSIGNEE)).thenReturn(userReference.toString());
        when(taskObject.getStringValue(Task.STATUS)).thenReturn(TASK_0_STATUS);
        when(taskObject.getLargeStringValue(Task.REPORTER)).thenReturn(userReference.toString());
        when(taskObject.getDateValue(Task.DUE_DATE)).thenReturn(TASK_0_DATE);
        when(taskObject.getDateValue(Task.CREATE_DATE)).thenReturn(TASK_0_DATE);
        when(taskObject.getDateValue(Task.COMPLETE_DATE)).thenReturn(TASK_0_DATE);

    }

    @Test
    public void getTaskByReferenceTest() throws TaskException
    {

        Task task = this.taskManager.getTask(documentReference);

        assertEquals(TASK_0_NUMBER, task.getNumber());
        assertEquals(documentReference, task.getReference());
        assertEquals(TASK_0_STATUS, task.getStatus());
        assertEquals(TASK_0_NAME, task.getName());
        assertEquals(userReference, task.getReporter());
        assertEquals(TASK_0_DATE, task.getCreateDate());
    }

    @Test
    public void getTaskByIdTest() throws TaskException, QueryException
    {
        when(this.queryManager.createQuery(any(), any())).thenReturn(this.query);
        when(this.query.bindValue(any(), any())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(Collections.singletonList(documentReference.toString()));

        Task task = this.taskManager.getTask(1);

        verify(this.queryManager).createQuery(any(), any());
        verify(this.query).bindValue("id", 1);
        assertEquals(TASK_0_NUMBER, task.getNumber());
        assertEquals(documentReference, task.getReference());
        assertEquals(TASK_0_STATUS, task.getStatus());
        assertEquals(TASK_0_NAME, task.getName());
        assertEquals(userReference, task.getReporter());
        assertEquals(TASK_0_DATE, task.getCreateDate());
    }

    @Test
    public void deleteTaskByOwnerTest() throws TaskException, QueryException, XWikiException
    {
        when(this.queryManager.createQuery(any(), any())).thenReturn(this.query);
        when(this.query.bindValue(any(), any())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(Collections.singletonList(documentReference.toString()));
        QueryParameter queryParameter = mock(QueryParameter.class);
        when(this.query.bindValue(any())).thenReturn(queryParameter);
        when(queryParameter.anyChars()).thenReturn(queryParameter);
        when(queryParameter.literal(any())).thenReturn(queryParameter);

        this.taskManager.deleteTasksByOwner(userReference);

        verify(this.queryManager).createQuery(any(), any());
        verify(this.wiki).deleteDocument(this.document, this.context);
    }
}
