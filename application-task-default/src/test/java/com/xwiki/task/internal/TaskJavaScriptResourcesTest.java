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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskJavaScriptResources}.
 *
 * @version $Id$
 */
@ComponentTest
class TaskJavaScriptResourcesTest
{
    private static final String RESOURCE = "taskmanager/js/taskMacro.js";

    @InjectMockComponents
    private TaskJavaScriptResources javaScriptResources;

    @MockComponent
    @Named("jsrx")
    private SkinExtension jsrx;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void useWithThePluginEnabled()
    {
        when(this.xwiki.getPlugin("jsrx", this.xcontext)).thenReturn(mock(XWikiPluginInterface.class));

        assertTrue(this.javaScriptResources.use(RESOURCE));

        verify(this.jsrx).use(RESOURCE);
    }

    @Test
    void useWithThePluginDisabled()
    {
        assertFalse(this.javaScriptResources.use(RESOURCE));
        assertFalse(this.javaScriptResources.use(RESOURCE));

        verify(this.jsrx, never()).use(RESOURCE);
        // The warning is logged only once.
        assertEquals(1, this.logCapture.size());
        assertEquals("The Task Manager JavaScript code can't be loaded because the resource skin extension plugin is "
            + "not enabled. Add [com.xpn.xwiki.plugin.skinx.JsResourceSkinExtensionPlugin] to the [xwiki.plugins] "
            + "property of xwiki.cfg.", this.logCapture.getMessage(0));
    }
}
