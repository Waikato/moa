/*
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

package moa.clusterers.outliers.utils.mtree.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Some utilities.
 */
public final class Utils {

    /**
     * Don't let anyone instantiate this class.
     */
	private Utils() {}

	
	/**
	 * Identifies the minimum and maximum elements from an iterable, according
	 * to the natural ordering of the elements.
	 * @param items An {@link Iterable} object with the elements
	 * @param <T> The type of the elements.
	 * @return A pair with the minimum and maximum elements.
	 */
	public static <T extends Comparable<T>> Pair<T> minMax(Iterable<T> items) {
		Iterator<T> iterator = items.iterator();
		if(!iterator.hasNext()) {
			return null;
		}
		
		T min = iterator.next();
		T max = min;
		
		while(iterator.hasNext()) {
			T item = iterator.next();
			if(item.compareTo(min) < 0) {
				min = item;
			}
			if(item.compareTo(max) > 0) {
				max = item;
			}
		}
		
		return new Pair<T>(min, max);
	}
	
	
	/**
	 * Randomly chooses elements from the collection.
	 * @param collection The collection.
	 * @param n The number of elements to choose.
	 * @param <T> The type of the elements.
	 * @return A list with the chosen elements.
	 */
	public static <T> List<T> randomSample(Collection<T> collection, int n) {
		List<T> list = new ArrayList<T>(collection);
		List<T> sample = new ArrayList<T>(n);
		Random random = new Random();
		while(n > 0  &&  !list.isEmpty()) {
			int index = random.nextInt(list.size());
			sample.add(list.get(index));
			int indexLast = list.size() - 1;
			T last = list.remove(indexLast);
			if(index < indexLast) {
				list.set(index, last);
			}
			n--;
		}
		return sample;
	}
	
}
