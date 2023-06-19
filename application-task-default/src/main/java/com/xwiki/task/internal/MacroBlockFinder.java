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

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;

import com.xwiki.task.MacroUtils;

/**
 * Class that is used to recursively traverse a {@link XDOM} and look for macro blocks, including those that are
 * nested.
 *
 * @version $Id$
 * @since 3.0.3
 */
@Component(roles = MacroBlockFinder.class)
@Singleton
public class MacroBlockFinder
{
    /**
     * An enum defining the actions to be done after finding a macro in the XDOM.
     */
    public enum Lookup
    {
        /**
         * The lookup for other macros should halt.
         */
        BREAK,
        /**
         * The lookup for other macros can continue without parsing the content of the processed macro.
         */
        SKIP,
        /**
         * The lookup for other macros can continue while also parsing the content of the processed macro.
         */
        CONTINUE
    }

    @Inject
    private MacroUtils macroUtils;

    @Inject
    private Logger logger;

    /**
     * @param content the content that will be searched recursively for macro blocks.
     * @param syntax the syntax of the content.
     * @param function function the function that is executed when a macro block is found. It receives the found
     *     {@link MacroBlock} and returns a {@link Lookup} value. BREAK if the lookup should stop; SKIP if the content
     *     of the current macro should not be parsed; CONTINUE if the content of the current macro should be parsed.
     * @return the modified content.
     */
    public XDOM visit(XDOM content, Syntax syntax, Function<MacroBlock, Lookup> function)
    {
        List<MacroBlock> macros = content.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT);
        for (MacroBlock macro : macros) {
            Lookup lookup = function.apply(macro);
            if (lookup.equals(Lookup.BREAK)) {
                break;
            } else if (lookup.equals(Lookup.SKIP)) {
                continue;
            }

            if (macro.getContent() != null && !macro.getContent().isEmpty()
                && this.macroUtils.isMacroContentParsable(macro.getId()))
            {
                try {
                    XDOM updatedContent =
                        visit(this.macroUtils.getMacroContentXDOM(macro, syntax), syntax, function);
                    macroUtils.updateMacroContent(macro,
                        macroUtils.renderMacroContent(updatedContent.getChildren(), syntax));
                } catch (ComponentLookupException | MacroExecutionException e) {
                    logger.warn("Failed to update the content of the macro identified by [{}]. Cause: [{}]",
                        macro.getId(), ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        return content;
    }
}
