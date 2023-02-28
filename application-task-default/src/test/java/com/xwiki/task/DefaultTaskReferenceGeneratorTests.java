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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.task.internal.DefaultTaskReferenceGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultTaskReferenceGeneratorTests
{
    @InjectMockComponents
    private DefaultTaskReferenceGenerator referenceGenerator;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    private final DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Page");

    private final DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User");

    @BeforeEach
    public void setup() {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getUserReference()).thenReturn(this.userReference);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.authorizationManager.hasAccess(Right.EDIT, this.userReference,
            this.documentReference.getLastSpaceReference())).thenReturn(true);
    }

    @Test
    public void generateMultipleReferencesTest() throws TaskException
    {
        when(this.wiki.exists(any(DocumentReference.class), any())).thenReturn(false);

        DocumentReference generatedReference = this.referenceGenerator.generate(documentReference);
        assertEquals(new DocumentReference("Task_0", documentReference.getLastSpaceReference()), generatedReference);

        DocumentReference generatedReference1 = this.referenceGenerator.generate(documentReference);
        assertEquals(new DocumentReference("Task_1", documentReference.getLastSpaceReference()), generatedReference1);
    }

    @Test
    public void generateReferenceWhenDocumentAlreadyExistsInWiki() throws TaskException
    {
        when(this.wiki.exists(any(DocumentReference.class), any())).thenReturn(false);
        when(this.wiki.exists(new DocumentReference("Task_0", documentReference.getLastSpaceReference()),
            this.context)).thenReturn(true);

        DocumentReference generatedReference = this.referenceGenerator.generate(documentReference);

        assertEquals(new DocumentReference("Task_1", documentReference.getLastSpaceReference()),
            generatedReference);
    }

    @Test
    public void generateReferenceWhenTheUserDoesNotHaveEditRightsOnTheSpace() throws TaskException
    {
        when(this.wiki.exists(any(DocumentReference.class), any())).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.EDIT, this.userReference,
            this.documentReference.getLastSpaceReference())).thenReturn(false);

        DocumentReference generatedReference = this.referenceGenerator.generate(documentReference);

        assertEquals(new DocumentReference("xwiki", "TaskManager", "Task_0"),
            generatedReference);
    }
}
