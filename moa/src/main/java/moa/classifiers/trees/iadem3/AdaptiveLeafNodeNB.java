/*
 *    AdaptiveLeafNodeNB.java
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
import moa.classifiers.trees.iadem2.VirtualNode;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;
import moa.core.DoubleVector;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveLeafNodeNB extends AdaptiveLeafNode {

    private static final long serialVersionUID = 1L;
    protected int limitNaiveBayes;

    public AdaptiveLeafNodeNB(IADEM3Tree tree,
            Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] initialClassCount,
            IademNumericAttributeObserver numericAttClassObserver,
            int limitNaiveBayes,
            AbstractChangeDetector estimator,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        super(tree,
                parent,
                instTreeCountSinceVirtual,
                instNodeCountSinceVirtual,
                initialClassCount,
                numericAttClassObserver,
                estimator,
                onlyMultiwayTest,
                onlyBinaryTest);
        this.limitNaiveBayes = limitNaiveBayes;
    }
    
    @Override
    public double[] getClassVotes(Instance inst) {
        double[] votes;
        if (instNodeCountSinceVirtual == 0 || instNodeCountSinceReal < limitNaiveBayes) {
            votes = getMajorityClassVotes();
        } else {
            votes = getNaiveBayesPrediction(inst); 
        }
        return votes;
    }

    protected double[] getNaiveBayesPrediction(Instance inst) {
        double[] classDist = getMajorityClassVotes();
        DoubleVector conditionalProbability = null;
        for (int i = 0; i < virtualChildren.size(); i++) {
            VirtualNode virtual = virtualChildren.get(i);
            if (virtual != null && virtual.hasInformation()) {
                double currentValue = inst.value(i);
                conditionalProbability = virtual.computeConditionalProbability(currentValue);
                if (conditionalProbability != null) {
                    for (int j = 0; j < classDist.length; j++) {
                        classDist[j] *= conditionalProbability.getValue(j);
                    }
                }
            }
        }
        double sum = 0.0;
        for (int i = 0; i < classDist.length; i++) {
            sum += classDist[i];
        }
        if (sum == 0.0) {
            for (int i = 0; i < classDist.length; i++) {
                classDist[i] = 1.0 / classDist.length;
            }
        } else {
            for (int i = 0; i < classDist.length; i++) {
                classDist[i] /= sum;
            }
        }
        return classDist;
    }
}
