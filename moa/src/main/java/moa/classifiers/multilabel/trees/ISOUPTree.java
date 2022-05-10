/*
 *    ISOUPTree.java
 *    Copyright (C) 2014 Jožef Stefan Institute, Ljubljana, Slovenia
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

/* Project MAESTRA (Learning from Massive, Incompletely annotated, and Structured Data)
 *
 * Contact: saso.dzeroski@ijs.si
 */

package moa.classifiers.multilabel.trees;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.*;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
//import moa.classifiers.AbstractMultiTargetRegressor;
import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
//import moa.classifiers.SemiSupervisedMultiTargetRegressor;
import moa.classifiers.MultiTargetLearnerSemiSupervised;
import moa.classifiers.rules.multilabel.attributeclassobservers.AttributeStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.MultiLabelBSTree;
import moa.classifiers.rules.multilabel.attributeclassobservers.MultiLabelBSTreePCT; //new
import moa.classifiers.rules.multilabel.attributeclassobservers.MultiLabelNominalAttributeObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NominalStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NumericStatisticsObserver;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.multilabel.core.splitcriteria.WeightedICVarianceReduction; //new
import moa.classifiers.multilabel.core.splitcriteria.PCTWeightedICVarianceReduction; //new
import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.core.Predicate;



/**
 * iSOUPTree class for structured output prediction.
 *
 * @author Aljaž Osojnik (aljaz.osojnik@ijs.si)
 * @version $Revision: 1 $
 */
//public class ISOUPTree extends AbstractMultiTargetRegressor implements MultiTargetRegressor, SemiSupervisedMultiTargetRegressor {
public class ISOUPTree extends AbstractMultiLabelLearner implements MultiTargetRegressor, MultiTargetLearnerSemiSupervised {

	private static final long serialVersionUID = 1L;

	public Node treeRoot;

	protected double learningWeight = 0.0;

	public DoubleVector examplesSeen = new DoubleVector();
	public DoubleVector sumOfValues = new DoubleVector();
	public DoubleVector sumOfSquares = new DoubleVector();

	public DoubleVector weightOfInputs = new DoubleVector();
	public DoubleVector sumOfAttrValues = new DoubleVector();
	public DoubleVector sumOfAttrSquares = new DoubleVector();

	private int numInputAttributes;
	private int numOutputAttributes;

	public int maxID = 0;

	private BufferedWriter writer;
	public DoubleVector targetWeights;

	// region ================ OPTIONS ================

	public IntOption gracePeriodOption = new IntOption("gracePeriod", 'g',
			"The number of instances a leaf should observe between split attempts.", 200, 0, Integer.MAX_VALUE);

	public FloatOption splitConfidenceOption = new FloatOption("splitConfidence", 'c',
			"The allowable error in split decision, values closer to 0 will take longer to decide.", 0.0000001, 0.0,
			1.0);

	public FloatOption tieThresholdOption = new FloatOption("tieThreshold", 't',
			"Threshold below which a split will be forced to break ties.", 0.05, 0.0, 1.0);

	public FloatOption PageHinckleyAlphaOption = new FloatOption("PageHinckleyAlpha", 'a',
			"The alpha value to use in the Page Hinckley change detection tests.", 0.005, 0.0, 1.0);

	public IntOption PageHinckleyThresholdOption = new IntOption("PageHinckleyThreshold", 'h',
			"The threshold value to be used in the Page Hinckley change detection tests.", 50, 0, Integer.MAX_VALUE);

	public FloatOption alternateTreeFadingFactorOption = new FloatOption("alternateTreeFadingFactor", 'f',
			"The fading factor to use when deciding if an alternate tree should replace an original.", 0.995, 0.0, 1.0);

	public IntOption alternateTreeTMinOption = new IntOption("alternateTreeTMin", 'y',
			"The Tmin value to use when deciding if an alternate tree should replace an original.", 150, 0,
			Integer.MAX_VALUE);

	public IntOption alternateTreeTimeOption = new IntOption("alternateTreeTime", 'u',
			"The 'time' (in terms of number of instances) value to use when deciding if an alternate tree should be discarded.",
			1500, 0, Integer.MAX_VALUE);

	public FlagOption regressionTreeOption = new FlagOption("regressionTree", 's',
			"Build a regression tree instead of a model tree.");

	public FloatOption learningRatioOption = new FloatOption("learningRatio", 'l',
			"Learning ratio to use for training the Perceptrons in the leaves.", 0.02);

	public FloatOption learningRateDecayFactorOption = new FloatOption("learningRatioDecayFactor", 'd',
			"Learning rate decay factor (not used when learning rate is constant).", 0.001);

	public FlagOption learningRatioConstOption = new FlagOption("learningRatioConst", 'o',
			"Keep learning rate constant instead of decaying (if kept constant learning ratio is suggested to be 0.001).");

	public FlagOption runAsPCTOption = new FlagOption("runAsPCT", 'p',
			"Run as a predictive clustering tree, i.e., use input attributes in heuristic calculation.");

	public FlagOption doNotNormalizeOption = new FlagOption("doNotNormalize", 'n', "Don't normalize.");

	public FileOption weightFile = new FileOption("targetWeightFile", 'w', "File with the weights of the targets.",
			null, null, false);

	// endregion ================ OPTIONS ================

	// region ================ CLASSES ================

	private static class InstanceWrapper extends InstanceImpl {

		public InstanceWrapper(InstanceImpl inst) {
			super(inst);
		}

		public boolean isInputMissing(int index) {
			return this.instanceData.isMissing(this.instanceHeader.getInstanceInformation().inputAttributeIndex(index));
		}

		public boolean isOutputMissing(int index) {
			return this.instanceData.isMissing(this.instanceHeader.getInstanceInformation().outputAttributeIndex(index));
		}

		public boolean missingOutputs() {
			if (this.instanceHeader.numOutputAttributes() == 1)
				return classIsMissing();
			else {
				for (int i = 0; i < this.instanceHeader.numOutputAttributes(); i++) {
					if (this.isOutputMissing(i)) {
						return true;
					}
				}
			}
			return false;
		}

	}

