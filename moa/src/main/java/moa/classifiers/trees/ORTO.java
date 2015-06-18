/*
 *    ORTO.java
 *    Copyright (C) 2015 Jožef Stefan Institute, Ljubljana, Slovenia
 *    @author Aljaž Osojnik <aljaz.osojnik@ijs.si>
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

package moa.classifiers.trees;

import java.util.Stack;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;

import moa.classifiers.Regressor;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.Measurement;

/*
 * Implementation of ORTO, option trees for data streams.
 */

public class ORTO extends FIMTDD implements Regressor {

	private static final long serialVersionUID = 1L;

	private int innerNodeCount = 0;
	private int optionNodeCount = 0;

	private int numTrees = 1;


	//region ================ OPTIONS ================

	public IntOption maxTreesOption = new IntOption(
			"maxTrees",
			'm',
			"The maximum number of trees contained in the option tree.",
			10, 1, Integer.MAX_VALUE);

	public IntOption maxOptionLevelOption = new IntOption(
			"maxOptionLevel",
			'x',
			"The maximal depth at which option nodes can be created.",
			10, 0, Integer.MAX_VALUE);

	public FloatOption optionDecayFactorOption = new FloatOption(
			"optionDecayFactor",
			'z',
			"The option decay factor that determines how many options can be selected at a given level.",
			0.9, 0.0, 1.0);

	public MultiChoiceOption optionNodeAggregationOption = new MultiChoiceOption(
			"optionNodeAggregation",
			'o',
			"The aggregation method used to combine predictions in option nodes.", 
			new String[]{"average", "bestTree"}, new String[]{"Average", "Best tree"}, 0);

	public FloatOption optionFadingFactorOption = new FloatOption(
			"optionFadingFactor",
			'q',
			"The fading factor used for comparing subtrees of an option node.",
			0.9995, 0.0, 1.0);

	//endregion ================ OPTIONS ================

	//region ================ CLASSES ================

	public static class OptionNode extends InnerNode {

		private static final long serialVersionUID = 1L;

		protected double[] optionFFSSL;
		protected double[] optionFFSeen;

		public OptionNode(FIMTDD tree) {
			super(tree);
		}

		public void resetFF() {
			optionFFSSL = new double[children.size()];
			optionFFSeen = new double[children.size()];
			
			for (int i = 0; i < children.size(); i++) {
				optionFFSSL[i] = 0.0;
				optionFFSeen[i] = 0.0;
			}
		}

		public int getNumSubtrees() {
			int num = 0;
			for (Node child : children) {
				num += child.getNumSubtrees();
			}
			return num;
		}

		public int directionForBestTree() {
			int d = 0;
			double tmp = 0.0, min = Double.MAX_VALUE;
			for (int i = 0; i < children.size(); i++) {
				tmp = getFFRatio(i);
				if (tmp < min) {
					min = tmp;
					d = i;
				}
			}
			return d;
		}

		public double getPrediction(Instance inst, ORTO tree) {
			double[] predictions = new double[numChildren()];
			for (int i = 0; i < numChildren(); i++) {
				predictions[i] = getChild(i).getPrediction(inst);
			}
			return aggregate(predictions, tree);
		}

		private double aggregate(double[] predictions, ORTO tree) {
			if (tree.optionNodeAggregationOption.getChosenIndex() == 0) { // Average
				double sum = 0.0;
				for (int i = 0; i < predictions.length; i++) {
					sum += predictions[i];
				}
				return sum / predictions.length;
			} else if (tree.optionNodeAggregationOption.getChosenIndex() == 1) {
				int d = directionForBestTree();
				return predictions[d];
			} else {
				return 0.0;
			}
		}

		public double getFFRatio(int childIndex) {
			return optionFFSSL[childIndex] / optionFFSeen[childIndex];
		}
		
		protected boolean skipInLevelCount() {
			return true;
		}
	}

	//endregion ================ CLASSES ================

	//region ================ METHODS ================

	// region --- Regressor methods

	public String getPurposeString() {
		return "Implementation of the ORTO tree as described by Ikonomovska et al.";
	}

