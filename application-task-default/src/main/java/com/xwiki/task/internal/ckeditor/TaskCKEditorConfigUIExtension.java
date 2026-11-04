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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.uiextension.UIExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwiki.date.DateMacroConfiguration;
import com.xwiki.task.TaskConfiguration;
import com.xwiki.task.internal.TaskJavaScriptResources;

/**
 * Loads, on every page, the code that integrates the task macros with the CKEditor instances (the
 * {@code xwiki-task-insert} plugin and the handling of the task checkboxes), along with the server side values it
 * needs. This replaces a JavaScript extension used always, which wasn't loaded for the users that can't view the page
 * holding it.
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component
@Named(TaskCKEditorConfigUIExtension.ID)
@Singleton
public class TaskCKEditorConfigUIExtension implements UIExtension
{
    /**
     * The id of this UI extension.
     */
    public static final String ID = "com.xwiki.task.ckeditor.config";

    /**
     * The JavaScript resource, from the application JAR, that registers the CKEditor plugin.
     */
    public static final String JAVASCRIPT_RESOURCE = "taskmanager/js/ckeditorPluginEnabler.js";

    /**
     * The id of the HTML element holding the configuration, in JSON format.
     */
    public static final String CONFIG_ELEMENT_ID = "taskmanager-ckeditor-config";

    /**
     * Rendered on every page, in view and edit mode, so it can stand for a JavaScript extension used always.
     */
    private static final String EXTENSION_POINT_ID = "org.xwiki.platform.template.header.after";

    private static final Map<String, String> TRANSLATION_KEYS = new LinkedHashMap<>();

    static {
        TRANSLATION_KEYS.put("taskMacroName", "rendering.macro.task.name");
        TRANSLATION_KEYS.put("taskMacroDescription", "rendering.macro.task.description");
        TRANSLATION_KEYS.put("checkboxMacroName", "rendering.macro.checkbox.name");
        TRANSLATION_KEYS.put("checkboxMacroDescription", "rendering.macro.checkbox.description");
        TRANSLATION_KEYS.put("dateMacroName", "rendering.macro.date.name");
        TRANSLATION_KEYS.put("dateMacroDescription", "rendering.macro.date.description");
        TRANSLATION_KEYS.put("macroContentPlaceholder", "ckeditor.plugin.task-insert.placeholder");
    }

    @Inject
    private TaskJavaScriptResources javaScriptResources;

    @Inject
    private TaskConfiguration taskConfiguration;

    @Inject
    private DateMacroConfiguration dateMacroConfiguration;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private Logger logger;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return EXTENSION_POINT_ID;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Collections.emptyMap();
    }

    @Override
    public Block execute()
    {
        if (!this.javaScriptResources.use(JAVASCRIPT_RESOURCE)) {
            // The configuration is useless without the code reading it.
            return new CompositeBlock();
        }

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("defaultInlineStatus", this.taskConfiguration.getDefaultInlineStatus());
        config.put("storageDateFormat", this.dateMacroConfiguration.getStorageDateFormat());
        Map<String, String> translations = new LinkedHashMap<>();
        TRANSLATION_KEYS
            .forEach((name, key) -> translations.put(name, this.localizationManager.getTranslationPlain(key)));
        config.put("translations", translations);

        try {
            // Escape '<' so that the JSON can't close the script element.
            String json = new ObjectMapper().writeValueAsString(config).replace("<", "\\u003c");
            return new RawBlock(String.format("<script id=\"%s\" type=\"application/json\">%s</script>",
                CONFIG_ELEMENT_ID, json), Syntax.XHTML_1_0);
        } catch (JsonProcessingException e) {
            this.logger.warn("Failed to serialize the configuration of the task CKEditor plugin. Root cause: [{}]",
                e.getMessage());
            return new CompositeBlock();
        }
    }
}