	public abstract static class Node implements Serializable {

		private static final long serialVersionUID = 1L;

		protected double weightSeenAtLastSplitEvaluation;

		public int ID;

		protected ISOUPTree tree;

		// The parent of this particular node
		protected InnerNode parent;

		protected Node alternateTree;
		protected Node originalNode;

		// The error values for the Page Hinckley test
		// PHmT = the cumulative sum of the errors
		// PHMT = the minimum error value seen so far
		protected boolean changeDetection = true;

		// The statistics for this node:
		// Number of instances that have reached it
		public DoubleVector examplesSeen = new DoubleVector();
		// Sum of y values
		public DoubleVector sumOfValues = new DoubleVector();
		// Sum of squared y values
		public DoubleVector sumOfSquares = new DoubleVector();

		// Sum of y values
		public DoubleVector weightOfInputs;
		// Sum of y values
		public DoubleVector sumOfInputValues;
		// Sum of squared y values
		public DoubleVector sumOfInputSquares;

		public Node(ISOUPTree tree) {
			this.tree = tree;
			this.ID = tree.maxID;
		}

		public void copyStatistics(Node node) {
			examplesSeen = (DoubleVector) node.examplesSeen.copy();
			sumOfValues = (DoubleVector) node.sumOfValues.copy();
			sumOfSquares = (DoubleVector) node.sumOfSquares.copy();
			if (tree.runAsPCTOption.isSet()) {
				weightOfInputs = (DoubleVector) node.weightOfInputs.copy();
				sumOfInputValues = (DoubleVector) node.sumOfInputValues.copy();
				sumOfInputSquares = (DoubleVector) node.sumOfInputSquares.copy();
			}
		}

		public long calcByteSize() {
			long size = SizeOf.sizeOf(this) + SizeOf.sizeOf(sumOfSquares) + SizeOf.sizeOf(sumOfValues)
					+ SizeOf.sizeOf(examplesSeen);
			if (tree.runAsPCTOption.isSet()) {
				size += SizeOf.sizeOf(sumOfInputSquares) + SizeOf.sizeOf(sumOfInputValues)
						+ SizeOf.sizeOf(weightOfInputs);
			}
			return size;
		}

		/**
		 * Set the parent node
		 */
		public void setParent(InnerNode parent) {
			this.parent = parent;
		}

		/**
		 * Return the parent node
		 */
		public Node getParent() {
			return parent;
		}

		public void disableChangeDetection() {
			changeDetection = false;
		}

		public void restartChangeDetection() {
			changeDetection = true;
		}

		public void getDescription(StringBuilder sb, int i) {
		}

		public abstract double[] getPrediction(Instance inst);

		public void describeSubtree(StringBuilder out, int indent) {
			StringUtils.appendIndented(out, indent, "Leaf");
		}

		public int getLevel() {
			Node target = this;
			int level = 0;
			while (target.getParent() != null) {
				if (target.skipInLevelCount()) {
					target = target.getParent();
					continue;
				}
				level = level + 1;
				target = target.getParent();
			}
			if (target.originalNode == null) {
				return level;
			} else {
				return level + originalNode.getLevel();
			}
		}

		public void setChild(int parentBranch, Node node) {
		}

		public int getChildIndex(Node child) {
			return -1;
		}

		public int getNumSubtrees() {
			return 1;
		}

		protected boolean skipInLevelCount() {
			return false;
		}
	}

	public static class LeafNode extends Node {

		private static final long serialVersionUID = 1L;

		// Perceptron model that carries out the actual learning in each node
		public MultitargetPerceptron learningModel;

		public double learningWeight = 0.0;

		public DoubleVector errorP = new DoubleVector();
		public DoubleVector errorM = new DoubleVector();

		public List<Integer> inputIndexes = null;

		protected double examplesSeenAtLastSplitEvaluation = 0;

		protected AutoExpandVector<AttributeStatisticsObserver> attributeObservers = new AutoExpandVector<>();

		/**
		 * Create a new LeafNode
		 */
		public LeafNode(ISOUPTree tree) {
			super(tree);
			if (tree.buildingModelTree()) {
				learningModel = tree.newLeafModel();
			}
			initializeInputIndexes();
			learningWeight = 0.0;
			examplesSeen = new DoubleVector();
			sumOfValues = new DoubleVector();
			sumOfSquares = new DoubleVector();
			if (tree.runAsPCTOption.isSet()) {
				weightOfInputs = new DoubleVector();
				sumOfInputValues = new DoubleVector();
				sumOfInputSquares = new DoubleVector();
			}
		}

		@Override
		public long calcByteSize() {
			long size = super.calcByteSize();
			if (tree.buildingModelTree()) {
				size += learningModel.calcByteSize();
				size += SizeOf.sizeOf(errorP);
				size += SizeOf.sizeOf(errorM);
			}
			size += SizeOf.sizeOf(inputIndexes);
			size += SizeOf.fullSizeOf(attributeObservers);
			return size;
		}

		public void initializeInputIndexes() {
			this.inputIndexes = tree.newInputIndexes();
		}

