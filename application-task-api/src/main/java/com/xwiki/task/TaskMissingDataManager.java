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
import org.xwiki.model.reference.DocumentReference;

/**
 * Handles operations on task macros that have missing data.
 * @version $Id$
 * @since 3.3
 */
@Role
public interface TaskMissingDataManager
{

    /**
     * Some task macros, such as the ones resulted from a migration, can have some data missing. This method tries to
     * infer the missing data from the history of the page.
     *
     * @throws TaskException if the query to retrieve the pages has failed.
     */
    void inferMissingTaskData() throws TaskException;

    /**
     * Similar to {@link #inferMissingTaskData()} only that the process is run on a given document reference.
     *
     * @param documentReference the reference of the document that will be analyzed
     * @throws TaskException if the retrieval of the document has failed.
     */
    void inferMissingTaskData(DocumentReference documentReference) throws TaskException;

    /**
     * @return a list of pages that contain task macros with missing data.
     */
    List<DocumentReference> getMissingDataTaskOwners() throws TaskException;
}
