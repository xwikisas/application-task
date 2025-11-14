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
package com.xwiki.task.internal.listener;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamingEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static com.xwiki.task.internal.AbstractTaskEventListener.TASK_CLASS_REFERENCE;

/**
 * This listener handles the case when a page containing task macros is moved. The task pages associated with the task
 * macros should have their owner updated. I.e. PageA is renamed to PageB together with its children -> owner property
 * of PageB.Tasks.Task_1 should be updated from PageA.WebHome to PageB.WebHome.
 *
 * @version $Id$
 * @since 3.10.0
 */
@Component
@Named("TaskPageMovedListener")
@Singleton
public class TaskPageMovedListener extends TaskPageCopiedListener
{
    private static final String EXECUTION_FLAG = "task-copy-move-listener-flag";

    /**
     * Default constructor.
     */
    public TaskPageMovedListener()
    {
        // Due to the fact that modifications make by listeners do not apply on renamed documents (see:
        // https://github.com/xwiki/xwiki-platform/blob/50dd573dd966b57a42bdc327ebf2e7d42c62cad0/xwiki-platform-core/
        // xwiki-platform-oldcore/src/main/java/com/xpn/xwiki/XWiki.java#L4963
        // ), we need to listen to the DocumentCreatedEvent, change the owner of the renamed document and resave it.
        super("TaskPageMovedListener", List.of(new DocumentCreatedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;

        // Stop recursion after save in the case of moved pages.
        if (context.get(EXECUTION_FLAG) != null) {
            return;
        }
        XWikiDocument document = ((XWikiDocument) source);
        logger.debug("Processing [{}].", document.getDocumentReference());

        if (!observationContext.isIn(otherEvent -> otherEvent instanceof DocumentRenamingEvent)) {
            logger.debug("Document [{}] was not created in a copying or renaming event. Returning.",
                document.getDocumentReference());
            return;
        }
        BaseObject taskObj = document.getXObject(TASK_CLASS_REFERENCE);
        if (taskObj == null) {
            logger.debug("Created document [{}] does not contain a task object.", document.getDocumentReference());
            return;
        }

        boolean changed = false;

        context.put(EXECUTION_FLAG, true);

        try {
            changed = maybeSetNewOwner(taskObj, context);

            maybeSave(changed, context, document);
        } catch (Exception e) {
            logger.error("There was an error during the handling of the copy/move of the task page [{}].",
                document.getDocumentReference(), e);
        } finally {
            context.remove(EXECUTION_FLAG);
        }
    }

    private void maybeSave(boolean changed, XWikiContext context, XWikiDocument document)
    {
        if (changed) {
            try {
                context.getWiki().saveDocument(document, context);
            } catch (XWikiException e) {
                logger.error("Failed to save the document [{}] after updating its id and/or owner.",
                    document.getDocumentReference(), e);
            }
        }
    }
}
