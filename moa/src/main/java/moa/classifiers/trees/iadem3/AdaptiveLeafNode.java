/*
 *    AdaptiveLeafNode.java
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
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iadem2.SplitNode;
import moa.classifiers.trees.iadem2.LeafNode;
import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;
import moa.core.DoubleVector;
import weka.core.Utils;


/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveLeafNode extends LeafNode implements Serializable {

    private static final long serialVersionUID = 1L;
    protected AbstractChangeDetector estimator;
    
    public AdaptiveLeafNode(IADEM3Tree arbol,
            Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] initialClassCount,
            IademNumericAttributeObserver numericAttClassObserver,
            AbstractChangeDetector estimator,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        super(arbol, 
                parent, instTreeCountSinceVirtual, instNodeCountSinceVirtual, initialClassCount, numericAttClassObserver, onlyMultiwayTest, onlyBinaryTest);
        if (estimator != null) {
            this.estimator = (AbstractChangeDetector) estimator.copy();
        } else {
            this.estimator = null;
        }
    }

    @Override
    protected void createVirtualNodes(IademNumericAttributeObserver numericAttClassObserver,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        ArrayList<Integer> nominalUsed = nominalAttUsed();
        TreeSet<Integer> sort = new TreeSet<>(nominalUsed);
        for (int i = 0; i < tree.getProblemDescription().numAttributes(); i++) {
            if (tree.getProblemDescription().classIndex() != i
                    && tree.getProblemDescription().attribute(i).isNominal()) {
                if ((!sort.isEmpty()) && (i == sort.first())) {
                    sort.remove(new Integer(sort.first()));
                    virtualChildren.set(i, null);
                } else {
                    virtualChildren.set(i, new AdaptiveNominalVirtualNode((IADEM3Tree) tree,
                            this,
                            i,
                            onlyMultiwayTest,
                            onlyBinaryTest));
                }
            } else if (tree.getProblemDescription().classIndex() != i
                    && tree.getProblemDescription().attribute(i).isNumeric()) {
                virtualChildren.set(i, new AdaptiveNumericVirtualNode((IADEM3Tree) tree,
                        this,
                        i,
                        numericAttClassObserver));
            } else {
                virtualChildren.set(i, null);
            }
        }
    }

    private void updateCounters(Instance experiencia) {
        double[] classVotes = this.getClassVotes(experiencia);
        boolean trueClass = (Utils.maxIndex(classVotes) == (int) experiencia.classValue());
        if (estimator != null && ((IADEM3Tree) this.tree).restartAtDrift) {
            double error = trueClass == true ? 0.0 : 1.0;
            this.estimator.input(error);
            if (this.estimator.getChange()) {
                this.restartVariablesAtDrift();
            }
        }
    }

    @Override
    public void attemptToSplit() {
        if (this.classValueDist.numNonZeroEntries() > 1) {
            if (hasInformationToSplit()) {
                try {
                    this.instSeenSinceLastSplitAttempt = 0;
                    IademAttributeSplitSuggestion bestSplitSuggestion;
                    if (this.instNodeCountSinceReal > 5000) {
                        ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(1);
                        bestSplitSuggestion = getFastSplitSuggestion();
                        if (bestSplitSuggestion != null) {
                            ((IADEM3Tree) this.tree).currentSplitState = ((IADEM3Tree) this.tree).SPLIT_BY_TIE_BREAKING;
                            doSplit(bestSplitSuggestion);
                        }
                    } else {
                        bestSplitSuggestion = getBestSplitSuggestion();
                        if (bestSplitSuggestion != null) {
                            ((IADEM3Tree) this.tree).currentSplitState = ((IADEM3Tree) this.tree).SPLIT_WITH_CONFIDENCE;
                            doSplit(bestSplitSuggestion);
                        }
                    }
                } catch (IademException ex) {
                    Logger.getLogger(LeafNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public Node learnFromInstance(Instance inst) {
        updateCounters(inst);
        return super.learnFromInstance(inst);
    }
    
    @Override
    public AdaptiveLeafNode[] doSplit(IademAttributeSplitSuggestion mejorExpansion) {
        AdaptiveSplitNode splitNode;
        splitNode = (AdaptiveSplitNode) virtualChildren.get(mejorExpansion.splitTest.getAttsTestDependsOn()[0]).getNewSplitNode(instTreeCountSinceReal,
                parent,
                mejorExpansion);
        splitNode.setParent(this.parent);
        splitNode.estimator = this.tree.newEstimator();

        if (this.parent == null) {
            tree.setTreeRoot(splitNode);
        } else { 
            ((SplitNode) parent).changeChildren(this, splitNode);
        }
        this.tree.newSplit(splitNode.getLeaves().size());
        return null;
    }

    protected void restartVariablesAtDrift() {
        instNodeCountSinceVirtual = 0;

        classValueDist = new DoubleVector();

        instTreeCountSinceReal = 0;
        instNodeCountSinceReal = 0;
        for (int i = 0; i < virtualChildren.size(); i++) {
            if (virtualChildren.get(i) != null) {
                ((restartsVariablesAtDrift) virtualChildren.get(i)).resetVariablesAtDrift();
            }
        }
    }
}
