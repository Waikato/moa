/*
 *    Ranking.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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
package moa.classifiers.active.budget;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import moa.classifiers.active.RingBuffer;

/**
 * This class is used to rank an instance.
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class Ranking<T extends Comparable<T>> implements Serializable{
	private static final long serialVersionUID = 1L;

	// list of sorted entries
	List<T> sortedEntries;
	// list of indices of sorted entries
	List<Integer> sortedEntryIndices;
	
	/**
	 * constructor
	 */
	public Ranking() {
		sortedEntries = new ArrayList<>();
		sortedEntryIndices = new ArrayList<>();
	}
	
	/**
	 * return the indices of the sorted data list
	 * @param data to be sorted data
	 * @param removedElement the element which was removed (set to null if none was removed)
	 * @return the index list on the sorted state
	 */
	public List<Integer> rank(RingBuffer<T> data, int index, T removedElement)
	{
		
		// remove the element with the index 0, which is the oldest
		if(removedElement != null)
		{
			// find index of the element to remove
			int idx = sortedEntryIndices.indexOf(0);
			// remove the element from both lists
			sortedEntryIndices.remove(idx);
			sortedEntries.remove(idx);
			// decrease all indices by one so that it starts with zero
			for(int i = 0; i < sortedEntryIndices.size(); ++i)
			{
				sortedEntryIndices.set(i, sortedEntryIndices.get(i) - 1);
			}
		}
		
		
		// search for the correct insertion position
		T elementToSort = data.get(index);
		boolean foundCorrectPosition = false;
		int insertionIndex = 0;
		for(insertionIndex = sortedEntries.size() - 1 ; insertionIndex >= 0 && !foundCorrectPosition; --insertionIndex)
		{
			T tmp = sortedEntries.get(insertionIndex);
			foundCorrectPosition = elementToSort.compareTo(tmp) >= 0;
		}
		// correct the insertionIndex 
		++insertionIndex;

		if(foundCorrectPosition)
		{
			// if a position was found insert it there
			sortedEntries.add(insertionIndex+1, elementToSort);
			sortedEntryIndices.add(insertionIndex+1, sortedEntries.size()-1);
			
		}
		else
		{
			// if a position was not found insert it at the beginning
			sortedEntries.add(0, elementToSort);
			sortedEntryIndices.add(0, sortedEntries.size()-1);
		}

		return sortedEntryIndices;
	}
}
