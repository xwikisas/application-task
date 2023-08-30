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
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.task.TaskConfiguration;

/**
 * Base class for {@link TaskObjectUpdateEventListener} and {@link TaskMacroUpdateEventListener}.
 *
 * @version $Id$
 * @since 3.0
 */
public abstract class AbstractTaskEventListener extends AbstractEventListener
{
    /**
     * The name of the space of the application.
     */
    public static final String TASK_MANAGER_SPACE = "TaskManager";

    /**
     * The reference to the TaskManagerClass page.
     */
    public static final LocalDocumentReference TASK_CLASS_REFERENCE =
        new LocalDocumentReference(Collections.singletonList(TASK_MANAGER_SPACE), "TaskManagerClass");

    protected static final String TASK_UPDATE_FLAG = "taskUpdating";

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    protected TaskXDOMProcessor taskXDOMProcessor;

    @Inject
    protected Logger logger;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private TaskConfiguration configuration;

    /**
     * @param name the name of the listener used to identify it.
     * @param events the list of events the listener will be configured to receive.
     */
    public AbstractTaskEventListener(String name, List<? extends Event> events)
    {
        super(name, events);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.observationContext.isIn(
            otherEvent -> otherEvent instanceof BeginFoldEvent && !configuration.getNotSkippedFoldEvents()
                .contains(otherEvent.getClass().getName())))
        {
            return;
        }
        XWikiContext context = (XWikiContext) data;
        XWikiDocument document = (XWikiDocument) source;

        processEvent(document, context, event);
    }

    protected abstract void processEvent(XWikiDocument document, XWikiContext context, Event event);
}
