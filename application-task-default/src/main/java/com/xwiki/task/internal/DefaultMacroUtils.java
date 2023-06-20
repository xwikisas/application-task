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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xwiki.task.MacroUtils;

/**
 * Default implementation of {@link MacroUtils}.
 *
 * @version $Id$
 * @since 3.0.3
 */
@Component
@Singleton
public class DefaultMacroUtils implements MacroUtils
{
    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private MacroContentParser contentParser;

    @Inject
    private MacroManager macroManager;

    @Override
    public void updateMacroContent(MacroBlock macro, String newContent)
    {
        if (macro.getParent() == null) {
            return;
        }
        List<Block> siblings = macro.getParent().getChildren();
        int macroIndex = siblings.indexOf(macro);
        siblings.remove(macroIndex);
        MacroBlock newMacroBlock =
            new MacroBlock(macro.getId(), macro.getParameters(), newContent, macro.isInline());
        siblings.add(macroIndex, newMacroBlock);
    }

    @Override
    public XDOM getMacroContentXDOM(MacroBlock macroBlock, Syntax syntax) throws MacroExecutionException
    {
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setCurrentMacroBlock(macroBlock);
        macroContext.setSyntax(syntax);
        return this.contentParser.parse(macroBlock.getContent(), macroContext, true, false);
    }

    @Override
    public String renderMacroContent(List<Block> contentBlocks, Syntax syntax)
        throws ComponentLookupException
    {
        BlockRenderer renderer = contextComponentManager.getInstance(BlockRenderer.class, syntax.toIdString());
        WikiPrinter printer = new DefaultWikiPrinter(new StringBuffer());
        renderer.render(contentBlocks, printer);
        return printer.toString();
    }

    @Override
    public boolean isMacroContentParsable(String macroId)
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
