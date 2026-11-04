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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.skinx.SkinExtension;

import com.xpn.xwiki.XWikiContext;

/**
 * Loads the JavaScript resources shipped in the application JARs, through the resource skin extension plugin
 * ({@code jsrx}). The plugin is enabled by default in the XWiki distribution, but it can be removed from
 * {@code xwiki.cfg}: the resources are then skipped, instead of failing the rendering of the page.
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component(roles = TaskJavaScriptResources.class)
@Singleton
public class TaskJavaScriptResources
{
    private static final String PLUGIN_NAME = "jsrx";

    @Inject
    @Named(PLUGIN_NAME)
    private SkinExtension jsrx;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    private final AtomicBoolean missingPluginLogged = new AtomicBoolean();

    /**
     * Load the given JavaScript resource in the current page.
     *
     * @param resource the path of the JavaScript resource, in the classpath
     * @return {@code true} if the resource is loaded, {@code false} if the resource skin extension plugin is not
     *     enabled
     */
    public boolean use(String resource)
    {
        XWikiContext xcontext = this.contextProvider.get();
        if (xcontext.getWiki().getPlugin(PLUGIN_NAME, xcontext) == null) {
            // Log only once, since this happens on every page.
            if (this.missingPluginLogged.compareAndSet(false, true)) {
                this.logger.warn("The Task Manager JavaScript code can't be loaded because the resource skin extension "
                    + "plugin is not enabled. Add [com.xpn.xwiki.plugin.skinx.JsResourceSkinExtensionPlugin] to the "
                    + "[xwiki.plugins] property of xwiki.cfg.");
            }
            return false;
        }
        this.jsrx.use(resource);
        return true;
    }
}
