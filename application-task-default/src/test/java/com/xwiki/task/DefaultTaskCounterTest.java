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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.task.internal.DefaultTaskCounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultTaskCounterTest
{
    @InjectMockComponents
    private DefaultTaskCounter taskCounter;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ModelContext modelContext;

    @Mock
    private Query query;

    private final WikiReference defaultWiki = new WikiReference("wiki");

    @BeforeEach
    void setup() throws QueryException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(defaultWiki);
        when(this.queryManager.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(this.query);
        when(this.query.setWiki(defaultWiki.getName())).thenReturn(this.query);
    }

    @Test
    void getNextNumberOnce() throws QueryException, TaskException
    {
        when(this.query.execute()).thenReturn(Collections.emptyList());

        assertEquals(1, this.taskCounter.getNextNumber());
    }

    @Test
    void getNextNumberTwiceOnTheSameWiki() throws QueryException, TaskException
    {
        when(this.query.execute()).thenReturn(Collections.emptyList());

        assertEquals(1, this.taskCounter.getNextNumber());

        when(this.query.execute()).thenReturn(Collections.singletonList(1));

        assertEquals(2, this.taskCounter.getNextNumber());
    }

    @Test
    void getNextNumberWhenQueryReturnsSameMaxNumber() throws QueryException, TaskException
    {
        when(this.query.execute()).thenReturn(Collections.singletonList(2));

        assertEquals(3, this.taskCounter.getNextNumber());
        assertEquals(4, this.taskCounter.getNextNumber());
    }

    @Test
    void getNextNumberOnTwoDifferentWikis() throws TaskException, QueryException
    {
        when(this.query.execute()).thenReturn(Collections.emptyList());
        assertEquals(1, this.taskCounter.getNextNumber());

        WikiReference subwiki = new WikiReference("subwiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(subwiki);
        when(this.query.setWiki(subwiki.getName())).thenReturn(this.query);

        assertEquals(1, this.taskCounter.getNextNumber());
    }

    @Test
    void getNextNumberOnTwoDifferentWikis2() throws QueryException, TaskException
    {
        when(this.query.execute()).thenReturn(Collections.singletonList(2));
        assertEquals(3, this.taskCounter.getNextNumber());

        WikiReference subwiki = new WikiReference("subwiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(subwiki);
        when(this.query.setWiki(subwiki.getName())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(Collections.singletonList(5));

        assertEquals(6, this.taskCounter.getNextNumber());
    }
}
