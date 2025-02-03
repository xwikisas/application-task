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
package com.xwiki.date.macro;

import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.stability.Unstable;
import org.xwiki.properties.annotation.PropertyDisplayHidden;
import com.xwiki.date.DateType;

/**
 * @version $Id$
 * @since 3.0
 */
@Unstable
public class DateMacroParameters
{
    private String value;

    private String format;

    private String displayFormat;

    /**
     * @return the string representation of the datetype.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value the string representation of the datetype.
     */
    @PropertyDisplayType(DateType.class)
    @PropertyMandatory
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return the format that will be used to parse the datetype parameter.
     */
    public String getFormat()
    {
        return format;
    }

    /**
     * @param format see {@link #getFormat()}
     */
    @PropertyDisplayHidden
    public void setFormat(String format)
    {
        this.format = format;
    }

    /**
     * @return the format in which the value parameter will be displayed.
     */
    public String getDisplayFormat()
    {
        return displayFormat;
    }

    /**
     * @param displayFormat see {@link #getDisplayFormat()}
     */
    @PropertyDisplayHidden
    public void setDisplayFormat(String displayFormat)
    {
        this.displayFormat = displayFormat;
    }
}
