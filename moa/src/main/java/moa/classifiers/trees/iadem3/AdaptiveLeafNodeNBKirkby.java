/*
 *    AdaptiveLeafNodeNBKirkby.java
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
import weka.core.Utils;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveLeafNodeNBKirkby extends AdaptiveLeafNodeNB {
    private static final long serialVersionUID = 1L;
    protected int naiveBayesError,
            majorityClassError;
    
    public AdaptiveLeafNodeNBKirkby(IADEM3Tree tree,
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
                estimator,
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
        double[] classVotes = getMajorityClassVotes();
        double error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
        this.majorityClassError += error;
        
        classVotes = getNaiveBayesPrediction(inst);
        error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
        this.naiveBayesError += error;
        
        return super.learnFromInstance(inst);
    }

}
