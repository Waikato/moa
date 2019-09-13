/*
 *    FixedLengthList.java
 *    Copyright (C) 2018
 *    @author Richard Hugh Moulton
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

package moa.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * FixedLengthList is an extension of an ArrayList with a fixed maximum size. If an element is added that would
 * put the list over its maximum size then the oldest element in the list is removed.
 * 
 * @author Richard Hugh Moulton
 *
 * @param <E> the type of object found in the FixedLengthList
 */
public class FixedLengthList<E> extends ArrayList<E>
{
	private static final long serialVersionUID = 5763493643390042080L;
	
	/**
	 * The fixed length of the FixedLengthList
	 */
	private int maxSize;
	
	/**
	 * Constructor
	 * 
	 * @param m the length of the list
	 */
	public FixedLengthList(int m)
	{
		maxSize = m;
	}
	
	/**
	 * Calls super.add(entry) to append the entry to the end of the FixedLengthList. Removes the oldest entry if required.
	 * Returns true at completion.
	 * 
	 * @param entry the entry to add to the list
	 */
	public boolean add(E entry)
	{
		super.add(entry);
		
		if(this.size() > this.maxSize)
		{
			super.removeRange(0, this.size() - maxSize);
		}
		
		return true;
	}
	
	/**
	 * Appends all of the elements in the argument collection in the order that they are returned by the collection's iterator.
	 * Does so by calling FixedLengthList.add(E entry).
	 */
	public boolean addAll(Collection<? extends E> c)
	{
		Iterator<? extends E> cIterator = c.iterator();
		
		while(cIterator.hasNext())
		{
			this.add(cIterator.next());
		}
		
		return true;
	}
	
	/**
	 * @return the least recently added object in the list.
	 */
	public E getOldestEntry()
	{
		return super.get(0);
	}
	
	/**
	 * @return the most recently added object in the list.
	 */
	public E getYoungestEntry()
	{
		return super.get(this.size()-1);
	}

	/**
	 * @return the size limit of the FixedLengthList.
	 */
	public int getMaxSize()
	{
		return this.maxSize;
	}
}
