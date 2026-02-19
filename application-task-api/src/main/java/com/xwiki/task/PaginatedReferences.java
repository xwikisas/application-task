package com.xwiki.task;

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

import java.util.List;

import org.xwiki.model.reference.DocumentReference;

/**
 * Represents a paginated result of document references that can be used to display a pagination component i.e.
 * livedata.
 *
 * @version $Id$
 * @since 3.11.0
 */
public class PaginatedReferences
{
    private List<DocumentReference> pages;

    private int count;

    private int offset;

    private int total;

    /**
     * Default constructor.
     */
    public PaginatedReferences()
    {

    }

    /**
     * Builds a paginated result based solely on the returned pages list. The other parameters will default to 0 or the
     * list size.
     *
     * @param pages a list of document references representing a subset of the queried data source.
     */
    public PaginatedReferences(List<DocumentReference> pages)
    {
        this.pages = pages;
        count = pages.size();
        offset = 0;
        total = pages.size();
    }

    /**
     * @return the list of pages retrieved from the data source. The members should be accurate to the offset and count
     *     attributes.
     */
    public List<DocumentReference> getPages()
    {
        return pages;
    }

    /**
     * @param pages see {@link #getPages()}.
     */
    public void setPages(List<DocumentReference> pages)
    {
        this.pages = pages;
    }

    /**
     * @return the number of items contained by the current result.
     */
    public int getCount()
    {
        return count;
    }

    /**
     * @param count see {@link #getCount()}.
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return the starting offset of the returned pages. If the dataset contains 0..n elements, the returned pages
     *     should correspond to the offset..offset+count elements of the dataset.
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * @param offset see {@link #getOffset()}.
     */
    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    /**
     * @return the total number elements contained by the datasource.
     */
    public int getTotal()
    {
        return total;
    }

    /**
     * @param total see {@link #getTotal()}.
     */
    public void setTotal(int total)
    {
        this.total = total;
    }
}
