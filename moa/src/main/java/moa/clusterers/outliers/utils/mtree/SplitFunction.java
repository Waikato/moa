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
 * Defines an object to be used to split a node in an M-Tree. A node must be
 * split when it has reached its maximum capacity and a new child node would be
 * added to it.
 * 
 * <p>The splitting consists in choosing a pair of "promoted" data objects from
 * the children and then partition the set of children in two partitions
 * corresponding to the two promoted data objects.
 *
 * @param <DATA> The type of the data objects.
 */
public interface SplitFunction<DATA> {

	/**
	 * Processes the splitting of a node.
	 * 
	 * @param dataSet A set of data that are keys to the children of the node
	 *        to be split.
	 * @param distanceFunction A {@linkplain DistanceFunction distance function}
	 *        that can be used to help splitting the node.
	 * @return A {@link SplitResult} object with a pair of promoted data objects
	 *         and a pair of corresponding partitions of the data objects.
	 */
	SplitResult<DATA> process(Set<DATA> dataSet, DistanceFunction<? super DATA> distanceFunction);
	
	
	/**
	 * An object used as the result for the
	 * {@link SplitFunction#process(Set, DistanceFunction)} method.
	 *
	 * @param <DATA> The type of the data objects.
	 */
	public static class SplitResult<DATA> {

		/**
		 * A pair of promoted data objects.
		 */
		public Pair<DATA> promoted;
		
		/**
		 * A pair of partitions corresponding to the {@code promoted} data
		 * objects.
		 */
		public Pair<Set<DATA>> partitions;

		/**
		 * The constructor for a {@link SplitResult} object.
		 */
		public SplitResult(Pair<DATA> promoted, Pair<Set<DATA>> partitions) {
			this.promoted = promoted;
			this.partitions = partitions;
		}
		
	}
	
}
