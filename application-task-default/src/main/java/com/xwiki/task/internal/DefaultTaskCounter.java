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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xwiki.task.TaskCounter;
import com.xwiki.task.TaskException;

/**
 * The default implementation of {@link com.xwiki.task.TaskCounter}.
 *
 * @version $Id$
 * @since 3.0
 */
@Component
@Singleton
public class DefaultTaskCounter implements TaskCounter
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private ModelContext modelContext;

    private final Map<String, Integer> counterMap = new HashMap<>();

    @Override
    public synchronized int getNextNumber() throws TaskException
    {
        String statement =
            "select max(taskObject.number) "
                + "from Document doc, doc.object(TaskManager.TaskManagerClass) as taskObject "
                + "where taskObject.number is not null";
        try {
            EntityReference reference = modelContext.getCurrentEntityReference();
            if (reference == null) {
                throw new TaskException("There is no object placed in the model context.");
            }
            String wiki = reference.getName();
            List<Integer> result = queryManager.createQuery(statement, Query.XWQL).setWiki(wiki).execute();
            int number = 0;
            if (result.size() > 0 && result.get(0) != null) {
                number = result.get(0);
            }
            counterMap.put(wiki, Integer.max(number, counterMap.getOrDefault(wiki, -1)) + 1);
            return counterMap.get(wiki);
        } catch (QueryException | TaskException e) {
            throw new TaskException("Failed to get the next valid number.", e);
        }
    }
}
