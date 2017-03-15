/*
 *    AdaptiveLeafNodeWeightedVote.java
 *
 *    @author Isvani Frías-Blanco
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

package moa.classifiers.trees.iadem3;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveLeafNodeWeightedVote extends AdaptiveLeafNodeNBAdaptive {

    private static final long serialVersionUID = 1L;

    public AdaptiveLeafNodeWeightedVote(IADEM3Tree tree,
            Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] classDist,
            IademNumericAttributeObserver observadorContinuos,
            int naiveBayesLimit,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest,
            AbstractChangeDetector estimator) {
        super(tree,
                parent,
                instTreeCountSinceVirtual,
                instNodeCountSinceVirtual,
                classDist,
                observadorContinuos,
                naiveBayesLimit,
                onlyMultiwayTest,
                onlyBinaryTest,
                estimator);
    }

    @Override
    public double[] getClassVotes(Instance instance) {        
        double NBweight = 1 - this.naiveBayesError.getEstimation(),
                MCweight = 1 - this.majorityClassError.getEstimation();
        double [] MC = getMajorityClassVotes(),
                NB = getNaiveBayesPrediction(instance),
                votes = new double [MC.length];
        for (int i = 0; i < MC.length; i++) {
            votes[i] = MC[i] * MCweight + NB [i] * NBweight;
        }
        return votes;
    }

    protected boolean isSignificantlyGreaterThan(double mean1, double mean2, int n1, int n2) {
        double m = 1.0 / n1 + 1.0 / n2,
                confidence = 0.001,
                log = Math.log(1.0 / confidence),
                bound = Math.sqrt(m * log / 2);
        return mean1 - mean2 > bound;
    }
}
