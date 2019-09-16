package moa.learners.predictors.trees;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.learners.predictors.core.splitcriteria.MultiLabelSplitCriterion;
import moa.learners.predictors.core.splitcriteria.WeightedICVarianceReduction;
import moa.learners.predictors.rules.core.AttributeExpansionSuggestion;

public class ISOUPOptionTree extends ISOUPTree {

	private static final long serialVersionUID = 1L;

	private int optionNodeCount = 0;

	private int numTrees = 1;

	// region ================ OPTIONS ================

	public IntOption maxTreesOption = new IntOption("maxTrees", 'm',
			"The maximum number of trees contained in the option tree.", 125, 1, Integer.MAX_VALUE);

	public IntOption maxOptionLevelOption = new IntOption("maxOptionLevel", 'x',
			"The maximal depth at which option nodes can be created.", 3, 0, Integer.MAX_VALUE);

	public FloatOption optionDecayFactorOption = new FloatOption("optionDecayFactor", 'z',
			"The option decay factor that determines how many options can be selected at a given level.", 0.9, 0.0,
			1.0);

	public MultiChoiceOption optionNodeAggregationOption = new MultiChoiceOption("optionNodeAggregation", 'p',
			"The aggregation method used to combine predictions in option nodes.",
			new String[] { "average", "bestTree" }, new String[] { "Average", "Best tree" }, 0);

	public FloatOption optionFadingFactorOption = new FloatOption("optionFadingFactor", 'q',
			"The fading factor used for comparing subtrees of an option node.", 0.9995, 0.0, 1.0);

	// endregion ================ OPTIONS ================

	// region ================ CLASSES ================

	public static class OptionNode extends InnerNode {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		protected AutoExpandVector<DoubleVector> optionFFSSL;
		protected AutoExpandVector<DoubleVector> optionFFSeen;

		public OptionNode(ISOUPTree tree) {
			super(tree);
		}

		@Override
		public long calcByteSize() {
			long size = super.calcByteSize();
			size += SizeOf.fullSizeOf(optionFFSSL) + SizeOf.fullSizeOf(optionFFSeen);
			return size;
		}

		public void resetFF() {
			optionFFSSL = new AutoExpandVector<>();
			optionFFSeen = new AutoExpandVector<>();
			for (int i = 0; i < numChildren(); i++) {
				optionFFSSL.set(i, new DoubleVector());
				optionFFSeen.set(i, new DoubleVector());
				for (int j = 0; j < tree.getModelContext().numOutputAttributes(); j++) {
					optionFFSSL.get(i).setValue(j, 0.0);
					optionFFSeen.get(i).setValue(j, 0.0);
				}
			}
		}

		@Override
		public int getNumSubtrees() {
			int num = 0;
			for (Node child : children) {
				num += child.getNumSubtrees();
			}
			return num;
		}

		@Override
		public double[] getPrediction(Instance inst) {
			double[][] predictions = new double[numChildren()][tree.getModelContext().numOutputAttributes()];
			for (int i = 0; i < numChildren(); i++) {
				predictions[i] = getChild(i).getPrediction(inst);
			}
			return aggregate(predictions);
		}

		private double[] aggregate(double[][] predictions) {
			if (((ISOUPOptionTree) tree).optionNodeAggregationOption.getChosenIndex() == 0) { // Average
				double[] sums = new double[tree.getModelContext().numOutputAttributes()];
				for (int j = 0; j < tree.getModelContext().numOutputAttributes(); j++) {
					for (int i = 0; i < numChildren(); i++) {
						sums[j] += predictions[i][j];
					}
					sums[j] = sums[j] / numChildren();
				}
				return sums;
			} else if (((ISOUPOptionTree) tree).optionNodeAggregationOption.getChosenIndex() == 1) {
				int d = directionForBestTree();
				return predictions[d];
			} else {
				return new double[] {};
			}
		}

		public int directionForBestTree() {
			int d = 0;
			double min = Double.MAX_VALUE;
			for (int i = 0; i < numChildren(); i++) {
				double avg = 0.0;
				for (int j = 0; j < tree.getModelContext().numOutputAttributes(); j++) {
					avg += getFFRatio(i, j);
				}
				avg /= tree.getModelContext().numOutputAttributes();
				if (avg < min) {
					min = avg;
					d = i;
				}
			}
			return d;
		}

		public double getFFRatio(int childIndex, int targetIndex) {
			return optionFFSSL.get(childIndex).getValue(targetIndex)
					/ optionFFSeen.get(childIndex).getValue(targetIndex);
		}

		@Override
		protected boolean skipInLevelCount() {
			return true;
		}

