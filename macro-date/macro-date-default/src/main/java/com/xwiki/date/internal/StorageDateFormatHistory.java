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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Collects, from the history of the configuration pages, the storage date formats used in the past on each wiki. The
 * result is cached per wiki until the configuration page is modified (see {@link StorageDateFormatHistoryListener}).
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component(roles = StorageDateFormatHistory.class)
@Singleton
public class StorageDateFormatHistory
{
    static final LocalDocumentReference CONFIGURATION_REFERENCE =
        new LocalDocumentReference(Arrays.asList("DateMacro", "Code"), "Configuration");

    private static final LocalDocumentReference CONFIGURATION_CLASS_REFERENCE =
        new LocalDocumentReference("ConfigurationClass", CONFIGURATION_REFERENCE.getParent());

    // Before 3.5.0 the storage format was a Task Manager setting.
    private static final LocalDocumentReference LEGACY_CONFIGURATION_REFERENCE =
        new LocalDocumentReference("TaskManager", "Administration");

    private static final LocalDocumentReference LEGACY_CONFIGURATION_CLASS_REFERENCE =
        new LocalDocumentReference("TaskManagerConfigurationClass", LEGACY_CONFIGURATION_REFERENCE.getParent());

    private static final String STORAGE_FORMAT_KEY = "storageDateFormat";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentRevisionProvider revisionProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    @Named("wiki")
    private ConfigurationSource preferencesConfiguration;

    @Inject
    private Logger logger;

    private final Map<String, List<String>> formatsByWiki = new ConcurrentHashMap<>();

    /**
     * @return the storage date formats used on the current wiki, most recent first (the current one included)
     */
    public List<String> getStorageDateFormats()
    {
        return this.formatsByWiki.computeIfAbsent(this.wikiDescriptorManager.getCurrentWikiId(), this::collect);
    }

    /**
     * Forget the formats collected for the given wiki, so that they are collected again on the next call.
     *
     * @param wikiId the wiki whose configuration changed
     */
    public void invalidate(String wikiId)
    {
        this.formatsByWiki.remove(wikiId);
    }

    private List<String> collect(String wikiId)
    {
        Set<String> formats = new LinkedHashSet<>();
        WikiReference wikiReference = new WikiReference(wikiId);
        // An empty value means the fallback of that time was used, see DefaultDateConfiguration.
        String fallback = this.xwikiProperties.getProperty("datemacro." + STORAGE_FORMAT_KEY,
            DefaultDateConfiguration.DEFAULT_DATE_FORMAT);
        collect(new DocumentReference(CONFIGURATION_REFERENCE, wikiReference), CONFIGURATION_CLASS_REFERENCE,
            fallback, formats);
        // Before 3.5.0 the fallback was the date format of the wiki.
        String legacyFallback = this.xwikiProperties.getProperty("task." + STORAGE_FORMAT_KEY,
            this.preferencesConfiguration.getProperty("dateformat", DefaultDateConfiguration.DEFAULT_DATE_FORMAT));
        collect(new DocumentReference(LEGACY_CONFIGURATION_REFERENCE, wikiReference),
            LEGACY_CONFIGURATION_CLASS_REFERENCE, legacyFallback, formats);
        // The history can be missing (imported pages, deleted versions), and most wikis kept the default.
        formats.add(DefaultDateConfiguration.DEFAULT_DATE_FORMAT);
        return Collections.unmodifiableList(new ArrayList<>(formats));
    }

    private void collect(DocumentReference documentReference, LocalDocumentReference classReference,
        String fallback, Set<String> formats)
    {
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            if (document.isNew()) {
                return;
            }
            Version[] versions = document.getRevisions(context);
            for (int i = versions.length - 1; i >= 0; i--) {
                XWikiDocument revision = this.revisionProvider.getRevision(document, versions[i].toString());
                BaseObject object = revision != null ? revision.getXObject(classReference) : null;
                String format = object != null ? object.getStringValue(STORAGE_FORMAT_KEY) : null;
                formats.add(StringUtils.isBlank(format) ? fallback : format);
            }
        } catch (XWikiException e) {
            this.logger.warn("Failed to collect the storage date formats from the history of [{}]. Cause: [{}].",
                documentReference, ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
