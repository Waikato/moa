/*
 *    ASHoeffdingTree.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers.trees;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * Adaptive Size Hoeffding Tree used in Bagging using trees of different size.
 * The Adaptive-Size Hoeffding Tree (ASHT) is derived from the Hoeffding Tree
 * algorithm with the following differences:
 * <ul>
 * <li> it has a maximum number of split nodes, or size
 * <li> after one node splits, if the number of split nodes of the ASHT tree
 * is higher than the maximum value, then it deletes some nodes to reduce its size
 * </ul>
 * The intuition behind this method is as follows: smaller trees adapt
 * more quickly to changes, and larger trees do better during periods with
 * no or little change, simply because they were built on more data. Trees
 * limited to size s will be reset about twice as often as trees with a size
 * limit of 2s. This creates a set of different reset-speeds for an ensemble of such
 * trees, and therefore a subset of trees that are a good approximation for the
 * current rate of change. It is important to note that resets will happen all
 * the time, even for stationary datasets, but this behaviour should not have
 * a negative impact on the ensemble’s predictive performance.
 * When the tree size exceeds the maximun size value, there are two different
 * delete options: <ul>
 * <li> delete the oldest node, the root, and all of its children except the one
 * where the split has been made. After that, the root of the child not
 * deleted becomes the new root
 * <li> delete all the nodes of the tree, i.e., restart from a new root.
 * </ul>
 * The maximum allowed size for the n-th ASHT tree is twice the maximum
 * allowed size for the (n-1)-th tree. Moreover, each tree has a weight
 * proportional to the inverse of the square of its error, and it monitors its
 * error with an exponential weighted moving average (EWMA) with alpha = .01.
 * The size of the first tree is 2.
 * <br/><br/>
 * With this new method, it is attempted to improve bagging performance
 * by increasing tree diversity. It has been observed that boosting tends to
 * produce a more diverse set of classifiers than bagging, and this has been
 * cited as a factor in increased performance.<br/>
 * See more details in:<br/><br/>
 * Albert Bifet, Geoff Holmes, Bernhard Pfahringer, Richard Kirkby,
 * and Ricard Gavaldà. New ensemble methods for evolving data
 * streams. In 15th ACM SIGKDD International Conference on Knowledge
 * Discovery and Data Mining, 2009.<br/><br/>
 * The learner must be ASHoeffdingTree, a Hoeffding Tree with a maximum
 * size value.<br/><br/>
 * Example:<br/><br/>
 * <code>OzaBagASHT -l ASHoeffdingTree -s 10 -u -r </code>
 * Parameters:<ul>
 * <li>Same parameters as <code>OzaBag</code>
 * <li>-f : the size of first classifier in the bag.
 * <li>-u : Enable weight classifiers
 * <li>-r : Reset trees when size is higher than the max
 * </ul>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class ASHoeffdingTree extends HoeffdingTree {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Adaptive Size Hoeffding Tree used in Bagging using trees of different size.";
    }    
    
    protected int maxSize = 10000; //EXTENSION TO ASHT

    protected boolean resetTree = false;

    @Override
    public void resetLearningImpl() {
        this.treeRoot = null;
        this.decisionNodeCount = 0;
        this.activeLeafNodeCount = 0;
        this.inactiveLeafNodeCount = 0;
        this.inactiveLeafByteSizeEstimate = 0.0;
        this.activeLeafByteSizeEstimate = 0.0;
        this.byteSizeEstimateOverheadFraction = 1.0;
        this.growthAllowed = true;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            this.activeLeafNodeCount = 1;
        }
        FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
        Node leafNode = foundNode.node;
        if (leafNode == null) {
            leafNode = newLearningNode();
            foundNode.parent.setChild(foundNode.parentBranch, leafNode);
            this.activeLeafNodeCount++;
        }
        if (leafNode instanceof LearningNode) {
            LearningNode learningNode = (LearningNode) leafNode;
            learningNode.learnFromInstance(inst, this);
            if (this.growthAllowed
                    && (learningNode instanceof ActiveLearningNode)) {
                ActiveLearningNode activeLearningNode = (ActiveLearningNode) learningNode;
                double weightSeen = activeLearningNode.getWeightSeen();
                if (weightSeen
                        - activeLearningNode.getWeightSeenAtLastSplitEvaluation() >= this.gracePeriodOption.getValue()) {
                    attemptToSplit(activeLearningNode, foundNode.parent,
                            foundNode.parentBranch);
                    //EXTENSION TO ASHT
                    // if size too big, resize tree ONLY Split Nodes
                    while (this.decisionNodeCount >= this.maxSize && this.treeRoot instanceof SplitNode) {
                        if (this.resetTree == false) {
                            resizeTree(this.treeRoot, ((SplitNode) this.treeRoot).instanceChildIndex(inst));
                            this.treeRoot = ((SplitNode) this.treeRoot).getChild(((SplitNode) this.treeRoot).instanceChildIndex(inst));
                        } else {
                            resetLearningImpl();
                        }
                    }
                    activeLearningNode.setWeightSeenAtLastSplitEvaluation(weightSeen);
                }
            }
        }
        if (this.trainingWeightSeenByModel
                % this.memoryEstimatePeriodOption.getValue() == 0) {
            estimateModelByteSizes();
        }
    }

    //EXTENSION TO ASHT
    public void setMaxSize(int mSize) {
        this.maxSize = mSize;
    }

    public void setResetTree() {
        this.resetTree = true;
    }

    public void deleteNode(Node node, int childIndex) {
        Node child = ((SplitNode) node).getChild(childIndex);
        //if (child != null) {
        //}
        if (child instanceof SplitNode) {
            for (int branch = 0; branch < ((SplitNode) child).numChildren(); branch++) {
                deleteNode(child, branch);
            }
            this.decisionNodeCount--;
        } else if (child instanceof InactiveLearningNode) {
            this.inactiveLeafNodeCount--;
        } else if (child instanceof ActiveLearningNode) {
            this.activeLeafNodeCount--;
        }
        child = null;
    }

    public void resizeTree(Node node, int childIndex) {
        //Assume that this is root node
        if (node instanceof SplitNode) {
            for (int branch = 0; branch < ((SplitNode) node).numChildren(); branch++) {
                if (branch != childIndex) {
                    deleteNode(node, branch);
                }
            }
        }
    }
}
