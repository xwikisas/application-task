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
package com.xwiki.date;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Parses the dates stored in macro parameters (date macro value, task create and complete dates, etc.). Since the
 * storage format can be changed by the administrator, the values written before the change are still in a previous
 * storage format, so they are parsed with the formats used in the past when they don't match the current one.
 *
 * @version $Id$
 * @since 3.12.0
 */
@Role
@Unstable
public interface StoredDateParser
{
    /**
     * Parses a stored date.
     *
     * @param value the stored value
     * @param format the format the value was explicitly written in (e.g. the {@code format} parameter of the date
     *     macro), or {@code null} or empty if the value was written in a storage format
     * @return the parsed date
     * @throws ParseException if the value can't be parsed
     */
    Date parse(String value, String format) throws ParseException;

    /**
     * @return the storage formats tried when parsing a value without an explicit format, the current one first, then
     *     the previous ones, most recent first
     */
    List<String> getStorageDateFormats();
}
