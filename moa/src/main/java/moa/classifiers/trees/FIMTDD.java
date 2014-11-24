/*
 *    FIMTDD.java
 *    Copyright (C) 2014 Jožef Stefan Institute, Ljubljana, Slovenia
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author Aljaž Osojnik <aljaz.osojnik@ijs.si>
 *    @author Katie de Lange, E. Almeida, J. Gama
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

/* Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC, 
 *
 * Contact: jgama@fep.up.pt
 */

package moa.classifiers.trees;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import moa.AbstractMOAObject;
import moa.classifiers.Regressor;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.AbstractClassifier;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.options.ClassOption;

/*
 * Implementation of FIMTDD, regression and model trees for data streams.
 */

public class FIMTDD extends AbstractClassifier implements Regressor {

	private static final long serialVersionUID = 1L;

	protected Node treeRoot;

	private int leafNodeCount = 0;
	private int splitNodeCount = 0;

	private double examplesSeen = 0.0;
	private double sumOfValues = 0.0;
	private double sumOfSquares = 0.0;

	private DoubleVector sumOfAttrValues = new DoubleVector();
	private DoubleVector sumOfAttrSquares = new DoubleVector();

	public int maxID = 0;

	//region ================ OPTIONS ================

	public ClassOption splitCriterionOption = new ClassOption(
			"splitCriterion",
			's',
			"Split criterion to use.",
			SplitCriterion.class,
			"moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion");

	public IntOption gracePeriodOption = new IntOption(
			"gracePeriod",
			'g',
			"The number of instances a leaf should observe between split attempts.",
			200, 0, Integer.MAX_VALUE);

	public FloatOption splitConfidenceOption = new FloatOption(
			"splitConfidence",
			'c',
			"The allowable error in split decision, values closer to 0 will take longer to decide.",
			0.0000001, 0.0, 1.0);

	public FloatOption tieThresholdOption = new FloatOption(
			"tieThreshold",
			't',
			"Threshold below which a split will be forced to break ties.",
			0.05, 0.0, 1.0);

	public FloatOption PageHinckleyAlphaOption = new FloatOption(
			"PageHinckleyAlpha",
			'a',
			"The alpha value to use in the Page Hinckley change detection tests.",
			0.005, 0.0, 1.0);

	public IntOption PageHinckleyThresholdOption = new IntOption(
			"PageHinckleyThreshold",
			'h',
			"The threshold value to be used in the Page Hinckley change detection tests.",
			50, 0, Integer.MAX_VALUE);

	public FloatOption alternateTreeFadingFactorOption = new FloatOption(
			"alternateTreeFadingFactor",
			'f',
			"The fading factor to use when deciding if an alternate tree should replace an original.",
			0.995, 0.0, 1.0);

	public IntOption alternateTreeTMinOption = new IntOption(
			"alternateTreeTMin",
			'y',
			"The Tmin value to use when deciding if an alternate tree should replace an original.",
			150, 0, Integer.MAX_VALUE);

	public IntOption alternateTreeTimeOption = new IntOption(
			"alternateTreeTime",
			'u',
			"The 'time' (in terms of number of instances) value to use when deciding if an alternate tree should be discarded.",
			1500, 0, Integer.MAX_VALUE);

	public FlagOption regressionTreeOption = new FlagOption(
			"regressionTree",
			'r',
			"Build a regression tree instead of a model tree.");

	public FloatOption learningRatioOption = new FloatOption(
			"learningRatio", 
			'l',
			"Learning ratio to use for training the Perceptrons in the leaves.",
			0.02);

	public FloatOption learningRateDecayFactorOption = new FloatOption(
			"learningRatioDecayFactor",
			'd',
			"Learning rate decay factor (not used when learning rate is constant).",
			0.001);

	public FlagOption learningRatioConstOption = new FlagOption(
			"learningRatioConst",
			'o',
			"Keep learning rate constant instead of decaying (if kept constant learning ratio is suggested to be 0.001).");

	//endregion ================ OPTIONS ================

	//region ================ CLASSES ================

	public abstract static class Node extends AbstractMOAObject {

		private static final long serialVersionUID = 1L;

		protected double weightSeenAtLastSplitEvaluation;

		public int ID;

		// The parent of this particular node
		protected SplitNode parent;

