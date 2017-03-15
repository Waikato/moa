/*
 *    IADEM3Tree.java
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
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iadem2.IADEM2cTree;
import moa.classifiers.trees.iadem2.LeafNode;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;
import moa.classifiers.trees.iademutils.IademSplitMeasure;
import moa.core.AutoExpandVector;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import weka.core.Utils;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class IADEM3Tree extends IADEM2cTree implements Serializable {

    protected int interchangedTrees = 0;
    protected int deletedTrees = 0;
    protected int numTrees = 0;
    protected boolean restartAtDrift;

    protected int lastPrediction = -1,
            lastPredictionInLeaf = -1;

    protected int maxSubtrees = -1;
    //
    protected int treeLevel;
    protected int maxTreeLevel;
    protected int maxAltSubtreesPerNode;
    protected AutoExpandVector<Subtree> subtreeList = new AutoExpandVector<Subtree>();
    
    protected int currentSplitState = -1;
    protected final int SPLIT_BY_TIE_BREAKING = 0,
            SPLIT_WITH_CONFIDENCE = 1;
    
    public int splitByBreakingTies = 0;

    public IADEM3Tree(InstancesHeader problemDescription,
            double attDif,
            IademSplitMeasure measure,
            int predictionType,
            int nbLimit,
            double percent,
            IademNumericAttributeObserver numericAttObs,
            int maxBins,
            AbstractChangeDetector estimator,
            boolean restartAtDrift,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest,
            int gracePeriod,
            int treeLevel,
            int maxTreeLevel,
            int maxAltSubtreesPerNode) {
        super(problemDescription,
                attDif,
                measure,
                predictionType,
                nbLimit,
                percent,
                numericAttObs,
                maxBins,
                onlyMultiwayTest,
                onlyBinaryTest,
                estimator,
                gracePeriod);
        if (estimator != null) {
            ((AdaptiveLeafNode) this.treeRoot).estimator = (AbstractChangeDetector) estimator.copy();
        } else {
            ((AdaptiveLeafNode) this.treeRoot).estimator = null;
        }
        this.restartAtDrift = restartAtDrift;
        
        this.treeLevel = treeLevel;
        this.maxTreeLevel = maxTreeLevel;
        this.maxAltSubtreesPerNode = maxAltSubtreesPerNode;
    }

    @Override
    public void createRoot(InstancesHeader description,
            IademNumericAttributeObserver numericAttObs) {
        
        double[] arrayCont = new double[description.attribute(description.classIndex()).numValues()];
        Arrays.fill(arrayCont, 0);

        this.treeRoot = newLeafNode(null, 0, 0, arrayCont);
    }

    public void addSubtree(Subtree subtree) {
        this.subtreeList.add(subtree);
    }

    public void removeSubtree(Subtree subtree) {
        this.subtreeList.remove(subtree);
    }

    public boolean canCreateSubtree() {
        if (this.maxSubtrees > 0) {
            int count = getNumberOfSubtrees();
            if (count >= this.maxSubtrees) {
                return false;
            }
        }
        return true;
    }

    @Override
    public LeafNode newLeafNode(Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] initialClassCount) {
        switch (this.predictionType) {
            case 0: {
                return new AdaptiveLeafNode(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        this.numericAttObserver,
                        this.estimator,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest);
            }
            case 1: {
                return new AdaptiveLeafNodeNB(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        this.numericAttObserver,
                        this.naiveBayesLimit,
                        this.estimator,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest);
            }
            case 2: {
                return new AdaptiveLeafNodeNBKirkby(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        this.numericAttObserver,
                        this.naiveBayesLimit,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest,
                        this.estimator);
            }
            default: {
                return new AdaptiveLeafNodeWeightedVote(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        this.numericAttObserver,
                        this.naiveBayesLimit,
                        this.onlyMultiwayTest,
                        this.onlyBinaryTest,
                        this.estimator);
            }
        }
    }

    public int getTreeLevel() {
        return treeLevel;
    }

    public int getMaxAltSubtreesPerNode() {
        return maxAltSubtreesPerNode;
    }

    public int getMaxTreeLevel() {
        return maxTreeLevel;
    }

    public boolean isRestaurarVectoresPrediccion() {
        return restartAtDrift;
    }

    public int numDeletedTrees() {
        return deletedTrees;
    }

    public int numTrees() {
        int subtreeCount;
        if (this.treeRoot instanceof AdaptiveLeafNode) {
            subtreeCount = 0;
        } else {
            AdaptiveSplitNode nodo = (AdaptiveSplitNode) this.treeRoot;
            subtreeCount = nodo.getNumTrees();
        }
        return subtreeCount;
    }

    public void newTreeChange() {
        interchangedTrees++;
        numTrees--;
    }

    public void newDeletedTree() {
        deletedTrees++;
        numTrees--;
    }

    public int numSubtrees() {
        return tmpNumSubtrees(treeRoot);
    }

    private int tmpNumSubtrees(Node node) {
        int count = 0;
        if (node instanceof AdaptiveSplitNode) {
            count++;
            AutoExpandVector<Subtree> subtree = ((AdaptiveSplitNode) node).alternativeTree;
            for (Subtree currentSubtree : subtree) {
                count += currentSubtree.numSubtrees();
            }
        }
        if (node instanceof AdaptiveSplitNode) {
            AdaptiveSplitNode nodoAuxiliar = (AdaptiveSplitNode) node;
            for (Node child : nodoAuxiliar.children) {
                count += tmpNumSubtrees(child);
            }
        }
        return count;
    }

    protected boolean hasTree(Node node) {

        boolean ret = false;
        if (node instanceof AdaptiveSplitNode) {
            AdaptiveSplitNode tmp = (AdaptiveSplitNode) node;
            if (tmp.alternativeTree != null) {
                ret = true;
            }
            for (int i = 0; ret == false && i < tmp.children.size(); i++) {
                ret = ret || hasTree(tmp.children.get(i));
            }
        }
        return ret;
    }

    @Override
    public void learnFromInstance(Instance instance)
            throws IademException {
        getClassVotes(instance); // to update lastPrediction in the trees 
        getClassVotesFromLeaf(instance);
        treeRoot.learnFromInstance(instance);
    }

    protected void getClassVotesFromLeaf(Instance instance) {
        double[] votes = null;
        Node node = this.treeRoot;
        while (votes == null) {
            if (node instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode splitNode = (AdaptiveSplitNode) node;
                int childIndex = splitNode.instanceChildIndex(instance);
                if (childIndex >= 0) {
                    node = splitNode.getChild(childIndex);
                } else {
                    votes = splitNode.leaf.getClassVotes(instance);
                }
            } else {
                AdaptiveLeafNode leafNode = (AdaptiveLeafNode) node;
                votes = leafNode.getClassVotes(instance);
            }
        }
        this.lastPredictionInLeaf = Utils.maxIndex(votes);
    }

    public void copyTree(Subtree arbol) {
        this.treeRoot = arbol.treeRoot;
    }

    void setNewTree() {
        numTrees++;
    }

    public int getChangedTrees() {
        return interchangedTrees;
    }

    @Override
    public double[] getClassVotes(Instance instance) {
        double[] votes = super.getClassVotes(instance);
        this.lastPrediction = Utils.maxIndex(votes);
        return votes;
    }

    public int getNumberOfSubtrees() {
        if (this.treeRoot instanceof AdaptiveSplitNode) {
            return ((AdaptiveSplitNode) this.treeRoot).getNumberOfSubtrees();
        }
        return 0;
    }

    protected IADEM3Tree getMainTree() {
        return this;
    }

    public void updateNumberOfLeaves(int amount) {
        this.numberOfLeaves += amount;
    }

    public void updateNumberOfNodes(int amount) {
        this.numberOfNodes += amount;
    }
    
    public void updateNumberOfNodesSplitByTieBreaking(int amount) {
        this.splitByBreakingTies += amount;
    }
}
