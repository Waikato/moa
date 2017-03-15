/*
 *    LeafNodeWeightedVote.java
 *
 *    @author Isvani Frias-Blanco
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

package moa.classifiers.trees.iadem2;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import weka.core.Utils;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

public class LeafNodeWeightedVote extends LeafNodeNB {

    private static final long serialVersionUID = 1L;
    protected AbstractChangeDetector naiveBayesError,
            majorityClassError;

    public LeafNodeWeightedVote(IADEM2cTree tree,
            Node parent,
            long instancesProcessedByTheTree,
            long instancesProcessedByThisLeaf,
            double[] classDist,
            IademNumericAttributeObserver observadorContinuos,
            int naiveBayesLimit,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest,
            AbstractChangeDetector estimator) {
        super(tree,
                parent,
                instancesProcessedByTheTree,
                instancesProcessedByThisLeaf,
                classDist,
                observadorContinuos,
                naiveBayesLimit,
                onlyMultiwayTest,
                onlyBinaryTest);
        this.naiveBayesError = (AbstractChangeDetector) estimator.copy();
        this.majorityClassError = (AbstractChangeDetector) estimator.copy();
    }

    @Override
    public double[] getClassVotes(Instance instance) {        
        double NBweight = 1 - this.naiveBayesError.getEstimation(),
                MCweight = 1 - this.majorityClassError.getEstimation();
        double [] MC = getMajorityClassVotes(),
                NB = getNaiveBayesPrediction(instance),
                classVotes = new double [MC.length];
        for (int i = 0; i < MC.length; i++) {
            classVotes[i] = MC[i] * MCweight + NB [i] * NBweight;
        }
        return classVotes;
    }
    
    @Override
    public Node learnFromInstance(Instance inst) {
        // test-then-train
        double[] classVotes = getMajorityClassVotes();
        double error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
        this.majorityClassError.input(error);
        
        classVotes = getNaiveBayesPrediction(inst);
        error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
        this.naiveBayesError.input(error);
        return super.learnFromInstance(inst);
    }
}
