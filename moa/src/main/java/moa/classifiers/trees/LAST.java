package moa.classifiers.trees;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.AbstractMOAObject;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.DiscreteAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NullAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.driftdetection.ADWINChangeDetector;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.core.splitcriteria.GiniSplitCriterion;
import moa.classifiers.core.splitcriteria.InfoGainSplitCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.core.Utils;
import moa.options.ClassOption;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Local Adaptive Streaming Tree
 *	
 *	Local Adaptive Streaming Tree (LAST) is an incremental decision tree with
 * 	adaptive splitting mechanisms. LAST maintains a change detector at each leaf and splits
 * 	this node if a change is detected in the error or the leaf`s data distribution.
 * 
 * 	LAST is still not suitable for use as a base classifier in ensembles due to the change detectors.
 * 	The authors of the paper are working on a version of LAST that overcomes this limitation.
 * 
 *  <p>D. N. Assis, J. P. Barddal, and F. Enembreck. 2024. 
 *  Just Change on Change: Adaptive Splitting Time for Decision Trees in Data Stream Classification.
 *  In Proceedings of the 39th ACM/SIGAPP Symposium on Applied Computing (SAC '24).</p>
 *	
 * <p>Parameters:</p> <ul> <li> -m : Maximum memory consumed by the tree</li>
 * <li> -n : Numeric estimator to use : <ul> <li>Gaussian approximation
 * evaluating 10 splitpoints</li> <li>Gaussian approximation evaluating 100
 * splitpoints</li> <li>Greenwald-Khanna quantile summary with 10 tuples</li>
 * <li>Greenwald-Khanna quantile summary with 100 tuples</li>
 * <li>Greenwald-Khanna quantile summary with 1000 tuples</li> <li>VFML method
 * with 10 bins</li> <li>VFML method with 100 bins</li> <li>VFML method with
 * 1000 bins</li> <li>Exhaustive binary tree</li> </ul> </li> <li> -e : How many
 * instances between memory consumption checks</li> <li> -g : The number of
 * instances a leaf should observe between split attempts</li> <li> -s : Split
 * criterion to use. Example : InfoGainSplitCriterion</li> <li> -c : The
 * allowable error in split decision, values closer to 0 will take longer to
 * decide</li> <li> -t : Threshold below which a split will be forced to break
 * ties</li> <li> -b : Only allow binary splits</li> <li> -z : Stop growing as
 * soon as memory limit is hit</li> <li> -r : Disable poor attributes</li> <li>
 * -p : Disable pre-pruning</li> 
 * <li> -x : Change detector that determines the split point </li>
 * <li> -D : activates distribution purity monitoring by the change detector </li>
 * <li> -l : Leaf prediction to use: MajorityClass (MC), Naive Bayes (NB) or NaiveBayes
 * adaptive (NBAdaptive).</li>
 * <li> -q : The number of instances a leaf should observe before
 * permitting Naive Bayes</li> 
 * 
 * </ul>
 *
 * @author Daniel Nowak Assis (daniel.nowak.assis@gmail.com)
 * @version $Revision: 1 $
 */
public class LAST extends AbstractClassifier implements MultiClassClassifier,
CapabilitiesHandler {

	private static final long serialVersionUID = 1L;

	@Override
	public String getPurposeString() {
		return "Local Adaptive Streaming Tree (LAST)";
	}

	public IntOption maxByteSizeOption = new IntOption("maxByteSize", 'm',
			"Maximum memory consumed by the tree.", 33554432, 0,
			Integer.MAX_VALUE);

	/*
	 * public MultiChoiceOption numericEstimatorOption = new MultiChoiceOption(
	 * "numericEstimator", 'n', "Numeric estimator to use.", new String[]{
	 * "GAUSS10", "GAUSS100", "GK10", "GK100", "GK1000", "VFML10", "VFML100",
	 * "VFML1000", "BINTREE"}, new String[]{ "Gaussian approximation evaluating
	 * 10 splitpoints", "Gaussian approximation evaluating 100 splitpoints",
	 * "Greenwald-Khanna quantile summary with 10 tuples", "Greenwald-Khanna
	 * quantile summary with 100 tuples", "Greenwald-Khanna quantile summary
	 * with 1000 tuples", "VFML method with 10 bins", "VFML method with 100
	 * bins", "VFML method with 1000 bins", "Exhaustive binary tree"}, 0);
	 */
	public ClassOption numericEstimatorOption = new ClassOption("numericEstimator",
			'n', "Numeric estimator to use.", NumericAttributeClassObserver.class,
			"GaussianNumericAttributeClassObserver");

	public ClassOption nominalEstimatorOption = new ClassOption("nominalEstimator",
			'd', "Nominal estimator to use.", DiscreteAttributeClassObserver.class,
			"NominalAttributeClassObserver");

	public IntOption memoryEstimatePeriodOption = new IntOption(
			"memoryEstimatePeriod", 'e',
			"How many instances between memory consumption checks.", 1000000,
			0, Integer.MAX_VALUE);


	public ClassOption splitCriterionOption = new ClassOption("splitCriterion",
			's', "Split criterion to use.", SplitCriterion.class,
			"InfoGainSplitCriterion");

	public FlagOption binarySplitsOption = new FlagOption("binarySplits", 'b',
			"Only allow binary splits.");

	public FlagOption stopMemManagementOption = new FlagOption(
			"stopMemManagement", 'z',
			"Stop growing as soon as memory limit is hit.");

	public FlagOption removePoorAttsOption = new FlagOption("removePoorAtts",
			'r', "Disable poor attributes.");

	public FlagOption noPrePruneOption = new FlagOption("noPrePrune", 'p',
			"Disable pre-pruning.");

	public ClassOption changeDetectionMethodOption = new ClassOption("changeDetectionMethodOption", 'x',
			"Change detector for determining split points. If a change is detected in the leaf node, a split will happen", ChangeDetector.class, "ADWINChangeDetector");
	
	public FlagOption distributionOption = new FlagOption("distributionOption", 'D',
			"Monitor distribution purity instead of error rate");

	


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

		public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent,
				int parentBranch) {
			return new FoundNode(this, parent, parentBranch);
		}

		public double[] getObservedClassDistribution() {
			return this.observedClassDistribution.getArrayCopy();
		}

		public double[] getObservedClassDistributionAtLeavesReachableThroughThisNode() {
			return this.observedClassDistribution.getArrayCopy();
		}

		public double[] getClassVotes(Instance inst, LAST ht) {
			return this.observedClassDistribution.getArrayCopy();
		}

		public boolean observedClassDistributionIsPure() {
			return this.observedClassDistribution.numNonZeroEntries() < 2;
		}

		public void describeSubtree(LAST ht, StringBuilder out,
				int indent) {
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
			return totalSeen > 0.0 ? (totalSeen - this.observedClassDistribution.getValue(this.observedClassDistribution.maxIndex()))
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
			return super.calcByteSize()
					+ (int) (SizeOf.sizeOf(this.children) + SizeOf.fullSizeOf(this.splitTest));
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

		@Override
		public double[] getObservedClassDistributionAtLeavesReachableThroughThisNode() {
			// Start a new DoubleVector with 0 in all positions.
			DoubleVector sumObservedClassDistributionAtLeaves =
					new DoubleVector(new double[this.getObservedClassDistribution().length]);

			for(Node childNode : this.children) {
				if(childNode != null) {
					double[] childDist = childNode.getObservedClassDistributionAtLeavesReachableThroughThisNode();
					sumObservedClassDistributionAtLeaves.addValues(childDist);
				}
			}
			return sumObservedClassDistributionAtLeaves.getArrayCopy();
		}

		public AutoExpandVector<Node> getChildren() {
			return children;
		}

		public InstanceConditionalTest getSplitTest() {
			return splitTest;
		}

		public SplitNode(InstanceConditionalTest splitTest,
				double[] classObservations, int size) {
			super(classObservations);
			this.splitTest = splitTest;
			this.children = new AutoExpandVector<Node>(size);
		}

		public SplitNode(InstanceConditionalTest splitTest,
				double[] classObservations) {
			super(classObservations);
			this.splitTest = splitTest;
			this.children = new AutoExpandVector<Node>();
		}


		public int numChildren() {
			return this.children.size();
		}

		public void setChild(int index, Node child) {
			if ((this.splitTest.maxBranches() >= 0)
					&& (index >= this.splitTest.maxBranches())) {
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
		public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent,
				int parentBranch) {
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
		public void describeSubtree(LAST ht, StringBuilder out,
				int indent) {
			for (int branch = 0; branch < numChildren(); branch++) {
				Node child = getChild(branch);
				if (child != null) {
					StringUtils.appendIndented(out, indent, "if ");
					out.append(this.splitTest.describeConditionForBranch(branch,
							ht.getModelContext()));
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

		public abstract void learnFromInstance(Instance inst, LAST ht);
	}

	public static class InactiveLearningNode extends LearningNode {

		private static final long serialVersionUID = 1L;

		public InactiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		@Override
		public void learnFromInstance(Instance inst, LAST ht) {
			this.observedClassDistribution.addToValue((int) inst.classValue(),
					inst.weight());
		}
	}

	public static class ActiveLearningNode extends LearningNode {

		ChangeDetector detector;

		boolean monitorDistributionPurity;

		private static final long serialVersionUID = 1L;

		protected double weightSeenAtLastSplitEvaluation;

		protected AutoExpandVector<AttributeClassObserver> attributeObservers = new AutoExpandVector<AttributeClassObserver>();

		protected boolean isInitialized;

		public ActiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);
			this.weightSeenAtLastSplitEvaluation = getWeightSeen();
			this.isInitialized = false;
			this.detector = new ADWINChangeDetector();
		}

		public ActiveLearningNode(double[] initialClassObservations, ChangeDetector detector, boolean monitorDistributionPurity) {
			super(initialClassObservations);
			this.weightSeenAtLastSplitEvaluation = getWeightSeen();
			this.isInitialized = false;
			this.detector = detector;
			this.monitorDistributionPurity = monitorDistributionPurity;
		}

		@Override
		public int calcByteSize() {
			return super.calcByteSize()
					+ (int) (SizeOf.fullSizeOf(this.attributeObservers));
		}

		@Override
		public void learnFromInstance(Instance inst, LAST ht) {
			int trueClass = (int) inst.classValue();
			int prediction = Utils.maxIndex(this.getClassVotes(inst, ht));
			this.detector.input(prediction == trueClass ? 0 : 1);
			this.updateAttributeClass(inst, ht);
		}
		
		public void updateAttributeClass(Instance inst, LAST ht) {
			if (this.isInitialized == false) {
				this.attributeObservers = new AutoExpandVector<AttributeClassObserver>(inst.numAttributes());
				this.isInitialized = true;
			}
			this.observedClassDistribution.addToValue((int) inst.classValue(),
					inst.weight());
			for (int i = 0; i < inst.numAttributes() - 1; i++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
				AttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs == null) {
					obs = inst.attribute(instAttIndex).isNominal() ? ht.newNominalClassObserver() : ht.newNumericClassObserver();
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

		public AttributeSplitSuggestion[] getBestSplitSuggestions(
				SplitCriterion criterion, LAST ht) {
			List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();
			double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
			if (!ht.noPrePruneOption.isSet()) {
				// add null split as an option
				bestSuggestions.add(new AttributeSplitSuggestion(null,
						new double[0][], criterion.getMeritOfSplit(
								preSplitDist,
								new double[][]{preSplitDist})));
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
			this.attributeObservers.set(attIndex,
					new NullAttributeClassObserver());
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

    protected long weightSeen; 

	public int calcByteSize() {
		int size = (int) SizeOf.sizeOf(this);
		if (this.treeRoot != null) {
			size += this.treeRoot.calcByteSizeIncludingSubtree();
		}
		return size;
	}

	public int getNodeCount() {
		return this.decisionNodeCount + this.activeLeafNodeCount + this.inactiveLeafNodeCount;
	}

	public Node getTreeRoot() {
		return this.treeRoot;
	}

	@Override
	public long measureByteSize() {
		return calcByteSize();
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
		if (this.leafpredictionOption.getChosenIndex()>0) { 
			this.removePoorAttsOption = null;
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		this.weightSeen += inst.weight();
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

				if (activeLearningNode.detector.getChange()) {
					attemptToSplit(activeLearningNode, foundNode.parent,
							foundNode.parentBranch);
					activeLearningNode.setWeightSeenAtLastSplitEvaluation(weightSeen);
				}
			}
		}
		if (this.trainingWeightSeenByModel
				% this.memoryEstimatePeriodOption.getValue() == 0) {
			estimateModelByteSizes();
		}
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		if (this.treeRoot != null) {
			FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst,
					null, -1);
			Node leafNode = foundNode.node;
			if (leafNode == null) {
				leafNode = foundNode.parent;
			}
			return leafNode.getClassVotes(inst, this);
		} else {
			int numClasses = inst.dataset().numClasses();
			return new double[numClasses];
		}
	}

	@Override
    protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{
                new Measurement("tree size (nodes)", this.decisionNodeCount
                + this.activeLeafNodeCount + this.inactiveLeafNodeCount),
                new Measurement("tree size (leaves)", this.activeLeafNodeCount
                + this.inactiveLeafNodeCount),
                new Measurement("active learning leaves",
                this.activeLeafNodeCount),
                new Measurement("tree depth", measureTreeDepth()),
                new Measurement("active leaf byte size estimate",
                this.activeLeafByteSizeEstimate),
                new Measurement("inactive leaf byte size estimate",
                this.inactiveLeafByteSizeEstimate),
                new Measurement("byte size estimate overhead",
                this.byteSizeEstimateOverheadFraction)};        	
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

	@Override
	public boolean isRandomizable() {
		return false;
	}

	//Procedure added for Hoeffding Adaptive Trees (ADWIN)
	protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
			double[] classObservations, int size) {
		return new SplitNode(splitTest, classObservations, size);
	}

	protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
			double[] classObservations) {
		return new SplitNode(splitTest, classObservations);
	}


	protected AttributeClassObserver newNominalClassObserver() {
		AttributeClassObserver nominalClassObserver = (AttributeClassObserver) getPreparedClassOption(this.nominalEstimatorOption);
		return (AttributeClassObserver) nominalClassObserver.copy();
	}

	protected AttributeClassObserver newNumericClassObserver() {
		AttributeClassObserver numericClassObserver = (AttributeClassObserver) getPreparedClassOption(this.numericEstimatorOption);
		return (AttributeClassObserver) numericClassObserver.copy();
	}
	
	
	
	protected void attemptToSplit(ActiveLearningNode node, SplitNode parent,
			int parentIndex) {
		SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);
		AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
		Arrays.sort(bestSplitSuggestions);
		boolean shouldSplit = false;
		if (bestSplitSuggestions.length < 2) {
			shouldSplit = bestSplitSuggestions.length > 0;
		} else {
			AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			if (bestSuggestion.merit > 0.0) {
				shouldSplit = true;
			}
			if ((this.removePoorAttsOption != null)
					&& this.removePoorAttsOption.isSet()) {
				Set<Integer> poorAtts = new HashSet<Integer>();
				// scan 1 - add any poor to set
				for (int i = 0; i < bestSplitSuggestions.length; i++) {
					if (bestSplitSuggestions[i].splitTest != null) {
						int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
						if (splitAtts.length == 1) {
							poorAtts.add(new Integer(splitAtts[0]));
						}
					}
				}
				// scan 2 - remove good ones from set
				for (int i = 0; i < bestSplitSuggestions.length; i++) {
					if (bestSplitSuggestions[i].splitTest != null) {
						int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
						if (splitAtts.length == 1) {
							poorAtts.remove(new Integer(splitAtts[0]));
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
				SplitNode newSplit = newSplitNode(splitDecision.splitTest,
						node.getObservedClassDistribution(),splitDecision.numSplits() );
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

	public void enforceTrackerLimit() {
		if ((this.inactiveLeafNodeCount > 0)
				|| ((this.activeLeafNodeCount * this.activeLeafByteSizeEstimate + this.inactiveLeafNodeCount
						* this.inactiveLeafByteSizeEstimate)
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
				if ((maxActive * this.activeLeafByteSizeEstimate + (learningNodes.length - maxActive)
						* this.inactiveLeafByteSizeEstimate)
						* this.byteSizeEstimateOverheadFraction > this.maxByteSizeOption.getValue()) {
					maxActive--;
					break;
				}
			}
			int cutoff = learningNodes.length - maxActive;
			for (int i = 0; i < cutoff; i++) {
				if (learningNodes[i].node instanceof ActiveLearningNode) {
					deactivateLearningNode(
							(ActiveLearningNode) learningNodes[i].node,
							learningNodes[i].parent,
							learningNodes[i].parentBranch);
				}
			}
			for (int i = cutoff; i < learningNodes.length; i++) {
				if (learningNodes[i].node instanceof InactiveLearningNode) {
					activateLearningNode(
							(InactiveLearningNode) learningNodes[i].node,
							learningNodes[i].parent,
							learningNodes[i].parentBranch);
				}
			}
		}
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
			this.activeLeafByteSizeEstimate = (double) totalActiveSize
					/ this.activeLeafNodeCount;
		}
		if (totalInactiveSize > 0) {
			this.inactiveLeafByteSizeEstimate = (double) totalInactiveSize
					/ this.inactiveLeafNodeCount;
		}
		long actualModelSize = this.measureByteSize();
		double estimatedModelSize = (this.activeLeafNodeCount
				* this.activeLeafByteSizeEstimate + this.inactiveLeafNodeCount
				* this.inactiveLeafByteSizeEstimate);
		this.byteSizeEstimateOverheadFraction = actualModelSize
				/ estimatedModelSize;
		if (actualModelSize > this.maxByteSizeOption.getValue()) {
			enforceTrackerLimit();
		}
	}

	public void deactivateAllLeaves() {
		FoundNode[] learningNodes = findLearningNodes();
		for (int i = 0; i < learningNodes.length; i++) {
			if (learningNodes[i].node instanceof ActiveLearningNode) {
				deactivateLearningNode(
						(ActiveLearningNode) learningNodes[i].node,
						learningNodes[i].parent, learningNodes[i].parentBranch);
			}
		}
	}

	protected void deactivateLearningNode(ActiveLearningNode toDeactivate,
			SplitNode parent, int parentBranch) {
		Node newLeaf = new InactiveLearningNode(toDeactivate.getObservedClassDistribution());
		if (parent == null) {
			this.treeRoot = newLeaf;
		} else {
			parent.setChild(parentBranch, newLeaf);
		}
		this.activeLeafNodeCount--;
		this.inactiveLeafNodeCount++;
	}

	protected void activateLearningNode(InactiveLearningNode toActivate,
			SplitNode parent, int parentBranch) {
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
		List<FoundNode> foundList = new LinkedList<FoundNode>();
		findLearningNodes(this.treeRoot, null, -1, foundList);
		return foundList.toArray(new FoundNode[foundList.size()]);
	}

	protected void findLearningNodes(Node node, SplitNode parent,
			int parentBranch, List<FoundNode> found) {
		if (node != null) {
			if (node instanceof LearningNode) {
				found.add(new FoundNode(node, parent, parentBranch));
			}
			if (node instanceof SplitNode) {
				SplitNode splitNode = (SplitNode) node;
				for (int i = 0; i < splitNode.numChildren(); i++) {
					findLearningNodes(splitNode.getChild(i), splitNode, i,
							found);
				}
			}
		}
	}

	public MultiChoiceOption leafpredictionOption = new MultiChoiceOption(
			"leafprediction", 'l', "Leaf prediction to use.", new String[]{
					"MC", "NB", "NBAdaptive"}, new String[]{
							"Majority class",
							"Naive Bayes",
			"Naive Bayes Adaptive"}, 2);

	public IntOption nbThresholdOption = new IntOption(
			"nbThreshold",
			'q',
			"The number of instances a leaf should observe before permitting Naive Bayes.",
			0, 0, Integer.MAX_VALUE);

	public static class LearningNodeNB extends ActiveLearningNode {

		private static final long serialVersionUID = 1L;

		public LearningNodeNB(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		public LearningNodeNB(double[] initialClassObservations, ChangeDetector detector, boolean monitorDistributionPurity) {
			super(initialClassObservations, detector, monitorDistributionPurity);
		}

		@Override
		public double[] getClassVotes(Instance inst, LAST ht) {
			if (getWeightSeen() >= ht.nbThresholdOption.getValue()) {
				return NaiveBayes.doNaiveBayesPrediction(inst,
						this.observedClassDistribution,
						this.attributeObservers);
			}
			return super.getClassVotes(inst, ht);
		}

		@Override
		public void disableAttribute(int attIndex) {
			// should not disable poor atts - they are used in NB calc
		}
	}

	public static class LearningNodeNBAdaptive extends LearningNodeNB {

		private static final long serialVersionUID = 1L;

		protected double mcCorrectWeight = 0.0;

		protected double nbCorrectWeight = 0.0;

		public LearningNodeNBAdaptive(double[] initialClassObservations) {
			super(initialClassObservations);
		}

		public LearningNodeNBAdaptive(double[] initialClassObservations, ChangeDetector detector, boolean monitorDistributionPurity) {
			super(initialClassObservations, detector, monitorDistributionPurity);
		}

		@Override
		public void learnFromInstance(Instance inst, LAST ht) {
			int trueClass = (int) inst.classValue();
			double mcRight = 1.0;
			double nbRight = 1.0;
			if (this.observedClassDistribution.maxIndex() == trueClass) {
				this.mcCorrectWeight += inst.weight();
				mcRight = 0.0;
			}
			if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
					this.observedClassDistribution, this.attributeObservers)) == trueClass) {
				this.nbCorrectWeight += inst.weight();
				nbRight = 0.0;
			}
			if (!this.monitorDistributionPurity) {
				if (this.mcCorrectWeight > this.nbCorrectWeight) {
					this.detector.input(mcRight);
				}else {
					this.detector.input(nbRight);
				}
			}else {
				double purity = 0.0;
				if(ht.splitCriterionOption.getValueAsCLIString().equals("InfoGainSplitCriterion")) {
					purity = InfoGainSplitCriterion.computeEntropy(this.observedClassDistribution.getArrayRef());
				}else {
					purity = GiniSplitCriterion.computeGini(this.observedClassDistribution.getArrayRef());
				}
				this.detector.input(purity); 
			}
			this.updateAttributeClass(inst, ht);
		}

		@Override
		public double[] getClassVotes(Instance inst, LAST ht) {
			if (this.mcCorrectWeight > this.nbCorrectWeight) {
				return this.observedClassDistribution.getArrayCopy();
			}
			return NaiveBayes.doNaiveBayesPrediction(inst,
					this.observedClassDistribution, this.attributeObservers);
		}
	}

	protected LearningNode newLearningNode() {
		return newLearningNode(new double[0]);
	}

	protected LearningNode newLearningNode(double[] initialClassObservations) {
		LearningNode ret;
		int predictionOption = this.leafpredictionOption.getChosenIndex();
		if (predictionOption == 0) { //MC
			ret = new ActiveLearningNode(initialClassObservations,((ChangeDetector) getPreparedClassOption(this.changeDetectionMethodOption)).copy() , this.distributionOption.isSet());
		} else if (predictionOption == 1) { //NB
			ret = new LearningNodeNB(initialClassObservations,((ChangeDetector) getPreparedClassOption(this.changeDetectionMethodOption)).copy() , this.distributionOption.isSet());
		} else { //NBAdaptive
			ret = new LearningNodeNBAdaptive(initialClassObservations,((ChangeDetector) getPreparedClassOption(this.changeDetectionMethodOption)).copy() , this.distributionOption.isSet());
		}
		return ret;
	}

	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		if (this.getClass() == LAST.class)
			return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
		else
			return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
}