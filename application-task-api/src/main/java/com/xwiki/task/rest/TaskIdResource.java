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
package com.xwiki.task.rest;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Provides operations on the task macros of a page.
 *
 * @version $Id$
 * @since 3.1
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/tasks/generate-reference")
@Unstable
public interface TaskIdResource
{
    /**
     * Generate an id for a task macro residing in a given page.
     *
     * @param wikiName the name of the wiki in which the page resides
     * @param spaces the spaces of the page
     * @param pageName the name of the page
     * @return 200 and a serialized, unused reference of a task. The reference will either be a child or sibling to the
     *     given page, depending on the user rights.
     * @throws XWikiRestException if the generation of the reference fails. Error code 500.
     */
    @GET
    String generateId(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaces,
        @PathParam("pageName") String pageName
    ) throws XWikiRestException;
}
