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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.task.internal.MacroBlockFinder;
import com.xwiki.task.internal.TaskBlockProcessor;
import com.xwiki.task.internal.TaskReferenceUtils;
import com.xwiki.task.internal.TaskXDOMProcessor;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class TaskXDOMProcessorTest
{
    @InjectMockComponents
    private TaskXDOMProcessor processor;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private TaskReferenceGenerator taskReferenceGenerator;

    @MockComponent
    private DateMacroConfiguration configuration;

    @MockComponent
    private TaskBlockProcessor taskBlockProcessor;

    @MockComponent
    private MacroBlockFinder blockFinder;

    @MockComponent
    private MacroUtils macroUtils;

    @MockComponent
    private TaskReferenceUtils taskReferenceUtils;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Captor
    private ArgumentCaptor<Function<MacroBlock, MacroBlockFinder.Lookup>> visitorLambdaCaptor;

    @Mock
    private XDOM docContent;

    @Mock
    private XDOM taskContent;

    @Mock
    private MacroBlock taskMacro1;

    @Mock
    private MacroBlock taskMacro2;

    @Mock
    private MetaData metaData;

    @Mock
    private BaseObject taskObject;

    private static final String TASK1_ID = "Task1";

    private static final String TASK2_ID = "Task2";

    private static final String DEFAULT_TASK_DATE_STRING = "01/01/2023";

    private static final Date DEFAULT_TASK_DATE = new Date(1672531200000L);

    private final DocumentReference task1Reference = new DocumentReference("xwiki", "XWiki", TASK1_ID);

    private final DocumentReference task2Reference = new DocumentReference("xwiki", "XWiki", TASK2_ID);

    private final DocumentReference adminReference = new DocumentReference("xwiki", "XWiki", "Admin");

    private final DocumentReference contentSource = new DocumentReference("xwiki", "XWiki", "Doc");

    @BeforeEach
    void setup() throws TaskException, MacroExecutionException, ComponentLookupException
    {
        Map<String, String> taskMacro2Params = initTaskMacroParams(TASK2_ID, DEFAULT_TASK_DATE_STRING,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE_STRING);
        when(this.taskMacro2.getParameters()).thenReturn(taskMacro2Params);
        when(this.taskReferenceUtils.resolveAsDocumentReference(TASK1_ID, contentSource)).thenReturn(this.task1Reference);
        when(this.taskReferenceUtils.resolveAsDocumentReference(TASK2_ID, contentSource)).thenReturn(this.task2Reference);
        when(this.resolver.resolve(adminReference.toString())).thenReturn(adminReference);
        when(this.configuration.getStorageDateFormat()).thenReturn("dd/MM/yyyy");
        when(this.docContent.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.getMetaData()).thenReturn(Collections.singletonMap(MetaData.SYNTAX, Syntax.XWIKI_2_1));
        when(this.macroUtils.getMacroContentXDOM(this.taskMacro1, Syntax.XWIKI_2_1)).thenReturn(
            this.taskContent);
        when(this.macroUtils.renderMacroContent(any(List.class), any(Syntax.class))).thenReturn("TaskContent");
        when(this.taskMacro1.getId()).thenReturn(Task.MACRO_NAME);
    }

    @Test
    void extract()
    {
        Map<String, String> taskMacro1Params = initTaskMacroParams(TASK1_ID, DEFAULT_TASK_DATE_STRING,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE_STRING);
        when(this.taskMacro1.getParameters()).thenReturn(taskMacro1Params);

        List<Task> result = this.processor.extract(this.docContent, this.contentSource);

        callVisitorLambdaFunction();

        verify(this.taskReferenceUtils).resolveAsDocumentReference(TASK1_ID, this.contentSource);
        assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals("TaskContent", task.getName());
        assertEquals(this.task1Reference, task.getReference());
    }

    @Test
    void extractWhenThereAreTasksWithNoReference() throws TaskException
    {
        Map<String, String> taskMacro1Params = initTaskMacroParams("", DEFAULT_TASK_DATE_STRING,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE_STRING);
        when(this.taskMacro1.getParameters()).thenReturn(taskMacro1Params);

        when(this.taskReferenceGenerator.generate(this.contentSource)).thenReturn(this.task1Reference);
        when(this.serializer.serialize(this.task1Reference, this.contentSource)).thenReturn(
            this.task1Reference.toString());

        List<Task> result = this.processor.extract(this.docContent, this.contentSource);

        callVisitorLambdaFunction();

        assertEquals(0, result.size());
    }

    @Test
    void updateTaskMacroCall() throws TaskException, ComponentLookupException
    {
        Map<String, String> taskMacro1Params = initTaskMacroParams(TASK1_ID, DEFAULT_TASK_DATE_STRING,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE_STRING);
        when(this.taskMacro1.getParameters()).thenReturn(taskMacro1Params);

        when(this.taskObject.getDocumentReference()).thenReturn(this.task1Reference);
        when(this.taskObject.getStringValue(Task.NAME)).thenReturn(TASK1_ID);
        when(this.taskObject.getStringValue(Task.STATUS)).thenReturn(Task.STATUS_DONE);
        when(this.taskObject.getLargeStringValue(Task.DESCRIPTION)).thenReturn(TASK1_ID);
        when(this.taskObject.getLargeStringValue(Task.ASSIGNEE)).thenReturn(adminReference.toString());
        when(this.taskObject.getLargeStringValue(Task.REPORTER)).thenReturn(adminReference.toString());
        when(this.taskObject.getDateValue(Task.DUE_DATE)).thenReturn(DEFAULT_TASK_DATE);
        when(this.taskObject.getDateValue(Task.COMPLETE_DATE)).thenReturn(DEFAULT_TASK_DATE);
        when(this.taskObject.getDateValue(Task.CREATE_DATE)).thenReturn(DEFAULT_TASK_DATE);
        when(this.taskObject.getIntValue(Task.NUMBER)).thenReturn(1);

        when(this.taskBlockProcessor.generateTaskContentBlocks(eq(List.of(adminReference.toString())),
            eq(DEFAULT_TASK_DATE),
            eq(TASK1_ID), any(SimpleDateFormat.class))).thenReturn(Collections.emptyList());
        when(this.macroUtils.renderMacroContent(Collections.emptyList(), Syntax.XWIKI_2_1)).thenReturn(
            "TaskContent");

        when(this.taskMacro1.getParent()).thenReturn(this.docContent);
        when(this.docContent.getChildren()).thenReturn(new ArrayList<>(Collections.singletonList(this.taskMacro1)));

        this.processor.updateTaskMacroCall(this.contentSource, this.taskObject, this.docContent, Syntax.XWIKI_2_1);

        callVisitorLambdaFunction();

        verify(this.taskMacro1).setParameter(Task.STATUS, Task.STATUS_DONE);
        verify(this.taskMacro1).setParameter(Task.CREATE_DATE, DEFAULT_TASK_DATE_STRING);
        verify(this.taskMacro1).setParameter(Task.REPORTER, this.adminReference.toString());
        verify(this.taskBlockProcessor).generateTaskContentBlocks(eq(List.of(adminReference.toString())),
            eq(DEFAULT_TASK_DATE),
            eq(TASK1_ID), any(SimpleDateFormat.class));
        verify(this.macroUtils).renderMacroContent(Collections.emptyList(), Syntax.XWIKI_2_1);
    }

    @Test
    void removeTaskMacroCall()
    {
        Map<String, String> taskMacro1Params = initTaskMacroParams(TASK1_ID, DEFAULT_TASK_DATE_STRING,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE_STRING);
        when(this.taskMacro1.getParameters()).thenReturn(taskMacro1Params);
        when(this.taskMacro1.getParent()).thenReturn(this.docContent);
        List<Block> contentChildren = new ArrayList<>(Collections.singletonList(this.taskMacro1));
        List<Block> spyContentChildren = spy(contentChildren);
        when(this.docContent.getChildren()).thenReturn(spyContentChildren);

        this.processor.removeTaskMacroCall(this.task1Reference, this.contentSource, this.docContent, Syntax.XWIKI_2_1);

        callVisitorLambdaFunction();

        verify(this.taskReferenceUtils).resolveAsDocumentReference(TASK1_ID, this.contentSource);
        verify(spyContentChildren).remove(this.taskMacro1);
    }

    private void callVisitorLambdaFunction()
    {
        verify(this.blockFinder).find(eq(this.docContent), eq(Syntax.XWIKI_2_1), visitorLambdaCaptor.capture());
        Function<MacroBlock, MacroBlockFinder.Lookup> extractLambda = visitorLambdaCaptor.getValue();
        extractLambda.apply(this.taskMacro1);
    }

    Map<String, String> initTaskMacroParams(String ref, String createDate, String status, String reporter,
        String completeDate)
    {
        Map<String, String> taskMacroParams = new HashMap<>();
        taskMacroParams.put(Task.REFERENCE, ref);
        taskMacroParams.put(Task.CREATE_DATE, createDate);
        taskMacroParams.put(Task.STATUS, status);
        taskMacroParams.put(Task.REPORTER, reporter);
        taskMacroParams.put(Task.COMPLETE_DATE, completeDate);
        return taskMacroParams;
    }
}
