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
package com.xwiki.task.internal.ckeditor;

import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.internal.TaskJavaScriptResources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskCKEditorConfigUIExtension}.
 *
 * @version $Id$
 */
@ComponentTest
class TaskCKEditorConfigUIExtensionTest
{
    @InjectMockComponents
    private TaskCKEditorConfigUIExtension uiExtension;

    @MockComponent
    private TaskJavaScriptResources javaScriptResources;

    @MockComponent
    private TaskConfiguration taskConfiguration;

    @MockComponent
    private DateMacroConfiguration dateMacroConfiguration;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @Test
    void execute()
    {
        when(this.javaScriptResources.use(TaskCKEditorConfigUIExtension.JAVASCRIPT_RESOURCE)).thenReturn(true);
        when(this.taskConfiguration.getDefaultInlineStatus()).thenReturn("ToDo");
        when(this.dateMacroConfiguration.getStorageDateFormat()).thenReturn("yyyy/MM/dd HH:mm");
        when(this.localizationManager.getTranslationPlain("rendering.macro.task.name")).thenReturn("Task");
        // A translation must not be able to close the script element.
        when(this.localizationManager.getTranslationPlain("ckeditor.plugin.task-insert.placeholder"))
            .thenReturn("</script><script>alert(1)</script>");

        RawBlock block = assertInstanceOf(RawBlock.class, this.uiExtension.execute());

        String html = block.getRawContent();
        assertTrue(html.startsWith("<script id=\"taskmanager-ckeditor-config\" type=\"application/json\">{"));
        assertTrue(html.endsWith("}</script>"));
        assertEquals(html.indexOf("</script>"), html.length() - "</script>".length());
        assertTrue(html.contains("\"defaultInlineStatus\":\"ToDo\""));
        assertTrue(html.contains("\"storageDateFormat\":\"yyyy/MM/dd HH:mm\""));
        assertTrue(html.contains("\"taskMacroName\":\"Task\""));
        assertTrue(html.contains(
            "\"macroContentPlaceholder\":\"\\u003c/script>\\u003cscript>alert(1)\\u003c/script>\""));
    }

    @Test
    void executeWithoutTheResourceSkinExtensionPlugin()
    {
        when(this.javaScriptResources.use(TaskCKEditorConfigUIExtension.JAVASCRIPT_RESOURCE)).thenReturn(false);

        Block block = this.uiExtension.execute();

        assertInstanceOf(CompositeBlock.class, block);
        assertTrue(block.getChildren().isEmpty());
    }
}
