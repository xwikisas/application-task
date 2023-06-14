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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.task.internal.TaskBlockProcessor;
import com.xwiki.task.internal.TaskXDOMProcessor;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private TaskConfiguration configuration;

    @MockComponent
    private TaskBlockProcessor taskBlockProcessor;

    @Mock
    private XDOM docContent;

    @Mock
    private XDOM taskContent;

    @Mock
    private XDOM macroContent;

    @Mock
    private MacroBlock taskMacro1;

    @Mock
    private MacroBlock taskMacro2;

    @Mock
    private MacroBlock macroWithContent;

    @Mock
    private MetaData metaData;

    private static final String TASK1_ID = "Task1";

    private static final String TASK2_ID = "Task2";

    private static final String DEFAULT_TASK_DATE = "01/01/2023";

    private final DocumentReference task1Reference = new DocumentReference("xwiki", "XWiki", TASK1_ID);

    private final DocumentReference task2Reference = new DocumentReference("xwiki", "XWiki", TASK2_ID);

    private final DocumentReference adminReference = new DocumentReference("xwiki", "XWiki", "Admin");

    private final DocumentReference contentSource = new DocumentReference("xwiki", "XWiki", "Doc");

    @BeforeEach
    void setup() throws TaskException
    {
        Map<String, String> taskMacro1Params = initTaskMacroParams(TASK1_ID, DEFAULT_TASK_DATE,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE);
        when(this.taskMacro1.getParameters()).thenReturn(taskMacro1Params);
        Map<String, String> taskMacro2Params = initTaskMacroParams(TASK2_ID, DEFAULT_TASK_DATE,
            Task.STATUS_DONE, adminReference.toString(), DEFAULT_TASK_DATE);
        when(this.taskMacro2.getParameters()).thenReturn(taskMacro2Params);
        when(this.taskMacro1.getId()).thenReturn(Task.MACRO_NAME);
        when(this.taskMacro2.getId()).thenReturn(Task.MACRO_NAME);
        when(this.resolver.resolve(TASK1_ID, contentSource)).thenReturn(this.task1Reference);
        when(this.resolver.resolve(TASK2_ID, contentSource)).thenReturn(this.task2Reference);
        when(this.resolver.resolve(adminReference.toString())).thenReturn(adminReference);
        when(this.configuration.getStorageDateFormat()).thenReturn("dd/MM/yyyy");
        when(this.docContent.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.getMetaData()).thenReturn(Collections.singletonMap(MetaData.SYNTAX, Syntax.XWIKI_2_1));
        when(this.taskBlockProcessor.getTaskContentXDOM(this.taskMacro1, Syntax.XWIKI_2_1)).thenReturn(
            this.taskContent);
        when(this.taskBlockProcessor.renderTaskContent(any(List.class), any(Syntax.class))).thenReturn("TaskContent");
        when(this.macroWithContent.getContent()).thenReturn("{{task /}}");
        when(this.macroContent.getMetaData()).thenReturn(this.metaData);
    }

    @Test
    void extractWhenThereAreTasksOnlyInDocumentContent()
    {

        when(docContent.getBlocks(any(ClassBlockMatcher.class), any(Block.Axes.class))).thenReturn(
            Collections.singletonList(taskMacro1));

        List<Task> result = this.processor.extract(this.docContent, this.contentSource);
        assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals("TaskContent", task.getName());
        assertEquals(this.task1Reference, task.getReference());
    }

    @Test
    void extractWhenThereAreTasksInsideMacroContent() throws TaskException
    {
        List<Block> macroChildren = Collections.singletonList(this.macroWithContent);
        when(this.docContent.getBlocks(any(ClassBlockMatcher.class), any(Block.Axes.class))).thenReturn(macroChildren);
        when(this.taskBlockProcessor.getTaskContentXDOM(eq(this.macroWithContent), eq(Syntax.XWIKI_2_1))).thenReturn(
            this.macroContent);
        when(this.macroContent.getBlocks(any(ClassBlockMatcher.class), any(Block.Axes.class))).thenReturn(
            Collections.singletonList(this.taskMacro1));
        when(this.taskBlockProcessor.renderTaskContent(macroChildren, Syntax.XWIKI_2_1)).thenReturn("{{task /}}");
        when(this.macroWithContent.getParent()).thenReturn(this.docContent);
        List<Block> contentChildren = new ArrayList<>(Collections.singletonList(this.macroWithContent));
        when(this.docContent.getChildren()).thenReturn(contentChildren);

        List<Task> result = this.processor.extract(this.docContent, this.contentSource);

        verify(this.docContent).getChildren();
        assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals("TaskContent", task.getName());
        assertEquals(this.task1Reference, task.getReference());
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
