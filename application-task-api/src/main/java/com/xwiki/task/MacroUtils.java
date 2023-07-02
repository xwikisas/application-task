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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Utility class for macro handling.
 *
 * @version $Id$
 * @since 3.0.3
 */
@Role
public interface MacroUtils
{
    /**
     * Update the content of a macro replacing it with a clone that has the desired content.
     *
     * @param macro the macro that needs to be updated. It has to have a parent.
     * @param newContent the new content that will replace the current content of the macro.
     */
    void updateMacroContent(Block macro, String newContent);

    /**
     * Get the XDOM of the content of a macro.
     *
     * @param macroBlock the block whose content we want to retrieve.
     * @param syntax The syntax in which the content is encoded.
     * @return the XDOM of the macro content.
     * @throws MacroExecutionException if parsing fails.
     */
    XDOM getMacroContentXDOM(MacroBlock macroBlock, Syntax syntax) throws MacroExecutionException;

    /**
     * Render the macro content in the given syntax.
     *
     * @param contentBlocks the content of a macro.
     * @param syntax the syntax in which the contentBlocks need to be rendered.
     * @return the result of rendering the content in the given syntax.
     * @throws ComponentLookupException if there is no renderer for the given syntax
     */
    String renderMacroContent(List<Block> contentBlocks, Syntax syntax) throws ComponentLookupException;

    /**
     * @param macroId the id of a macro. i.e. info
     * @return true if the content of the macro can be parsed, supports xwiki syntax; false if not
     */
    boolean isMacroContentParsable(String macroId);
}
