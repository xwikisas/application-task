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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.webjars.script.WebJarsScriptService;

import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.date.DateType;
import com.xwiki.date.StoredDateParser;
import com.xwiki.date.macro.DateMacroParameters;

/**
 * A Date macro that will display a specified {@link DateType} in a pretty way and a configurable format.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Named(DateMacro.MACRO_NAME)
@Singleton
public class DateMacro extends AbstractMacro<DateMacroParameters>
{
    /**
     * The name of the macro.
     */
    public static final String MACRO_NAME = "date";

    /**
     * The id of the WebJar holding the style sheet of the Date macro.
     */
    public static final String WEBJAR_ID = "com.xwiki.date:macro-date-webjar";

    @Inject
    private DateMacroConfiguration configuration;

    @Inject
    private StoredDateParser parser;

    // The style sheet is not loaded from a wiki page (SSX), because it would be missing for the users that can't view
    // that page.
    @Inject
    @Named("linkx")
    private SkinExtension linkx;

    @Inject
    @Named("webjars")
    private ScriptService webjarsScriptService;

    /**
     * The default Constructor for the Date macro.
     */
    public DateMacro()
    {
        super("Date", "Insert a date that will be displayed nicely.", DateMacroParameters.class);
        setDefaultCategories(Collections.singleton(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(DateMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        useStyleSheet();
        Date paramDate;
        try {
            paramDate = parser.parse(parameters.getValue(), parameters.getFormat());
        } catch (ParseException e) {
            throw new MacroExecutionException(String.format("Failed to parse the given date, expected format [%s]!",
                StringUtils.defaultIfEmpty(parameters.getFormat(), configuration.getStorageDateFormat())));
        }

        String displayFormat =
            StringUtils.isEmpty(parameters.getDisplayFormat()) ? configuration.getDisplayDateFormat()
                : parameters.getDisplayFormat();
        String displayDate = new SimpleDateFormat(displayFormat).format(paramDate);

        Block returnBlock =
            context.isInline() ? new FormatBlock(Collections.singletonList(new WordBlock(displayDate)), Format.NONE)
                : new GroupBlock(Collections.singletonList(new WordBlock(displayDate)));
        returnBlock.setParameters(Collections.singletonMap("class", "xwiki-date"));

        return Collections.singletonList(returnBlock);
    }

    private void useStyleSheet()
    {
        // The style sheet is a LESS file, evaluated to get the variables of the current color theme.
        String styleSheetURL = ((WebJarsScriptService) this.webjarsScriptService).url(WEBJAR_ID, "dateMacro.less",
            Collections.singletonMap("evaluate", true));
        Map<String, Object> linkParameters = new HashMap<>();
        linkParameters.put("type", "text/css");
        linkParameters.put("rel", "stylesheet");
        this.linkx.use(styleSheetURL, linkParameters);
    }
}

