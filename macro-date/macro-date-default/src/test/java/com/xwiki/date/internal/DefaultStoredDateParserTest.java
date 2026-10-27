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
package com.xwiki.date.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.date.DateMacroConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultStoredDateParser}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultStoredDateParserTest
{
    private static final String CURRENT_FORMAT = "dd/MM/yyyy";

    private static final String PREVIOUS_FORMAT = "yyyy/MM/dd";

    @InjectMockComponents
    private DefaultStoredDateParser parser;

    @MockComponent
    private DateMacroConfiguration configuration;

    @MockComponent
    private StorageDateFormatHistory history;

    @BeforeEach
    void setUp()
    {
        when(this.configuration.getStorageDateFormat()).thenReturn(CURRENT_FORMAT);
        when(this.history.getStorageDateFormats()).thenReturn(Arrays.asList(CURRENT_FORMAT, PREVIOUS_FORMAT));
    }

    @Test
    void parseWithCurrentFormat() throws Exception
    {
        assertEquals(date("2026-02-24 00:00"), this.parser.parse("24/02/2026", null));
        verify(this.history, never()).getStorageDateFormats();
    }

    @Test
    void parseWithPreviousFormat() throws Exception
    {
        // Leniently parsed with the current format, this would be the 18th of August 0029.
        assertEquals(date("2026-02-24 00:00"), this.parser.parse("2026/02/24", ""));
    }

    @Test
    void parseRequiresTheWholeValueToMatch() throws Exception
    {
        when(this.configuration.getStorageDateFormat()).thenReturn(PREVIOUS_FORMAT);
        when(this.history.getStorageDateFormats())
            .thenReturn(Arrays.asList(PREVIOUS_FORMAT, DefaultDateConfiguration.DEFAULT_DATE_FORMAT));

        assertEquals(date("2026-02-24 10:30"), this.parser.parse("2026/02/24 10:30", null));
    }

    @Test
    void parseSkipsInvalidPreviousFormats() throws Exception
    {
        when(this.history.getStorageDateFormats()).thenReturn(Arrays.asList("invalid", PREVIOUS_FORMAT));

        assertEquals(date("2026-02-24 00:00"), this.parser.parse("2026/02/24", null));
    }

    @Test
    void parseFallsBackOnLenientParsing() throws Exception
    {
        assertEquals(date("2026-03-02 00:00"), this.parser.parse("30/02/2026", null));
    }

    @Test
    void parseWithExplicitFormat() throws Exception
    {
        assertEquals(date("2026-02-24 00:00"), this.parser.parse("24.02.2026", "dd.MM.yyyy"));
        verify(this.history, never()).getStorageDateFormats();
    }

    @Test
    void parseInvalidValue()
    {
        when(this.history.getStorageDateFormats()).thenReturn(Collections.singletonList(PREVIOUS_FORMAT));

        assertThrows(ParseException.class, () -> this.parser.parse("not a date", null));
        assertThrows(ParseException.class, () -> this.parser.parse("", null));
        assertThrows(ParseException.class, () -> this.parser.parse(null, null));
    }

    @Test
    void getStorageDateFormats()
    {
        assertEquals(Arrays.asList(CURRENT_FORMAT, PREVIOUS_FORMAT), this.parser.getStorageDateFormats());

        // The current format comes first even when it was used before.
        when(this.configuration.getStorageDateFormat()).thenReturn(PREVIOUS_FORMAT);
        assertEquals(Arrays.asList(PREVIOUS_FORMAT, CURRENT_FORMAT), this.parser.getStorageDateFormats());
    }

    private static Date date(String value) throws ParseException
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(value);
    }
}
