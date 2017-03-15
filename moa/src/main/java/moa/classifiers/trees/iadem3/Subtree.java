/*
 *    Subtree.java
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
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademSplitMeasure;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;


/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class Subtree extends IADEM3Tree {

    protected static final long serialVersionUID = 1L;
    protected AbstractChangeDetector errorEstimator;
    protected Node nodo;

    protected IADEM3Tree mainTree;

    public Subtree(InstancesHeader problemDescription,
            double attDif,
            IademSplitMeasure measure,
            int predictionType,
            int limitNaiveBayes,
            double percentInCommon,
            IademNumericAttributeObserver numericAttClassObserver,
            int maxBins,
            AbstractChangeDetector estimator,
            boolean restartVariablesAtDrift,
            Node node,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest,
            int gracePeriod,
            int treeLevel,
            int maxTreeLevel,
            int maxAltSubtreesPerNode,
            IADEM3Tree mainTree) {
        super(problemDescription,
                attDif,
                measure,
                predictionType,
                limitNaiveBayes,
                percentInCommon,
                numericAttClassObserver,
                maxBins,
                estimator,
                restartVariablesAtDrift,
                onlyMultiwayTest,
                onlyBinaryTest,
                gracePeriod,
                treeLevel,
                maxTreeLevel,
                maxAltSubtreesPerNode);
        this.errorEstimator = (AbstractChangeDetector) estimator.copy();
        this.nodo = node;
        this.mainTree = mainTree;
        this.mainTree.updateNumberOfLeaves(1);
        this.mainTree.updateNumberOfNodes(1);
    }

    @Override
    public void learnFromInstance(Instance instance)
            throws IademException {
        getClassVotes(instance);
        getClassVotesFromLeaf(instance);

        boolean rightPredicted = this.lastPredictionInLeaf == instance.classValue();
        double lossValue = (rightPredicted == true ? 0.0 : 1.0);
        this.errorEstimator.input(lossValue);

        treeRoot.learnFromInstance(instance);
    }

    @Override
    public boolean canCreateSubtree() {
        return this.mainTree.canCreateSubtree();
    }

    public double estimacionValorMedio() {
        return this.errorEstimator != null ? this.errorEstimator.getEstimation() : -1;
    }
    
    public int windowWidth() {
        return (int) (this.errorEstimator != null ? this.errorEstimator.getDelay() : 0);
    }
    
    public AbstractChangeDetector getEstimador() {
        return errorEstimator;
    }
    
    public void setEstimador(AbstractChangeDetector estimador) {
        this.errorEstimator = estimador;
    }

    @Override
    public void setNewTree() {
        ((IADEM3Tree) this.nodo.getTree()).setNewTree();
    }

    @Override
    public void newDeletedTree() {
        ((IADEM3Tree) this.nodo.getTree()).newDeletedTree();
    }

    @Override
    public void newTreeChange() {
        ((IADEM3Tree) this.nodo.getTree()).newTreeChange();
    }

    @Override
    protected IADEM3Tree getMainTree() {
        return this.mainTree;
    }

    @Override
    public void newSplit(int numOfLeaves) {
        this.numberOfLeaves += numOfLeaves - 1;
        this.numberOfNodes += numOfLeaves;
        this.mainTree.newSplit(numOfLeaves);
    }

    @Override
    public void updateNumberOfLeaves(int amount) {
        this.numberOfLeaves += amount;
        this.mainTree.updateNumberOfLeaves(amount);
    }

    @Override
    public void updateNumberOfNodes(int amount) {
        this.numberOfNodes += amount;
        this.mainTree.updateNumberOfNodes(amount);
    }
    
    @Override
    public void updateNumberOfNodesSplitByTieBreaking(int amount) {
        this.splitByBreakingTies += amount;
        this.mainTree.updateNumberOfNodesSplitByTieBreaking(amount);
    }

    @Override
    public void addSubtree(Subtree subtree) {
        this.mainTree.addSubtree(subtree);
    }

    @Override
    public void removeSubtree(Subtree subtree) {
        this.mainTree.removeSubtree(subtree);
    }
}
