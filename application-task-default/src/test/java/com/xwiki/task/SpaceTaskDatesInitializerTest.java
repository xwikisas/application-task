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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.AddDelta;
import org.suigeneris.jrcs.diff.delta.ChangeDelta;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xwiki.task.internal.SpaceTaskDatesInitializer;
import com.xwiki.task.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class SpaceTaskDatesInitializerTest
{
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    @InjectMockComponents
    private SpaceTaskDatesInitializer taskInit;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private TaskConfiguration configuration;

    @MockComponent
    private DocumentRevisionProvider revisionProvider;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XDOM xdom;

    private final DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Admin");

    private final String serializedDocRef = "XWiki.Admin";

    private final MacroBlock task2 = new MacroBlock(Task.MACRO_NAME, Collections.singletonMap("reference", "Task_2"),
        true);

    private final SpaceReference spaceRef = new SpaceReference("xwiki", "Test");

    private final DocumentReference docRef = new DocumentReference("Name", spaceRef);

    private final Calendar calendar = Calendar.getInstance();

    @BeforeEach
    void setup() throws XWikiException, TaskException
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWiki().getDocument(this.docRef, this.context)).thenReturn(this.doc);
        when(this.taskManager.getTaskOwnersFromSpace(this.spaceRef)).thenReturn(Collections.singletonList(this.docRef));
        when(this.doc.clone()).thenReturn(this.doc);
        when(this.doc.getXDOM()).thenReturn(this.xdom);
        when(this.doc.getAuthorReference()).thenReturn(this.userReference);
        when(this.serializer.serialize(this.userReference)).thenReturn(this.serializedDocRef);
        when(this.configuration.getStorageDateFormat()).thenReturn(DATE_FORMAT);
    }

    @Test
    void taskCreatedInVersion2OfTheDocumentWithStatusDone() throws XWikiException, DifferentiationFailedException
    {
        Map<String, String> macroParams = new HashMap<>();
        macroParams.put("reference", "Task_1");
        macroParams.put("status", "Done");
        MacroBlock task1 = new MacroBlock(Task.MACRO_NAME, macroParams, true);
        when(this.xdom.getBlocks(any(), any())).thenReturn(Collections.singletonList(task1));
        when(this.doc.getRevisions(this.context)).thenReturn(new Version[] { new Version(1, 1), new Version(2, 1),
            new Version(3, 1) });

        when(this.revisionProvider.getRevision(this.doc, "1.1")).thenReturn(this.doc);
        when(this.doc.getContent()).thenReturn("");

        XWikiRCSNodeInfo nodeInfo2 = mock(XWikiRCSNodeInfo.class);
        XWikiRCSNodeInfo nodeInfo3 = mock(XWikiRCSNodeInfo.class);
        AddDelta delta12 = mock(AddDelta.class);
        ChangeDelta delta23 = mock(ChangeDelta.class);
        Chunk chunk12 = mock(Chunk.class);
        Chunk chunk23 = mock(Chunk.class);

        when(this.doc.getContentDiff("1.1", "2.1", this.context)).thenReturn(Collections.singletonList(delta12));
        when(this.doc.getContentDiff("2.1", "3.1", this.context)).thenReturn(Collections.singletonList(delta23));

        when(this.doc.getRevisionInfo("2.1", this.context)).thenReturn(nodeInfo2);
        when(this.doc.getRevisionInfo("3.1", this.context)).thenReturn(nodeInfo3);
        this.calendar.set(2023, Calendar.JANUARY, 2);
        Date docV2Date = calendar.getTime();
        when(nodeInfo2.getDate()).thenReturn(docV2Date);
        when(nodeInfo2.getAuthor()).thenReturn(serializedDocRef);
        this.calendar.set(2023, Calendar.JANUARY, 3);
        Date docV3Date = calendar.getTime();
        when(nodeInfo3.getDate()).thenReturn(docV3Date);
        when(nodeInfo3.getAuthor()).thenReturn(serializedDocRef);

        when(delta12.getRevised()).thenReturn(chunk12);
        when(chunk12.toString()).thenReturn("{{task reference=\"Task_1\"}}");
        when(delta23.getRevised()).thenReturn(chunk23);
        when(chunk23.toString()).thenReturn("{{task reference=\"Task_1\" status=\"Done\"}}");

        this.taskInit.run(this.spaceRef);

        assertEquals(new SimpleDateFormat(DATE_FORMAT).format(docV2Date),
            task1.getParameter(Task.CREATE_DATE));
        assertEquals(new SimpleDateFormat(DATE_FORMAT).format(docV3Date),
            task1.getParameter(Task.COMPLETE_DATE));
        assertEquals("XWiki.Admin", task1.getParameter(Task.REPORTER));

        verify(this.doc).setContent(this.xdom);
        verify(this.context.getWiki()).saveDocument(this.doc, "Inferred createDate parameter for tasks.", this.context);
    }

    @Test
    void taskCreatedInTheFirstVersionOfTheDocument() throws XWikiException, DifferentiationFailedException
    {
        Map<String, String> macroParams = new HashMap<>();
        macroParams.put("reference", "Task_1");
        macroParams.put("status", "Done");
        MacroBlock task1 = new MacroBlock(Task.MACRO_NAME, macroParams, true);
        when(this.xdom.getBlocks(any(), any())).thenReturn(Collections.singletonList(task1));
        when(this.doc.getRevisions(this.context)).thenReturn(new Version[] { new Version(1, 1), new Version(2, 1),
            new Version(3, 1) });

        when(this.revisionProvider.getRevision(this.doc, "1.1")).thenReturn(this.doc);
        this.calendar.set(2023, Calendar.JANUARY, 1);
        Date docV1Date = calendar.getTime();
        when(this.doc.getDate()).thenReturn(docV1Date);
        when(this.doc.getContent()).thenReturn("{{task reference=\"Task_1\" status=\"Done\"}}");

        AddDelta delta12 = mock(AddDelta.class);
        ChangeDelta delta23 = mock(ChangeDelta.class);
        Chunk chunk12 = mock(Chunk.class);
        Chunk chunk23 = mock(Chunk.class);

        when(this.doc.getContentDiff("1.1", "2.1", this.context)).thenReturn(Collections.singletonList(delta12));
        when(this.doc.getContentDiff("2.1", "3.1", this.context)).thenReturn(Collections.singletonList(delta23));

        when(delta12.getRevised()).thenReturn(chunk12);
        when(chunk12.toString()).thenReturn("Nothing important added!");
        when(delta23.getRevised()).thenReturn(chunk23);
        when(chunk23.toString()).thenReturn("Something unrelated changed.");

        this.taskInit.run(this.spaceRef);

        assertEquals(new SimpleDateFormat(DATE_FORMAT).format(docV1Date),
            task1.getParameter(Task.CREATE_DATE));
        assertEquals(new SimpleDateFormat(DATE_FORMAT).format(docV1Date),
            task1.getParameter(Task.COMPLETE_DATE));
        assertEquals("XWiki.Admin", task1.getParameter(Task.REPORTER));

        verify(this.doc, never()).getContentDiff(any(String.class), any(String.class), any(XWikiContext.class));
        verify(this.doc).setContent(this.xdom);
        verify(this.context.getWiki()).saveDocument(this.doc, "Inferred createDate parameter for tasks.", this.context);
    }

    @Test
    void noTasksThatNeedUpdating() throws XWikiException, DifferentiationFailedException
    {
        Map<String, String> macroParams = new HashMap<>();
        macroParams.put("reference", "Task_1");
        macroParams.put("status", "Done");
        macroParams.put("createDate", "01/01/2023");
        macroParams.put("completeDate", "01/01/2023");
        MacroBlock task1 = new MacroBlock(Task.MACRO_NAME, macroParams, true);
        when(this.xdom.getBlocks(any(), any())).thenReturn(Collections.singletonList(task1));

        this.taskInit.run(this.spaceRef);

        verify(this.revisionProvider, never()).getRevision(any(XWikiDocument.class), any(String.class));
        verify(this.doc, never()).getContentDiff(any(String.class), any(String.class), any(XWikiContext.class));
        verify(this.doc, never()).setContent(this.xdom);
        verify(this.context.getWiki(), never()).saveDocument(this.doc, "Inferred createDate parameter for tasks.",
            this.context);
    }
}
