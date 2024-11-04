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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.task.internal.MacroBlockFinder;
import com.xwiki.task.internal.TaskMacroReferenceMigrator;
import com.xwiki.task.model.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class TaskMacroReferenceMigratorTest
{
    @InjectMockComponents
    private TaskMacroReferenceMigrator referenceMigrator;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private MacroBlockFinder blockFinder;

    @MockComponent
    @Named("supercompact")
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @Captor
    private ArgumentCaptor<Function<MacroBlock, MacroBlockFinder.Lookup>> visitorLambdaCaptor;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWiki xWiki;

    @Mock
    private XDOM xdom;

    @Mock
    private MacroBlock taskMacro1;

    private final DocumentReference documentReference = new DocumentReference("xwiki", "Task", "Page");

    private final PageReference pageReference = new PageReference("xwiki", Arrays.asList("Task", "Page"));

    @BeforeEach
    void setup() throws Exception
    {
        when(contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(documentReference, context)).thenReturn(document);
        when(document.getXDOM()).thenReturn(xdom);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        Map<String, String> params = new HashMap<>();
        params.put(Task.REFERENCE, "Task/Page/Tasks/Task_0");
        when(this.taskMacro1.getParameters()).thenReturn(params);
        when(this.taskMacro1.getParameter(Task.REFERENCE)).thenReturn("Task/Page/Tasks/Task_0");
        when(this.taskMacro1.getId()).thenReturn(Task.MACRO_NAME);
        when(this.blockFinder.find(any(), any(), any())).thenReturn(mock(XDOM.class));

        when(pageReferenceResolver.resolve(documentReference)).thenReturn(pageReference);
        when(serializer.serialize(pageReference)).thenReturn("Task/Page");
    }

    @Test
    void serializePageReference() throws TaskException, XWikiException
    {
        referenceMigrator.relativizeReference(Arrays.asList(documentReference));

        verify(this.blockFinder).find(eq(xdom), eq(Syntax.XWIKI_2_1), visitorLambdaCaptor.capture());
        Function<MacroBlock, MacroBlockFinder.Lookup> extractLambda = visitorLambdaCaptor.getValue();
        extractLambda.apply(this.taskMacro1);

        verify(taskMacro1).setParameter(Task.REFERENCE, "/Tasks/Task_0");
        verify(context, times(2)).setUserReference(any());
        verify(document).setContent(any(XDOM.class));
        verify(xWiki).saveDocument(eq(document), any(String.class), eq(true), eq(context));
    }
}
