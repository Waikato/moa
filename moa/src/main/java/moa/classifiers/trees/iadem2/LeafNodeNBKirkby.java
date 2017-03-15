/*
 *    LeafNodeNBKirkby.java
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

public class LeafNodeNBKirkby extends LeafNodeNB {

    private static final long serialVersionUID = 1L;
    protected int naiveBayesError,
            majorityClassError;

    public LeafNodeNBKirkby(IADEM2cTree tree,
            Node parent,
            long instancesProcessedByTheTree,
            long instancesProcessedByThisLeaf,
            double[] classDist,
            IademNumericAttributeObserver numericAttClassObserver,
            int naiveBayesLimit,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest,
            AbstractChangeDetector estimator) {
        super(tree,
                parent,
                instancesProcessedByTheTree,
                instancesProcessedByThisLeaf,
                classDist,
                numericAttClassObserver,
                naiveBayesLimit,
                onlyMultiwayTest,
                onlyBinaryTest);
        this.naiveBayesError = 0;
        this.majorityClassError = 0;
    }

    @Override
    public double[] getClassVotes(Instance instance) {
        if (naiveBayesError > majorityClassError) {
            return getMajorityClassVotes();
        } else {
            return getNaiveBayesPrediction(instance);
        }
    }
    
    @Override
    public Node learnFromInstance(Instance inst) {
        // test-then-train
        double[] prediccion = getMajorityClassVotes();
        double error = (Utils.maxIndex(prediccion) == (int) inst.classValue()) ? 0.0 : 1.0;
        this.majorityClassError += error;
        
        prediccion = getNaiveBayesPrediction(inst);
        error = (Utils.maxIndex(prediccion) == (int) inst.classValue()) ? 0.0 : 1.0;
        this.naiveBayesError += error;
        
        return super.learnFromInstance(inst);
    }
}
