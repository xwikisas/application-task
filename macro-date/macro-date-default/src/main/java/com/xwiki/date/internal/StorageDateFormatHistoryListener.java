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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Invalidates the storage date formats collected by {@link StorageDateFormatHistory} when the configuration changes.
 *
 * @version $Id$
 * @since 3.12.0
 */
@Component
@Named(StorageDateFormatHistoryListener.ROLE_HINT)
@Singleton
public class StorageDateFormatHistoryListener extends AbstractEventListener
{
    /**
     * The role hint.
     */
    public static final String ROLE_HINT = "com.xwiki.date.internal.StorageDateFormatHistoryListener";

    @Inject
    private StorageDateFormatHistory history;

    /**
     * Default constructor.
     */
    public StorageDateFormatHistoryListener()
    {
        super(ROLE_HINT,
            Arrays.asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReference reference = ((XWikiDocument) source).getDocumentReference();
        if (StorageDateFormatHistory.CONFIGURATION_REFERENCE.equals(reference.getLocalDocumentReference())) {
            this.history.invalidate(reference.getWikiReference().getName());
        }
    }
}
