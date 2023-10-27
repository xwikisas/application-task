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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;

// TODO: Since 3.3 we use PageReferences for the task reference. However, if the reference is a document
//  reference that exists, we update that document. This is done for backwards compatibility. To be
//  removed after ~1 year of the release of v3.3.

/**
 * A class that handles Task References resolving and serialization.
 *
 * @version $Id$
 * @since 3.3
 */
@Component(roles = TaskReferenceUtils.class)
@Singleton
public class TaskReferenceUtils
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private PageReferenceResolver<String> pageReferenceResolver;

    @Inject
    private DocumentReferenceResolver<String> docStringResolver;

    @Inject
    private DocumentReferenceResolver<EntityReference> docRefResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Receives a string representation of a task reference and, it resolves it as either a document reference (if the
     * document exists) or a page reference.
     *
     * @param representation representation of the task reference. It can be a serialized DocumentReference or
     *     PageReference.
     * @param relativeTo the parent of the task reference.
     * @return either a DocumentReference or a PageReference depending on the representation.
     */
    public EntityReference resolve(String representation, EntityReference relativeTo)
    {
        DocumentReference docRef = docStringResolver.resolve(representation, relativeTo);
        if (!documentAccessBridge.exists(docRef)) {
            return pageReferenceResolver.resolve(representation, relativeTo);
        }
        return docRef;
    }

    /**
     * Serialize an entity reference as a DocumentReference.
     *
     * @param reference either a DocumentReference or a PageReference.
     * @param relativeTo the parent of the task reference.
     * @return the reference serialized as a DocumentReference.
     */
    public String serializeAsDocumentReference(EntityReference reference, EntityReference relativeTo)
    {
        if (reference instanceof DocumentReference) {
            return serializer.serialize(reference, relativeTo);
        }
        DocumentReference documentReference = docRefResolver.resolve(reference);
        return serializer.serialize(documentReference, relativeTo);
    }

    /**
     * Receives a string representation of a task reference and, it resolves it as a document reference.
     *
     * @param representation representation of the task reference. It can be a serialized DocumentReference or
     *     PageReference.
     * @param relativeTo the parent of the task reference.
     * @return a DocumentReference.
     */
    public DocumentReference resolveAsDocumentReference(String representation, EntityReference relativeTo)
    {
        EntityReference reference = resolve(representation, relativeTo);
        if (reference instanceof PageReference) {
            return docRefResolver.resolve(reference);
        }
        return (DocumentReference) reference;
    }
}
