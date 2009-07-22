/*
 *    ASHoeffdingTree.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import moa.AbstractMOAObject;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.MultiChoiceOption;
import sizeof.agent.SizeOfAgent;
import weka.core.Instance;
import weka.core.Utils;

public class ASHoeffdingTree extends HoeffdingTreeNBAdaptive {

	private static final long serialVersionUID = 1L;

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
		FoundNode foundNode = this.treeRoot
				.filterInstanceToLeaf(inst, null, -1);
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
						- activeLearningNode
								.getWeightSeenAtLastSplitEvaluation() >= this.gracePeriodOption
						.getValue()) {
					attemptToSplit(activeLearningNode, foundNode.parent,
							foundNode.parentBranch);
					//EXTENSION TO ASHT
					// if size too big, resize tree ONLY Split Nodes
					while (this.decisionNodeCount  >= this.maxSize && this.treeRoot instanceof SplitNode) {
						if (this.resetTree == false) {					
							resizeTree(this.treeRoot, ((SplitNode) this.treeRoot).instanceChildIndex(inst));
							this.treeRoot = ((SplitNode) this.treeRoot).getChild(((SplitNode) this.treeRoot).instanceChildIndex(inst));
						}else {
							resetLearningImpl();
						}
					}	
					activeLearningNode
							.setWeightSeenAtLastSplitEvaluation(weightSeen);
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
			Node child =((SplitNode) node).getChild(childIndex);
			//if (child != null) {	
			//}
			if (child instanceof SplitNode) {
					for (int branch = 0; branch < ((SplitNode) child).numChildren(); branch++) {
						deleteNode(child, branch);
					}
					this.decisionNodeCount--;	
			} else if (child instanceof InactiveLearningNode ) {
					this.inactiveLeafNodeCount--;	
			} else if (child instanceof ActiveLearningNode ) {
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
