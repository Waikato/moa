/*
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
 */

package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * Extension of MOA's {@link LinearNNSearch} that supports nearest-neighbour
 * lookup including the class attribute via {@link EuclideanDistanceModified}.
 */
public class LinearNNSearchModified extends LinearNNSearch {

    private static final long serialVersionUID = 1915484723703917241L;

    public LinearNNSearchModified() {
        super();
        this.m_DistanceFunction = new EuclideanDistanceModified();
    }

    public LinearNNSearchModified(Instances insts) {
        super(insts);
        this.m_DistanceFunction = new EuclideanDistanceModified(insts);
    }

    public Instances kNearestNeighboursUsingClass(Instance target, int kNN) throws Exception {
        MyHeap heap = new MyHeap(kNN);
        EuclideanDistanceModified distFn = (EuclideanDistanceModified) m_DistanceFunction;
        int firstkNN = 0;

        for (int i = 0; i < m_Instances.numInstances(); i++) {
            if (target == m_Instances.instance(i)) {
                continue;
            }
            if (firstkNN < kNN) {
                double distance = distFn.distanceWithClass(target, m_Instances.instance(i), Double.POSITIVE_INFINITY, true);
                if (distance == 0.0 && m_SkipIdentical) {
                    if (i < m_Instances.numInstances() - 1) {
                        continue;
                    } else {
                        heap.put(i, distance);
                    }
                }
                heap.put(i, distance);
                firstkNN++;
            } else {
                MyHeapElement temp = heap.peek();
                double distance = distFn.distanceWithClass(target, m_Instances.instance(i), temp.distance, true);
                if (distance == 0.0 && m_SkipIdentical) {
                    continue;
                }
                if (distance < temp.distance) {
                    heap.putBySubstitute(i, distance);
                } else if (distance == temp.distance) {
                    heap.putKthNearest(i, distance);
                }
            }
        }

        int total = heap.size() + heap.noOfKthNearest();
        Instances neighbours = new Instances(m_Instances, total);
        m_Distances = new double[total];
        int[] indices = new int[total];
        int pos = 1;
        while (heap.noOfKthNearest() > 0) {
            MyHeapElement h = heap.getKthNearest();
            indices[total - pos] = h.index;
            m_Distances[total - pos] = h.distance;
            pos++;
        }
        while (heap.size() > 0) {
            MyHeapElement h = heap.get();
            indices[total - pos] = h.index;
            m_Distances[total - pos] = h.distance;
            pos++;
        }

        m_DistanceFunction.postProcessDistances(m_Distances);
        for (int k = 0; k < indices.length; k++) {
            neighbours.add(m_Instances.instance(indices[k]));
        }
        return neighbours;
    }
}