	public void resetLearningImpl() {
		super.resetLearningImpl();
		this.innerNodeCount = 0;
		this.optionNodeCount = 0;
	}

	
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{ 
				new Measurement("number of subtrees", this.numTrees),
				new Measurement("tree size (nodes)", this.leafNodeCount + this.innerNodeCount),
				new Measurement("tree size (leaves)", this.leafNodeCount),
				new Measurement("number of option nodes", this.optionNodeCount),};
	}

	public void processInstance(Instance inst, Node node, double prediction, double normalError, boolean growthAllowed, boolean inAlternate) {
		if (node instanceof OptionNode) {
			processInstanceOptionNode(inst, (OptionNode) node, prediction, normalError, growthAllowed, inAlternate);
		} else {
			Node currentNode = node;
			while (true) {
				if (currentNode instanceof LeafNode) {
					((LeafNode) currentNode).learnFromInstance(inst, growthAllowed);
					break;
				} else {
					currentNode.examplesSeen += inst.weight();
					currentNode.sumOfAbsErrors += inst.weight() * normalError;
					InnerNode iNode = (InnerNode) currentNode;
					if (!inAlternate && iNode.alternateTree != null) {
						boolean altTree = true;
						double lossO = Math.pow(inst.classValue() - prediction, 2);
						double lossA = Math.pow(inst.classValue() - currentNode.alternateTree.getPrediction(inst), 2);
						
						// Loop for compatibility with bagging methods
						for (int i = 0; i < inst.weight(); i++) {
							iNode.lossFadedSumOriginal = lossO + alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumOriginal;
							iNode.lossFadedSumAlternate = lossA + alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumAlternate;
							iNode.lossExamplesSeen++;
							
							double Qi = Math.log((iNode.lossFadedSumOriginal) / (iNode.lossFadedSumAlternate));
							iNode.lossSumQi += Qi;
							iNode.lossNumQiTests += 1;
						}
						double Qi = Math.log((iNode.lossFadedSumOriginal) / (iNode.lossFadedSumAlternate));
						double previousQiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
						double QiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
						
						if (iNode.lossExamplesSeen - iNode.previousWeight >= alternateTreeTMinOption.getValue()) {
							iNode.previousWeight = iNode.lossExamplesSeen;
							if (Qi > 0) {
								// Switch the subtrees
								Node parent = currentNode.parent;
								
								if (parent != null) {
									Node replacementTree = iNode.alternateTree;
									parent.setChild(parent.getChildIndex(iNode), replacementTree);
									if (growthAllowed) replacementTree.restartChangeDetection();
								} else {
									treeRoot = iNode.alternateTree;
									treeRoot.restartChangeDetection();
								}
								optionNodeCount += currentNode.alternateTree.getNumSubtrees() - currentNode.getNumSubtrees();
								removeExcessTrees();
										
								currentNode = iNode.alternateTree;
								currentNode.originalNode = null;
								altTree = false;
							} else if (QiAverage < previousQiAverage && iNode.lossExamplesSeen >= (10 * alternateTreeTMinOption.getValue()) || iNode.lossExamplesSeen >= alternateTreeTimeOption.getValue()) {
								// Remove the alternate tree
								iNode.alternateTree = null;
								if (growthAllowed) iNode.restartChangeDetection();
								altTree = false;
							}
						}
						if (altTree) {
							growthAllowed = false; // this is the growth of the original tree
							processInstance(inst, currentNode.alternateTree, prediction, normalError, true, true); // growth is allowed in the alt tree
						} else if (currentNode instanceof OptionNode) {
							// this happens when an option node is switched into the tree
							for (Node child : ((OptionNode) currentNode).children) {
								processInstance(inst, child, child.getPrediction(inst), normalError, growthAllowed, inAlternate);
							}
							break;
						}
					}
				
					if (iNode.changeDetection && !inAlternate) {
						if (iNode.PageHinckleyTest(normalError - iNode.sumOfAbsErrors / iNode.examplesSeen - PageHinckleyAlphaOption.getValue(), PageHinckleyThresholdOption.getValue())) {
							iNode.initializeAlternateTree();
						}
					}
					if (currentNode instanceof SplitNode) {
						currentNode = ((SplitNode) currentNode).descendOneStep(inst);
					} else if (currentNode instanceof OptionNode) {
						processInstanceOptionNode(inst, (OptionNode) currentNode, prediction, normalError, growthAllowed, inAlternate);
						break;
					}
				}
			}
		}
		
	}
	
	public void processInstanceOptionNode(Instance inst, OptionNode node, double prediction, double normalError, boolean growthAllowed, boolean inAlternate) {
		if (node.changeDetection) {
			double error = Math.abs(prediction - inst.classValue());
			node.sumOfAbsErrors += error;
			
			if (((InnerNode) node).PageHinckleyTest(error - node.sumOfAbsErrors / node.examplesSeen + PageHinckleyAlphaOption.getValue(), PageHinckleyThresholdOption.getValue())) {
				node.initializeAlternateTree();
			}
		}

		for (Node child : node.children) {
			int index = node.getChildIndex(child);
			double childPrediction = child.getPrediction(inst);
			// Loop for compatibility with bagging methods
			for (int i = 0; i < inst.weight(); i++) {
				node.optionFFSeen[index] = node.optionFFSeen[index] * optionFadingFactorOption.getValue() + 1;
				node.optionFFSSL[index] = node.optionFFSSL[index] * optionFadingFactorOption.getValue() + Math.pow(childPrediction - inst.classValue(), 2);
			}
		}

		for (Node child : node.children) {
			processInstance(inst, child, child.getPrediction(inst), normalError, growthAllowed && node.alternateTree == null, inAlternate);
		}
	}
	
	// endregion --- Regressor methods
	
	// region --- Object instantiation methods

	protected OptionNode newOptionNode() {
		maxID++;
		return new OptionNode(this);
	}
	
	// endregion --- Object instantiation methods

	// region --- Processing methods

	protected void attemptToSplit(LeafNode node, Node parent, int parentIndex) {
		// Initialize the split criterion 
		SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(splitCriterionOption);

		// Using this criterion, find the best split per attribute and rank the results
		AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion);
		List<AttributeSplitSuggestion> acceptedSplits = new LinkedList<AttributeSplitSuggestion>();
		Arrays.sort(bestSplitSuggestions);

		// Declare a variable to determine the number of splits to be performed
		int numSplits = 0;

		// If only one split was returned, use it (this generally shouldn't happen)
		if (bestSplitSuggestions.length == 1) {
			numSplits = 1;
			acceptedSplits.add(bestSplitSuggestions[0]);
		} else if (bestSplitSuggestions.length > 1) { // Otherwise, consider which of the splits proposed may be worth trying

			// Determine the Hoeffding bound value, used to select how many instances should be used to make a test decision
			// to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
			double hoeffdingBound = computeHoeffdingBound(1, splitConfidenceOption.getValue(), node.examplesSeen);

			// Determine the top two ranked splitting suggestions
			AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];

			// If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
			// as determined using the Hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
			// particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
			// splitting should occur.
			// Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
			// (default 0.05) is applied to the Hoeffding bound; if the Hoeffding bound is smaller than this limit then the two
			// competing attributes are equally good, and the split will be made on the one with the higher SDR value.
			if (secondBestSuggestion.merit / bestSuggestion.merit < 1 - hoeffdingBound) {
				numSplits = 1;
				acceptedSplits.add(bestSuggestion);
			} else if (numTrees < maxTreesOption.getValue() && node.getLevel() <= maxOptionLevelOption.getValue()) {
				for (AttributeSplitSuggestion suggestion : bestSplitSuggestions) {
					if (suggestion.merit / bestSuggestion.merit >= 1 - hoeffdingBound) {
						numSplits++;
						acceptedSplits.add(suggestion);
					}
				}
			} else if (hoeffdingBound < tieThresholdOption.getValue()) {
				numSplits = 1;
				acceptedSplits.add(bestSplitSuggestions[0]);
			} else { // If the splitting criterion was not met, initiate pruning of the E-BST structures in each attribute observer
				for (int i = 0; i < node.attributeObservers.size(); i++) {
					AttributeClassObserver obs = node.attributeObservers.get(i);
					if (obs != null) {
						((FIMTDDNumericAttributeClassObserver) obs).removeBadSplits(splitCriterion, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound);    
					}
				}
			}
		}

		// If the splitting criterion was met, split the current node using the chosen attribute test, and
		// make two new branches leading to (empty) leaves
		if (numSplits > 0) {
			double optionFactor = numSplits * Math.pow(optionDecayFactorOption.getValue(), (double) node.getLevel());

			if (numSplits == 1 || optionFactor < 2.0 || maxTreesOption.getValue() - numTrees <= 1) {
				AttributeSplitSuggestion splitDecision = acceptedSplits.get(0);
				SplitNode newSplit = newSplitNode(splitDecision.splitTest);
				for (int i = 0; i < splitDecision.numSplits(); i++) {
					LeafNode newChild = newLeafNode();
					newChild.setParent(newSplit);
					newSplit.setChild(i, newChild);
				}
				leafNodeCount--;
				innerNodeCount++;
				leafNodeCount += splitDecision.numSplits();
				if (parent == null) {
					treeRoot = newSplit;
				} else {
					parent.setChild(parent.getChildIndex(node), newSplit);
					newSplit.setParent(parent);
				}
			} else {
				OptionNode optionNode = newOptionNode();
				leafNodeCount--;
				int j = 0;

				for (AttributeSplitSuggestion splitDecision : acceptedSplits) {
					if (j > optionFactor || maxTreesOption.getValue() - numTrees <= 0) {
						break;
					}
					SplitNode newSplit = newSplitNode(splitDecision.splitTest);
					for (int i = 0; i < splitDecision.numSplits(); i++) {
						LeafNode newChild = newLeafNode();
						newChild.setParent(newSplit);
						newSplit.setChild(i, newChild);
					}

					leafNodeCount += splitDecision.numSplits();
					innerNodeCount++;
					numTrees++;

					newSplit.setParent(optionNode);
					optionNode.setChild(j, newSplit);
					j++;
				}

				innerNodeCount++;
				optionNodeCount++;

				if (parent == null) {
					treeRoot = optionNode;
				} else {
					parent.setChild(parent.getChildIndex(node), optionNode);
					optionNode.setParent(parent);
				}

				optionNode.resetFF();
			}
		}
	}

	// endregion --- Processing methods 
	
	// region --- Option tree methods
	protected Node findWorstOption() {
		Stack<Node> stack = new Stack<Node>();
		stack.add(treeRoot);

		double ratio = Double.MIN_VALUE;
		Node out = null;

		while (!stack.empty()) {
			Node node = stack.pop();
			if (node.getParent() instanceof OptionNode) {
				OptionNode myParent = (OptionNode) node.getParent();
				int nodeIndex = myParent.getChildIndex(node);
				double nodeRatio = myParent.getFFRatio(nodeIndex); 

				if (nodeRatio > ratio) {
					ratio = nodeRatio;
					out = node;
				}
			}
			if (node instanceof InnerNode) {
				for (Node child : ((InnerNode) node).children) {
					stack.add(child);
				}
			}
		}

		return out;
	}

	protected void removeExcessTrees() {
		while (numTrees > maxTreesOption.getValue()) {
			Node option = findWorstOption();
			OptionNode parent = (OptionNode) option.parent;
			int index = parent.getChildIndex(option);

			if (parent.children.size() == 2) {
				parent.children.remove(index);
				for (Node chld : parent.children) {
					chld.parent = parent.parent;
					parent.parent.setChild(parent.parent.getChildIndex(parent), chld);
				}
			} else {
				AutoExpandVector<Node> children = new AutoExpandVector<Node>();
				double[] optionFFSSL = new double[parent.children.size() - 1];
				double[] optionFFSeen = new double[parent.children.size() - 1];

				int seen = 0;

				for (int i = 0; i < parent.children.size() - 1; i++) {
					if (parent.getChild(i) != option) {
						children.add(parent.getChild(i));
						optionFFSSL[i] = parent.optionFFSSL[i + seen];
						optionFFSeen[i] = parent.optionFFSeen[i + seen];
					} else {
						seen = 1;
					}
				}

				parent.children = children;
				parent.optionFFSSL = optionFFSSL;
				parent.optionFFSeen = optionFFSeen;

				assert parent.children.size() == parent.optionFFSSL.length;
			}
			numTrees--;
		}
	}
	
	// endregion --- Option tree methods
	
	//endregion ================ METHODS ================
}