		protected Node alternateTree;
		protected Node originalNode;

		protected AutoExpandVector<FIMTDDNumericAttributeClassObserver> attributeObservers = new AutoExpandVector<FIMTDDNumericAttributeClassObserver>();

		// The error values for the Page Hinckley test
		// PHmT = the cumulative sum of the errors
		// PHMT = the minimum error value seen so far
		protected boolean changeDetection = true;
		protected double PHsum = 0;
		protected double PHmin = Double.MAX_VALUE;

		// The statistics for this node:
		// Number of instances that have reached it
		protected double examplesSeen;
		// Sum of y values
		protected double sumOfValues;
		// Sum of squared y values
		protected double sumOfSquares;
		// Sum of absolute errors
		protected double sumOfAbsErrors; // Needed for PH tracking of mean error

		public Node() {
		}

		public void copyStatistics(Node node) {
			examplesSeen = node.examplesSeen;
			sumOfValues = node.sumOfValues;
			sumOfSquares = node.sumOfSquares;
			sumOfAbsErrors = node.sumOfAbsErrors;
			// sumOfSquaredErrors = node.sumOfSquaredErrors;
		}

		public int calcByteSize() {
			return (int) (SizeOf.sizeOf(this)) + (int) (SizeOf.fullSizeOf(this.attributeObservers));
		}

		public int calcByteSizeIncludingSubtree() {
			return calcByteSize();
		}

		public boolean isLeaf() {
			return true;
		}

		public double examplesSeen() {
			return examplesSeen;
		}

		/**
		 * Set the parent node
		 */
		public void setParent(SplitNode parent) {
			this.parent = parent;    
		}

		/**
		 * Return the parent node
		 */
		public SplitNode getParent() {
			return parent;
		}

		public void disableChangeDetection() {
			changeDetection = false;
		}

		public void restartChangeDetection() {
			changeDetection = true;
			PHsum = 0;
			PHmin = Integer.MAX_VALUE;
		}


		/**
		 * Check to see if the tree needs updating
		 */
		public boolean PageHinckleyTest(double error, double threshold) {    
			// Update the cumulative mT sum
			PHsum += error;

			// Update the minimum mT value if the new mT is
			// smaller than the current minimum
			if(PHsum < PHmin) {
				PHmin = PHsum;
			}
			// Return true if the cumulative value - the current minimum is
			// greater than the current threshold (in which case we should adapt)
			return PHsum - PHmin > threshold;
		}

		public void getDescription(StringBuilder sb, int i) {}

		public int countLeaves() {
			return 1;
		}

		public void describeSubtree(FIMTDD tree, StringBuilder out, int indent) {
			StringUtils.appendIndented(out, indent, "Leaf");
		}

		public double getPrediction(Instance inst, FIMTDD tree) {
			return 0;
		}

	}

	public static class LeafNode extends Node {

		private static final long serialVersionUID = 1L;

		// Perceptron model that carries out the actual learning in each node
		public FIMTDDPerceptron learningModel;

		protected double examplesSeenAtLastSplitEvaluation = 0;

		public double examplesSeenAtLastSplitEvaluation() {
			return examplesSeenAtLastSplitEvaluation;
		}

		public void setExamplesSeenAtLastSplitEvaluation(double seen) {
			examplesSeenAtLastSplitEvaluation = seen;
		}

		/**
		 * Create a new LeafNode
		 */
		public LeafNode(FIMTDD tree) {
			ID = tree.maxID; 
			if (tree.buildingModelTree()) {
				learningModel = tree.newLeafModel();
			}
			examplesSeen = 0;
			sumOfValues = 0;
			sumOfSquares = 0;
			sumOfAbsErrors = 0;
			// sumOfSquaredErrors = 0;
		}

