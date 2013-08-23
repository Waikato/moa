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

import java.util.List;
import java.util.Set;
import moa.clusterers.outliers.utils.mtree.utils.Pair;
import moa.clusterers.outliers.utils.mtree.utils.Utils;

/**
 * Some pre-defined implementations of {@linkplain PromotionFunction promotion
 * functions}.
 */
public final class PromotionFunctions {

    /**
     * Don't let anyone instantiate this class.
     */
	private PromotionFunctions() {}
	
	
	/**
	 * A {@linkplain PromotionFunction promotion function} object that randomly
	 * chooses ("promotes") two data objects.
	 *
	 * @param <DATA> The type of the data objects.
	 */
	public static class RandomPromotion<DATA> implements PromotionFunction<DATA> {
		@Override
		public Pair<DATA> process(Set<DATA> dataSet,
				DistanceFunction<? super DATA> distanceFunction)
		{
			List<DATA> promotedList = Utils.randomSample(dataSet, 2);
			return new Pair<DATA>(promotedList.get(0), promotedList.get(1));
		}
	}
	
}
