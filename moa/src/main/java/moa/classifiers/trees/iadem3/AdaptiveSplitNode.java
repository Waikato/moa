/*
 *    AdaptiveSplitNode.java
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
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iadem2.Node;
import moa.classifiers.trees.iadem2.SplitNode;
import moa.classifiers.trees.iadem2.VirtualNode;
import moa.classifiers.trees.iademutils.IademCommonProcedures;
import moa.classifiers.trees.iademutils.IademException;
import moa.classifiers.trees.iademutils.IademNominalAttributeBinaryTest;
import moa.core.AutoExpandVector;


/**
 * 
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class AdaptiveSplitNode extends SplitNode implements Serializable {

    private static final long serialVersionUID = 1L;

    protected AutoExpandVector<Subtree> alternativeTree = new AutoExpandVector<Subtree>();
    // Detector de cambio de concepto
    protected AbstractChangeDetector estimator;

    protected int causeOfSplit;

    protected AdaptiveLeafNode leaf;

    public AdaptiveSplitNode(IADEM3Tree tree,
            Node parent,
            Node[] child,
            double[] freq,
            InstanceConditionalTest splitTest,
            AbstractChangeDetector estimator,
            AdaptiveLeafNode predictionLeaf,
            int causeOfSplit) {
        super(tree, parent, child, freq, splitTest);
        if (estimator != null) {
            this.estimator = (AbstractChangeDetector) estimator.copy();
        } else {
            this.estimator = null;
        }
        this.leaf = predictionLeaf;
        this.leaf.setSplit(false);
        this.causeOfSplit = causeOfSplit;
    }

    @Override
    public Node learnFromInstance(Instance instance) {

        double thisError = this.estimator.getEstimation(),
                thisSize = this.estimator.getDelay();
        double leafError = this.leaf.estimator.getEstimation(),
                leafSize = this.leaf.estimator.getDelay();

        double m = 1.0 / thisSize + 1.0 / leafSize;
        double delta = 0.0001;
        double bound = Math.sqrt(m * Math.log(2.0 / delta) / 2.0);
        double diff = thisError - leafError;
        if (diff > bound && thisSize > 600 && leafSize > 600/**/) {
            prune();
            return this.leaf;
        } else if (-diff > bound) {
            this.leaf.restartVariablesAtDrift();
            this.leaf.estimator = (AbstractChangeDetector) this.leaf.estimator.copy();
        }

        Node node;
        boolean rightPredicted = ((IADEM3Tree) this.tree).lastPredictionInLeaf == instance.classValue();
        node = checkAlternativeSubtrees(rightPredicted);
        if (node == null) {
            // no subtree change
            for (Subtree subtree : this.alternativeTree) {
                try {
                    subtree.learnFromInstance(instance);
                    subtree.incrNumberOfInstancesProcessed();
                } catch (IademException ex) {
                    Logger.getLogger(AdaptiveSplitNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            this.leaf.learnFromInstance(instance);
            return super.learnFromInstance(instance);
        } else {
            // subtree change
            return node.learnFromInstance(instance);
        }
    }

    private Node checkAlternativeSubtrees(boolean acierto) {
        if (this.estimator != null) {
            double loss = (acierto == true ? 0.0 : 1.0);
            estimator.input(loss);
            if (estimator.getChange()) {
                this.createTree();
            }
            for (int i = 0; i < this.alternativeTree.size(); i++) {
                Subtree subtree = alternativeTree.get(i);
                double treeError = subtree.estimacionValorMedio(),
                        thisError = this.estimator.getEstimation();
                double bound = IademCommonProcedures.AverageComparitionByHoeffdingCorollary(this.estimator.getDelay(),
                        subtree.windowWidth(),
                        1e-4);

                if (thisError - treeError > bound/**/) {
                    ((IADEM3Tree) this.tree).interchangedTrees++;
                    return changeTrees(i);
                } else if (isUseless(i)) {
                    ((IADEM3Tree) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                    ((IADEM3Tree) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                    ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.splitByBreakingTies);
                    i--;
                } else if (this.estimator.getDelay() > 6000
                        && subtree.windowWidth() > 6000/**/) {
                    {
                        if (treeError - thisError > bound) {
                            this.alternativeTree.remove(i);
                            // update number of nodes
                            ((IADEM3Tree) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                            ((IADEM3Tree) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                            ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.splitByBreakingTies);
                            i--;
                        } else /**/ {
                            int[] countMain = new int[3],
                                    countAlt = new int[3];
                            for (Node child : this.children) {
                                child.getNumberOfNodes(countMain);
                            }
                            subtree.getNumberOfNodes(countAlt);/**/

                            if (countMain[0] + countMain[1] + 1 > countAlt[0] + countAlt[1]) {
                                return changeTrees(i);
                            } else {
                                this.alternativeTree.remove(i);
                                // update number of nodes
                                ((IADEM3Tree) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                                ((IADEM3Tree) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                                ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.splitByBreakingTies);
                                i--;
                            }
                        }
                    }/**/

                }

            }
        }
        return null;
    }

    public boolean isUseless(int i) {
        boolean removed = false;
        Subtree subtree = this.alternativeTree.get(i);
        if (subtree.getTreeRoot() instanceof AdaptiveSplitNode) /**/ {
            // change if it already has an alternative subtree 
            AdaptiveSplitNode splitNode = ((AdaptiveSplitNode) subtree.getTreeRoot());

            int nMain = (int) this.estimator.getDelay(),
                    nAlt = (int) subtree.getEstimador().getDelay();
            double errorMain = this.estimator.getEstimation(),
                    errorAlt = subtree.getEstimador().getEstimation();
            double errorDifference = errorAlt - errorMain,
                    absError = Math.abs(errorDifference);
            if (!removed && nMain > 0 && nAlt > 0) {
                double m = 1.0 / nMain + 1.0 / nAlt;
                double delta = 1e-4;
                double bound = Math.sqrt(m * Math.log(2.0 / delta) / 2.0);
                if (errorDifference > bound) {
                    // alternative tree is too inaccurate
                    this.alternativeTree.remove(i);
                    removed = true;
                }
            }
            if (!removed) {
                InstanceConditionalTest condTest = splitNode.splitTest;
                if (condTest instanceof IademNominalAttributeBinaryTest
                        && this.splitTest instanceof IademNominalAttributeBinaryTest) {
                    IademNominalAttributeBinaryTest altTest = (IademNominalAttributeBinaryTest) condTest,
                            mainTest = (IademNominalAttributeBinaryTest) this.splitTest;
                    if (mainTest.getAttValue() == altTest.getAttValue()
                            && mainTest.getAttsTestDependsOn()[0] == altTest.getAttsTestDependsOn()[0]) {
                        this.alternativeTree.remove(i);
                        removed = true;
                    }
                } else if (condTest instanceof NominalAttributeMultiwayTest
                        && this.splitTest instanceof NominalAttributeMultiwayTest) {
                    NominalAttributeMultiwayTest altTest = (NominalAttributeMultiwayTest) condTest,
                            mainTest = (NominalAttributeMultiwayTest) this.splitTest;
                    if (mainTest.getAttsTestDependsOn()[0] == altTest.getAttsTestDependsOn()[0]) {
                        this.alternativeTree.remove(i);
                        removed = true;
                    }
                } else if (condTest instanceof NumericAttributeBinaryTest
                        && this.splitTest instanceof NumericAttributeBinaryTest) {
                    NumericAttributeBinaryTest altTest = (NumericAttributeBinaryTest) condTest,
                            mainTest = (NumericAttributeBinaryTest) this.splitTest;
                    if (mainTest.getAttsTestDependsOn()[0] == altTest.getAttsTestDependsOn()[0]
                            && mainTest.getSplitValue() == altTest.getSplitValue()) {
                        this.alternativeTree.remove(i);
                        removed = true;
                    }
                }
            }
        }
        return removed;
    }

    private Node changeTrees(int index) {
        for (int i = 0; i < this.alternativeTree.size(); i++) {
            if (i != index) {
                Subtree subtree = this.alternativeTree.get(i);
                ((IADEM3Tree) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                ((IADEM3Tree) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.splitByBreakingTies);
            }
        }
        Subtree subtree = this.alternativeTree.get(index);
        // rest nodes of this main tree
        int count[] = new int[3];
        super.getNumberOfNodes(count);
        if (this.causeOfSplit == ((IADEM3Tree) this.tree).SPLIT_BY_TIE_BREAKING) {
            count[2]++;
        }
        ((IADEM3Tree) this.tree).updateNumberOfLeaves(-count[1]);
        ((IADEM3Tree) this.tree).updateNumberOfNodes(-count[0] - count[1]);
        ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(-count[2]);
        //
        AdaptiveSplitNode tmpParent = (AdaptiveSplitNode) this.parent;
        Node newNode = subtree.getTreeRoot();
        ((IADEM3Tree) tree).newTreeChange();
        if (tmpParent == null) {
            ((IADEM3Tree) tree).copyTree(subtree);
        } else {
            for (int i = 0; i < tmpParent.children.size(); i++) {
                if (tmpParent.children.get(i) == this) {
                    tmpParent.children.set(i, newNode);
                    newNode.parent = tmpParent;
                }
            }
        }
        updateAttributes(newNode);
        if (newNode instanceof AdaptiveSplitNode) {
            AdaptiveSplitNode splitNode = (AdaptiveSplitNode) newNode;
            for (Subtree currentSubtree : splitNode.alternativeTree) {
                updateSubtreeLevel(currentSubtree.getTreeRoot());
            }
        }
        return newNode;
    }

    void updateAttributes(Node newNode) {
        if (newNode == null) {
            return;
        }
        newNode.setTree(this.tree);
        if (newNode instanceof AdaptiveSplitNode) {
            AdaptiveSplitNode splitNode = (AdaptiveSplitNode) newNode;
            splitNode.leaf.setTree(this.tree);
            for (Node child : splitNode.children) {
                updateAttributes(child);
            }
        } else if (newNode instanceof AdaptiveLeafNode) {
            AdaptiveLeafNode leafNode = (AdaptiveLeafNode) newNode;
            AutoExpandVector<VirtualNode> virtualChildren = leafNode.getVirtualChildren();
            for (VirtualNode child : virtualChildren) {
                if (child != null) {
                    child.setTree(this.tree);
                }
            }
        }
    }

    protected void updateSubtreeLevel(Node node) {
        if (node != null) {
            ((IADEM3Tree) node.getTree()).treeLevel--;
            if (node instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode splitNode = (AdaptiveSplitNode) node;
                for (Node child : splitNode.children) {
                    updateSubtreeLevelAux(child);
                }
                for (Subtree subtree : splitNode.alternativeTree) {
                    updateSubtreeLevel(subtree.getTreeRoot());
                }
            }
        }
    }

    protected void updateSubtreeLevelAux(Node node) {
        if (node != null) {
            if (node instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode splitNode = (AdaptiveSplitNode) node;
                // update level in alternative subtrees
                for (Subtree subtree : splitNode.alternativeTree) {
                    updateSubtreeLevel(subtree.getTreeRoot());
                }
                for (Node child : splitNode.children) {
                    updateSubtreeLevelAux(child);
                }
            }
        }
    }

    void createTree() {
        IADEM3Tree iadem3Tree = ((IADEM3Tree) this.tree);
        if (iadem3Tree.canCreateSubtree()) {
            int maxTreeLevel = iadem3Tree.getMaxTreeLevel();
            int maxAltSubtrees = iadem3Tree.getMaxAltSubtreesPerNode();
            if ((maxTreeLevel == -1 || iadem3Tree.getTreeLevel() < maxTreeLevel)
                    && (maxAltSubtrees == -1 || this.alternativeTree.size() < maxAltSubtrees)) {
                if (this.estimator != null) {
                    AbstractChangeDetector tmpEstimator = this.estimator;
                    int numBins = this.tree.getMaxNumberOfBins();
                    Subtree subtree = new Subtree(tree.getProblemDescription(),
                            tree.getAttributeDifferentiation(),
                            tree.getMeasure(),
                            tree.getPredictionType(),
                            tree.getNaiveBayesLimit(),
                            tree.getPercentInCommon(),
                            tree.getNumericAttObserver(),
                            numBins,
                            tmpEstimator,
                            iadem3Tree.restartAtDrift,
                            this,
                            this.tree.isOnlyMultiwayTest(),
                            this.tree.isOnlyBinaryTest(),
                            this.tree.getGracePeriod(),
                            iadem3Tree.getTreeLevel() + 1,
                            iadem3Tree.getMaxTreeLevel(),
                            iadem3Tree.getMaxAltSubtreesPerNode(),
                            (IADEM3Tree) this.tree);
                    this.alternativeTree.add(subtree);
                    ((IADEM3Tree) tree).setNewTree();
                }
            }
        }
    }
    
    public int getNumTrees() {
        int trees = this.alternativeTree.size() == 0 ? 0 : 1;
        for (Node child : children) {
            if (child instanceof AdaptiveSplitNode) {
                trees += ((AdaptiveSplitNode) child).getNumTrees();
            }
        }
        for (Subtree subtree : this.alternativeTree) {
            if (subtree.getTreeRoot() instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode node = (AdaptiveSplitNode) subtree.getTreeRoot();
                trees += node.getNumTrees();
            }
        }
        return trees;
    }

    @Override
    public double[] getClassVotes(Instance observacion) {
        double[] classDist = this.leaf.getClassVotes(observacion);

        double thisError = this.estimator.getEstimation();
        double leafError = this.leaf.estimator.getEstimation();

        int childIndex = instanceChildIndex(observacion);
        if (childIndex >= 0 && thisError < leafError) {
            Node hijo = getChild(childIndex);
            classDist = hijo.getClassVotes(observacion);
        }

        for (Subtree subtree : this.alternativeTree) {
            double[] tmp = subtree.getClassVotes(observacion);
            double altWeight = 1.0 - subtree.estimacionValorMedio();
            for (int j = 0; j < classDist.length; j++) {
                classDist[j] = classDist[j] + tmp[j] * altWeight;
            }
        }
        return classDist;
    }

    @Override
    public int getSubtreeNodeCount() {
        int tmp = super.getSubtreeNodeCount();
        for (Subtree subtree : this.alternativeTree) {
            tmp += subtree.getTreeRoot().getSubtreeNodeCount();
        }
        return tmp;
    }

    public double getErrorEstimation() {
        return this.estimator.getEstimation();
    }

    @Override
    public void getNumberOfNodes(int[] count) {
        for (Subtree tree : this.alternativeTree) {
            tree.getNumberOfNodes(count);
        }
        if (this.causeOfSplit == ((IADEM3Tree) this.tree).SPLIT_BY_TIE_BREAKING) {
            count[2]++;
        }
        super.getNumberOfNodes(count);
    }

    public int getNumberOfSubtrees() {
        int count = this.alternativeTree.size();
        for (Subtree subtree : this.alternativeTree) {
            count += ((IADEM3Tree) subtree).getNumberOfSubtrees();
        }
        for (Node child : children) {
            if (child instanceof AdaptiveSplitNode) {
                count += ((AdaptiveSplitNode) child).getNumberOfSubtrees();
            }
        }
        return count;
    }

    private void prune() {
        this.leaf.setSplit(true);
        for (Node node = this.parent; node != null; node = node.parent) {
            ((AdaptiveSplitNode) node).leaf.restartVariablesAtDrift();
            this.leaf.estimator = (AbstractChangeDetector) this.leaf.estimator.copy();
        }
        // update tree
        this.leaf.setTree(this.tree);
        AutoExpandVector<VirtualNode> nodeList = this.leaf.getVirtualChildren();
        for (VirtualNode node : nodeList) {
            if (node != null) {
                node.setTree(this.tree);
            }
        }

        this.leaf.setParent(this.parent);

        if (this.parent == null) {
            this.tree.setTreeRoot(this.leaf);
        } else {
            ((SplitNode) this.parent).changeChildren(this, this.leaf);
        }

        int count[] = new int[3];
        getNumberOfNodes(count);

        ((IADEM3Tree) this.tree).updateNumberOfLeaves(-count[1] + 1);
        ((IADEM3Tree) this.tree).updateNumberOfNodes(-count[0] - count[1] + 1);
        ((IADEM3Tree) this.tree).updateNumberOfNodesSplitByTieBreaking(-count[2]);
    }
}
