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

class MTreeMicroClusters extends MTree<MicroCluster> {

    private static final PromotionFunction<MicroCluster> nonRandomPromotion = new PromotionFunction<MicroCluster>() {

        @Override
        public Pair<MicroCluster> process(Set<MicroCluster> dataSet, DistanceFunction<? super MicroCluster> distanceFunction) {
            return Utils.minMax(dataSet);
        }
    };

    MTreeMicroClusters() {
        super(2, DistanceFunctions.EUCLIDEAN,
                new ComposedSplitFunction<MicroCluster>(
                nonRandomPromotion,
                new PartitionFunctions.BalancedPartition<MicroCluster>()));
    }

    public void add(MicroCluster data) {
        super.add(data);
        _check();
    }

    public boolean remove(MicroCluster data) {
        boolean result = super.remove(data);
        _check();
        return result;
    }

    DistanceFunction<? super MicroCluster> getDistanceFunction() {
        return distanceFunction;
    }
};
