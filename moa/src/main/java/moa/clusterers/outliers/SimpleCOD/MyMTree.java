/*
 *    MyMTree.java
 *    Copyright (C) 2013 Aristotle University of Thessaloniki, Greece
 *    @author D. Georgiadis, A. Gounaris, A. Papadopoulos, K. Tsichlas, Y. Manolopoulos
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.clusterers.outliers.SimpleCOD;

import java.util.Set;
import moa.clusterers.outliers.utils.mtree.ComposedSplitFunction;
import moa.clusterers.outliers.utils.mtree.DistanceFunction;
import moa.clusterers.outliers.utils.mtree.DistanceFunctions;
import moa.clusterers.outliers.utils.mtree.MTree;
import moa.clusterers.outliers.utils.mtree.PartitionFunctions;
import moa.clusterers.outliers.utils.mtree.PromotionFunction;
import moa.clusterers.outliers.utils.mtree.utils.Pair;
import moa.clusterers.outliers.utils.mtree.utils.Utils;

class MyMTree extends MTree<StreamObj> {

    private static final PromotionFunction<StreamObj> nonRandomPromotion = new PromotionFunction<StreamObj>() {

        @Override
        public Pair<StreamObj> process(Set<StreamObj> dataSet, DistanceFunction<? super StreamObj> distanceFunction) {
            return Utils.minMax(dataSet);
        }
    };

    MyMTree() {
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
