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
package com.xwiki.task;

import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.SymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.PageReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xwiki.task.internal.SuperCompactReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
public class SuperCompactReferenceSerializerTest
{
    @InjectMockComponents
    private SuperCompactReferenceSerializer serializer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("current")
    private EntityReferenceProvider provider;

    private final PageReference pageReference = new PageReference("xwiki", Arrays.asList("Page1", "Page2", "Page3"));

    private final DocumentReference documentReference = new DocumentReference("xwiki", "Space", "WebHome");

    @BeforeComponent
    public void registerComponents(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(SymbolScheme.class, new DefaultSymbolScheme());
    }

    @BeforeEach
    void setup()
    {
        when(provider.getDefaultReference(EntityType.WIKI)).thenReturn(new EntityReference("xwiki", EntityType.WIKI));
    }
    @Test
    void serializePageReference()
    {
        String serialized = serializer.serialize(pageReference);

        assertEquals("Page1/Page2/Page3", serialized);
    }

    @Test
    void serializeDocumentReference() {
        String serialized = serializer.serialize(documentReference);

        assertEquals("Space.WebHome", serialized);
    }

    @Test
    void serializePageReferenceRelativeToParent() {
        String serialized = serializer.serialize(pageReference, pageReference.getParent());

        assertEquals("/Page3", serialized);

        serialized = serializer.serialize(pageReference, pageReference.getParent().getParent());

        assertEquals("/Page2/Page3", serialized);

    }
}
