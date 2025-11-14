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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener responsible with enabling the "xwiki-task-init" CKEditor plugin by adding it to the CKEditor Config wiki
 * page.
 *
 * @version $Id$
 * @since 3.10.0
 */
@Component
@Singleton
@Named("CKEditorTaskPluginActivatorListener")
public class CKEditorTaskPluginActivatorListener extends AbstractEventListener implements Initializable
{
    private static final String PLUGIN_NAME = "xwiki-task-insert";

    private static final String CKEDITOR_SPACE = "CKEditor";

    private static final String PARAM_ADVANCED = "advanced";

    private static final LocalDocumentReference CKEDITOR_CONFIG_REF =
        new LocalDocumentReference(CKEDITOR_SPACE, "Config");

    private static final LocalDocumentReference CKEDITOR_CONFIG_CLASS =
        new LocalDocumentReference(CKEDITOR_SPACE, "ConfigClass");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userRefResolver;

    /**
     * Default constructor.
     */
    public CKEditorTaskPluginActivatorListener()
    {
        super("CKEditorTaskPluginActivatorListener", Collections.emptyList());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Do nothing.
    }

    @Override
    public void initialize() throws InitializationException
    {
        XWikiContext context = contextProvider.get();

        if (context == null) {
            return;
        }
        // Get CKEditor.Config file
        try {
            XWikiDocument document = context.getWiki().getDocument(CKEDITOR_CONFIG_REF, context);
            BaseObject ckeditorCfg = document.getXObject(CKEDITOR_CONFIG_CLASS, true, context);
            UserReference currentUser = userRefResolver.resolve(context.getUserReference());
            if (document.isNew()) {
                // Copied from the ckeditor config.js file - seems to not have changed from 14.10 to 17.x.
                ckeditorCfg.set("removePlugins", "bidi,colorbutton,font,justify,save,sourcearea", context);
                ckeditorCfg.set("removeButtons",
                    "Anchor,BulletedList,Copy,CopyFormatting,Cut,Find,HorizontalRule,Indent,Language"
                        + ",NumberedList,Outdent,Paste,PasteFromWord,PasteText,RemoveFormat,SpecialChar,Strike"
                        + ",Subscript,Superscript,Underline,Unlink,officeImporter,xwiki-macro",
                    context);
                document.getAuthors().setCreator(currentUser);
            }

            String advancedCfgProp = ckeditorCfg.getLargeStringValue(PARAM_ADVANCED);
            if (advancedCfgProp.contains(PLUGIN_NAME)) {
                // The activator (this class) already ran.
                return;
            }
            // Find any non-commented config.extraPlugins line.
            Pattern pattern = Pattern.compile("^\\s*config\\.extraPlugins\\s*=\\s*(['\"])(.*?)\\1", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(advancedCfgProp);
            if (matcher.find()) {
                String quote = matcher.group(1);
                String plugins = matcher.group(2);
                String updatedPlugins = plugins.isEmpty()
                    ? PLUGIN_NAME
                    : plugins + "," + PLUGIN_NAME;

                String replacement = "config.extraPlugins = " + quote + updatedPlugins + quote;

                advancedCfgProp = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
            } else {
                advancedCfgProp = "config.extraPlugins = '" + PLUGIN_NAME + "';\n\n" + advancedCfgProp;
            }
            ckeditorCfg.set(PARAM_ADVANCED, advancedCfgProp, context);

            document.getAuthors().setEffectiveMetadataAuthor(currentUser);
            context.getWiki().saveDocument(document, String.format("Enabled '%s' plugin.", PLUGIN_NAME), context);
        } catch (XWikiException e) {
            logger.warn("Failed to initialize enable the [{}] plugin. Cause: [{}].", PLUGIN_NAME,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
