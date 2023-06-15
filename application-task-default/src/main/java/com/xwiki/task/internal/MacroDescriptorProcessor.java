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

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;

/**
 * Class that will handle processing of macro descriptors.
 *
 * @version $Id$
 * @since 3.0.3
 */
@Component(roles = MacroDescriptorProcessor.class)
@Singleton
public class MacroDescriptorProcessor
{    @Inject
private MacroManager macroManager;

    /**
     * @param macroId the id of a macro. i.e. info
     * @return true if the content of the macro should be parsed, supports xwiki syntax; false if not
     */
    public boolean shouldMacroContentBeParsed(String macroId)
    {
        try {
            Macro<?> macro = macroManager.getMacro(new MacroId(macroId));
            ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
            return contentDescriptor != null && Block.LIST_BLOCK_TYPE.equals(contentDescriptor.getType());
        } catch (MacroLookupException e) {
            return false;
        }
    }


}