		/**
		 * Method to learn from an instance that passes the new instance to the perceptron learner,
		 * and also prevents the class value from being truncated to an int when it is passed to the
		 * attribute observer
		 */
		public void learnFromInstance(Instance inst, FIMTDD tree, boolean growthAllowed) {
			// Update the statistics for this node
			// number of instances passing through the node
			examplesSeen += 1;

			// sum of y values
			sumOfValues += inst.classValue();

			// sum of squared y values
			sumOfSquares += inst.classValue() * inst.classValue();

			// sum of absolute errors
			sumOfAbsErrors += Math.abs(tree.normalizeTargetValue(inst.classValue()) - tree.normalizeTargetValue(getPrediction(inst, tree)));

			if (tree.buildingModelTree()) learningModel.updatePerceptron(inst, tree);;

			for (int i = 0; i < inst.numAttributes() - 1; i++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
				FIMTDDNumericAttributeClassObserver obs = attributeObservers.get(i);
				if (obs == null) {
					// At this stage all nominal attributes are ignored
					if (inst.attribute(instAttIndex).isNumeric()) {
						obs = tree.newNumericClassObserver();
						this.attributeObservers.set(i, obs);
					}
				}
				if (obs != null) {
					obs.observeAttributeClass(inst.value(instAttIndex),inst.classValue(), inst.weight());
				}
			}

			if (growthAllowed) {
				checkForSplit(tree);
			}
		}

