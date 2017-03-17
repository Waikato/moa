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
package moa.classifiers.trees.iadem;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;


/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class Iadem3Subtree extends Iadem3 {

    protected static final long serialVersionUID = 1L;
    protected AbstractChangeDetector errorEstimator;
    protected Node nodo;

    protected Iadem3 mainTree;

    public Iadem3Subtree(Node node,
            int treeLevel,
            Iadem3 mainTree,
            Instance instance) {
        // subtree configuration from main tree
        this.numericEstimatorOption.setValueViaCLIString(mainTree.numericEstimatorOption.getValueAsCLIString());
        this.gracePeriodOption.setValue(mainTree.gracePeriodOption.getValue());
        this.splitCriterionOption.setChosenIndex(mainTree.splitCriterionOption.getChosenIndex());
        this.splitConfidenceOption.setValue(mainTree.splitConfidenceOption.getValue());
        this.splitTestsOption.setChosenIndex(mainTree.splitTestsOption.getChosenIndex());
        this.leafPredictionOption.setChosenIndex(mainTree.leafPredictionOption.getChosenIndex());
        this.driftDetectionMethodOption.setValueViaCLIString(mainTree.driftDetectionMethodOption.getValueAsCLIString());
        this.attributeDiferentiation.setValue(mainTree.attributeDiferentiation.getValue());
        this.maxNestingLevelOption.setValue(mainTree.maxNestingLevelOption.getValue());
        this.maxSubtreesPerNodeOption.setValue(mainTree.maxSubtreesPerNodeOption.getValue());
        
        // subtree inicializations
        this.estimator = mainTree.getEstimatorCopy();
        this.errorEstimator = mainTree.getEstimatorCopy();
        this.nodo = node;
        this.mainTree = mainTree;
        this.mainTree.updateNumberOfLeaves(1);
        this.mainTree.updateNumberOfNodes(1);
        createRoot(instance);
    }
    
    @Override
    public AbstractChangeDetector getEstimatorCopy() {
        return this.mainTree.getEstimatorCopy();
    }
    
    @Override
    protected IademNumericAttributeObserver newNumericClassObserver() {
        return this.mainTree.newNumericClassObserver();
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
        ((Iadem3) this.nodo.getTree()).setNewTree();
    }

    @Override
    public void newDeletedTree() {
        ((Iadem3) this.nodo.getTree()).newDeletedTree();
    }

    @Override
    public void newTreeChange() {
        ((Iadem3) this.nodo.getTree()).newTreeChange();
    }

    @Override
    protected Iadem3 getMainTree() {
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
        this.numSplitsByBreakingTies += amount;
        this.mainTree.updateNumberOfNodesSplitByTieBreaking(amount);
    }

    @Override
    public void addSubtree(Iadem3Subtree subtree) {
        this.mainTree.addSubtree(subtree);
    }

    @Override
    public void removeSubtree(Iadem3Subtree subtree) {
        this.mainTree.removeSubtree(subtree);
    }
}
