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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

import com.xwiki.date.DateMacroConfiguration;

/**
 * The default implementation of {@link DateMacroConfiguration}.
 *
 * @version $Id$
 * @since 3.5.0
 */
@Component
@Singleton
@Unstable
public class DefaultDateConfiguration implements DateMacroConfiguration
{
    private static final String STORAGE_FORMAT_KEY = "storageDateFormat";

    private static final String DISPLAY_FORMAT_KEY = "displayDateFormat";

    private static final String DEFAULT_DATE_FORMAT = "yyyy/MM/dd HH:mm";

    private static final String PROPERTIES_PREFIX = "datemacro.";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    @Named(DateMacroConfigurationSource.HINT)
    private ConfigurationSource configurationSource;

    @Override
    public String getStorageDateFormat()
    {
        return getProperty(STORAGE_FORMAT_KEY, DEFAULT_DATE_FORMAT);
    }

    @Override
    public String getDisplayDateFormat()
    {
        return getProperty(DISPLAY_FORMAT_KEY, DEFAULT_DATE_FORMAT);
    }

    private <T> T getProperty(String key, T defaultValue)
    {
        if (this.configurationSource.containsKey(key)) {
            return this.configurationSource.getProperty(key, defaultValue);
        } else {
            return this.xwikiProperties.getProperty(PROPERTIES_PREFIX + key, defaultValue);
        }
    }
}
