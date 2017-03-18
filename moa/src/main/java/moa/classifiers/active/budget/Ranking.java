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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class is used to rank an instance.
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class Ranking<T extends Comparable<T>> {

	/**
	 * This class is used to sort indices by data from another array.
	 * 
	 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
	 * @version $Revision: 1 $
	 */
	private class IndexComparator implements Comparator<Integer>
	{
		List<T> data;
		
		public IndexComparator(List<T> data) {
			this.data = data;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			return data.get(o1).compareTo(data.get(o2));
		}
	}
	
	/**
	 * return the indices of the sorted data list
	 * @param data to be sorted data
	 * @return the index list on the sorted state
	 */
	public List<Integer> rank(List<T> data, int index)
	{
		Comparator<Integer> comparator = new IndexComparator(data);
		List<Integer> indexList = generateIndexList(data.size());
		indexList.sort(comparator);
		return indexList;
	}
	
	/**
	 * generates a list containing indices from an array's length
	 * @param numData the array's length
	 * @return a list with all indices
	 */
	private List<Integer> generateIndexList(int numData)
	{
		List<Integer> list = new ArrayList<>();
		for(int i = 0; i < numData; ++i)
		{
			list.add(i);
		}
		
		return list;
	}
}