		@Override
		public void describeSubtree(StringBuilder out, int indent) {
			for (int branch = 0; branch < children.size(); branch++) {
				Node child = getChild(branch);
				if (child != null) {
					StringUtils.appendIndented(out, indent, "option");
					out.append(branch);
					StringUtils.appendNewline(out);
					child.describeSubtree(out, indent + 2);
				}
			}
		}
	}

	// endregion ================ CLASSES ================

	// region ================ METHODS ================

	@Override
	public String getPurposeString() {
		return "Implementation of the iSOUPOptionTree";
	}

	@Override
	public void resetLearningImpl() {
		super.resetLearningImpl();
		this.optionNodeCount = 0;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[] { new Measurement("number of subtrees", this.numTrees),
				// new Measurement("tree size (nodes)", this.leafNodeCount +
				// this.innerNodeCount),
				// new Measurement("tree size (leaves)", this.leafNodeCount),
				new Measurement("number of option nodes", this.optionNodeCount), };
	}

	// region --- Object instantiation methods

	protected OptionNode newOptionNode() {
		maxID++;
		return new OptionNode(this);
	}

	// endregion --- Object instantiation methods

	@Override
	public void processInstance(Instance inst, Node node, double[] prediction, double[] normalError,
			boolean growthAllowed, boolean inAlternate) {
		if (node instanceof OptionNode) {
			processInstanceOptionNode(inst, (OptionNode) node, prediction, normalError, growthAllowed, inAlternate);
		} else {
			Node currentNode = node;
			while (true) {
				if (currentNode instanceof LeafNode) {
					((LeafNode) currentNode).learnFromInstance(inst, prediction, growthAllowed);
					break;
				} else {
					// currentNode.examplesSeen += inst.weight();
//					if (!inAlternate && iNode.alternateTree != null) {
//						boolean altTree = true;
//						double lossO = Math.pow(inst.classValue() - prediction, 2);
//						double lossA = Math.pow(inst.classValue() - currentNode.alternateTree.getPrediction(inst), 2);
//
//						iNode.lossFadedSumOriginal = lossO + alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumOriginal;
//						iNode.lossFadedSumAlternate = lossA + alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumAlternate;
//						iNode.lossExamplesSeen++;
//
//						double Qi = Math.log((iNode.lossFadedSumOriginal) / (iNode.lossFadedSumAlternate));
//						double previousQiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
//						iNode.lossSumQi += Qi;
//						iNode.lossNumQiTests += 1;
//						double QiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
//
//						if (iNode.lossExamplesSeen - iNode.previousWeight >= alternateTreeTMinOption.getValue()) {
//							iNode.previousWeight = iNode.lossExamplesSeen;
//							if (Qi > 0) {
//								// Switch the subtrees
//								Node parent = currentNode.parent;
//
//								if (parent != null) {
//									Node replacementTree = iNode.alternateTree;
//									parent.setChild(parent.getChildIndex(iNode), replacementTree);
//									if (growthAllowed) replacementTree.restartChangeDetection();
//								} else {
//									treeRoot = iNode.alternateTree;
//									treeRoot.restartChangeDetection();
//								}
//								optionNodeCount += currentNode.alternateTree.getNumSubtrees() - currentNode.getNumSubtrees();
//								removeExcessTrees();
//
//								currentNode = iNode.alternateTree;
//								currentNode.originalNode = null;
//								altTree = false;
//							} else if (QiAverage < previousQiAverage && iNode.lossExamplesSeen >= (10 * alternateTreeTMinOption.getValue()) || iNode.lossExamplesSeen >= alternateTreeTimeOption.getValue()) {
//								// Remove the alternate tree
//								iNode.alternateTree = null;
//								if (growthAllowed) iNode.restartChangeDetection();
//								altTree = false;
//							}
//						}
//						if (altTree) {
//							growthAllowed = false; // this is the growth of the original tree
//							processInstance(inst, currentNode.alternateTree, prediction, normalError, true, true); // growth is allowed in the alt tree
//						} else if (currentNode instanceof OptionNode) {
//							// this happens when an option node is switched into the tree
//							for (Node child : ((OptionNode) currentNode).children) {
//								processInstance(inst, child, child.getPrediction(inst), normalError, growthAllowed, inAlternate);
//							}
//							break;
//						}
//					}

//					if (iNode.changeDetection && !inAlternate) {
//						if (iNode.PageHinckleyTest(normalError - iNode.sumOfAbsErrors / iNode.examplesSeen - PageHinckleyAlphaOption.getValue(), PageHinckleyThresholdOption.getValue())) {
//							iNode.initializeAlternateTree();
//						}
//					}
					if (currentNode instanceof SplitNode) {
						currentNode = ((SplitNode) currentNode)
								.getChild(((SplitNode) currentNode).instanceChildIndex(inst));
					} else if (currentNode instanceof OptionNode) {
						processInstanceOptionNode(inst, (OptionNode) currentNode, prediction, normalError,
								growthAllowed, inAlternate);
						break;
					}
				}
			}
		}

	}