		/**
		 * Return the best split suggestions for this node using the given split criteria
		 */
		public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion, FIMTDD tree) {

			List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();

			// Set the nodeStatistics up as the preSplitDistribution, rather than the observedClassDistribution
			double[] nodeSplitDist = new double[] {examplesSeen, sumOfValues, sumOfSquares};

			for (int i = 0; i < this.attributeObservers.size(); i++) {
				FIMTDDNumericAttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs != null) {

					// AT THIS STAGE NON-NUMERIC ATTRIBUTES ARE IGNORED
					AttributeSplitSuggestion bestSuggestion = null;
					if (obs instanceof FIMTDDNumericAttributeClassObserver) {
						bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, nodeSplitDist, i, false);
					}

					if (bestSuggestion != null) {
						bestSuggestions.add(bestSuggestion);
					}
				}
			}
			return bestSuggestions.toArray(new AttributeSplitSuggestion[bestSuggestions.size()]);
		}

		/**
		 * Retrieve the class votes using the perceptron learner
		 */
		public double getPredictionModel(Instance inst, FIMTDD tree) {
			return learningModel.prediction(inst, tree);
		}

		public double getPredictionTargetMean(Instance inst, FIMTDD tree) {
			return (examplesSeen > 0.0) ? sumOfValues / examplesSeen : 0.0;
		}

		public double getPrediction(Instance inst, FIMTDD tree) {
			return (tree.buildingModelTree()) ? getPredictionModel(inst, tree) : getPredictionTargetMean(inst, tree);
		}

		public double[] getClassVotes(Instance inst, FIMTDD tree) {
			return new double[] {getPrediction(inst, tree)};
		}

		public void checkForSplit(FIMTDD tree) {
			// If it has seen Nmin examples since it was last tested for splitting, attempt a split of this node
			if (examplesSeen - examplesSeenAtLastSplitEvaluation >= tree.gracePeriodOption.getValue()) {
				int index = (parent != null) ? parent.getChildIndex(this) : 0;
				tree.attemptToSplit(this, parent, index);

				// Take note of how many instances were seen when this split evaluation was made, so we know when to perform the next split evaluation
				examplesSeenAtLastSplitEvaluation = examplesSeen;
			}
		}

		public void describeSubtree(FIMTDD tree, StringBuilder out, int indent) {
			StringUtils.appendIndented(out, indent, "Leaf ");
			if (tree.buildingModelTree()) {
				learningModel.getModelDescription(out, 0);
			} else {
				out.append(tree.getClassNameString() + " = " + String.format("%.4f", (sumOfValues / examplesSeen)));
				StringUtils.appendNewline(out);
			}
		}

	}

	public static class SplitNode extends Node {

		private static final long serialVersionUID = 1L;

		protected InstanceConditionalTest splitTest;

		protected AutoExpandVector<Node> children = new AutoExpandVector<Node>();

		// Keep track of the statistics for loss error calculations
		protected double lossExamplesSeen;
		protected double lossFadedSumOriginal;
		protected double lossFadedSumAlternate;
		protected double lossNumQiTests;
		protected double lossSumQi;
		protected double previousWeight = 0;

		/**
		 * Create a new SplitNode
		 * @param tree 
		 */
		public SplitNode(InstanceConditionalTest splitTest, FIMTDD tree) {
			this.splitTest = splitTest;
			ID = tree.maxID;
		}

		public void disableChangeDetection() {
			changeDetection = false;
			for (Node child : children) {
				child.disableChangeDetection();
			}
		}

		public void restartChangeDetection() {
			if (this.alternateTree == null) {
				changeDetection = true;
				PHsum = 0;
				PHmin = Integer.MAX_VALUE;
				for (Node child : children)
					child.restartChangeDetection();
			}
		}
		
		protected void setChild(int i, Node child) {
			children.set(i, child);
		}

		public int instanceChildIndex(Instance inst) {
			return this.splitTest.branchForInstance(inst);
		}

		public Node getChild(int i) {
			return children.get(i);
		}

		public int getChildIndex(Node child) {
			return children.indexOf(child);
		}

		public void initializeAlternateTree(FIMTDD tree) {
			// Start a new alternate tree, beginning with a learning node
			alternateTree = tree.newLeafNode();
			alternateTree.originalNode = this;

			// Set up the blank statistics
			// Number of instances reaching this node since the alternate tree was started
			lossExamplesSeen = 0;
			// Faded squared error (original tree)
			lossFadedSumOriginal = 0;
			// Faded squared error (alternate tree)
			lossFadedSumAlternate = 0;
			// Number of evaluations of alternate tree
			lossNumQiTests = 0;
			// Sum of Qi values
			lossSumQi = 0;
			// Number of examples at last test
			previousWeight = 0;

			// Disable the change detection mechanism bellow this node
			disableChangeDetection();
		}


		public int countLeaves() {
			Stack<Node> stack = new Stack<Node>();
			stack.addAll(children);
			int ret = 0;
			while (!stack.isEmpty()) {
				Node node = stack.pop();
				if (node instanceof LeafNode) {
					ret++;
				} else if (node instanceof SplitNode) {
					stack.addAll(((SplitNode) node).children);
				}
			}
			return ret;
		}

		@Override
		public void describeSubtree(FIMTDD tree, StringBuilder out, int indent) {
			for (int branch = 0; branch < children.size(); branch++) {
				Node child = getChild(branch);
				if (child != null) {
					StringUtils.appendIndented(out, indent, "if ");
					out.append(this.splitTest.describeConditionForBranch(branch,
							tree.getModelContext()));
					out.append(": ");
					StringUtils.appendNewline(out);
					child.describeSubtree(tree, out, indent + 2);
				}
			}
		}

		public double getPrediction(Instance inst, FIMTDD tree) {
			return children.get(splitTest.branchForInstance(inst)).getPrediction(inst, tree);
		}
	}

	public class FIMTDDPerceptron {

		// The Perception weights 
		protected DoubleVector weightAttribute = new DoubleVector(); 

		protected double sumOfValues;
		protected double sumOfSquares;

		// The number of instances contributing to this model
		protected int instancesSeen = 0;

		// If the model should be reset or not
		protected boolean reset;

		public String getPurposeString() {
			return "A perceptron regressor as specified by Ikonomovska et al. used for FIMTDD";
		}

		public FIMTDDPerceptron(FIMTDDPerceptron original) {
			weightAttribute = (DoubleVector) original.weightAttribute.copy();
			reset = false;
		}

		public FIMTDDPerceptron() {
			reset = true;
		}


		public DoubleVector getWeights() {
			return weightAttribute;
		}

		/**
		 * Update the model using the provided instance
		 */
		public void updatePerceptron(Instance inst, FIMTDD tree) {

			// Initialize perceptron if necessary   
			if (this.reset == true) {
				Random r = new Random();
				reset = false;
				weightAttribute = new DoubleVector();
				instancesSeen = 0;
				for (int j = 0; j < inst.numAttributes(); j++) { // The last index corresponds to the constant b
					weightAttribute.setValue(j, 2 * r.nextDouble() - 1);
				}
			}

			// Update attribute statistics
			instancesSeen++;

			// Update weights
			double learningRatio = 0.0;
			if (tree.learningRatioConstOption.isSet()) {
				learningRatio = learningRatioOption.getValue();
			} else {
				learningRatio = learningRatioOption.getValue() / (1 + instancesSeen * tree.learningRateDecayFactorOption.getValue());
			}

			sumOfValues += inst.classValue();
			sumOfSquares += inst.classValue() * inst.classValue();

			updateWeights(inst, learningRatio, tree);
		}

		public void updateWeights(Instance inst, double learningRatio, FIMTDD tree) {
			// Compute the normalized instance and the delta
			DoubleVector normalizedInstance = normalizedInstance(inst, tree); 
			double normalizedPrediction = prediction(normalizedInstance);
			double normalizedValue = tree.normalizeTargetValue(inst.classValue());
			double delta = normalizedValue - normalizedPrediction;
			normalizedInstance.scaleValues(delta * learningRatio);
			
			weightAttribute.addValues(normalizedInstance);
		}

		public DoubleVector normalizedInstance(Instance inst, FIMTDD tree) {
			// Normalize Instance
			DoubleVector normalizedInstance = new DoubleVector();
			for (int j = 0; j < inst.numAttributes() - 1; j++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(j, inst);
				double mean = tree.sumOfAttrValues.getValue(j) / tree.examplesSeen;
				double sd = computeSD(tree.sumOfAttrSquares.getValue(j), tree.sumOfAttrValues.getValue(j), tree.examplesSeen);
				if (inst.attribute(instAttIndex).isNumeric() && tree.examplesSeen > 1) 
					normalizedInstance.setValue(j, (inst.value(instAttIndex) - mean) / (3 * sd));
				else
					normalizedInstance.setValue(j, 0);
			}
			normalizedInstance.setValue(inst.numAttributes() - 1, 1.0); // Value to be multiplied with the constant factor
			return normalizedInstance;
		}

		/**
		 * Output the prediction made by this perceptron on the given instance
		 */
		public double prediction(DoubleVector instanceValues)
		{
			return scalarProduct(weightAttribute, instanceValues);
		}

		private double prediction(Instance inst, FIMTDD tree) {
			DoubleVector normalizedInstance = normalizedInstance(inst, tree);
			double normalizedPrediction = prediction(normalizedInstance);
			return denormalizePrediction(normalizedPrediction, tree);
		}

		private double denormalizePrediction(double normalizedPrediction, FIMTDD tree) {
			double mean = tree.sumOfValues / tree.examplesSeen;
			double sd = computeSD(tree.sumOfSquares, tree.sumOfValues, tree.examplesSeen);
			return normalizedPrediction * sd * 3 + mean;
		}
		
		public void getModelDescription(StringBuilder out, int indent) {
	        StringUtils.appendIndented(out, indent, getClassNameString() + " =");
			if (getModelContext() != null) {
				for (int j = 0; j < getModelContext().numAttributes() - 1; j++) {
					if (getModelContext().attribute(j).isNumeric()) {
						out.append((j == 0 || weightAttribute.getValue(j) < 0) ? " " : " + ");
		                out.append(String.format("%.4f", weightAttribute.getValue(j)));
		                out.append(" * ");
		                out.append(getAttributeNameString(j));
		            }
		        }
				out.append(" + " + weightAttribute.getValue((getModelContext().numAttributes() - 1)));
			}
	        StringUtils.appendNewline(out);
		}
	}


	//endregion ================ CLASSES ================

	//region ================ METHODS ================

	// Regressor methods
	public FIMTDD() {}

	public String getPurposeString() {
		return "Implementation of the FIMT-DD tree as described by Ikonomovska et al.";
	}

	public void resetLearningImpl() {
		this.treeRoot = null;
		this.leafNodeCount = 0;
		this.splitNodeCount = 0;
		this.maxID = 0;
	}

	public boolean isRandomizable() {
		return false;
	}

	public void getModelDescription(StringBuilder out, int indent) {
		if (treeRoot != null) treeRoot.describeSubtree(this, out, indent);
	}

	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{ 
				new Measurement("tree size (nodes)", this.leafNodeCount + this.splitNodeCount),
				new Measurement("tree size (leaves)", this.leafNodeCount)
		};
	}

	public int calcByteSize() {
		int size = (int) SizeOf.sizeOf(this);
		if (this.treeRoot != null) {
			size += this.treeRoot.calcByteSize();
		}
		return size;
	}

	public double[] getVotesForInstance(Instance inst) {
		if (treeRoot == null) {
			return new double[] {0};
		}

		double prediction = treeRoot.getPrediction(inst, this);

		return new double[] {prediction};
	}

	public double normalizeTargetValue(double value) {
		if (examplesSeen > 1) {
			double sd = Math.sqrt((sumOfSquares - ((sumOfValues * sumOfValues)/examplesSeen))/examplesSeen);
			double average = sumOfValues / examplesSeen;
			if (sd > 0)
				return (value - average) / (3 * sd);
			else
				return 0.0;
		}
		return 0.0;
	}

	public double getNormalizedError(Instance inst) {
		double normalPrediction = normalizeTargetValue(treeRoot.getPrediction(inst, this));
		double normalValue = normalizeTargetValue(inst.classValue());
		return Math.abs(normalValue - normalPrediction);
	}


	/**
	 * Method for updating (training) the model using a new instance
	 */
	public void trainOnInstanceImpl(Instance inst) {
		checkRoot();

		examplesSeen ++;
		sumOfValues += inst.classValue();
		sumOfSquares += inst.classValue() * inst.classValue();

		for (int i = 0; i < inst.numAttributes() - 1; i++) {
			int aIndex = modelAttIndexToInstanceAttIndex(i, inst);
			sumOfAttrValues.addToValue(aIndex, inst.value(aIndex));
			sumOfAttrSquares.addToValue(aIndex, inst.value(aIndex) * inst.value(aIndex));
		}

		processInstance(inst, treeRoot, treeRoot.getPrediction(inst, this), getNormalizedError(inst), true, false);
	}

	public void processInstance(Instance inst, Node node, double prediction, double normalError, boolean growthAllowed, boolean inAlternate) {
		Node currentNode = node;
		while (true) {
			if (currentNode instanceof LeafNode) {
				((LeafNode) currentNode).learnFromInstance(inst, this, growthAllowed);
				break;
			} else {
				currentNode.examplesSeen++;
				currentNode.sumOfAbsErrors += normalError;
				SplitNode iNode = (SplitNode) currentNode;
				if (!inAlternate && iNode.alternateTree != null) {
					boolean altTree = true;
					double lossO = Math.pow(inst.classValue() - prediction, 2);
					double lossA = Math.pow(inst.classValue() - iNode.alternateTree.getPrediction(inst, this), 2);

					iNode.lossFadedSumOriginal = lossO + alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumOriginal;
					iNode.lossFadedSumAlternate = lossA + alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumAlternate;
					iNode.lossExamplesSeen++;

					double Qi = Math.log((iNode.lossFadedSumOriginal) / (iNode.lossFadedSumAlternate));
					double previousQiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
					iNode.lossSumQi += Qi;
					iNode.lossNumQiTests += 1;
					double QiAverage = iNode.lossSumQi / iNode.lossNumQiTests;

					if (iNode.lossExamplesSeen - iNode.previousWeight >= alternateTreeTMinOption.getValue()) {
						iNode.previousWeight = iNode.lossExamplesSeen;
						if (Qi > 0) {
							SplitNode parent = currentNode.getParent();

							if (parent != null) {
								Node replacementTree = iNode.alternateTree;
								parent.setChild(parent.getChildIndex(currentNode), replacementTree);
								if (growthAllowed) replacementTree.restartChangeDetection();
							} else {
								treeRoot = iNode.alternateTree;
								treeRoot.restartChangeDetection();
							}

							currentNode = iNode.alternateTree;
							altTree = false;
						} else if (
								(QiAverage < previousQiAverage && iNode.lossExamplesSeen >= (10 * this.gracePeriodOption.getValue()))
								|| iNode.lossExamplesSeen >= alternateTreeTimeOption.getValue()
								) {
							iNode.alternateTree = null;
							if (growthAllowed) iNode.restartChangeDetection();
							altTree = false;
						}
					}

					if (altTree) {
						growthAllowed = false;
						processInstance(inst, iNode.alternateTree, prediction, normalError, true, true);
					}
				}

				if (iNode.changeDetection && !inAlternate) {
					if (iNode.PageHinckleyTest(normalError - iNode.sumOfAbsErrors / iNode.examplesSeen - PageHinckleyAlphaOption.getValue(), PageHinckleyThresholdOption.getValue())) {
						iNode.initializeAlternateTree(this);
					}
				}
				if (currentNode instanceof SplitNode) {
					currentNode = ((SplitNode) currentNode).getChild(iNode.instanceChildIndex(inst));
				} else { // if the replaced alternate tree is just a leaf node
					((LeafNode) currentNode).learnFromInstance(inst, this, growthAllowed);
					break;
				}
			}
		}
	}

	//region --- Object instatiation methods

	protected FIMTDDNumericAttributeClassObserver newNumericClassObserver() {
		return new FIMTDDNumericAttributeClassObserver();
	}

	protected SplitNode newSplitNode(InstanceConditionalTest splitTest) {
		//maxID++;
		return new SplitNode(splitTest, this);
	}

	protected LeafNode newLeafNode() {
		maxID++;
		return new LeafNode(this);
	}

	protected FIMTDDPerceptron newLeafModel() {
		return new FIMTDDPerceptron();
	}

	//endregion --- Object instatiation methods
	
	//region --- Processing methods
	protected void checkRoot() {
		if (treeRoot == null) {
			treeRoot = newLeafNode();
			leafNodeCount = 1;
		}
	}

	public static double computeHoeffdingBound(double range, double confidence, double n) {
		return Math.sqrt(( (range * range) * Math.log(1 / confidence)) / (2.0 * n));
	}

	public boolean buildingModelTree() {
		return !regressionTreeOption.isSet();
	}

	protected void attemptToSplit(LeafNode node, SplitNode parent, int parentIndex) {
		// Set the split criterion to use to the SDR split criterion as described by Ikonomovska et al. 
		SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);

		// Using this criterion, find the best split per attribute and rank the results
		AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
		Arrays.sort(bestSplitSuggestions);

		// Declare a variable to determine if any of the splits should be performed
		boolean shouldSplit = false;

		// If only one split was returned, use it
		if (bestSplitSuggestions.length < 2) {
			shouldSplit = bestSplitSuggestions.length > 0;
		} else { // Otherwise, consider which of the splits proposed may be worth trying

			// Determine the Hoeffding bound value, used to select how many instances should be used to make a test decision
			// to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
			double hoeffdingBound = computeHoeffdingBound(1, this.splitConfidenceOption.getValue(), node.examplesSeen());
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
			if ((secondBestSuggestion.merit / bestSuggestion.merit < 1 - hoeffdingBound) || (hoeffdingBound < this.tieThresholdOption.getValue())) {
				shouldSplit = true;
			}
			// If the splitting criterion was not met, initiate pruning of the E-BST structures in each attribute observer
			else {
				for (int i = 0; i < node.attributeObservers.size(); i++) {
					FIMTDDNumericAttributeClassObserver obs = node.attributeObservers.get(i);
					if (obs != null) {
						obs.removeBadSplits(splitCriterion, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound);    
					}
				}
			}
		}

		// If the splitting criterion were met, split the current node using the chosen attribute test, and
		// make two new branches leading to (empty) leaves
		if (shouldSplit) {
			AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];

			SplitNode newSplit = newSplitNode(splitDecision.splitTest);
			newSplit.copyStatistics(node);
			newSplit.changeDetection = node.changeDetection;
			newSplit.ID = node.ID;
			leafNodeCount--;
			for (int i = 0; i < splitDecision.numSplits(); i++) {
				LeafNode newChild = newLeafNode();
				if (buildingModelTree()) {
					// Copy the splitting node's perceptron to it's children
					newChild.learningModel = new FIMTDDPerceptron((FIMTDDPerceptron) node.learningModel);
					
				}
				newChild.changeDetection = node.changeDetection;
				newChild.setParent(newSplit);
				newSplit.setChild(i, newChild);
				leafNodeCount++;
			}
			if (parent == null && node.originalNode == null) {
				treeRoot = newSplit;
			} else if (parent == null && node.originalNode != null) {
				node.originalNode.alternateTree = newSplit;
			} else {
				parent.setChild(parentIndex, newSplit);
				newSplit.setParent(parent);
			}
			
			splitNodeCount++;
		}
	}
	
	public  double computeSD(double squaredVal, double val, double size) {
		if (size > 1)
			return Math.sqrt((squaredVal - ((val * val) / size)) / size);
		else
			return 0.0;
	}
	
	public double scalarProduct(DoubleVector u, DoubleVector v) {
		double ret = 0.0;
		for (int i = 0; i < Math.max(u.maxIndex(), v.maxIndex()); i++) {
			ret += u.getValue(i) * v.getValue(i);
		}
		return ret;
	}
	//endregion --- Processing methods
	
	//endregion ================ METHODS ================
}