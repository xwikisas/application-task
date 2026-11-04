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
package com.xwiki.date.internal.macro;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.webjars.script.WebJarsScriptService;

import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.date.StoredDateParser;
import com.xwiki.date.macro.DateMacroParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DateMacro}.
 *
 * @version $Id$
 */
@ComponentTest
class DateMacroTest
{
    private static final String STYLE_SHEET_URL = "/xwiki/webjars/macro-date-webjar/1.0/dateMacro.less?evaluate=true";

    @InjectMockComponents
    private DateMacro macro;

    @MockComponent
    private DateMacroConfiguration configuration;

    @MockComponent
    private StoredDateParser parser;

    @MockComponent
    @Named("linkx")
    private SkinExtension linkx;

    @MockComponent(classToMock = WebJarsScriptService.class)
    @Named("webjars")
    private ScriptService webjarsScriptService;

    @Test
    void executeLoadsTheStyleSheetFromTheWebJar() throws Exception
    {
        DateMacroParameters parameters = new DateMacroParameters();
        parameters.setValue("2023/01/31 10:20");
        when(this.parser.parse("2023/01/31 10:20", null))
            .thenReturn(new GregorianCalendar(2023, 0, 31, 10, 20).getTime());
        when(this.configuration.getDisplayDateFormat()).thenReturn("dd.MM.yyyy");
        when(((WebJarsScriptService) this.webjarsScriptService).url(DateMacro.WEBJAR_ID, "dateMacro.less",
            Collections.singletonMap("evaluate", true))).thenReturn(STYLE_SHEET_URL);
        MacroTransformationContext context = new MacroTransformationContext();
        context.setInline(true);

        List<Block> result = this.macro.execute(parameters, null, context);

        assertEquals(1, result.size());
        assertInstanceOf(FormatBlock.class, result.get(0));
        assertEquals("xwiki-date", result.get(0).getParameter("class"));
        verify(this.linkx).use(STYLE_SHEET_URL, Map.of("type", "text/css", "rel", "stylesheet"));
    }

    @Test
    void executeInBlockMode() throws Exception
    {
        DateMacroParameters parameters = new DateMacroParameters();
        parameters.setValue("31/01/2023");
        parameters.setFormat("dd/MM/yyyy");
        parameters.setDisplayFormat("yyyy-MM-dd");
        when(this.parser.parse("31/01/2023", "dd/MM/yyyy")).thenReturn(new GregorianCalendar(2023, 0, 31).getTime());

        List<Block> result = this.macro.execute(parameters, null, new MacroTransformationContext());

        assertInstanceOf(GroupBlock.class, result.get(0));
        assertEquals("2023-01-31", ((WordBlock) result.get(0).getChildren().get(0)).getWord());
    }
}
