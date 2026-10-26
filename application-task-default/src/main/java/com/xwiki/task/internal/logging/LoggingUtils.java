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
package com.xwiki.task.internal.logging;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Class containing utility functions for logging.
 *
 * @version $Id$
 * @since 3.11.5
 */
public final class LoggingUtils
{
    /**
     * Matches the characters that are replaced before logging: control characters ({@code \p{Cc}}), the line and
     * paragraph separators (U+2028, U+2029) and the bidirectional formatting characters (U+202A..U+202E,
     * U+2066..U+2069, U+200E, U+200F).
     */
    private static final Pattern REPLACED_CHARS =
        Pattern.compile("[\\p{Cc}\\p{Zl}\\p{Zp}\\u202A-\\u202E\\u2066-\\u2069\\u200E\\u200F]");

    private LoggingUtils()
    {

    }

    /**
     * @param value a string that needs to be prepared for logging.
     * @return the value with its control and formatting characters replaced by {@code _}
     */
    public static String sanitize(String value)
    {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        return REPLACED_CHARS.matcher(value).replaceAll("_");
    }
}
