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

/**
 * Updates the taskbox present in a given page, identified by its id.
 *
 * @version $Id$
 * @since 3.8.0
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/taskbox/{id}")
@Unstable
public interface TaskboxResource
{
    /**
     * Update the status of a taskbox macro.
     *
     * @param wikiName the wiki where the taskbox macro is located.
     * @param spaces the spaces of the page where the taskbox macro is located.
     * @param pageName the page where the taskbox macro is located.
     * @param id the id of the taskbox that should be updated.
     * @param checked the new status - either
     * @return status code 200 if it succeeds.
     * @throws XWikiRestException if the status of the taskbox macro could not be changed.
     */
    @PUT
    Response changeTaskStatus(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaces,
        @PathParam("pageName") String pageName,
        @PathParam("id") @DefaultValue("") String id,
        @QueryParam("status") @DefaultValue("true") String checked
    ) throws XWikiRestException;
}
