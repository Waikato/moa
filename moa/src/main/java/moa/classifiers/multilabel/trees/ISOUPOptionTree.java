package moa.classifiers.multilabel.trees;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

import moa.classifiers.multilabel.trees.ISOUPTree.LeafNode;
import moa.classifiers.multilabel.trees.ISOUPTree.Node;
import moa.classifiers.multilabel.trees.ISOUPTree.SplitNode;
import moa.classifiers.trees.ORTO;
import moa.classifiers.trees.FIMTDD.InnerNode;
import moa.classifiers.trees.ORTO.OptionNode;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;

public class ISOUPOptionTree extends ISOUPTree {

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
		protected AutoExpandVector<DoubleVector> optionFFSSL;
		protected AutoExpandVector<DoubleVector> optionFFSeen;
		
		public OptionNode(ISOUPTree tree) {
			super(tree);
		}

		public void resetFF() {
			optionFFSSL = new AutoExpandVector<DoubleVector>();
			optionFFSeen = new AutoExpandVector<DoubleVector>();
			for (int i = 0; i < numChildren(); i++) {
				optionFFSSL.set(i, new DoubleVector());
				optionFFSeen.set(i, new DoubleVector());
				for (int j = 0; j < tree.getModelContext().numOutputAttributes(); j++) {
					optionFFSSL.get(i).setValue(j, 0.0);
					optionFFSeen.get(i).setValue(j, 0.0);
				}
			}
		}
		
		public int getNumSubtrees() {
			int num = 0;
			for (Node child : children) {
				num += child.getNumSubtrees();
			}
			return num;
		}
		
		public double[] getPrediction(MultiLabelInstance inst) {
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
			return optionFFSSL.get(childIndex).getValue(targetIndex) / optionFFSeen.get(childIndex).getValue(targetIndex); 
		}
		
		protected boolean skipInLevelCount() {
			return true;
		}
	}
	
	//endregion ================ CLASSES ================
	
	//region ================ METHODS ================
	
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
				//new Measurement("tree size (nodes)", this.leafNodeCount + this.innerNodeCount),
				//new Measurement("tree size (leaves)", this.leafNodeCount),
				new Measurement("number of option nodes", this.optionNodeCount),};
	}
	
	@Override
	public void processInstance(MultiLabelInstance inst, Node node, double[] prediction, double[] normalError, boolean growthAllowed, boolean inAlternate) {
		if (node instanceof OptionNode) {
			processInstanceOptionNode(inst, (OptionNode) node, prediction, normalError, growthAllowed, inAlternate);
		} else {
			Node currentNode = node;
			while (true) {
				if (currentNode instanceof LeafNode) {
					((LeafNode) currentNode).learnFromInstance(inst, prediction, growthAllowed);
					break;
				} else {
					currentNode.examplesSeen += inst.weight();
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
						currentNode = ((SplitNode) currentNode).getChild(((SplitNode) currentNode).instanceChildIndex(inst));
					} else if (currentNode instanceof OptionNode) {
						processInstanceOptionNode(inst, (OptionNode) currentNode, prediction, normalError, growthAllowed, inAlternate);
						break;
					}
				}
			}
		}
		
	}
	
	public void processInstanceOptionNode(MultiLabelInstance inst, OptionNode node, double[] prediction, double[] normalError, boolean growthAllowed, boolean inAlternate) {
//		if (node.changeDetection) {
//			double error = Math.abs(prediction - inst.classValue());
//			node.sumOfAbsErrors += error;
//			
//			if (((InnerNode) node).PageHinckleyTest(error - node.sumOfAbsErrors / node.examplesSeen + PageHinckleyAlphaOption.getValue(), PageHinckleyThresholdOption.getValue())) {
//				node.initializeAlternateTree();
//			}
//		}

		for (Node child : node.children) {
			int i = node.getChildIndex(child);
			double[] childPrediction = child.getPrediction(inst);
			for (int j = 0; j < getModelContext().numOutputAttributes(); j++)  {
				node.optionFFSeen.get(i).setValue(j, node.optionFFSeen.get(i).getValue(j) * optionFadingFactorOption.getValue() + 1);
				node.optionFFSSL.get(i).setValue(j, node.optionFFSSL.get(i).getValue(j) * optionFadingFactorOption.getValue() + Math.pow(childPrediction[j] - inst.valueOutputAttribute(j), 2));
			}
		}

		for (Node child : node.children) {
			processInstance(inst, child, child.getPrediction(inst), normalError, growthAllowed && node.alternateTree == null, inAlternate);
		}
	}
	
	//endregion ================ METHODS ================

}