		/**
		 * Method to learn from an instance that passes the new instance to the
		 * perceptron learner, and also prevents the class value from being truncated to
		 * an int when it is passed to the attribute observer
		 */
		public void learnFromInstance(Instance instance, double[] prediction, boolean growthAllowed) {
			InstanceWrapper inst = new InstanceWrapper((InstanceImpl) instance);
			// Update the statistics for this node
			double weight = inst.weight();
			// Update the statistics for this node
			double[] predictionP = tree.buildingModelTree() ? getPredictionModel(inst) : null;
			double[] predictionM = getPredictionTargetMean(inst);

			DoubleVector[] observations = new DoubleVector[tree.numOutputAttributes];
			DoubleVector[] inputObservations = null;
			// number of instances passing through the node
			learningWeight += weight;

			// sum of y values

			if (tree.buildingModelTree() && !inst.missingOutputs())
				learningModel.updatePerceptron(inst);
			// sum of squared y values
			for (int i = 0; i < tree.numOutputAttributes; i++) {
				if (!inst.isOutputMissing(i)) {
					double outVal = inst.valueOutputAttribute(i);
					examplesSeen.addToValue(i, weight);
					// sum of y values
					sumOfValues.addToValue(i, weight * outVal);

					// sum of squared y values
					sumOfSquares.addToValue(i, weight * outVal * outVal);

					if (tree.buildingModelTree()) {
						errorP.setValue(i, errorP.getValue(i) * 0.95 + Math.abs(predictionP[i] - outVal));
						errorM.setValue(i, errorM.getValue(i) * 0.95 + Math.abs(predictionM[i] - outVal));
					}

					observations[i] = new DoubleVector(
							new double[] { weight, weight * outVal, weight * outVal * outVal });
				}
			}

			if (tree.runAsPCTOption.isSet()) {
				inputObservations = new DoubleVector[tree.numInputAttributes];
				for (int i : inputIndexes) {
					if (!inst.isInputMissing(i)) {
						double inVal = inst.valueInputAttribute(i);
						weightOfInputs.addToValue(i, weight);

						// sum of attr values
						sumOfInputValues.addToValue(i, weight * inVal);

						// sum of squared attr values
						sumOfInputSquares.addToValue(i, weight * inVal * inVal);

						inputObservations[i] = new DoubleVector(
								new double[] { weight, weight * inVal, weight * inVal * inVal });
					}
				}
			}

			for (int i : inputIndexes) {
				AttributeStatisticsObserver obs = attributeObservers.get(i);
				if (obs == null) {
					if (inst.inputAttribute(i).isNumeric()) {
						obs = tree.newNumericClassObserver();
						attributeObservers.set(i, obs);
					} else if (inst.inputAttribute(i).isNominal()) {
						obs = tree.newNominalClassObserver();
						attributeObservers.set(i, obs);
					}

				}
				if (!tree.runAsPCTOption.isSet() || !inst.inputAttribute(i).isNumeric()) {
					obs.observeAttribute(inst.valueInputAttribute(i), observations);
				} else {
					((MultiLabelBSTreePCT) obs).observeAttribute(inst.valueInputAttribute(i), observations,
							inputObservations);
				}
			}

			if (growthAllowed) {
				checkForSplit();
			}
		}

		/**
		 * Return the best split suggestions for this node using the given split
		 * criteria
		 */
		public AttributeExpansionSuggestion[] getBestSplitSuggestions(MultiLabelSplitCriterion criterion) {

			List<AttributeExpansionSuggestion> bestSuggestions = new LinkedList<>();

			for (int i : inputIndexes) {
				AttributeStatisticsObserver obs = attributeObservers.get(i);
				if (obs != null) {
					DoubleVector[] preSplitStatistics = new DoubleVector[tree.numOutputAttributes];
					DoubleVector[] preSplitInputStatistics = null;
					for (int j = 0; j < tree.numOutputAttributes; j++) {
						preSplitStatistics[j] = new DoubleVector();
						preSplitStatistics[j].setValue(0, examplesSeen.getValue(j));
						preSplitStatistics[j].setValue(1, sumOfValues.getValue(j));
						preSplitStatistics[j].setValue(2, sumOfSquares.getValue(j));
					}

					if (tree.runAsPCTOption.isSet() && tree.modelContext.inputAttribute(i).isNumeric()) {
						preSplitInputStatistics = new DoubleVector[tree.numInputAttributes];
						for (int j = 0; j < tree.numInputAttributes; j++) {
							preSplitInputStatistics[j] = new DoubleVector();
							preSplitInputStatistics[j].setValue(0, weightOfInputs.getValue(j));
							preSplitInputStatistics[j].setValue(1, sumOfInputValues.getValue(j));
							preSplitInputStatistics[j].setValue(2, sumOfInputSquares.getValue(j));
						}

					}

					AttributeExpansionSuggestion bestSuggestion = null;
					if (tree.runAsPCTOption.isSet() && tree.modelContext.inputAttribute(i).isNumeric()) {
						bestSuggestion = ((MultiLabelBSTreePCT) obs).getBestEvaluatedSplitSuggestion(criterion,
								preSplitStatistics, preSplitInputStatistics, i);
					} else {
						bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, preSplitStatistics, i);
					}

					if (bestSuggestion != null) {
						bestSuggestions.add(bestSuggestion);
					}
				}
			}
			return bestSuggestions.toArray(new AttributeExpansionSuggestion[bestSuggestions.size()]);
		}

		/**
		 * Retrieve the class votes using the perceptron learner
		 */
		public double[] getPredictionModel(Instance inst) {
			return learningModel.prediction(inst);
		}

		public double[] getPredictionTargetMean(Instance inst) {
			double[] pred = new double[tree.numOutputAttributes];
			for (int i = 0; i < tree.numOutputAttributes; i++) {
				if (examplesSeen.getValue(i) > 0) {
					pred[i] = sumOfValues.getValue(i) / examplesSeen.getValue(i);
				} else {
					pred[i] = 0;
				}
			}
			return pred;
		}

		@Override
		public double[] getPrediction(Instance inst) {
			if (tree.buildingModelTree()) {
				double[] predictionP = getPredictionModel(inst);
				double[] predictionM = getPredictionTargetMean(inst);
				double[] prediction = new double[predictionP.length];
				for (int i = 0; i < predictionP.length; i++) {
					if (errorP.getValue(i) < errorM.getValue(i)) {
						prediction[i] = predictionP[i];
					} else {
						prediction[i] = predictionM[i];
					}
				}
				return prediction;
			} else {
				return getPredictionTargetMean(inst);
			}

		}

		// public double[] getClassVotes(MultiLabelInstance inst, ISOUPTrees tree) {
		// return new double[] {getPrediction(inst, tree)};
		// }

