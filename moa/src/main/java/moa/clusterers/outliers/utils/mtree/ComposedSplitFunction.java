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
 * A {@linkplain SplitFunction split function} that is defined by composing
 * a {@linkplain PromotionFunction promotion function} and a
 * {@linkplain PartitionFunction partition function}.
 *
 * @param <DATA> The type of the data objects.
 */
public class ComposedSplitFunction<DATA> implements SplitFunction<DATA> {

	private PromotionFunction<DATA> promotionFunction;
	private PartitionFunction<DATA> partitionFunction;

	/**
	 * The constructor of a {@link SplitFunction} composed by a
	 * {@link PromotionFunction} and a {@link PartitionFunction}.
	 */
	public ComposedSplitFunction(
			PromotionFunction<DATA> promotionFunction,
			PartitionFunction<DATA> partitionFunction
		)
	{
		this.promotionFunction = promotionFunction;
		this.partitionFunction = partitionFunction;
	}

	
	@Override
	public SplitResult<DATA> process(Set<DATA> dataSet, DistanceFunction<? super DATA> distanceFunction) {
		Pair<DATA> promoted = promotionFunction.process(dataSet, distanceFunction);
		Pair<Set<DATA>> partitions = partitionFunction.process(promoted, dataSet, distanceFunction);
		return new SplitResult<DATA>(promoted, partitions);
	}

}
