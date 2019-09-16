/*
 *    MultilabelMultilabelHoeffdingTree.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.learners.predictors.trees;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.AbstractMOAObject;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.learners.predictors.AbstractMultiLabelClassifier;
import moa.learners.predictors.Classifier;
import moa.learners.predictors.MultiLabelClassifier;
import moa.learners.predictors.core.AttributeSplitSuggestion;
import moa.learners.predictors.core.attributeclassobservers.AttributeClassObserver;
import moa.learners.predictors.core.attributeclassobservers.DiscreteAttributeClassObserver;
import moa.learners.predictors.core.attributeclassobservers.NullAttributeClassObserver;
import moa.learners.predictors.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.learners.predictors.core.conditionaltests.InstanceConditionalTest;
import moa.learners.predictors.core.splitcriteria.SplitCriterion;
import moa.options.ClassOption;

/**
 * Hoeffding Tree for classifying multi-label data.
 *
 * A Hoeffding tree is an incremental, anytime decision tree induction algorithm
 * that is capable of learning from massive data streams, assuming that the
 * distribution generating examples does not change over time.
 *
 *
 */
public class MultilabelHoeffdingTree extends AbstractMultiLabelClassifier implements MultiLabelClassifier {
// Needs to use InfoGainSplitCriterionMultiLabel, since multilabel entropy is calculated in a different way
// Trains a mlinstance adding statistics of several class values and training node classifiers
// Get votes from the classifier in the learning node of the tree

	private static final long serialVersionUID = 1L;

	public ClassOption learnerOption = new ClassOption("learner", 'a', "Classifier to train.", Classifier.class,
			"bayes.NaiveBayes");

	public IntOption maxByteSizeOption = new IntOption("maxByteSize", 'm', "Maximum memory consumed by the tree.",
			33554432, 0, Integer.MAX_VALUE);

	public ClassOption numericEstimatorOption = new ClassOption("numericEstimator", 'n', "Numeric estimator to use.",
			NumericAttributeClassObserver.class, "GaussianNumericAttributeClassObserver");

	public ClassOption nominalEstimatorOption = new ClassOption("nominalEstimator", 'd', "Nominal estimator to use.",
			DiscreteAttributeClassObserver.class, "NominalAttributeClassObserver");

	public IntOption memoryEstimatePeriodOption = new IntOption("memoryEstimatePeriod", 'e',
			"How many instances between memory consumption checks.", 1000000, 0, Integer.MAX_VALUE);

	public IntOption gracePeriodOption = new IntOption("gracePeriod", 'g',
			"The number of instances a leaf should observe between split attempts.", 200, 0, Integer.MAX_VALUE);

	public ClassOption splitCriterionOption = new ClassOption("splitCriterion", 's', "Split criterion to use.",
			SplitCriterion.class, "InfoGainSplitCriterion");

	public FloatOption splitConfidenceOption = new FloatOption("splitConfidence", 'c',
			"The allowable error in split decision, values closer to 0 will take longer to decide.", 0.0000001, 0.0,
			1.0);

	public FloatOption tieThresholdOption = new FloatOption("tieThreshold", 't',
			"Threshold below which a split will be forced to break ties.", 0.05, 0.0, 1.0);

	public FlagOption binarySplitsOption = new FlagOption("binarySplits", 'b', "Only allow binary splits.");

	public FlagOption stopMemManagementOption = new FlagOption("stopMemManagement", 'z',
			"Stop growing as soon as memory limit is hit.");

	public FlagOption removePoorAttsOption = new FlagOption("removePoorAtts", 'r', "Disable poor attributes.");

	public FlagOption noPrePruneOption = new FlagOption("noPrePrune", 'p', "Disable pre-pruning.");

	public MultiChoiceOption leafpredictionOption = new MultiChoiceOption("leafprediction", 'l',
			"Leaf prediction to use.", new String[] { "MC", "NB", "NBAdaptive" },
			new String[] { "Majority class", "Naive Bayes", "Naive Bayes Adaptive" }, 2);

	public IntOption nbThresholdOption = new IntOption("nbThreshold", 'q',
			"The number of instances a leaf should observe before permitting Naive Bayes.", 0, 0, Integer.MAX_VALUE);

