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
package com.xwiki.task.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Listener that assures the compatibility with older versions that upgrade. Migrates the date format preferences from
 * the task application configuration to the new Date macro configuration.
 *
 * @version $Id$
 * @since 3.5.0
 */
@Component
@Named(DateFormatMigrationListener.ROLE_HINT)
@Singleton
public class DateFormatMigrationListener extends AbstractEventListener implements Initializable
{
    /**
     * The role hint.
     */
    public static final String ROLE_HINT = "com.xwiki.task.internal.DateFormatMigrationListener";

    private static final String DATE_STORAGE_FORMAT_KEY = "storageDateFormat";

    private static final String DATE_DISPLAY_FORMAT_KEY = "displayDateFormat";

    @Inject
    @Named("taskmanager")
    private Provider<ConfigurationSource> configurationSourceProvider;

    @Inject
    @Named("datemacro")
    private Provider<ConfigurationSource> dateMacroConfigurationSourceProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DateFormatMigrationListener()
    {
        super(ROLE_HINT, Collections.emptyList());
    }

    @Override
    public void initialize() throws InitializationException
    {
        // When this component is initialized (on instance start up / new version install) it should migrate the date
        // format preferences from the task application configuration to the new Date macro configuration.
        try {
            maybeReplaceDateFormat();
        } catch (Exception e) {
            logger.warn("Failed to migrate the date configuration. Cause: [{}].",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Do nothing as the purpose of this class is fulfilled at initialization time.
    }

    private void maybeReplaceDateFormat() throws ConfigurationSaveException
    {
        Map<String, Object> properties = new HashMap<>();
        ConfigurationSource configurationSource = configurationSourceProvider.get();
        ConfigurationSource dateMacroConfigurationSource = dateMacroConfigurationSourceProvider.get();
        String storageDateFormatProperty = configurationSource.getProperty(DATE_STORAGE_FORMAT_KEY);
        String displayDateFormatProperty = configurationSource.getProperty(DATE_DISPLAY_FORMAT_KEY);
        if (storageDateFormatProperty != null) {
            properties.put(DATE_STORAGE_FORMAT_KEY, storageDateFormatProperty);
        }
        if (displayDateFormatProperty != null) {
            properties.put(DATE_DISPLAY_FORMAT_KEY, displayDateFormatProperty);
        }
        if (!properties.isEmpty()) {
            dateMacroConfigurationSource.setProperties(properties);
            clearOldDateFormatConfiguration(configurationSource);
        }
    }

    private void clearOldDateFormatConfiguration(ConfigurationSource configurationSource)
        throws ConfigurationSaveException
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DATE_STORAGE_FORMAT_KEY, null);
        properties.put(DATE_DISPLAY_FORMAT_KEY, null);
        configurationSource.setProperties(properties);
    }
}
