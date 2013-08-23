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
 * An object with partitions a set of data into two sub-sets.
 *
 * @param <DATA> The type of the data on the sets.
 */
public interface PartitionFunction<DATA> {
	
	/**
	 * Executes the partitioning.
	 * 
	 * @param promoted The pair of data objects that will guide the partition
	 *        process.
	 * @param dataSet The original set of data objects to be partitioned.
	 * @param distanceFunction A {@linkplain DistanceFunction distance function}
	 *        to be used on the partitioning.
	 * @return A pair of partition sub-sets. Each sub-set must correspond to one
	 *         of the {@code promoted} data objects.
	 */
	Pair<Set<DATA>> process(Pair<DATA> promoted, Set<DATA> dataSet, DistanceFunction<? super DATA> distanceFunction);
	
}