	public void processInstanceOptionNode(Instance inst, OptionNode node, double[] prediction, double[] normalError,
			boolean growthAllowed, boolean inAlternate) {
		// if (node.changeDetection) {
		// double error = Math.abs(prediction - inst.classValue());
		// node.sumOfAbsErrors += error;
		//
		// if (((InnerNode) node).PageHinckleyTest(error - node.sumOfAbsErrors /
		// node.examplesSeen + PageHinckleyAlphaOption.getValue(),
		// PageHinckleyThresholdOption.getValue())) {
		// node.initializeAlternateTree();
		// }
		// }

		double[][] childPredictions = new double[node.numChildren()][];
		for (Node child : node.children) {
			int i = node.getChildIndex(child);
			childPredictions[i] = child.getPrediction(inst);
			for (int j = 0; j < getModelContext().numOutputAttributes(); j++) {
				node.optionFFSeen.get(i).setValue(j,
						node.optionFFSeen.get(i).getValue(j) * optionFadingFactorOption.getValue() + 1);
				node.optionFFSSL.get(i).setValue(j,
						node.optionFFSSL.get(i).getValue(j) * optionFadingFactorOption.getValue()
								+ (childPredictions[i][j] - inst.valueOutputAttribute(j))
										* (childPredictions[i][j] - inst.valueOutputAttribute(j)));
			}
			processInstance(inst, child, childPredictions[node.getChildIndex(child)], normalError,
					growthAllowed && node.alternateTree == null, inAlternate);
		}
	}

	// endregion ================ METHODS ================

	@Override
	protected void attemptToSplit(LeafNode node, InnerNode parent, int parentIndex) {
		// System.out.println(examplesSeen);
		// Set the split criterion to use to the SDR split criterion as described by
		// Ikonomovska et al.
		MultiLabelSplitCriterion splitCriterion = new WeightedICVarianceReduction(targetWeights);

		// Using this criterion, find the best split per attribute and rank the results
		AttributeExpansionSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion); // TODO
																											// update
																											// with
																											// split
																											// criterion
																											// option
		List<AttributeExpansionSuggestion> acceptedSplits = new LinkedList<>();

		// Declare a variable to determine the number of splits to be performed
		int numSplits = 0;

		// If only one split was returned, use it
		if (bestSplitSuggestions.length == 1) {
			numSplits = 1;
			acceptedSplits.add(bestSplitSuggestions[0]);
		} else if (bestSplitSuggestions.length > 1) { // Otherwise, consider which of the splits proposed may be worth
														// trying
			Arrays.sort(bestSplitSuggestions);
			// Determine the Hoeffding bound value, used to select how many instances should
			// be used to make a test decision
			// to feel reasonably confident that the test chosen by this sample is the same
			// as what would be chosen using infinite examples
			double numExamples = node.examplesSeen.getValue(node.examplesSeen.maxIndex()); // Use the max index (TODO
																							// for partially labeled)
			double hoeffdingBound = computeHoeffdingBound(1, splitConfidenceOption.getValue(), numExamples);
			// Determine the top two ranked splitting suggestions
			AttributeExpansionSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			AttributeExpansionSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];

			// If the upper bound of the sample mean for the ratio of SDR(best suggestion)
			// to SDR(second best suggestion),
			// as determined using the Hoeffding bound, is less than 1, then the true mean
			// is also less than 1, and thus at this
			// particular moment of observation the bestSuggestion is indeed the best split
			// option with confidence 1-delta, and
			// splitting should occur.
			// Alternatively, if two or more splits are very similar or identical in terms
			// of their splits, then a threshold limit
			// (default 0.05) is applied to the Hoeffding bound; if the Hoeffding bound is
			// smaller than this limit then the two
			// competing attributes are equally good, and the split will be made on the one
			// with the higher SDR value.
			// System.out.print(hoeffdingBound);
			// System.out.print(" ");
			/// System.out.println(secondBestSuggestion.merit / bestSuggestion.merit);
			if (secondBestSuggestion.merit / bestSuggestion.merit < 1 - hoeffdingBound) {
				numSplits = 1;
				acceptedSplits.add(bestSuggestion);
			} else if (numTrees < maxTreesOption.getValue() && node.getLevel() <= maxOptionLevelOption.getValue()) {
				for (int i = 0; i < bestSplitSuggestions.length; i++) {
					AttributeExpansionSuggestion suggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1 - i];
					if (suggestion.merit / bestSuggestion.merit >= 1 - hoeffdingBound) {
						numSplits++;
						acceptedSplits.add(suggestion);
					} else {
						break;
					}
				}

			} else if (hoeffdingBound < this.tieThresholdOption.getValue()) {
				numSplits = 1;
				acceptedSplits.add(bestSuggestion);
			}
