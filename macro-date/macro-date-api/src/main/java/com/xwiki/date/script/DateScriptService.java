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
package com.xwiki.date.script;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.date.StoredDateParser;

/**
 * Script service for retrieving information about the Date macro.
 *
 * @version $Id$
 * @since 3.5.0
 */
@Component
@Named("datemacro")
@Singleton
@Unstable
public class DateScriptService implements ScriptService
{
    @Inject
    private DateMacroConfiguration configuration;

    @Inject
    private StoredDateParser parser;

    /**
     * @return the configuration of the Date macro.
     */
    public DateMacroConfiguration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * @param value a date stored in a macro parameter
     * @return the parsed date, or {@code null} if the value can't be parsed
     * @see StoredDateParser#parse(String, String)
     * @since 3.12.0
     */
    @Unstable
    public Date parse(String value)
    {
        return parse(value, null);
    }

    /**
     * @param value a date stored in a macro parameter
     * @param format the format the value was explicitly written in, or {@code null} if it was written in a storage
     *     format
     * @return the parsed date, or {@code null} if the value can't be parsed
     * @see StoredDateParser#parse(String, String)
     * @since 3.12.0
     */
    @Unstable
    public Date parse(String value, String format)
    {
        try {
            return this.parser.parse(value, format);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * @return the storage formats used to parse the stored dates, the current one first, then the previous ones, most
     *     recent first
     * @see StoredDateParser#getStorageDateFormats()
     * @since 3.12.0
     */
    @Unstable
    public List<String> getStorageDateFormats()
    {
        return this.parser.getStorageDateFormats();
    }
}