		public void checkForSplit() {
			// If it has seen Nmin examples since it was last tested for splitting, attempt
			// a split of this node
			// TODO How does this interact with partially labeled examples
			if (learningWeight - examplesSeenAtLastSplitEvaluation >= tree.gracePeriodOption.getValue()) {
				int index = (parent != null) ? parent.getChildIndex(this) : 0;
				tree.attemptToSplit(this, parent, index);

				// Take note of how many instances were seen when this split evaluation was
				// made, so we know when to perform the next split evaluation
				examplesSeenAtLastSplitEvaluation = learningWeight;
			}
		}

		@Override
		public void describeSubtree(StringBuilder out, int indent) {
			StringUtils.appendIndented(out, indent, "Leaf ");
			StringUtils.appendNewline(out);

			if (tree.buildingModelTree()) {
				learningModel.getModelDescription(out, indent + 2);
			} else {
				// out.append(tree.getClassNameString() + " = " + String.format("%.4f",
				// (sumOfValues / examplesSeen)));
				out.append("Leaf node");
				StringUtils.appendNewline(out);
			}
		}

	}

	public static abstract class InnerNode extends Node {
		// The InnerNode and SplitNode design is used for easy extension in
		// ISOUPOptionTree
		private static final long serialVersionUID = 1L;

		protected AutoExpandVector<Node> children = new AutoExpandVector<>();

		// Sum of absolute errors
		protected DoubleVector sumOfAbsErrors = new DoubleVector(); // Needed for PH tracking of mean error

		protected DoubleVector PHsums = new DoubleVector();
		protected DoubleVector PHmins = new DoubleVector();

		// Keep track of the statistics for loss error calculations
		protected double lossExamplesSeen;
		protected double lossFadedSumOriginal;
		protected double lossFadedSumAlternate;
		protected double lossNumQiTests;
		protected double lossSumQi;
		protected double previousWeight = 0;

		public InnerNode(ISOUPTree tree) {
			super(tree);
		}

		@Override
		public long calcByteSize() {
			long size = super.calcByteSize();
			size += SizeOf.sizeOf(PHsums) + SizeOf.sizeOf(PHmins) + SizeOf.sizeOf(sumOfAbsErrors);
			for (Node child : children)
				size += child.calcByteSize();
			return size;
		}

		public int numChildren() {
			return children.size();
		}

		public Node getChild(int i) {
			return children.get(i);
		}

		@Override
		public int getChildIndex(Node child) {
			return children.indexOf(child);
		}

		@Override
		public void setChild(int i, Node child) {
			children.set(i, child);
		}

		@Override
		public void disableChangeDetection() {
			changeDetection = false;
			for (Node child : children) {
				child.disableChangeDetection();
			}
		}

		@Override
		public void restartChangeDetection() {
			if (this.alternateTree == null) {
				changeDetection = true;
				PHsums = new DoubleVector();
				PHmins = new DoubleVector();
				for (int i = 0; i < tree.numOutputAttributes; i++) {
					PHmins.setValue(i, Double.MAX_VALUE);
				}
				for (Node child : children)
					child.restartChangeDetection();
			}
		}

		/**
		 * Check to see if the tree needs updating
		 */
		public boolean PageHinckleyTest(double error, double threshold, int targetIndex) {
			// Update the cumulative mT sum
			PHsums.addToValue(targetIndex, error);

			// Update the minimum mT value if the new mT is
			// smaller than the current minimum
			if (PHsums.getValue(targetIndex) < PHmins.getValue(targetIndex)) {
				PHmins.setValue(targetIndex, PHsums.getValue(targetIndex));
			}
			// Return true if the cumulative value - the current minimum is
			// greater than the current threshold (in which case we should adapt)
			return PHsums.getValue(targetIndex) - PHmins.getValue(targetIndex) > threshold;
		}

		public void initializeAlternateTree(ISOUPTree tree) {
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
	}

	public static class SplitNode extends InnerNode {

		private static final long serialVersionUID = 1L;

		public Predicate predicate;

		/**
		 * Create a new SplitNode
		 * 
		 * @param predicate
		 * @param tree
		 */
		public SplitNode(Predicate predicate, ISOUPTree tree) {
			super(tree);
			this.predicate = predicate;
			ID = tree.maxID;
		}

		@Override
		public long calcByteSize() {
			return super.calcByteSize() + SizeOf.sizeOf(predicate);
		}

		public int instanceChildIndex(Instance inst) {
			return (predicate.evaluate(inst)) ? 0 : 1;
		}

		@Override
		public void describeSubtree(StringBuilder out, int indent) {
			for (int branch = 0; branch < children.size(); branch++) {
				Node child = getChild(branch);
				if (child != null) {
					if (branch == 0) {
						StringUtils.appendIndented(out, indent, "if ");
						this.predicate.getDescription(out, 0, tree.getModelContext().getInstanceInformation());
					} else {
						StringUtils.appendIndented(out, indent, "else");
					}
					out.append(": ");
					StringUtils.appendNewline(out);
					child.describeSubtree(out, indent + 2);
				}
			}
		}

		@Override
		public double[] getPrediction(Instance inst) {
			return children.get(predicate.evaluate(inst) ? 0 : 1).getPrediction(inst);
		}
	}

	public class MultitargetPerceptron implements Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		protected ISOUPTree tree;

		// The Perception weights
		public double[][] weights;

		// The number of instances contributing to this model
		protected int instancesSeen = 0;

		public MultitargetPerceptron(ISOUPTree tree, MultitargetPerceptron original) {
			this.tree = tree;
			weights = new double[original.weights.length][original.weights[0].length];
			for (int i = 0; i < original.weights.length; i++)
				for (int j = 0; j < original.weights[0].length; j++)
					weights[i][j] = original.weights[i][j];
		}

		public MultitargetPerceptron(ISOUPTree tree) {
			this.tree = tree;
			initializeWeights();
		}

		public long calcByteSize() {
			return SizeOf.sizeOf(this);
		}

		public String getPurposeString() {
			return "A multi-target perceptron";
		}

		public void initializeWeights() {
			instancesSeen = 0;
			int numTargets = tree.numOutputAttributes;
			int numInputs = tree.numInputAttributes;
			weights = new double[numTargets][numInputs + 1];
			for (int i = 0; i < numTargets; i++) {
				for (int j = 0; j < numInputs + 1; j++) {
					// The last index corresponds to the constant b
					weights[i][j] = 2 * tree.classifierRandom.nextDouble() - 1;
				}
			}
			normalizeWeights();
		}

		/**
		 * Update the model using the provided instance
		 */
		public void updatePerceptron(Instance inst) {
			// Update attribute statistics
			instancesSeen += inst.weight();

			// Update weights
			double learningRatio = 0.0;
			if (tree.learningRatioConstOption.isSet()) {
				learningRatio = tree.learningRatioOption.getValue();
			} else {
				learningRatio = tree.learningRatioOption.getValue()
						/ (1 + instancesSeen * tree.learningRateDecayFactorOption.getValue());
			}

			// Loop for compatibility with bagging methods
			for (int i = 0; i < (int) inst.weight(); i++) {
				updateWeights(inst, learningRatio);
			}
		}

		public void updateWeights(Instance inst, double learningRatio) {
			if (instancesSeen > 1.0) {
				// Compute the normalized instance and the delta
				double[] normalizedInput = tree.normalizedInputVector(inst);
				double[] normalizedPrediction = prediction(normalizedInput);

				double[] normalizedTarget = tree.normalizedTargetVector(inst);
				for (int i = 0; i < tree.numOutputAttributes; i++) {
					if (!Double.isNaN(normalizedTarget[i])) { /* to account for missing target values */
						double delta = normalizedTarget[i] - normalizedPrediction[i];
						for (int j = 0; j < normalizedInput.length; j++) {
							weights[i][j] += delta * learningRatio * normalizedInput[j];
						}
					}
				}
				normalizeWeights();
			}
		}

		public void normalizeWeights() {
			for (int j = 0; j < weights.length; j++) {
				double sum = 0;
				for (int i = 0; i < weights[j].length; i++)
					sum += Math.abs(weights[j][i]);
				for (int i = 0; i < weights[j].length; i++)
					weights[j][i] /= sum;
			}
		}

		/**
		 * Output the prediction made by this perceptron on the given instance
		 */
		public double[] prediction(double[] instanceValues) {
			double[] out = new double[tree.numOutputAttributes];
			for (int i = 0; i < tree.numOutputAttributes; i++) {
				out[i] = 0;
				for (int j = 0; j < instanceValues.length; j++) {
					out[i] += weights[i][j] * instanceValues[j];
				}
			}
			return out;
		}

		private double[] prediction(Instance inst) {
			double[] normalizedInput = tree.normalizedInputVector(inst);
			double[] normalizedPrediction = prediction(normalizedInput);
			return denormalizePrediction(normalizedPrediction);
		}

		private double[] denormalizePrediction(double[] normalizedPrediction) {
			double[] out = new double[normalizedPrediction.length];
			if (tree.normalize()) {
				for (int i = 0; i < tree.numOutputAttributes; i++) {
					double mean = tree.sumOfValues.getValue(i) / tree.examplesSeen.getValue(i);
					double sd = computeSD(tree.sumOfSquares.getValue(i), tree.sumOfValues.getValue(i),
							tree.examplesSeen.getValue(i));
					if (examplesSeen.getValue(i) > 1)
						out[i] = normalizedPrediction[i] * sd + mean;
					else
						out[i] = 0;
				}
				return out;
			} else
				return normalizedPrediction;
		}

		public void getModelDescription(StringBuilder out, int indent) {
			if (getModelContext() != null) {
				for (int i = 0; i < tree.numOutputAttributes; i++) {
					StringUtils.appendIndented(out, indent,
							" [" + tree.getModelContext().outputAttribute(i).name() + "] =");
					for (int j = 0; j < tree.numInputAttributes; j++) {
						if (getModelContext().inputAttribute(j).isNumeric()) {
							out.append((j == 0 && weights[i][j] >= 0) ? " " : (weights[i][j] < 0) ? " - " : " + ");
							out.append(String.format("%.4f", Math.abs(weights[i][j])));
							out.append(" * ");
							out.append(getModelContext().inputAttribute(j).name());
						}
					}
					out.append((weights[i][tree.numInputAttributes] < 0 ? " - " : " + ")
							+ String.format("%.4f", Math.abs(weights[i][tree.numInputAttributes])));
				}
				StringUtils.appendNewline(out);
			}
		}
	}

	// endregion ================ CLASSES ================

	// region ================ METHODS ================

	// Regressor methods
	public ISOUPTree() {
		super();
	}

	public List<Integer> newInputIndexes() {
		List<Integer> indexes = new Vector<>();
		for (int i = 0; i < this.numInputAttributes; i++) {
			indexes.add(i, i);
		}
		return indexes;
	}

	@Override
	public String getPurposeString() {
		return "Implementation of the iSOUP-Tree algorithm as described by Osojnik et al.";
	}

	@Override
	public void resetLearningImpl() {
		treeRoot = null;

		if (this.getModelContext() != null)
			checkRoot();

		learningWeight = 0.0;
		examplesSeen = new DoubleVector();
		sumOfValues = new DoubleVector();
		sumOfSquares = new DoubleVector();

		weightOfInputs = new DoubleVector();
		sumOfAttrValues = new DoubleVector();
		sumOfAttrSquares = new DoubleVector();

	}