//			else {
//				// If the splitting criterion was not met, initiate pruning of the E-BST structures in each attribute observer
//				// TODO pruning is currently disabled
////				for (int i = 0; i < node.attributeObservers.size(); i++) {
////					AttributeStatisticsObserver obs = node.attributeObservers.get(i);
////					if (obs != null) {
////						if (getModelContext().attribute(i).isNumeric());
////						//TODO obs.removeBadSplits(null, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound, getModelContext().numOutputAttributes());
////						if (getModelContext().attribute(i).isNominal());
////						// TODO nominal class observers
////					}
////				}
//			}
		}

		// System.out.println("Found " + numSplits + " candidates.");

		// If the splitting criterion were met, split the current node using the chosen
		// attribute test, and
		// make two new branches leading to (empty) leaves
		if (numSplits > 0) {
			double optionFactor = numSplits * Math.pow(optionDecayFactorOption.getValue(), node.getLevel());
			log(Integer.toString(node.ID) + ',' + this.examplesSeen.toString());

			if (numSplits == 1) {
				AttributeExpansionSuggestion splitDecision = acceptedSplits.get(0);
				SplitNode newSplit = newSplitNode(splitDecision.getPredicate());
				newSplit.ID = node.ID;

				for (int i = 0; i < 2 /* TODO Hardcoded for Predicate class */; i++) {
					LeafNode newChild = newLeafNode();
					newChild.setParent(newSplit);
					newSplit.setChild(i, newChild);
				}
				// leafNodeCount--;
				// innerNodeCount++;
				// leafNodeCount += splitDecision.numSplits();
				if (parent == null && node.originalNode == null) {
					treeRoot = newSplit;
				} else if (parent == null && node.originalNode != null) {
					node.originalNode.alternateTree = newSplit;
				} else {
					parent.setChild(parentIndex, newSplit);
					newSplit.setParent(parent);
				}
			} else if (optionFactor >= 2.0 || maxTreesOption.getValue() - numTrees > 1) {

				OptionNode optionNode = newOptionNode();
				optionNode.ID = node.ID;

				// leafNodeCount--;
				int j = 0;

				for (AttributeExpansionSuggestion splitDecision : acceptedSplits) {
					if (j > optionFactor || maxTreesOption.getValue() - numTrees <= 0 || j > 4) {
						break;
					}
					SplitNode newSplit = newSplitNode(splitDecision.getPredicate());
					for (int i = 0; i < 2 /* TODO Hardcoded for Predicate class */; i++) {
						LeafNode newChild = newLeafNode();
						newChild.setParent(newSplit);
						newSplit.setChild(i, newChild);
					}

					// leafNodeCount += splitDecision.numSplits();
					// innerNodeCount++;
					numTrees++;

					newSplit.setParent(optionNode);
					optionNode.setChild(j, newSplit);
					j++;
				}

				// innerNodeCount++;
				optionNodeCount++;

				if (parent == null) {
					treeRoot = optionNode;
				} else {
					parent.setChild(parentIndex, optionNode);
					optionNode.setParent(parent);
				}

				optionNode.resetFF();
			}
		}
		// System.out.println("Splits finished...");
	}

	// region --- Option tree methods
	protected Node findWorstOption() {
		Stack<Node> stack = new Stack<>();
		stack.add(treeRoot);

		double ratio = Double.MIN_VALUE;
		Node out = null;

		while (!stack.empty()) {
			Node node = stack.pop();
			if (node.getParent() instanceof OptionNode) {
				OptionNode myParent = (OptionNode) node.getParent();
				int nodeIndex = myParent.getChildIndex(node);
				DoubleVector nodeRatios = new DoubleVector();
				for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
					nodeRatios.setValue(i, myParent.getFFRatio(nodeIndex, i));
				}
				double nodeRatio = nodeRatios.sumOfValues() / nodeRatios.numValues();

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
				AutoExpandVector<Node> children = new AutoExpandVector<>();
				AutoExpandVector<DoubleVector> optionFFSSL = new AutoExpandVector<>();
				AutoExpandVector<DoubleVector> optionFFSeen = new AutoExpandVector<>();

				int seen = 0;

				for (int i = 0; i < parent.children.size() - 1; i++) {
					if (parent.getChild(i) != option) {
						children.add(parent.getChild(i));
						optionFFSSL.set(i, parent.optionFFSSL.get(i + seen));
						optionFFSeen.set(i, parent.optionFFSeen.get(i + seen));
					} else {
						seen = 1;
					}
				}

				parent.children = children;
				parent.optionFFSSL = optionFFSSL;
				parent.optionFFSeen = optionFFSeen;

				assert parent.children.size() == parent.optionFFSSL.size();
			}
			numTrees--;
		}
	}

}