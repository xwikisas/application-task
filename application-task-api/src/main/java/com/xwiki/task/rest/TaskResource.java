package com.xwiki.task.rest;

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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

import com.xwiki.task.model.Task;

/**
 * Provides operations on task pages.
 *
 * @version $Id$
 * @since 3.0
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/task")
@Unstable
public interface TaskResource
{
    /**
     * Modify the status of a Task macro.
     *
     * @param wikiName the name of the wiki in which the page resides
     * @param spaces the spaces of the page
     * @param pageName the name of the page
     * @param status whether the task has been completed or not
     * @return 200 is the status has been changed successfully of 404 if the task was not found
     * @throws XWikiRestException when failing in retrieving the document or saving it
     */
    // TODO: Replace with a generic method that can be used to modify any value of the task.
    @PUT
    Response changeTaskStatus(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaces,
        @PathParam("pageName") String pageName,
        @QueryParam("status") @DefaultValue(Task.STATUS_DONE) String status
    ) throws XWikiRestException;
}
