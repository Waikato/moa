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

package moa.clusterers.outliers.utils.mtree;

import java.util.Set;

import moa.clusterers.outliers.utils.mtree.utils.Pair;

/**
 * An object that chooses a pair from a set of data objects.
 *
 * @param <DATA> The type of the data objects.
 */
public interface PromotionFunction<DATA> {
	
	/**
	 * Chooses (promotes) a pair of objects according to some criteria that is
	 * suitable for the application using the M-Tree.
	 * 
	 * @param dataSet The set of objects to choose a pair from.
	 * @param distanceFunction A function that can be used for choosing the 
	 *        promoted objects.
	 * @return A pair of chosen objects.
	 */
	Pair<DATA> process(Set<DATA> dataSet, DistanceFunction<? super DATA> distanceFunction);
	
}