	/*
	 * public MultilabelMultilabelHoeffdingTree() { this.removePoorAttsOption =
	 * null; }
	 */

	public static class FoundNode {

		public Node node;

		public SplitNode parent;

		public int parentBranch;

		public FoundNode(Node node, SplitNode parent, int parentBranch) {
			this.node = node;
			this.parent = parent;
			this.parentBranch = parentBranch;
		}
	}

	public static class Node extends AbstractMOAObject {

		private static final long serialVersionUID = 1L;

		protected DoubleVector observedClassDistribution;

		public Node(double[] classObservations) {
			this.observedClassDistribution = new DoubleVector(classObservations);
		}

		public int calcByteSize() {
			return (int) (SizeOf.sizeOf(this) + SizeOf.fullSizeOf(this.observedClassDistribution));
		}

		public int calcByteSizeIncludingSubtree() {
			return calcByteSize();
		}

		public boolean isLeaf() {
			return true;
		}

		public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent, int parentBranch) {
			return new FoundNode(this, parent, parentBranch);
		}

		public double[] getObservedClassDistribution() {
			return this.observedClassDistribution.getArrayCopy();
		}

		public double[] getClassVotes(Instance inst, MultilabelHoeffdingTree ht) {
			return this.observedClassDistribution.getArrayCopy();
		}

		public boolean observedClassDistributionIsPure() {
			return this.observedClassDistribution.numNonZeroEntries() < 2;
		}

		public void describeSubtree(MultilabelHoeffdingTree ht, StringBuilder out, int indent) {
			StringUtils.appendIndented(out, indent, "Leaf ");
			out.append(ht.getClassNameString());
			out.append(" = ");
			out.append(ht.getClassLabelString(this.observedClassDistribution.maxIndex()));
			out.append(" weights: ");
			this.observedClassDistribution.getSingleLineDescription(out,
					ht.treeRoot.observedClassDistribution.numValues());
			StringUtils.appendNewline(out);
		}

		public int subtreeDepth() {
			return 0;
		}

		public double calculatePromise() {
			double totalSeen = this.observedClassDistribution.sumOfValues();
			return totalSeen > 0.0
					? (totalSeen - this.observedClassDistribution.getValue(this.observedClassDistribution.maxIndex()))
					: 0.0;
		}

