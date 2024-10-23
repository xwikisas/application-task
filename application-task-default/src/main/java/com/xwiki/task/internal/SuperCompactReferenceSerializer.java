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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.SymbolScheme;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;

import com.xpn.xwiki.internal.model.reference.CompactStringEntityReferenceSerializer;

/**
 * Serializes a reference relative to a given parameter. For example. we want {@code Space1.Space2.Tasks.Task_0} to be
 * serialized as {@code Tasks.Task_0} relative to {@code Space1.Space2.WebHome}.
 *
 * @version $Id$
 * @since 3.5.2
 */
@Component
@Named("supercompact")
@Singleton
public class SuperCompactReferenceSerializer extends CompactStringEntityReferenceSerializer
{
    private static final String DELIMITER_PAGE = "/";

    @Inject
    private SymbolScheme scheme;

    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        if (!(reference instanceof PageReference) || parameters.length == 0
            || !(parameters[0] instanceof EntityReference))
        {
            return super.serialize(reference, parameters);
        }

        EntityReference relativeToRef = (EntityReference) parameters[0];
        if (relativeToRef.getName().equals("WebHome")) {
            relativeToRef = relativeToRef.getParent();
        }
        if (!(relativeToRef instanceof PageReference)) {
            relativeToRef = convertToPageReference(relativeToRef);
        }
        try {
            String serialization = super.serialize(reference.removeParent(relativeToRef), parameters);
            String pageDelimiter = getPageDelimiter();
            return serialization.startsWith(pageDelimiter) ? serialization : pageDelimiter.concat(serialization);
        } catch (IllegalArgumentException e) {
            return super.serialize(reference, parameters);
        }
    }

    private String getPageDelimiter()
    {
        if (scheme == null) {
            return DELIMITER_PAGE;
        }
        if (!scheme.getSeparatorSymbols().containsKey(EntityType.PAGE)) {
            return DELIMITER_PAGE;
        }
        if (!scheme.getSeparatorSymbols().get(EntityType.PAGE).containsKey(EntityType.PAGE)) {
            return DELIMITER_PAGE;
        }
        return scheme.getSeparatorSymbols().get(EntityType.PAGE).get(EntityType.PAGE).toString();
    }

    private EntityReference convertToPageReference(EntityReference reference)
    {
        EntityReference pageReference = null;
        for (EntityReference refInChain : reference.getReversedReferenceChain()) {
            if (refInChain.getType().equals(EntityType.WIKI)) {
                pageReference = refInChain;
            } else {
                pageReference = new EntityReference(refInChain.getName(), EntityType.PAGE, pageReference);
            }
        }
        return pageReference;
    }
}
