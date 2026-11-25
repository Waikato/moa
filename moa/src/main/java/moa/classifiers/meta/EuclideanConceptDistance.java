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

package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.lazy.neighboursearch.EuclideanDistanceModified;
import moa.classifiers.lazy.neighboursearch.LinearNNSearchModified;

/**
 * Euclidean nearest-neighbour concept distance strategy including the class
 * attribute via {@link EuclideanDistanceModified} and {@link LinearNNSearchModified}.
 */
public class EuclideanConceptDistance implements ConceptDistance {

    private static final long serialVersionUID = 1L;
    private final boolean debugNN;

    public EuclideanConceptDistance(boolean debugNN) {
        this.debugNN = debugNN;
    }

    @Override
    public double distance(Instances query, Instances target) {
        int nPrototypes = 1;
        try {
            EuclideanDistanceModified d = new EuclideanDistanceModified(query);
            double[] dist = new double[query.numInstances()];
            double totalDist = 0.0;
            for (int i = 0; i < query.numInstances(); i++) {
                Instances nearest = getNearestPrototypes(query.get(i), target, nPrototypes);
                for (int j = 0; j < nearest.numInstances(); j++) {
                    dist[i] += d.distanceUsingClass(query.get(i), nearest.get(j));
                }
                dist[i] /= nearest.numInstances();
                if (this.debugNN) {
                    assert nearest.numInstances() == nPrototypes;
                }
            }
            for (int i = 0; i < dist.length; i++) {
                totalDist += dist[i];
            }
            return totalDist / query.numInstances();
        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    private Instances getNearestPrototypes(Instance inst, Instances topologyPrototypes, int nPrototypes) throws Exception {
        LinearNNSearchModified nnSearch = new LinearNNSearchModified();
        nnSearch.setInstances(topologyPrototypes);
        return nnSearch.kNearestNeighboursUsingClass(inst, nPrototypes);
    }
}
