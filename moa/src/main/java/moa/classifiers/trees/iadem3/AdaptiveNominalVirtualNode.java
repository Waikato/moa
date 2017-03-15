/*
 *    AdaptiveNominalVirtualNode.java
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
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.io.Serializable;
import java.util.Arrays;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iadem2.SplitNode;
import moa.classifiers.trees.iadem2.LeafNode;
import moa.classifiers.trees.iadem2.NominalVirtualNode;
import moa.classifiers.trees.iademutils.IademNominalAttributeBinaryTest;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import weka.core.Utils;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveNominalVirtualNode extends NominalVirtualNode implements Serializable, restartsVariablesAtDrift {

    private static final long serialVersionUID = 1L;

    protected AbstractChangeDetector estimador;

    public AdaptiveNominalVirtualNode(IADEM3Tree tree,
            Node parent,
            int attID,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        super(tree, parent, attID, onlyMultiwayTest, onlyBinaryTest);
    }

    @Override
    public Node learnFromInstance(Instance inst) {
        double attValue = inst.value(attIndex);
        if (Utils.isMissingValue(attValue)) {
        } else {
            updateCountersForChange(inst);
        }
        return super.learnFromInstance(inst);
    }

    private void updateCountersForChange(Instance inst) {
        double[] classVotes = this.getClassVotes(inst);
        boolean trueClass = (Utils.maxIndex(classVotes) == (int) inst.classValue()); 
        if (estimador != null && ((IADEM3Tree) this.tree).restartAtDrift) {
            double error = trueClass == true ? 0.0 : 1.0;
            this.estimador.input(error);
            if (this.estimador.getChange()) {
                this.resetVariablesAtDrift();
            }
        }
    }

    @Override
    public SplitNode getNewSplitNode(long counter,
            Node parent,
            IademAttributeSplitSuggestion bestSplit) {
        InstancesHeader problemDescription = this.tree.getProblemDescription();
        AdaptiveSplitNode splitNode = new AdaptiveSplitNode((IADEM3Tree) this.tree,
                parent,
                null,
                ((LeafNode) this.parent).getMajorityClassVotes(),
                bestSplit.splitTest,
                ((AdaptiveLeafNode) this.parent).estimator,
                (AdaptiveLeafNode) this.parent,
                ((IADEM3Tree) this.tree).currentSplitState);

        Node[] children;
        if (bestSplit.splitTest instanceof NominalAttributeMultiwayTest) {
            children = new Node[this.tree.getProblemDescription().attribute(this.attIndex).numValues()];
            for (int i = 0; i < children.length; i++) {
                long tmpConter = 0;
                double[] newClassDist = new double[problemDescription.attribute(problemDescription.classIndex()).numValues()];
                Arrays.fill(newClassDist, 0);
                for (int j = 0; j < newClassDist.length; j++) {
                    DoubleVector tmpClassDist = nominalAttClassObserver.get(i);
                    double tmpAttClassCounter = tmpClassDist != null ? tmpClassDist.getValue(j) : 0.0;
                    newClassDist[j] = tmpAttClassCounter;
                    tmpConter += newClassDist[j];
                }
                children[i] = ((IADEM3Tree) tree).newLeafNode(splitNode,
                        counter,
                        tmpConter,
                        newClassDist);
            }
        } else { // binary split
            children = new Node[2];
            IademNominalAttributeBinaryTest binarySplit = (IademNominalAttributeBinaryTest) bestSplit.splitTest;
            double[] newClassDist = new double[problemDescription.attribute(problemDescription.classIndex()).numValues()];
            double tmpCounter = 0;
            Arrays.fill(newClassDist, 0);
            DoubleVector classDist = nominalAttClassObserver.get(binarySplit.getAttValue());
            for (int i = 0; i < newClassDist.length; i++) {
                newClassDist[i] = classDist.getValue(i);
                tmpCounter += classDist.getValue(i);
            }
            children[0] = ((IADEM3Tree) tree).newLeafNode(splitNode,
                    counter,
                    (int) tmpCounter,
                    newClassDist);
            // a la derecha...
            tmpCounter = this.classValueDist.sumOfValues() - tmpCounter;
            for (int i = 0; i < newClassDist.length; i++) {
                newClassDist[i] = this.classValueDist.getValue(i) - newClassDist[i];
            }
            children[1] = ((IADEM3Tree) tree).newLeafNode(splitNode,
                    counter,
                    (int) tmpCounter,
                    newClassDist);
        }

        splitNode.setChildren(children);

        return splitNode;
    }

    @Override
    public void resetVariablesAtDrift() {
        attValueDist = new DoubleVector();
        nominalAttClassObserver = new AutoExpandVector<DoubleVector>();
        classValueDist = new DoubleVector();
    }
}
