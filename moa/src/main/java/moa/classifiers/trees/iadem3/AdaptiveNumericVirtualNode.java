/*
 *    AdaptiveNumericVirtualNode.java
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
import java.io.Serializable;
import java.util.Arrays;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iadem2.SplitNode;
import moa.classifiers.trees.iadem2.LeafNode;
import moa.classifiers.trees.iadem2.NumericVirtualNode;
import moa.classifiers.trees.iademutils.IademVFMLNumericAttributeClassObserver;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;
import moa.core.DoubleVector;
import weka.core.Utils;


/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveNumericVirtualNode extends NumericVirtualNode implements Serializable, restartsVariablesAtDrift {

    private static final long serialVersionUID = 1L;
    protected IademNumericAttributeObserver altAttClassObserver;
    protected DoubleVector altClassDist;
    protected AbstractChangeDetector estimator;
    
    public AdaptiveNumericVirtualNode(IADEM3Tree tree,
            Node parent,
            int attID,
            IademNumericAttributeObserver observadorContinuos) {
        super(tree, parent, attID, observadorContinuos);
    }

    @Override
    public Node learnFromInstance(Instance inst) {
        updateCounters(inst);
        return super.learnFromInstance(inst);
    }

    private void updateCounters(Instance inst) {
        double[] classVotes = this.getClassVotes(inst);
        boolean correct = (Utils.maxIndex(classVotes) == (int) inst.classValue());
        if (this.estimator != null && ((IADEM3Tree) this.tree).restartAtDrift) {
            double error = correct == true ? 0.0 : 1.0;
            this.estimator.input(error);
            if (this.estimator.getChange()) {
                this.resetVariablesAtDrift();
            }
        }
    }

    private long sum(long[] arr) {
        long s = 0;
        for (int i = 0; i < arr.length; i++) {
            s += arr[i];
        }
        return s;
    }

    @Override
    public SplitNode getNewSplitNode(long counter,
            Node parent,
            IademAttributeSplitSuggestion bestSplit) {
        double[] cutPoints = new double[]{bestCutPoint};
        Node[] children = new Node[2]; // a binary split
        long[] newClassDist = numericAttClassObserver.getLeftClassDist(bestCutPoint);
        long sumClassDist = numericAttClassObserver.getValueCount();
        long[] sumAttClassDist = numericAttClassObserver.getClassDist();
        boolean equalsPassesTest = true;
        if (this.numericAttClassObserver instanceof IademVFMLNumericAttributeClassObserver) {
            equalsPassesTest = false;
        }
        AdaptiveSplitNode splitNode = new AdaptiveSplitNode((IADEM3Tree) this.tree,
                parent,
                null,
                ((LeafNode) this.parent).getMajorityClassVotes(),
                new NumericAttributeBinaryTest(this.attIndex, cutPoints[0], equalsPassesTest),
                ((AdaptiveLeafNode) this.parent).estimator,
                (AdaptiveLeafNode) this.parent,
                ((IADEM3Tree) this.tree).currentSplitState);
        long leftClassDist = sum(newClassDist); 
        long rightClassDist = sumClassDist - leftClassDist; 
        double[] newLeftClassDist = new double[this.tree.getProblemDescription().attribute(this.tree.getProblemDescription().classIndex()).numValues()];
        double[] newRightClassDist = new double[this.tree.getProblemDescription().attribute(this.tree.getProblemDescription().classIndex()).numValues()];
        
        Arrays.fill(newLeftClassDist, 0);
        Arrays.fill(newRightClassDist, 0);

        for (int i = 0; i < newClassDist.length; i++) {
            newLeftClassDist[i] = newClassDist[i];
            newRightClassDist[i] = sumAttClassDist[i] - newLeftClassDist[i];
        }
        splitNode.setChildren(null);
        children[0] = ((IADEM3Tree) tree).newLeafNode(splitNode,
                counter,
                leftClassDist,
                newLeftClassDist);
        children[1] = ((IADEM3Tree) tree).newLeafNode(splitNode,
                counter,
                rightClassDist,
                newRightClassDist);
        splitNode.setChildren(children);
        return splitNode;
    }

    @Override
    public void resetVariablesAtDrift() {
        this.bestSplitSuggestion = null;
        this.heuristicMeasureUpdated = false;
        numericAttClassObserver.reset();
        classValueDist = new DoubleVector();
    }
}
