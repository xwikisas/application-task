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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;

/**
 * Display a custom template for {@link TaskChangedEvent}.
 * 
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Singleton
@Named("com.xwiki.task.internal.notifications.TaskChangedEventDisplayer")
public class TaskChangedEventDisplayer implements NotificationDisplayer
{
    protected static final List<String> EVENTS = Arrays.asList(TaskChangedEvent.class.getCanonicalName());

    protected static final String EVENT_BINDING_NAME = "compositeEvent";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Logger logger;

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
        Template customTemplate =
            this.templateManager.getTemplate("notification/com.xwiki.task.internal.notifications.TaskChangedEvent.vm");

        try {
            // Bind the event to some variable in the velocity context.
            scriptContext.setAttribute(EVENT_BINDING_NAME, eventNotification, ScriptContext.ENGINE_SCOPE);

            return this.templateManager.execute(customTemplate);
        } catch (Exception e) {
            logger.warn("Failed to render custom template. Cause:", e);
        }
        return null;
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return EVENTS;
    }
}