		@Override
		public void getDescription(StringBuilder sb, int indent) {
			describeSubtree(null, sb, indent);
		}
	}

	public static class SplitNode extends Node {

		private static final long serialVersionUID = 1L;

		protected InstanceConditionalTest splitTest;

		protected AutoExpandVector<Node> children; // = new AutoExpandVector<Node>();

		@Override
		public int calcByteSize() {
			return super.calcByteSize() + (int) (SizeOf.sizeOf(this.children) + SizeOf.fullSizeOf(this.splitTest));
		}

		@Override
		public int calcByteSizeIncludingSubtree() {
			int byteSize = calcByteSize();
			for (Node child : this.children) {
				if (child != null) {
					byteSize += child.calcByteSizeIncludingSubtree();
				}
			}
			return byteSize;
		}

		public SplitNode(InstanceConditionalTest splitTest, double[] classObservations, int size) {
			super(classObservations);
			this.splitTest = splitTest;
			this.children = new AutoExpandVector<>(size);
		}

		public SplitNode(InstanceConditionalTest splitTest, double[] classObservations) {
			super(classObservations);
			this.splitTest = splitTest;
			this.children = new AutoExpandVector<>();
		}

		public int numChildren() {
			return this.children.size();
		}

		public void setChild(int index, Node child) {
			if ((this.splitTest.maxBranches() >= 0) && (index >= this.splitTest.maxBranches())) {
				throw new IndexOutOfBoundsException();
			}
			this.children.set(index, child);
		}

		public Node getChild(int index) {
			return this.children.get(index);
		}

		public int instanceChildIndex(Instance inst) {
			return this.splitTest.branchForInstance(inst);
		}

		@Override
		public boolean isLeaf() {
			return false;
		}

		@Override
		public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent, int parentBranch) {
			int childIndex = instanceChildIndex(inst);
			if (childIndex >= 0) {
				Node child = getChild(childIndex);
				if (child != null) {
					return child.filterInstanceToLeaf(inst, this, childIndex);
				}
				return new FoundNode(null, this, childIndex);
			}
			return new FoundNode(this, parent, parentBranch);
		}

		@Override
		public void describeSubtree(MultilabelHoeffdingTree ht, StringBuilder out, int indent) {
			for (int branch = 0; branch < numChildren(); branch++) {
				Node child = getChild(branch);
				if (child != null) {
					StringUtils.appendIndented(out, indent, "if ");
					out.append(this.splitTest.describeConditionForBranch(branch, ht.getModelContext()));
					out.append(": ");
					StringUtils.appendNewline(out);
					child.describeSubtree(ht, out, indent + 2);
				}
			}
		}

		@Override
		public int subtreeDepth() {
			int maxChildDepth = 0;
			for (Node child : this.children) {
				if (child != null) {
					int depth = child.subtreeDepth();
					if (depth > maxChildDepth) {
						maxChildDepth = depth;
					}
				}
			}
			return maxChildDepth + 1;
		}
	}

	public static abstract class LearningNode extends Node {

		private static final long serialVersionUID = 1L;

		public LearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		public abstract void learnFromInstance(Instance inst, MultilabelHoeffdingTree ht);
	}

	public static class InactiveLearningNode extends LearningNode {

		private static final long serialVersionUID = 1L;

		public InactiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, MultilabelHoeffdingTree ht) {
			this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
		}
	}

	public static class ActiveLearningNode extends LearningNode {

		private static final long serialVersionUID = 1L;

		protected double weightSeenAtLastSplitEvaluation;

		protected AutoExpandVector<AttributeClassObserver> attributeObservers = new AutoExpandVector<>();

		protected boolean isInitialized;

		public ActiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
			this.weightSeenAtLastSplitEvaluation = getWeightSeen();
			this.isInitialized = false;
		}

		@Override
		public int calcByteSize() {
			return super.calcByteSize() + (int) (SizeOf.fullSizeOf(this.attributeObservers));
		}

		@Override
		public void learnFromInstance(Instance inst, MultilabelHoeffdingTree ht) {
			if (this.isInitialized == false) {
				this.attributeObservers = new AutoExpandVector<>(inst.numAttributes());
				this.isInitialized = true;
			}
			this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
			for (int i = 0; i < inst.numAttributes() - 1; i++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
				AttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs == null) {
					obs = inst.attribute(instAttIndex).isNominal() ? ht.newNominalClassObserver()
							: ht.newNumericClassObserver();
					this.attributeObservers.set(i, obs);
				}
				obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
			}
		}

		public double getWeightSeen() {
			return this.observedClassDistribution.sumOfValues();
		}

		public double getWeightSeenAtLastSplitEvaluation() {
			return this.weightSeenAtLastSplitEvaluation;
		}

		public void setWeightSeenAtLastSplitEvaluation(double weight) {
			this.weightSeenAtLastSplitEvaluation = weight;
		}

		public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion,
				MultilabelHoeffdingTree ht) {
			List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<>();
			double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
			if (!ht.noPrePruneOption.isSet()) {
				// add null split as an option
				bestSuggestions.add(new AttributeSplitSuggestion(null, new double[0][],
						criterion.getMeritOfSplit(preSplitDist, new double[][] { preSplitDist })));
			}
			for (int i = 0; i < this.attributeObservers.size(); i++) {
				AttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs != null) {
					AttributeSplitSuggestion bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion,
							preSplitDist, i, ht.binarySplitsOption.isSet());
					if (bestSuggestion != null) {
						bestSuggestions.add(bestSuggestion);
					}
				}
			}
			return bestSuggestions.toArray(new AttributeSplitSuggestion[bestSuggestions.size()]);
		}

		public void disableAttribute(int attIndex) {
			this.attributeObservers.set(attIndex, new NullAttributeClassObserver());
		}
	}

	public class LearningNodeClassifier extends ActiveLearningNode {

		protected Classifier classifier;

		private static final long serialVersionUID = 1L;

		public LearningNodeClassifier(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		public LearningNodeClassifier(double[] initialClassObservations, Classifier cl, HoeffdingTreeClassifLeaves ht) {
			super(initialClassObservations);
			// public void LearningNodeClassifier1(double[] initialClassObservations,
			// Classifier cl, HoeffdingTreeClassifLeaves ht ) {

			if (cl == null) {
				this.classifier = (Classifier) getPreparedClassOption(ht.learnerOption);
			} else {
				this.classifier = (Classifier) cl.copy();
			}
		}

		@Override
		public double[] getClassVotes(Instance inst, MultilabelHoeffdingTree ht) {
			if (getWeightSeen() >= ht.nbThresholdOption.getValue()) {
				return this.classifier.getPredictionForInstance(inst).asDoubleArray();
			}
			return super.getClassVotes(inst, ht);
		}

		@Override
		public void disableAttribute(int attIndex) {
			// should not disable poor atts - they are used in NB calc
		}

		@Override
		public void learnFromInstance(Instance inst, MultilabelHoeffdingTree ht) {
			this.classifier.trainOnInstance(inst);
			super.learnFromInstance(inst, ht);
		}

		public Classifier getClassifier() {
			return this.classifier;
		}
	}

	public static class MultilabelInactiveLearningNode extends InactiveLearningNode {

		private static final long serialVersionUID = 1L;

		public MultilabelInactiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, MultilabelHoeffdingTree ht) {
			List<Integer> labels = getRelevantLabels(inst);
			for (int l : labels) {
				this.observedClassDistribution.addToValue(l, inst.weight());
			}
		}
	}

	public class MultilabelLearningNodeClassifier extends LearningNodeClassifier {
		// It uses classifier at nodes, and to be able to train with several class
		// values

		private static final long serialVersionUID = 1L;

		public MultilabelLearningNodeClassifier(double[] initialClassObservations, Classifier cl,
				MultilabelHoeffdingTree ht) {
			super(initialClassObservations);
			this.observedClassDistribution = new DoubleVector(initialClassObservations);
			this.weightSeenAtLastSplitEvaluation = getWeightSeen();
			this.isInitialized = false;

			if (cl == null) {
				this.classifier = (Classifier) ((Classifier) getPreparedClassOption(ht.learnerOption)).copy();
				this.classifier.resetLearning();

				InstancesHeader raw_header = ht.getModelContext();
				this.classifier.setModelContext(raw_header);
			} else {
				this.classifier = (Classifier) cl.copy();
			}
		}

		public Prediction getPrediction(Instance inst, MultilabelHoeffdingTree ht) {

			return this.classifier.getPredictionForInstance(inst);
		}

		public Prediction getPredictionForInstance(Instance inst, MultilabelHoeffdingTree ht) {

			return this.classifier.getPredictionForInstance(inst);
		}

		@Override
		public void disableAttribute(int attIndex) {
			// should not disable poor atts - they are used in NB calc
		}

		@Override
		public Classifier getClassifier() {
			return this.classifier;
		}

		@Override
		public void learnFromInstance(Instance inst, MultilabelHoeffdingTree ht) {

			// It uses different class values, not only one
			this.classifier.trainOnInstance(inst);
			List<Integer> labels = MultilabelHoeffdingTree.getRelevantLabels(inst);
			for (int l : labels) {
				this.observedClassDistribution.addToValue(l, inst.weight());
			}

			for (int i = 0; i < inst.numInputAttributes(); i++) {
				int instAttIndex = i;
				AttributeClassObserver obs = this.attributeObservers.get(instAttIndex); // i
				if (obs == null) {
					obs = inst.inputAttribute(i).isNominal() ? ht.newNominalClassObserver()
							: ht.newNumericClassObserver();
					this.attributeObservers.set(i, obs);
				}
				for (int l : labels) {
					obs.observeAttributeClass(inst.valueInputAttribute(i), l, inst.weight());
				}
			}
		}

		@Override
		public void describeSubtree(MultilabelHoeffdingTree ht, StringBuilder out, int indent) {
			StringUtils.appendIndented(out, indent, "Leaf ");
			out.append(" = ");
			out.append(" weights: ");
			this.observedClassDistribution.getSingleLineDescription(out, this.observedClassDistribution.numValues());
			StringUtils.appendNewline(out);
		}
	}

	protected Node treeRoot;

	protected int decisionNodeCount;

	protected int activeLeafNodeCount;

	protected int inactiveLeafNodeCount;

	protected double inactiveLeafByteSizeEstimate;

	protected double activeLeafByteSizeEstimate;

	protected double byteSizeEstimateOverheadFraction;

	protected boolean growthAllowed;

	protected LearningNode newLearningNode(double[] initialClassObservations) {
		// Create new Learning Node with a null classifier
		return new MultilabelLearningNodeClassifier(initialClassObservations, null, this);
	}

	// @Override
	protected LearningNode newLearningNode(double[] initialClassObservations, Classifier cl) {
		// Create new Learning Node
		return new MultilabelLearningNodeClassifier(initialClassObservations, cl, this);
	}

	protected void deactivateLearningNode(ActiveLearningNode toDeactivate, SplitNode parent, int parentBranch) {
		// It uses MultilabelInactiveLearningNode since there are several class values
		Node newLeaf = new MultilabelInactiveLearningNode(toDeactivate.getObservedClassDistribution());
		if (parent == null) {
			this.treeRoot = newLeaf;
		} else {
			parent.setChild(parentBranch, newLeaf);
		}
		this.activeLeafNodeCount--;
		this.inactiveLeafNodeCount++;
	}

	@Override
	public Prediction getPredictionForInstance(Example<Instance> example) {
		return getPredictionForInstance(example.getData());
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {

		if (this.treeRoot != null) {
			FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
			Node leafNode = foundNode.node;
			MultilabelLearningNodeClassifier multilabelLeafNode = (MultilabelLearningNodeClassifier) leafNode;
			if (leafNode == null) {
				leafNode = foundNode.parent;
			}
			return multilabelLeafNode.getPrediction(inst, this);
		} else {
			System.err.println("[WARNING] Root Node == Null !!!!!!");
		}

		// Return empty array (this should only happen once! -- before we build the root
		// node).
		return null;
	}

	@Override
	public void trainOnInstance(Instance inst) {
		boolean isTraining = (inst.weight() > 0.0);
		if (isTraining) {
			this.trainingWeightSeenByModel += inst.weight();
			trainOnInstanceImpl(inst);
		}
	}

	public static List<Integer> getRelevantLabels(Instance x) {
		List<Integer> classValues = new LinkedList<>();
		// get all class attributes
		for (int j = 0; j < x.numOutputAttributes(); j++) {
			if (x.classValue(j) > 0.0) {
				classValues.add(j);
			}
		}
		return classValues;
	}

	protected AttributeClassObserver newNominalClassObserver() {
		AttributeClassObserver nominalClassObserver = (AttributeClassObserver) getPreparedClassOption(
				this.nominalEstimatorOption);
		return (AttributeClassObserver) nominalClassObserver.copy();
	}

	protected AttributeClassObserver newNumericClassObserver() {
		AttributeClassObserver numericClassObserver = (AttributeClassObserver) getPreparedClassOption(
				this.numericEstimatorOption);
		return (AttributeClassObserver) numericClassObserver.copy();
	}

	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}

	public void estimateModelByteSizes() {
		FoundNode[] learningNodes = findLearningNodes();
		long totalActiveSize = 0;
		long totalInactiveSize = 0;
		for (FoundNode foundNode : learningNodes) {
			if (foundNode.node instanceof ActiveLearningNode) {
				totalActiveSize += SizeOf.fullSizeOf(foundNode.node);
			} else {
				totalInactiveSize += SizeOf.fullSizeOf(foundNode.node);
			}
		}
		if (totalActiveSize > 0) {
			this.activeLeafByteSizeEstimate = (double) totalActiveSize / this.activeLeafNodeCount;
		}
		if (totalInactiveSize > 0) {
			this.inactiveLeafByteSizeEstimate = (double) totalInactiveSize / this.inactiveLeafNodeCount;
		}
		long actualModelSize = this.measureByteSize();
		double estimatedModelSize = (this.activeLeafNodeCount * this.activeLeafByteSizeEstimate
				+ this.inactiveLeafNodeCount * this.inactiveLeafByteSizeEstimate);
		this.byteSizeEstimateOverheadFraction = actualModelSize / estimatedModelSize;
		if (actualModelSize > this.maxByteSizeOption.getValue()) {
			enforceTrackerLimit();
		}
	}

	public void deactivateAllLeaves() {
		FoundNode[] learningNodes = findLearningNodes();
		for (int i = 0; i < learningNodes.length; i++) {
			if (learningNodes[i].node instanceof ActiveLearningNode) {
				deactivateLearningNode((ActiveLearningNode) learningNodes[i].node, learningNodes[i].parent,
						learningNodes[i].parentBranch);
			}
		}
	}

	public void enforceTrackerLimit() {
		if ((this.inactiveLeafNodeCount > 0) || ((this.activeLeafNodeCount * this.activeLeafByteSizeEstimate
				+ this.inactiveLeafNodeCount * this.inactiveLeafByteSizeEstimate)
				* this.byteSizeEstimateOverheadFraction > this.maxByteSizeOption.getValue())) {
			if (this.stopMemManagementOption.isSet()) {
				this.growthAllowed = false;
				return;
			}
			FoundNode[] learningNodes = findLearningNodes();
			Arrays.sort(learningNodes, new Comparator<FoundNode>() {

				@Override
				public int compare(FoundNode fn1, FoundNode fn2) {
					return Double.compare(fn1.node.calculatePromise(), fn2.node.calculatePromise());
				}
			});
			int maxActive = 0;
			while (maxActive < learningNodes.length) {
				maxActive++;
				if ((maxActive * this.activeLeafByteSizeEstimate
						+ (learningNodes.length - maxActive) * this.inactiveLeafByteSizeEstimate)
						* this.byteSizeEstimateOverheadFraction > this.maxByteSizeOption.getValue()) {
					maxActive--;
					break;
				}
			}
			int cutoff = learningNodes.length - maxActive;
			for (int i = 0; i < cutoff; i++) {
				if (learningNodes[i].node instanceof ActiveLearningNode) {
					deactivateLearningNode((ActiveLearningNode) learningNodes[i].node, learningNodes[i].parent,
							learningNodes[i].parentBranch);
				}
			}
			for (int i = cutoff; i < learningNodes.length; i++) {
				if (learningNodes[i].node instanceof InactiveLearningNode) {
					activateLearningNode((InactiveLearningNode) learningNodes[i].node, learningNodes[i].parent,
							learningNodes[i].parentBranch);
				}
			}
		}
	}

	protected void activateLearningNode(InactiveLearningNode toActivate, SplitNode parent, int parentBranch) {
		Node newLeaf = newLearningNode(toActivate.getObservedClassDistribution());
		if (parent == null) {
			this.treeRoot = newLeaf;
		} else {
			parent.setChild(parentBranch, newLeaf);
		}
		this.activeLeafNodeCount++;
		this.inactiveLeafNodeCount--;
	}

	protected FoundNode[] findLearningNodes() {
		List<FoundNode> foundList = new LinkedList<>();
		findLearningNodes(this.treeRoot, null, -1, foundList);
		return foundList.toArray(new FoundNode[foundList.size()]);
	}

	protected void findLearningNodes(Node node, SplitNode parent, int parentBranch, List<FoundNode> found) {
		if (node != null) {
			if (node instanceof LearningNode) {
				found.add(new FoundNode(node, parent, parentBranch));
			}
			if (node instanceof SplitNode) {
				SplitNode splitNode = (SplitNode) node;
				for (int i = 0; i < splitNode.numChildren(); i++) {
					findLearningNodes(splitNode.getChild(i), splitNode, i, found);
				}
			}
		}
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
			if (this.growthAllowed && (learningNode instanceof ActiveLearningNode)) {
				ActiveLearningNode activeLearningNode = (ActiveLearningNode) learningNode;
				double weightSeen = activeLearningNode.getWeightSeen();
				if (weightSeen - activeLearningNode.getWeightSeenAtLastSplitEvaluation() >= this.gracePeriodOption
						.getValue()) {
					attemptToSplit(activeLearningNode, foundNode.parent, foundNode.parentBranch);
					activeLearningNode.setWeightSeenAtLastSplitEvaluation(weightSeen);
				}
			}
		}
		if (this.trainingWeightSeenByModel % this.memoryEstimatePeriodOption.getValue() == 0) {
			estimateModelByteSizes();
		}
	}

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
		if (this.leafpredictionOption.getChosenIndex() > 0) {
			this.removePoorAttsOption = null;
		}
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[] {
				new Measurement("tree size (nodes)",
						this.decisionNodeCount + this.activeLeafNodeCount + this.inactiveLeafNodeCount),
				new Measurement("tree size (leaves)", this.activeLeafNodeCount + this.inactiveLeafNodeCount),
				new Measurement("active learning leaves", this.activeLeafNodeCount),
				new Measurement("tree depth", measureTreeDepth()),
				new Measurement("active leaf byte size estimate", this.activeLeafByteSizeEstimate),
				new Measurement("inactive leaf byte size estimate", this.inactiveLeafByteSizeEstimate),
				new Measurement("byte size estimate overhead", this.byteSizeEstimateOverheadFraction) };
	}

	public int measureTreeDepth() {
		if (this.treeRoot != null) {
			return this.treeRoot.subtreeDepth();
		}
		return 0;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		this.treeRoot.describeSubtree(this, out, indent);
	}

	protected void attemptToSplit(ActiveLearningNode node, SplitNode parent, int parentIndex) {
		if (!node.observedClassDistributionIsPure()) {
			SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);
			AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
			Arrays.sort(bestSplitSuggestions);
			boolean shouldSplit = false;
			if (bestSplitSuggestions.length < 2) {
				shouldSplit = bestSplitSuggestions.length > 0;
			} else {
				double hoeffdingBound = computeHoeffdingBound(
						splitCriterion.getRangeOfMerit(node.getObservedClassDistribution()),
						this.splitConfidenceOption.getValue(), node.getWeightSeen());
				AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
				AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];
				if ((bestSuggestion.merit - secondBestSuggestion.merit > hoeffdingBound)
						|| (hoeffdingBound < this.tieThresholdOption.getValue())) {
					shouldSplit = true;
				}
				// }
				if ((this.removePoorAttsOption != null) && this.removePoorAttsOption.isSet()) {
					Set<Integer> poorAtts = new HashSet<>();
					// scan 1 - add any poor to set
					for (int i = 0; i < bestSplitSuggestions.length; i++) {
						if (bestSplitSuggestions[i].splitTest != null) {
							int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
							if (splitAtts.length == 1) {
								if (bestSuggestion.merit - bestSplitSuggestions[i].merit > hoeffdingBound) {
									poorAtts.add(new Integer(splitAtts[0]));
								}
							}
						}
					}
					// scan 2 - remove good ones from set
					for (int i = 0; i < bestSplitSuggestions.length; i++) {
						if (bestSplitSuggestions[i].splitTest != null) {
							int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
							if (splitAtts.length == 1) {
								if (bestSuggestion.merit - bestSplitSuggestions[i].merit < hoeffdingBound) {
									poorAtts.remove(new Integer(splitAtts[0]));
								}
							}
						}
					}
					for (int poorAtt : poorAtts) {
						node.disableAttribute(poorAtt);
					}
				}
			}
			if (shouldSplit) {
				AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
				if (splitDecision.splitTest == null) {
					// preprune - null wins
					deactivateLearningNode(node, parent, parentIndex);
				} else {
					SplitNode newSplit = newSplitNode(splitDecision.splitTest, node.getObservedClassDistribution(),
							splitDecision.numSplits());
					for (int i = 0; i < splitDecision.numSplits(); i++) {
						Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i));
						newSplit.setChild(i, newChild);
					}
					this.activeLeafNodeCount--;
					this.decisionNodeCount++;
					this.activeLeafNodeCount += splitDecision.numSplits();
					if (parent == null) {
						this.treeRoot = newSplit;
					} else {
						parent.setChild(parentIndex, newSplit);
					}
				}
				// manage memory
				enforceTrackerLimit();
			}
		}
	}

	// Procedure added for Hoeffding Adaptive Trees (ADWIN)
	protected SplitNode newSplitNode(InstanceConditionalTest splitTest, double[] classObservations, int size) {
		return new SplitNode(splitTest, classObservations, size);
	}

	protected LearningNode newLearningNode() {
		return newLearningNode(new double[0]);
	}

	public static double computeHoeffdingBound(double range, double confidence, double n) {
		return Math.sqrt(((range * range) * Math.log(1.0 / confidence)) / (2.0 * n));
	}

}
