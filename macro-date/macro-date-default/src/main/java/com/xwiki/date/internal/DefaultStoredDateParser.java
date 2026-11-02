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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.date.StoredDateParser;

/**
 * Parses the stored dates with the current storage format, then with the previous ones, newest first. The parsing is
 * strict so that a value written in another format isn't silently read as a wrong date (e.g. {@code 2026/02/24} read
 * as {@code dd/MM/yyyy}).
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component
@Singleton
public class DefaultStoredDateParser implements StoredDateParser
{
    @Inject
    private DateMacroConfiguration configuration;

    @Inject
    private StorageDateFormatHistory history;

    @Override
    public Date parse(String value, String format) throws ParseException
    {
        if (StringUtils.isEmpty(value)) {
            throw new ParseException("No date to parse.", 0);
        }
        if (StringUtils.isNotEmpty(format)) {
            return new SimpleDateFormat(format).parse(value);
        }

        String storageFormat = this.configuration.getStorageDateFormat();
        Date date = parseStrictly(value, storageFormat);
        if (date == null) {
            // The history is only collected when needed, since most values are in the current format.
            date = parseWithPreviousFormats(value);
        }
        // Fall back on the lenient parsing used before, so that the values it accepted are still accepted.
        return date != null ? date : new SimpleDateFormat(storageFormat).parse(value);
    }

    @Override
    public List<String> getStorageDateFormats()
    {
        Set<String> formats = new LinkedHashSet<>();
        formats.add(this.configuration.getStorageDateFormat());
        formats.addAll(this.history.getStorageDateFormats());
        return new ArrayList<>(formats);
    }

    private Date parseWithPreviousFormats(String value)
    {
        for (String format : this.history.getStorageDateFormats()) {
            Date date = parseStrictly(value, format);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private Date parseStrictly(String value, String format)
    {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date date = dateFormat.parse(value, position);
            // Unlike parse(String), require the whole value to match the format.
            return position.getIndex() == value.length() ? date : null;
        } catch (IllegalArgumentException e) {
            // Invalid format.
            return null;
        }
    }
}
