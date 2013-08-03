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

package moa.clusterers.outliers.MCOD;

import java.util.Set;
import moa.clusterers.utils.mtree.ComposedSplitFunction;
import moa.clusterers.utils.mtree.DistanceFunction;
import moa.clusterers.utils.mtree.DistanceFunctions;
import moa.clusterers.utils.mtree.MTree;
import moa.clusterers.utils.mtree.PartitionFunctions;
import moa.clusterers.utils.mtree.PromotionFunction;
import moa.clusterers.outliers.utils.mtree.utils.Pair;
import moa.clusterers.outliers.utils.mtree.utils.Utils;

class MTreeStreamObjects extends MTree<StreamObj> {

    private static final PromotionFunction<StreamObj> nonRandomPromotion = new PromotionFunction<StreamObj>() {

        @Override
        public Pair<StreamObj> process(Set<StreamObj> dataSet, DistanceFunction<? super StreamObj> distanceFunction) {
            return Utils.minMax(dataSet);
        }
    };

    MTreeStreamObjects() {
        super(2, DistanceFunctions.EUCLIDEAN,
                new ComposedSplitFunction<StreamObj>(
                nonRandomPromotion,
                new PartitionFunctions.BalancedPartition<StreamObj>()));
    }

    public void add(StreamObj data) {
        super.add(data);
        _check();
    }

    public boolean remove(StreamObj data) {
        boolean result = super.remove(data);
        _check();
        return result;
    }

    DistanceFunction<? super StreamObj> getDistanceFunction() {
        return distanceFunction;
    }
};