//	@Override
//	public void modelContextSet() {
//		this.numInputAttributes = getModelContext().numInputAttributes();
//		this.numOutputAttributes = getModelContext().numOutputAttributes();
//		loadWeights();
//		checkRoot();
//	}
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		this.numInputAttributes = this.getModelContext().numInputAttributes();
		this.numOutputAttributes = this.getModelContext().numOutputAttributes();
		loadWeights();
		checkRoot();
	}

	public void loadWeights() {
		targetWeights = new DoubleVector();
		try {
			List<String> lines = Files.readAllLines(Paths.get(weightFile.getValue()), Charset.defaultCharset());
			for (int i = 0; i < lines.size(); i++)
				targetWeights.setValue(i, Double.valueOf(lines.get(i)));
		} catch (Exception e) {
			for (int i = 0; i < this.numOutputAttributes; i++)
				targetWeights.setValue(i, 1.0);
		}
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		if (treeRoot != null)
			treeRoot.describeSubtree(out, indent);
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[] {
				// new Measurement("tree size (nodes)", this.leafNodeCount +
				// this.splitNodeCount),
				// new Measurement("tree size (leaves)", this.leafNodeCount)
		};
	}

//	public long measureByteSize() {
//		return calcByteSize();
//	}

	public long calcByteSize() {
		long size = SizeOf.sizeOf(this);
		if (this.treeRoot != null) {
			size += this.treeRoot.calcByteSize();
		}
		size += SizeOf.sizeOf(examplesSeen) + SizeOf.sizeOf(weightOfInputs);
		size += SizeOf.sizeOf(sumOfValues) + SizeOf.sizeOf(sumOfSquares);
		size += SizeOf.sizeOf(sumOfAttrValues) + SizeOf.sizeOf(sumOfAttrSquares);
		size += SizeOf.sizeOf(targetWeights);
		return size;
	}

	@Override
