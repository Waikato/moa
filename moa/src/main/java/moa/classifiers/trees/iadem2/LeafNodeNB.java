/*
 *    LeafNodeNB.java
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
import moa.core.DoubleVector;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

public class LeafNodeNB extends LeafNode {

    private static final long serialVersionUID = 1L;
    protected int naiveBayesLimit;

    public LeafNodeNB(IADEM2cTree tree,
            Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] initialClassVotes,
            IademNumericAttributeObserver numericAttClassObserver,
            int naiveBayesLimit,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        super(tree,
                parent,
                instTreeCountSinceVirtual,
                instNodeCountSinceVirtual,
                initialClassVotes,
                numericAttClassObserver,
                onlyMultiwayTest,
                onlyBinaryTest);
        this.naiveBayesLimit = naiveBayesLimit;
    }
    
    @Override
    public double[] getClassVotes(Instance inst) {
        double[] classVotes;
        if (instNodeCountSinceVirtual == 0 || instNodeCountSinceReal < naiveBayesLimit) {
            classVotes = getMajorityClassVotes();
        } else {
            classVotes = getNaiveBayesPrediction(inst);
        }
        return classVotes;
    }

    protected double[] getNaiveBayesPrediction(Instance obs) { 
        double[] classVotes = getMajorityClassVotes(); 
        DoubleVector condProbabilities; 
        for (int i = 0; i < virtualChildren.size(); i++) { 
            VirtualNode currentVirtualNode = virtualChildren.get(i);
            if (currentVirtualNode != null && currentVirtualNode.hasInformation()) { 
                double valor = obs.value(i);
                condProbabilities = currentVirtualNode.computeConditionalProbability(valor); 
                if (condProbabilities != null) {
                    for (int j = 0; j < classVotes.length; j++) {
                        classVotes[j] *= condProbabilities.getValue(j);
                    }
                }
            }
        }
        // normalize class votes
        double classVoteCount = 0.0;
        for (int i = 0; i < classVotes.length; i++) {
            classVoteCount += classVotes[i];
        }
        if (classVoteCount == 0.0) {
            for (int i = 0; i < classVotes.length; i++) {
                classVotes[i] = 1.0 / classVotes.length;
            }
        } else {
            for (int i = 0; i < classVotes.length; i++) {
                classVotes[i] /= classVoteCount;
            }
        }
        return classVotes;
    }
}
