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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xwiki.task.TaskConfiguration;

/**
 * The default implementation of {@link TaskConfiguration}.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Singleton
public class DefaultTaskConfiguration implements TaskConfiguration
{
    private static final String NOT_SKIPPED_FOLD_EVENTS = "notSkippedFoldEvents";

    private static final String METHOD_DEPRECATED = "Method is deprecated";

    @Inject
    @Named("taskmanager")
    private ConfigurationSource configurationSource;

    @Override
    public String getStorageDateFormat()
    {
        throw new NotImplementedException(METHOD_DEPRECATED);
    }

    @Override
    public List<String> getNotSkippedFoldEvents()
    {
        if (this.configurationSource.containsKey(NOT_SKIPPED_FOLD_EVENTS)) {
            String notSkippedEventsString = configurationSource.getProperty(NOT_SKIPPED_FOLD_EVENTS);
            List<String> notSkippedEvents = Arrays.asList(notSkippedEventsString.split("\\s*,\\s*"));
            notSkippedEvents.removeIf(String::isEmpty);
            return notSkippedEvents;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getDisplayDateFormat()
    {
        throw new NotImplementedException(METHOD_DEPRECATED);
    }
}
