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
package com.xwiki.task.ckeditor;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.task.internal.ckeditor.CKEditorTaskPluginActivatorListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
public class CKEditorTaskPluginActivatorListenerTest
{
    @InjectMockComponents
    private CKEditorTaskPluginActivatorListener activatorListener;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userRefResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xWiki;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private DocumentAuthors documentAuthors;

    @Mock
    private BaseObject baseObject;

    @BeforeEach
    void setup() throws QueryException, XWikiException
    {

        when(contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(any(LocalDocumentReference.class), eq(context))).thenReturn(xWikiDocument);
        when(xWikiDocument.getXObject(any(LocalDocumentReference.class), eq(true), eq(context))).thenReturn(baseObject);
        when(xWikiDocument.isNew()).thenReturn(false);
        when(xWikiDocument.getAuthors()).thenReturn(documentAuthors);
    }

    @Test
    void initializationHappensAtXWikiStartUpTest() throws InitializationException
    {
        when(contextProvider.get()).thenReturn(null);
        activatorListener.initialize();
        verifyNoInteractions(context);
    }

    @Test
    void initializationAlreadyHappenedTest() throws InitializationException, XWikiException
    {
        when(baseObject.getLargeStringValue("advanced")).thenReturn("config.extraPlugins = 'xwiki-task-insert';");
        activatorListener.initialize();
        verify(baseObject, never()).set(any(), any(), any());
        verify(xWiki, never()).saveDocument(any(), any(), any());
    }

    @Test
    void editorConfigHasNeverBeenSavedTest() throws InitializationException, XWikiException
    {
        when(baseObject.getLargeStringValue("advanced")).thenReturn("");
        when(xWikiDocument.isNew()).thenReturn(true);
        activatorListener.initialize();
        verify(baseObject).set("removePlugins", "bidi,colorbutton,font,justify,save,sourcearea", context);
        verify(baseObject).set("removeButtons",
            "Anchor,BulletedList,Copy,CopyFormatting,Cut,Find,HorizontalRule,Indent,Language"
                + ",NumberedList,Outdent,Paste,PasteFromWord,PasteText,RemoveFormat,SpecialChar,Strike"
                + ",Subscript,Superscript,Underline,Unlink,officeImporter,xwiki-macro", context);
        verify(documentAuthors).setCreator(any());
        verify(baseObject).set(eq("advanced"), contains("config.extraPlugins = 'xwiki-task-insert';"), eq(context));
        verify(documentAuthors).setEffectiveMetadataAuthor(any());
        verify(xWiki).saveDocument(eq(xWikiDocument), any(), eq(context));
    }

    @Test
    void configurationAlreadyContainsExtraPlugins() throws InitializationException, XWikiException
    {
        when(baseObject.getLargeStringValue("advanced")).thenReturn(
            "// config.extraPlugins = 'somePlugin';\n"
                + "config.extraPlugins = 'some-plugin,some-other-plugin';");
        activatorListener.initialize();
        verify(baseObject).set(
            "advanced",
            "// config.extraPlugins = 'somePlugin';\n"
                + "config.extraPlugins = 'some-plugin,some-other-plugin,xwiki-task-insert';",
            context);
        verify(documentAuthors).setEffectiveMetadataAuthor(any());
        verify(xWiki).saveDocument(eq(xWikiDocument), any(), eq(context));
    }

    @Test
    void configurationHasEmptyExtraPlugins() throws InitializationException, XWikiException
    {
        when(baseObject.getLargeStringValue("advanced")).thenReturn(
            "config.extraPlugins =\"\"");
        activatorListener.initialize();
        verify(baseObject).set(
            "advanced",
            "config.extraPlugins = \"xwiki-task-insert\"",
            context);
        verify(documentAuthors).setEffectiveMetadataAuthor(any());
        verify(xWiki).saveDocument(eq(xWikiDocument), any(), eq(context));
    }

    @Test
    void configurationHasMoreComplexCode() throws InitializationException, XWikiException
    {
        when(baseObject.getLargeStringValue("advanced")).thenReturn(
            "// 'xwiki-link' is the name of a CKEditor plugin.\n"
                + "config['xwiki-link'] = config['xwiki-link'] || {};\n"
                + "// Set the used plugins.\n"
                + "config.extraPlugins = \"foo,bar\";\n"
                + "// 'autoGenerateLabels' is the configuration parameter for that plugin.\n"
                + "config['xwiki-link'].autoGenerateLabels = true;\n"
        );
        activatorListener.initialize();
        verify(baseObject).set(
            "advanced",
            "// 'xwiki-link' is the name of a CKEditor plugin.\n"
                + "config['xwiki-link'] = config['xwiki-link'] || {};\n"
                + "// Set the used plugins.\n"
                + "config.extraPlugins = \"foo,bar,xwiki-task-insert\";\n"
                + "// 'autoGenerateLabels' is the configuration parameter for that plugin.\n"
                + "config['xwiki-link'].autoGenerateLabels = true;\n",
            context);
        verify(documentAuthors).setEffectiveMetadataAuthor(any());
        verify(xWiki).saveDocument(eq(xWikiDocument), any(), eq(context));
    }
}