//	public Prediction getPredictionForInstance(Instance inst) {
//		double[] predictionVector = treeRoot.getPrediction(inst);
//		return new MultiTargetRegressionPrediction(predictionVector);
//	}
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		checkRoot();
		double[] predictionVector = treeRoot.getPrediction(inst);
		MultiLabelPrediction prediction = new MultiLabelPrediction(getModelContext().numOutputAttributes());
		for (int i = 0; i < getModelContext().numOutputAttributes(); i++) {
			prediction.setVote(i, 1, predictionVector[i]);
		}
		return prediction;
	}

//	@Override
//	public Prediction getTrainingPrediction(Instance inst) {
//		return getPredictionForInstance(inst);
//	}
	@Override
	public Prediction getTrainingPrediction() { return null; }

	
	public double[] normalizedInputVector(Instance inst) {
		// Normalize Instance
		double[] normalizedInput = new double[this.numInputAttributes + 1];
		if (normalize()) {
			for (int j = 0; j < this.numInputAttributes; j++) {
				Attribute attr = inst.inputAttribute(j);
				double value = inst.valueInputAttribute(j);
				double mean = sumOfAttrValues.getValue(j) / weightOfInputs.getValue(j);
				double sd = computeSD(sumOfAttrSquares.getValue(j), sumOfAttrValues.getValue(j),
						weightOfInputs.getValue(j));
				if (weightOfInputs.getValue(j) > 1 && sd > 0.00001) {
					if (attr.isNumeric())
						normalizedInput[j] = (value - mean) / sd;
					else
						normalizedInput[j] = value;
				} else
					normalizedInput[j] = 0;
			}
			normalizedInput[this.numInputAttributes] = 1.0; // Value to be multiplied with the constant factor
		} else {
			for (int j = 0; j < this.numInputAttributes; j++) {
				normalizedInput[j] = inst.valueInputAttribute(j);
			}
			normalizedInput[this.numInputAttributes] = 1.0;
		}
		return normalizedInput;
	}

	public double[] normalizedTargetVector(Instance instance) {
		InstanceWrapper inst = new InstanceWrapper((InstanceImpl) instance);
		double[] out = new double[this.numOutputAttributes];
		if (normalize()) {
			for (int i = 0; i < this.numOutputAttributes; i++) {
				double value = (inst.isOutputMissing(i)) ? Double.NaN : inst.valueOutputAttribute(i);
				if (!Double.isNaN(value)) {
					double sd = computeSD(sumOfSquares.getValue(i), sumOfValues.getValue(i), examplesSeen.getValue(i));
					double average = sumOfValues.getValue(i) / examplesSeen.getValue(i);
					if (sd > 0 && examplesSeen.getValue(i) > 1)
						out[i] = (value - average) / (sd);
					else
						out[i] = 0;
				} else
					out[i] = value;
			}
		} else {
			for (int i = 0; i < this.numOutputAttributes; i++) {
				out[i] = (inst.isOutputMissing(i)) ? Double.NaN : inst.valueOutputAttribute(i);
			}
		}
		return out;
	}

	public double[] normalizeTargetVector(double[] pred) {
		if (pred != null && normalize()) {
			double[] out = new double[pred.length];
			for (int i = 0; i < pred.length; i++) {
				double value = pred[i];
				double sd = computeSD(sumOfSquares.getValue(i), sumOfValues.getValue(i), examplesSeen.getValue(i));
				double average = sumOfValues.getValue(i) / examplesSeen.getValue(i);
				if (sd > 0 && examplesSeen.getValue(i) > 1)
					out[i] = (value - average) / sd;
				else
					out[i] = 0;
			}
			return out;
		} else
			return pred;
	}

	public double normalizeTargetValue(Instance inst, int i) {
		if (normalize()) {
			if (examplesSeen.getValue(i) > 1) {
				double value = inst.valueOutputAttribute(i);
				double sd = computeSD(sumOfSquares.getValue(i), sumOfValues.getValue(i), examplesSeen.getValue(i));
				double average = sumOfValues.getValue(i) / examplesSeen.getValue(i);
				if (sd > 0)
					return (value - average) / sd;
				else
					return 0;
			}
			return 0;
		} else
			return inst.valueOutputAttribute(i);
	}

	public double normalizeTargetValue(double value, int i) {
		if (normalize()) {
			if (examplesSeen.getValue(i) > 1) {
				double sd = computeSD(sumOfSquares.getValue(i), sumOfValues.getValue(i), examplesSeen.getValue(i));
				double average = sumOfValues.getValue(i) / examplesSeen.getValue(i);
				if (sd > 0)
					return (value - average) / sd;
				else
					return 0;
			}
			return 0;
		} else
			return value;
	}

	public double[] getNormalizedError(Instance inst, double[] prediction) {
		double[] normalPrediction = normalizeTargetVector(prediction);
		double[] normalValue = normalizedTargetVector(inst);
		double[] out = new double[this.numOutputAttributes];
		if (normalPrediction != null)
			for (int i = 0; i < this.numOutputAttributes; i++) {
				out[i] = Math.abs(normalValue[i] - normalPrediction[i]);
			}
		return out;
	}

	/**
	 * Method for updating (training) the model using a new instance
	 */
	@Override
	//public void trainOnInstanceImpl(Instance inst) {
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		InstanceWrapper inst = new InstanceWrapper((InstanceImpl) instance);
		if (inst.weight() > 0) {

			double[] prediction = treeRoot.getPrediction(inst);
			// double[] normalError = getNormalizedError(inst, prediction);
			double[] normalError = null;
			// normalError.scaleValues(inst.weight());
			processInstance(inst, treeRoot, prediction, normalError, true, false);

			double weight = inst.weight();

			learningWeight += weight;
			for (int i = 0; i < this.numOutputAttributes; i++) {
				if (!inst.isOutputMissing(i)) {
					double iVal = inst.valueOutputAttribute(i);
					examplesSeen.addToValue(i, weight);
					sumOfValues.addToValue(i, weight * iVal);
					sumOfSquares.addToValue(i, weight * iVal * iVal);
				}
			}

			for (int i = 0; i < this.numInputAttributes; i++) {
				if (!inst.isInputMissing(i)) {
					double iVal = inst.valueInputAttribute(i);
					weightOfInputs.addToValue(i, weight);
					sumOfAttrValues.addToValue(i, weight * iVal);
					sumOfAttrSquares.addToValue(i, weight * iVal * iVal);
				}
			}
		}
	}

	public void processInstance(Instance inst, Node node, double[] prediction, double[] normalError,
			boolean growthAllowed, boolean inAlternate) {
		Node currentNode = node;
		while (true) {
			if (currentNode instanceof LeafNode) {
				((LeafNode) currentNode).learnFromInstance(inst, prediction, growthAllowed);
				break;
			} else {
				// currentNode.sumOfAbsErrors.addValues(normalError);
				// SplitNode iNode = (SplitNode) currentNode;
				// if (!inAlternate && iNode.alternateTree != null) {
				// boolean altTree = true;
				// double lossO = Math.pow(inst.valueOutputAttribute() - prediction, 2);
				// double lossA = Math.pow(inst.valueOutputAttribute() -
				// iNode.alternateTree.getPrediction(inst), 2);
				//
				// iNode.lossFadedSumOriginal = lossO +
				// alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumOriginal;
				// iNode.lossFadedSumAlternate = lossA +
				// alternateTreeFadingFactorOption.getValue() * iNode.lossFadedSumAlternate;
				// iNode.lossExamplesSeen++;
				//
				// double Qi = Math.log((iNode.lossFadedSumOriginal) /
				// (iNode.lossFadedSumAlternate));
				// double previousQiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
				// iNode.lossSumQi += Qi;
				// iNode.lossNumQiTests += 1;
				// double QiAverage = iNode.lossSumQi / iNode.lossNumQiTests;
				//
				// if (iNode.lossExamplesSeen - iNode.previousWeight >=
				// alternateTreeTMinOption.getValue()) {
				// iNode.previousWeight = iNode.lossExamplesSeen;
				// if (Qi > 0) {
				// SplitNode parent = currentNode.getParent();
				//
				// if (parent != null) {
				// Node replacementTree = iNode.alternateTree;
				// parent.setChild(parent.getChildIndex(currentNode), replacementTree);
				// if (growthAllowed) replacementTree.restartChangeDetection();
				// } else {
				// treeRoot = iNode.alternateTree;
				// treeRoot.restartChangeDetection();
				// }
				//
				// currentNode = iNode.alternateTree;
				// altTree = false;
				// } else if (
				// (QiAverage < previousQiAverage && iNode.lossExamplesSeen >= (10 *
				// this.gracePeriodOption.getValue()))
				// || iNode.lossExamplesSeen >= alternateTreeTimeOption.getValue()
				// ) {
				// iNode.alternateTree = null;
				// if (growthAllowed) iNode.restartChangeDetection();
				// altTree = false;
				// }
				// }
				//
				// if (altTree) {
				// growthAllowed = false;
				// processInstance(inst, iNode.alternateTree, prediction, normalError, true,
				// true);
				// }
				// }

				// if (iNode.changeDetection && !inAlternate) {
				// if (iNode.PageHinckleyTest(normalError - iNode.sumOfAbsErrors /
				// iNode.examplesSeen - PageHinckleyAlphaOption.getValue(),
				// PageHinckleyThresholdOption.getValue())) {
				// iNode.initializeAlternateTree(this);
				// }
				// }
				// }
				// if (currentNode instanceof SplitNode) {
				currentNode = ((SplitNode) currentNode).getChild(((SplitNode) currentNode).instanceChildIndex(inst));
				// } else { // if the replaced alternate tree is just a leaf node
				// ((LeafNode) currentNode).learnFromInstance(inst, prediction, growthAllowed);
				// break;
			}
		}
	}

	// region --- Object instatiation methods

	protected NumericStatisticsObserver newNumericClassObserver() {
		try {
			// NumericStatisticsObserver o = (NumericStatisticsObserver)
			// ClassOption.cliStringToObject("MultiLabelBSTree -z 2000",
			// MultiLabelBSTree.class, null);
			// NumericStatisticsObserver o = (NumericStatisticsObserver)
			// ClassOption.cliStringToObject("MultiLabelBSTree2 -z 2000",
			// MultiLabelBSTree2.class, null);
			if (runAsPCTOption.isSet()) {
				return new MultiLabelBSTreePCT();
			} else {
				return new MultiLabelBSTree();
			}

		} catch (Exception e) {
			return null;
		}
	}

	public NominalStatisticsObserver newNominalClassObserver() {
		return new MultiLabelNominalAttributeObserver();
	}

	// protected SplitNode newSplitNode(InstanceConditionalTest splitTest) {
	// maxID++;
	// return new SplitNode(splitTest, this);
	// }

	protected SplitNode newSplitNode(Predicate predicate) {
		maxID++;
		return new SplitNode(predicate, this);
	}

	protected LeafNode newLeafNode() {
		maxID++;
		return new LeafNode(this);
	}

	public MultitargetPerceptron newLeafModel() {
		return new MultitargetPerceptron(this);
	}

	// endregion --- Object instatiation methods

	// region --- Processing methods
	protected void checkRoot() {
		if (treeRoot == null) {
			treeRoot = newLeafNode();
		}
	}

	public static double computeHoeffdingBound(double range, double confidence, double n) {
		return Math.sqrt(((range * range) * Math.log(1 / confidence)) / (2.0 * n));
	}

	public boolean buildingModelTree() {
		return !regressionTreeOption.isSet();
	}

	public boolean normalize() {
		return !doNotNormalizeOption.isSet();
	}

	protected void attemptToSplit(LeafNode node, InnerNode parent, int parentIndex) {
		// System.out.println("Evaluating splits");
		MultiLabelSplitCriterion splitCriterion = null;
		if (!runAsPCTOption.isSet()) {
			splitCriterion = new WeightedICVarianceReduction(targetWeights);
		} else {
			DoubleVector inputWeights = new DoubleVector();
			for (int i = 0; i < numInputAttributes; i++)
				inputWeights.setValue(i, 1);
			splitCriterion = new PCTWeightedICVarianceReduction(targetWeights, inputWeights, 0.5);
		}
		// System.out.println(examplesSeen);
		// Set the split criterion to use to the SDR split criterion as described by
		// Ikonomovska et al.
		AttributeExpansionSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion); // TODO
																											// update
																											// with
																											// split
																											// criterion
																											// option

		// Using this criterion, find the best split per attribute and rank the results

		// Declare a variable to determine if any of the splits should be performed
		boolean shouldSplit = false;

		if (bestSplitSuggestions.length < 2) {
			shouldSplit = bestSplitSuggestions.length > 0;
		} else { // Otherwise, consider which of the splits proposed may be worth trying
			Arrays.sort(bestSplitSuggestions);

			// Determine the Hoeffding bound value, used to select how many instances should
			// be used to make a test decision
			double numExamples = node.learningWeight;
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
			shouldSplit = (secondBestSuggestion.merit / bestSuggestion.merit < 1 - hoeffdingBound)
					|| (hoeffdingBound < this.tieThresholdOption.getValue());
			// System.out.print(hoeffdingBound);
			// System.out.print(" ");
			/// System.out.println(secondBestSuggestion.merit / bestSuggestion.merit);
			// If the splitting criterion was not met, initiate pruning of the E-BST
			// structures in each attribute observer
			// TODO pruning is currently disabled
			// TODO obs.removeBadSplits(null, secondBestSuggestion.merit /
			// bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound,
			// getModelContext().numOutputAttributes());
//					AttributeStatisticsObserver obs = node.attributeObservers.get(i);
//					if (obs != null) {
//						if (getModelContext().attribute(i).isNumeric());
//						//TODO obs.removeBadSplits(null, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound, getModelContext().numOutputAttributes());
//						if (getModelContext().attribute(i).isNominal());
//						// TODO nominal class observers
//					}
//				}
			// TODO nominal class observers
		}

		// If the splitting criterion were met, split the current node using the chosen
		// attribute test, and
		// make two new branches leading to (empty) leaves
		if (shouldSplit) {
			AttributeExpansionSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];

			SplitNode newSplit = newSplitNode(splitDecision.predicate);
			newSplit.ID = node.ID;
			newSplit.copyStatistics(node);
			newSplit.changeDetection = node.changeDetection;
			log(Integer.toString(node.ID) + ',' + this.examplesSeen.toString());
			for (int i = 0; i < 2; i++) { // Hardcoded for 2 values (due to the use of the Predicate class)
				LeafNode newChild = newLeafNode();
				if (buildingModelTree()) {
					// Copy the splitting node's perceptron to it's children
					newChild.learningModel = new MultitargetPerceptron(this, node.learningModel);
					newChild.errorM = (DoubleVector) node.errorM.copy();
					newChild.errorP = (DoubleVector) node.errorP.copy();
				} else {

					for (int j = 0; j < getModelContext().numOutputAttributes(); j++) {
						newChild.examplesSeen.setValue(j, splitDecision.getResultingNodeStatistics()[j][i].getValue(0));
						newChild.sumOfValues.setValue(j, splitDecision.getResultingNodeStatistics()[j][i].getValue(1));
						newChild.sumOfSquares.setValue(j, splitDecision.getResultingNodeStatistics()[j][i].getValue(2));
					}
				}
				newChild.changeDetection = node.changeDetection;
				newChild.setParent(newSplit);
				newSplit.setChild(i, newChild);
			}
			if (parent == null && node.originalNode == null) {
				treeRoot = newSplit;
			} else if (parent == null && node.originalNode != null) {
				node.originalNode.alternateTree = newSplit;
			} else {
				parent.setChild(parentIndex, newSplit);
				newSplit.setParent(parent);
			}
		}
	}

	public double computeSD(double squaredVal, double val, double size) {
		if (size > 1)
			return Math.sqrt((squaredVal - ((val * val) / size)) / size);
		else
			return 0.0;
	}

	public static double scalarProduct(DoubleVector u, DoubleVector v) {
		double ret = 0.0;
		for (int i = 0; i < Math.max(u.numValues(), v.numValues()); i++) {
			ret += u.getValue(i) * v.getValue(i);
		}
		return ret;
	}
	// endregion --- Processing methods

	public void initWriter(String filename) {
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write("");
			writer.close();
			writer = new BufferedWriter(new FileWriter(filename, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void closeWriter() {
		try {
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		writer = null;

	}

	public void log(String s) {
		if (writer != null) {
			try {
				writer.write(s + "\n");
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	// endregion --- Processing methods

	// endregion ================ METHODS ================
}

