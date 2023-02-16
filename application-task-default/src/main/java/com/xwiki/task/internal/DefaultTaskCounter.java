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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xwiki.task.TaskCounter;

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
    private Provider<QueryManager> queryManagerProvider;

    @Inject
    private Logger logger;

    @Override
    public int getNextNumber()
    {
        String statement =
            "select max(taskObject.number) "
                + "from Document doc, doc.object(TaskManager.TaskManagerClass) as taskObject";
        try {
            List<Integer> result = queryManagerProvider.get().createQuery(statement, Query.XWQL).execute();
            if (result.size() > 0 && result.get(0) != null) {
                return result.get(0) + 1;
            } else {
                return 1;
            }
        } catch (QueryException e) {
            logger.warn("Failed to retrieve a valid [number] for a task.");
            return -1;
        }
    }
}
