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

package com.xwiki.task.internal.notifications;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Event sent when a blog post has been published.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Singleton
@Named(TaskChangedEventConverter.NAME)
@Component
public class TaskChangedEventConverter implements RecordableEventConverter
{
    /**
     * The name of this component.
     */
    public static final String NAME = "TaskChangedEventConverter";

    @Inject
    private RecordableEventConverter defaultConverter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Utility method to convert an object to JSON.
     * 
     * @param params the localization parameters to serialize.
     */
    private String serializeParams(Map<String, Object> params)
    {
        String json = null;
        try {
            ObjectWriter ow = new ObjectMapper().writer();
            json = ow.writeValueAsString(params);
        } catch (Exception e) {
            logger.warn("Error while serializing parameters of TaskChangedEvent:", e);
        }
        return json;
    }

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        TaskChangedEvent event = (TaskChangedEvent) recordableEvent;

        XWikiContext context = contextProvider.get();
        Event convertedEvent = this.defaultConverter.convert(event, source, data);
        XWikiDocument document = context.getWiki().getDocument(event.getDocumentReference(), context);

        convertedEvent.setDocument(event.getDocumentReference());
        convertedEvent.setDocumentVersion(event.getDocumentVersion());
        convertedEvent.setDocumentTitle(document.getRenderedTitle(context));
        convertedEvent.setBody(serializeParams(event.getLocalizationParams()));

        return convertedEvent;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(new TaskChangedEvent());
    }
}
