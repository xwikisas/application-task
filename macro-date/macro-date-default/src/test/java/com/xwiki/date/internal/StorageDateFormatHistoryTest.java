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
package com.xwiki.date.internal;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StorageDateFormatHistory}.
 *
 * @version $Id$
 */
@ComponentTest
class StorageDateFormatHistoryTest
{
    private static final String WIKI = "wiki";

    private static final String STORAGE_FORMAT_KEY = "storageDateFormat";

    private static final DocumentReference CONFIGURATION_REFERENCE =
        new DocumentReference(WIKI, Arrays.asList("DateMacro", "Code"), "Configuration");

    private static final DocumentReference LEGACY_CONFIGURATION_REFERENCE =
        new DocumentReference(WIKI, "TaskManager", "Administration");

    @InjectMockComponents
    private StorageDateFormatHistory history;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentRevisionProvider revisionProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @MockComponent
    @Named("wiki")
    private ConfigurationSource preferencesConfiguration;

    private XWikiContext context;

    private XWiki xwiki;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(WIKI);
        when(this.xwikiProperties.getProperty("datemacro.storageDateFormat",
            DefaultDateConfiguration.DEFAULT_DATE_FORMAT)).thenReturn(DefaultDateConfiguration.DEFAULT_DATE_FORMAT);
        when(this.preferencesConfiguration.getProperty("dateformat", DefaultDateConfiguration.DEFAULT_DATE_FORMAT))
            .thenReturn("dd.MM.yyyy");
        when(this.xwikiProperties.getProperty("task.storageDateFormat", "dd.MM.yyyy")).thenReturn("dd.MM.yyyy");

        XWikiDocument legacyDocument = mock(XWikiDocument.class);
        when(legacyDocument.isNew()).thenReturn(true);
        when(this.xwiki.getDocument(LEGACY_CONFIGURATION_REFERENCE, this.context)).thenReturn(legacyDocument);
    }

    @Test
    void getStorageDateFormats() throws Exception
    {
        // Oldest first: installed with the default, then two changes.
        mockHistory(CONFIGURATION_REFERENCE, new LocalDocumentReference(Arrays.asList("DateMacro", "Code"),
            "ConfigurationClass"), "", "yyyy/MM/dd", "dd/MM/yyyy");

        assertEquals(Arrays.asList("dd/MM/yyyy", "yyyy/MM/dd", DefaultDateConfiguration.DEFAULT_DATE_FORMAT),
            this.history.getStorageDateFormats());
    }

    @Test
    void getStorageDateFormatsIncludesTheTaskManagerConfiguration() throws Exception
    {
        mockHistory(CONFIGURATION_REFERENCE, new LocalDocumentReference(Arrays.asList("DateMacro", "Code"),
            "ConfigurationClass"), "dd/MM/yyyy");
        // Before 3.5.0 an empty value meant the date format of the wiki.
        mockHistory(LEGACY_CONFIGURATION_REFERENCE,
            new LocalDocumentReference("TaskManager", "TaskManagerConfigurationClass"), "", "MM-dd-yyyy");

        assertEquals(
            Arrays.asList("dd/MM/yyyy", "MM-dd-yyyy", "dd.MM.yyyy", DefaultDateConfiguration.DEFAULT_DATE_FORMAT),
            this.history.getStorageDateFormats());
    }

    @Test
    void getStorageDateFormatsWithoutHistory() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(true);
        when(this.xwiki.getDocument(CONFIGURATION_REFERENCE, this.context)).thenReturn(document);

        assertEquals(Collections.singletonList(DefaultDateConfiguration.DEFAULT_DATE_FORMAT),
            this.history.getStorageDateFormats());
    }

    @Test
    void getStorageDateFormatsIsCachedUntilInvalidated() throws Exception
    {
        XWikiDocument document = mockHistory(CONFIGURATION_REFERENCE,
            new LocalDocumentReference(Arrays.asList("DateMacro", "Code"), "ConfigurationClass"), "yyyy/MM/dd");

        this.history.getStorageDateFormats();
        this.history.getStorageDateFormats();
        verify(document, times(1)).getRevisions(this.context);

        this.history.invalidate(WIKI);
        this.history.getStorageDateFormats();
        verify(document, times(2)).getRevisions(this.context);
    }

    private XWikiDocument mockHistory(DocumentReference reference, LocalDocumentReference classReference,
        String... formats) throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(reference, this.context)).thenReturn(document);
        Version[] versions = new Version[formats.length];
        for (int i = 0; i < formats.length; i++) {
            versions[i] = new Version(i + 1, 1);
            XWikiDocument revision = mock(XWikiDocument.class);
            BaseObject object = mock(BaseObject.class);
            when(object.getStringValue(STORAGE_FORMAT_KEY)).thenReturn(formats[i]);
            when(revision.getXObject(classReference)).thenReturn(object);
            when(this.revisionProvider.getRevision(document, versions[i].toString())).thenReturn(revision);
        }
        when(document.getRevisions(this.context)).thenReturn(versions);
        return document;
    }
}
