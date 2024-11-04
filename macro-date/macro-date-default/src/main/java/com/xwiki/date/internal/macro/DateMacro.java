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
import java.util.List;

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
import org.xwiki.skinx.SkinExtension;

import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.date.DateType;
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
     * The reference to the document that contains the necessary CSS for the Date macro.
     */
    public static final String SKIN_RESOURCES_DOCUMENT_REFERENCE = "DateMacro.Code.SkinExtension";

    @Inject
    private DateMacroConfiguration configuration;

    @Inject
    @Named("ssx")
    private SkinExtension ssx;

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
        this.ssx.use(SKIN_RESOURCES_DOCUMENT_REFERENCE);
        Date paramDate;
        String format = configuration.getStorageDateFormat();
        try {
            if (!StringUtils.isEmpty(parameters.getFormat())) {
                format = parameters.getFormat();
            }
            paramDate = new SimpleDateFormat(format).parse(parameters.getValue());
        } catch (ParseException e) {
            throw new MacroExecutionException(
                String.format("Failed to parse the given date, expected format [%s]!", format));
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
}

