/*
 *    RandomHoeffdingTree.java
 *    Copyright (C) 2019 University of Waikato, Hamilton, New Zealand
 *    @author Chaitanya Manapragada
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

/*
 * Author: Chaitanya Manapragada.
 * <p>
 * Based on HoeffdingTree.java by Richard Kirkby.
 * <p>
 * There is a lot of code repetition from VFDT.java. This needs to be fixed as per DRY principles.
 * <p>
 * Research code written to test the EFDT idea.
 * <p>
 * Bug-fixed version of Richard Kirkby's  HoeffdingTree.Java written by Chaitanya Manapragada.
 * <p>
 * Bug1 : average delta G's are computed where specified in the Domingos/Hulten paper algorithm listing. This was a bug in the HoeffdingTree.java implementation- fixed here.
 * <p>
 * Bug2: splitting was occurring when the top attribute had no IG, and splitting was occurring on the same attribute. This was also fixed.
 * <p>
 * The correct reference is:
 * Domingos, P., & Hulten, G. (2000, August). Mining high-speed data streams. In Proceedings of the sixth ACM SIGKDD international conference on Knowledge discovery and data mining (pp. 71-80). ACM.
 */

package moa.classifiers.trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.AbstractMOAObject;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.DiscreteAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NullAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;
import moa.core.Utils;
import moa.options.ClassOption;

public class EFDT extends AbstractClassifier implements MultiClassClassifier {

  private static final long serialVersionUID = 2L;

  public IntOption reEvalPeriodOption = new IntOption(
    "reevaluationPeriod",
    'R',
    "The number of instances an internal node should observe between re-evaluation attempts.",
    2000, 0, Integer.MAX_VALUE);

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

  public IntOption gracePeriodOption = new IntOption(
    "gracePeriod",
    'g',
    "The number of instances a leaf should observe between split attempts.",
    200, 0, Integer.MAX_VALUE);

  public ClassOption splitCriterionOption = new ClassOption("splitCriterion",
    's', "Split criterion to use.", SplitCriterion.class,
    "InfoGainSplitCriterion");

  public FloatOption splitConfidenceOption = new FloatOption(
    "splitConfidence",
    'c',
    "The allowable error in split decision, values closer to 0 will take longer to decide.",
    0.0000001, 0.0, 1.0);

  public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
    't', "Threshold below which a split will be forced to break ties.",
    0.05, 0.0, 1.0);

  public FlagOption binarySplitsOption = new FlagOption("binarySplits", 'b',
    "Only allow binary splits.");

  public FlagOption stopMemManagementOption = new FlagOption(
    "stopMemManagement", 'z',
    "Stop growing as soon as memory limit is hit.");

  public FlagOption removePoorAttsOption = new FlagOption("removePoorAtts",
    'r', "Disable poor attributes.");

  public FlagOption noPrePruneOption = new FlagOption("noPrePrune", 'p',
    "Disable pre-pruning.");

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

  protected Node treeRoot = null;

  protected int decisionNodeCount;

  protected int activeLeafNodeCount;

  protected int inactiveLeafNodeCount;

  protected double inactiveLeafByteSizeEstimate;

  protected double activeLeafByteSizeEstimate;

  protected double byteSizeEstimateOverheadFraction;

  protected boolean growthAllowed;

  protected int numInstances = 0;

  protected int splitCount = 0;

  @Override
  public String getPurposeString() {
    return "Hoeffding Tree or VFDT.";
  }

  public int calcByteSize() {
    int size = (int) SizeOf.sizeOf(this);
    if (this.treeRoot != null) {
      size += this.treeRoot.calcByteSizeIncludingSubtree();
    }
    return size;
  }

  @Override
  public int measureByteSize() {
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
    if (this.leafpredictionOption.getChosenIndex() > 0) {
      this.removePoorAttsOption = null;
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
    }
    else {
      int numClasses = inst.dataset().numClasses();
      return new double[numClasses];
    }
  }

  @Override
  protected Measurement[] getModelMeasurementsImpl() {
    FoundNode[] learningNodes = findLearningNodes();

    return new Measurement[]{

      new Measurement("tree size (nodes)", this.decisionNodeCount
	+ this.activeLeafNodeCount + this.inactiveLeafNodeCount),
      new Measurement("tree size (leaves)", learningNodes.length),
      new Measurement("active learning leaves",
	this.activeLeafNodeCount),
      new Measurement("tree depth", measureTreeDepth()),
      new Measurement("active leaf byte size estimate",
	this.activeLeafByteSizeEstimate),
      new Measurement("inactive leaf byte size estimate",
	this.inactiveLeafByteSizeEstimate),
      new Measurement("byte size estimate overhead",
	this.byteSizeEstimateOverheadFraction),
      new Measurement("splits",
	this.splitCount)};
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

  public static double computeHoeffdingBound(double range, double confidence,
					     double n) {
    return Math.sqrt(((range * range) * Math.log(1.0 / confidence))
      / (2.0 * n));
  }

  protected AttributeClassObserver newNominalClassObserver() {
    AttributeClassObserver nominalClassObserver = (AttributeClassObserver) getPreparedClassOption(this.nominalEstimatorOption);
    return (AttributeClassObserver) nominalClassObserver.copy();
  }

  protected AttributeClassObserver newNumericClassObserver() {
    AttributeClassObserver numericClassObserver = (AttributeClassObserver) getPreparedClassOption(this.numericEstimatorOption);
    return (AttributeClassObserver) numericClassObserver.copy();
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
      }
      else {
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
    int actualModelSize = this.measureByteSize();
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
    for (FoundNode learningNode : learningNodes) {
      if (learningNode.node instanceof ActiveLearningNode) {
	deactivateLearningNode(
	  (ActiveLearningNode) learningNode.node,
	  learningNode.parent, learningNode.parentBranch);
      }
    }
  }

  protected void deactivateLearningNode(ActiveLearningNode toDeactivate,
					SplitNode parent, int parentBranch) {
    Node newLeaf = new InactiveLearningNode(toDeactivate.getObservedClassDistribution());
    if (parent == null) {
      this.treeRoot = newLeaf;
    }
    else {
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
    }
    else {
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

  protected void attemptToSplit(ActiveLearningNode node, SplitNode parent,
				int parentIndex) {

    if (!node.observedClassDistributionIsPure()) {
      node.addToSplitAttempts(1); // even if we don't actually attempt to split, we've computed infogains


      SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);
      AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
      Arrays.sort(bestSplitSuggestions);
      boolean shouldSplit = false;

      for (AttributeSplitSuggestion bestSplitSuggestion : bestSplitSuggestions) {

	if (bestSplitSuggestion.splitTest != null) {
	  if (!node.getInfogainSum().containsKey((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]))) {
	    node.getInfogainSum().put((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]), 0.0);
	  }
	  double currentSum = node.getInfogainSum().get((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]));
	  node.getInfogainSum().put((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]), currentSum + bestSplitSuggestion.merit);
	}

	else { // handle the null attribute
	  double currentSum = node.getInfogainSum().get(-1); // null split
	  node.getInfogainSum().put(-1, Math.max(0.0, currentSum + bestSplitSuggestion.merit));
	  assert node.getInfogainSum().get(-1) >= 0.0 : "Negative infogain shouldn't be possible here.";
	}

      }

      if (bestSplitSuggestions.length < 2) {
	shouldSplit = bestSplitSuggestions.length > 0;
      }

      else {
	double hoeffdingBound = computeHoeffdingBound(splitCriterion.getRangeOfMerit(node.getObservedClassDistribution()),
	  this.splitConfidenceOption.getValue(), node.getWeightSeen());
	AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];

	double bestSuggestionAverageMerit;
	double currentAverageMerit = node.getInfogainSum().get(-1) / node.getNumSplitAttempts();

	// because this is an unsplit leaf. current average merit should be always zero on the null split.

	if (bestSuggestion.splitTest == null) { // if you have a null split
	  bestSuggestionAverageMerit = node.getInfogainSum().get(-1) / node.getNumSplitAttempts();
	}
	else {
	  bestSuggestionAverageMerit = node.getInfogainSum().get((bestSuggestion.splitTest.getAttsTestDependsOn()[0])) / node.getNumSplitAttempts();
	}

	if (bestSuggestion.merit < 1e-10) {
	  shouldSplit = false; // we don't use average here
	}

	else if ((bestSuggestionAverageMerit - currentAverageMerit) >
	  hoeffdingBound
	  || (hoeffdingBound < this.tieThresholdOption.getValue())) {
	  if (bestSuggestionAverageMerit - currentAverageMerit < hoeffdingBound) {
	    // Placeholder to list this possibility
	  }
	  shouldSplit = true;
	}

	if (shouldSplit) {
	  for (Integer i : node.usedNominalAttributes) {
	    if (bestSuggestion.splitTest.getAttsTestDependsOn()[0] == i) {
	      shouldSplit = false;
	      break;
	    }
	  }
	}

	// }
	if ((this.removePoorAttsOption != null)
	  && this.removePoorAttsOption.isSet()) {
	  Set<Integer> poorAtts = new HashSet<>();
	  // scan 1 - add any poor to set
	  for (AttributeSplitSuggestion bestSplitSuggestion : bestSplitSuggestions) {
	    if (bestSplitSuggestion.splitTest != null) {
	      int[] splitAtts = bestSplitSuggestion.splitTest.getAttsTestDependsOn();
	      if (splitAtts.length == 1) {
		if (bestSuggestion.merit
		  - bestSplitSuggestion.merit > hoeffdingBound) {
		  poorAtts.add(splitAtts[0]);
		}
	      }
	    }
	  }
	  // scan 2 - remove good ones from set
	  for (AttributeSplitSuggestion bestSplitSuggestion : bestSplitSuggestions) {
	    if (bestSplitSuggestion.splitTest != null) {
	      int[] splitAtts = bestSplitSuggestion.splitTest.getAttsTestDependsOn();
	      if (splitAtts.length == 1) {
		if (bestSuggestion.merit
		  - bestSplitSuggestion.merit < hoeffdingBound) {
		  poorAtts.remove(splitAtts[0]);
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
	splitCount++;

	AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
	if (splitDecision.splitTest == null) {
	  // preprune - null wins
	  deactivateLearningNode(node, parent, parentIndex);
	}
	else {
	  Node newSplit = newSplitNode(splitDecision.splitTest,
	    node.getObservedClassDistribution(), splitDecision.numSplits());
	  ((EFDTSplitNode) newSplit).attributeObservers = node.attributeObservers; // copy the attribute observers
	  newSplit.setInfogainSum(node.getInfogainSum());  // transfer infogain history, leaf to split

	  for (int i = 0; i < splitDecision.numSplits(); i++) {

	    double[] j = splitDecision.resultingClassDistributionFromSplit(i);

	    Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i));

	    if (splitDecision.splitTest.getClass() == NominalAttributeBinaryTest.class
	      || splitDecision.splitTest.getClass() == NominalAttributeMultiwayTest.class) {
	      newChild.usedNominalAttributes = new ArrayList<>(node.usedNominalAttributes); //deep copy
	      newChild.usedNominalAttributes.add(splitDecision.splitTest.getAttsTestDependsOn()[0]);
	      // no  nominal attribute should be split on more than once in the path
	    }
	    ((EFDTSplitNode) newSplit).setChild(i, newChild);
	  }
	  this.activeLeafNodeCount--;
	  this.decisionNodeCount++;
	  this.activeLeafNodeCount += splitDecision.numSplits();
	  if (parent == null) {
	    this.treeRoot = newSplit;
	  }
	  else {
	    parent.setChild(parentIndex, newSplit);
	  }

	}
	// manage memory
	enforceTrackerLimit();
      }
    }
  }

  @Override
  public void trainOnInstanceImpl(Instance inst) {

    if (this.treeRoot == null) {
      this.treeRoot = newLearningNode();
      ((EFDTNode) this.treeRoot).setRoot(true);
      this.activeLeafNodeCount = 1;
    }

    FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
    Node leafNode = foundNode.node;

    if (leafNode == null) {
      leafNode = newLearningNode();
      foundNode.parent.setChild(foundNode.parentBranch, leafNode);
      this.activeLeafNodeCount++;
    }

    ((EFDTNode) this.treeRoot).learnFromInstance(inst, this, null, -1);

    numInstances++;
  }


  protected LearningNode newLearningNode() {
    return new EFDTLearningNode(new double[0]);
  }

  protected LearningNode newLearningNode(double[] initialClassObservations) {
    return new EFDTLearningNode(initialClassObservations);
  }

  protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
				   double[] classObservations, int size) {
    return new EFDTSplitNode(splitTest, classObservations, size);
  }

  protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
				   double[] classObservations) {
    return new EFDTSplitNode(splitTest, classObservations);
  }

  private int argmax(double[] array) {

    double max = array[0];
    int maxarg = 0;

    for (int i = 1; i < array.length; i++) {

      if (array[i] > max) {
	max = array[i];
	maxarg = i;
      }
    }
    return maxarg;
  }

  public interface EFDTNode {

    boolean isRoot();

    void setRoot(boolean isRoot);

    void learnFromInstance(Instance inst, EFDT ht, EFDTSplitNode parent, int parentBranch);

    void setParent(EFDTSplitNode parent);

    EFDTSplitNode getParent();

  }

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

    private HashMap<Integer, Double> infogainSum;

    private int numSplitAttempts = 0;

    private static final long serialVersionUID = 1L;

    protected DoubleVector observedClassDistribution;

    protected DoubleVector classDistributionAtTimeOfCreation;

    protected int nodeTime;

    protected List<Integer> usedNominalAttributes = new ArrayList<>();

    public Node(double[] classObservations) {
      this.observedClassDistribution = new DoubleVector(classObservations);
      this.classDistributionAtTimeOfCreation = new DoubleVector(classObservations);
      this.infogainSum = new HashMap<>();
      this.infogainSum.put(-1, 0.0); // Initialize for null split

    }

    public int getNumSplitAttempts() {
      return numSplitAttempts;
    }

    public void addToSplitAttempts(int i) {
      numSplitAttempts += i;
    }

    public HashMap<Integer, Double> getInfogainSum() {
      return infogainSum;
    }

    public void setInfogainSum(HashMap<Integer, Double> igs) {
      infogainSum = igs;
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

    public double[] getClassVotes(Instance inst, EFDT ht) {
      return this.observedClassDistribution.getArrayCopy();
    }

    public double[] getClassDistributionAtTimeOfCreation() {
      return this.classDistributionAtTimeOfCreation.getArrayCopy();
    }

    public boolean observedClassDistributionIsPure() {
      return this.observedClassDistribution.numNonZeroEntries() < 2;
    }

    public void describeSubtree(EFDT ht, StringBuilder out,
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

    public SplitNode(InstanceConditionalTest splitTest,
		     double[] classObservations, int size) {
      super(classObservations);
      this.splitTest = splitTest;
      this.children = new AutoExpandVector<>(size);
    }

    public SplitNode(InstanceConditionalTest splitTest,
		     double[] classObservations) {
      super(classObservations);
      this.splitTest = splitTest;
      this.children = new AutoExpandVector<>();
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

      //System.err.println("OVERRIDING ");

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
    public void describeSubtree(EFDT ht, StringBuilder out,
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


  public class EFDTSplitNode extends SplitNode implements EFDTNode {

    /**
     *
     */

    private boolean isRoot;

    private EFDTSplitNode parent = null;

    private static final long serialVersionUID = 1L;

    protected AutoExpandVector<AttributeClassObserver> attributeObservers;

    public EFDTSplitNode(InstanceConditionalTest splitTest, double[] classObservations, int size) {
      super(splitTest, classObservations, size);
    }

    public EFDTSplitNode(InstanceConditionalTest splitTest, double[] classObservations) {
      super(splitTest, classObservations);
    }

    @Override
    public boolean isRoot() {
      return isRoot;
    }

    @Override
    public void setRoot(boolean isRoot) {
      this.isRoot = isRoot;
    }

    public void killSubtree(EFDT ht) {
      for (Node child : this.children) {
	if (child != null) {

	  //Recursive delete of SplitNodes
	  if (child instanceof SplitNode) {
	    ((EFDTSplitNode) child).killSubtree(ht);
	  }
	  else if (child instanceof ActiveLearningNode) {
	    ht.activeLeafNodeCount--;
	  }
	  else if (child instanceof InactiveLearningNode) {
	    ht.inactiveLeafNodeCount--;
	  }
	}
      }
    }


    // DRY Don't Repeat Yourself... code duplicated from ActiveLearningNode in VFDT.java. However, this is the most practical way to share stand-alone.
    public AttributeSplitSuggestion[] getBestSplitSuggestions(
      SplitCriterion criterion, EFDT ht) {
      List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<>();
      double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
      if (!ht.noPrePruneOption.isSet()) {
	// add null split as an option
	bestSuggestions.add(new AttributeSplitSuggestion(null,
	  new double[0][], criterion.getMeritOfSplit(
	  preSplitDist, new double[][]{preSplitDist})));
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


    @Override
    public void learnFromInstance(Instance inst, EFDT ht, EFDTSplitNode parent, int parentBranch) {

      nodeTime++;
      //// Update node statistics and class distribution

      this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight()); // update prior (predictor)

      for (int i = 0; i < inst.numAttributes() - 1; i++) { //update likelihood
	int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
	AttributeClassObserver obs = this.attributeObservers.get(i);
	if (obs == null) {
	  obs = inst.attribute(instAttIndex).isNominal() ? ht.newNominalClassObserver() : ht.newNumericClassObserver();
	  this.attributeObservers.set(i, obs);
	}
	obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
      }

      // check if a better split is available. if so, chop the tree at this point, copying likelihood. predictors for children are from parent likelihood.
      if (ht.numInstances % ht.reEvalPeriodOption.getValue() == 0) {
	this.reEvaluateBestSplit(this, parent, parentBranch);
      }

      int childBranch = this.instanceChildIndex(inst);
      Node child = this.getChild(childBranch);

      if (child != null) {
	((EFDTNode) child).learnFromInstance(inst, ht, this, childBranch);
      }

    }

    protected void reEvaluateBestSplit(EFDTSplitNode node, EFDTSplitNode parent,
				       int parentIndex) {


      node.addToSplitAttempts(1);

      // EFDT must transfer over gain averages when replacing a node: leaf to split, split to leaf, or split to split
      // It must replace split nodes with leaves if null wins


      // node is a reference to this anyway... why have it at all?

      int currentSplit = -1;
      // and if we always choose to maintain tree structure

      //lets first find out X_a, the current split

      if (this.splitTest != null) {
	currentSplit = this.splitTest.getAttsTestDependsOn()[0];
	// given the current implementations in MOA, we're only ever expecting one int to be returned
      }

      //compute Hoeffding bound
      SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(EFDT.this.splitCriterionOption);
      double hoeffdingBound = computeHoeffdingBound(splitCriterion.getRangeOfMerit(node.getClassDistributionAtTimeOfCreation()),
	EFDT.this.splitConfidenceOption.getValue(), node.observedClassDistribution.sumOfValues());

      // get best split suggestions
      AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, EFDT.this);
      Arrays.sort(bestSplitSuggestions);

      // get the best suggestion
      AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];


      for (AttributeSplitSuggestion bestSplitSuggestion : bestSplitSuggestions) {

	if (bestSplitSuggestion.splitTest != null) {
	  if (!node.getInfogainSum().containsKey((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]))) {
	    node.getInfogainSum().put((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]), 0.0);
	  }
	  double currentSum = node.getInfogainSum().get((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]));
	  node.getInfogainSum().put((bestSplitSuggestion.splitTest.getAttsTestDependsOn()[0]), currentSum + bestSplitSuggestion.merit);
	}

	else { // handle the null attribute. this is fine to do- it'll always average zero, and we will use this later to potentially burn bad splits.
	  double currentSum = node.getInfogainSum().get(-1); // null split
	  node.getInfogainSum().put(-1, currentSum + bestSplitSuggestion.merit);
	}

      }

      // get the average merit for best and current splits

      double bestSuggestionAverageMerit;
      double currentAverageMerit;

      if (bestSuggestion.splitTest == null) { // best is null
	bestSuggestionAverageMerit = node.getInfogainSum().get(-1) / node.getNumSplitAttempts();
      }
      else {

	bestSuggestionAverageMerit = node.getInfogainSum().get(bestSuggestion.splitTest.getAttsTestDependsOn()[0]) / node.getNumSplitAttempts();
      }

      if (node.splitTest == null) { // current is null- shouldn't happen, check for robustness
	currentAverageMerit = node.getInfogainSum().get(-1) / node.getNumSplitAttempts();
      }
      else {
	currentAverageMerit = node.getInfogainSum().get(node.splitTest.getAttsTestDependsOn()[0]) / node.getNumSplitAttempts();
      }

      double tieThreshold = EFDT.this.tieThresholdOption.getValue();

      // compute the average deltaG
      double deltaG = bestSuggestionAverageMerit - currentAverageMerit;

      if (deltaG > hoeffdingBound
	|| (hoeffdingBound < tieThreshold && deltaG > tieThreshold / 2)) {

	System.err.println(numInstances);

	AttributeSplitSuggestion splitDecision = bestSuggestion;

	// if null split wins
	if (splitDecision.splitTest == null) {

	  node.killSubtree(EFDT.this);
	  EFDTLearningNode replacement = (EFDTLearningNode) newLearningNode();
	  replacement.setInfogainSum(node.getInfogainSum()); // transfer infogain history, split to replacement leaf
	  if (node.getParent() != null) {
	    node.getParent().setChild(parentIndex, replacement);
	  }
	  else {
	    assert (node.isRoot());
	    node.setRoot(true);
	  }
	}

	else {

	  Node newSplit = newSplitNode(splitDecision.splitTest,
	    node.getObservedClassDistribution(), splitDecision.numSplits());

	  ((EFDTSplitNode) newSplit).attributeObservers = node.attributeObservers; // copy the attribute observers
	  newSplit.setInfogainSum(node.getInfogainSum());  // transfer infogain history, split to replacement split

	  if (node.splitTest == splitDecision.splitTest
	    && node.splitTest.getClass() == NumericAttributeBinaryTest.class &&
	    (argmax(splitDecision.resultingClassDistributions[0]) == argmax(node.getChild(0).getObservedClassDistribution())
	      || argmax(splitDecision.resultingClassDistributions[1]) == argmax(node.getChild(1).getObservedClassDistribution()))
	  ) {
	    // change split but don't destroy the subtrees
	    for (int i = 0; i < splitDecision.numSplits(); i++) {
	      ((EFDTSplitNode) newSplit).setChild(i, this.getChild(i));
	    }

	  }
	  else {

	    // otherwise, torch the subtree and split on the new best attribute.

	    this.killSubtree(EFDT.this);

	    for (int i = 0; i < splitDecision.numSplits(); i++) {

	      double[] j = splitDecision.resultingClassDistributionFromSplit(i);

	      Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i));

	      if (splitDecision.splitTest.getClass() == NominalAttributeBinaryTest.class
		|| splitDecision.splitTest.getClass() == NominalAttributeMultiwayTest.class) {
		newChild.usedNominalAttributes = new ArrayList<>(node.usedNominalAttributes); //deep copy
		newChild.usedNominalAttributes.add(splitDecision.splitTest.getAttsTestDependsOn()[0]);
		// no  nominal attribute should be split on more than once in the path
	      }
	      ((EFDTSplitNode) newSplit).setChild(i, newChild);
	    }

	    EFDT.this.activeLeafNodeCount--;
	    EFDT.this.decisionNodeCount++;
	    EFDT.this.activeLeafNodeCount += splitDecision.numSplits();

	  }


	  if (parent == null) {
	    ((EFDTNode) newSplit).setRoot(true);
	    ((EFDTNode) newSplit).setParent(null);
	    EFDT.this.treeRoot = newSplit;
	  }
	  else {
	    ((EFDTNode) newSplit).setRoot(false);
	    ((EFDTNode) newSplit).setParent(parent);
	    parent.setChild(parentIndex, newSplit);
	  }
	}
      }
    }

    @Override
    public void setParent(EFDTSplitNode parent) {
      this.parent = parent;
    }

    @Override
    public EFDTSplitNode getParent() {
      return this.parent;
    }
  }

  public static abstract class LearningNode extends Node {

    private static final long serialVersionUID = 1L;

    public LearningNode(double[] initialClassObservations) {
      super(initialClassObservations);
    }

    public abstract void learnFromInstance(Instance inst, EFDT ht);
  }


  public class EFDTLearningNode extends LearningNodeNBAdaptive implements EFDTNode {

    private boolean isRoot;

    private EFDTSplitNode parent = null;

    public EFDTLearningNode(double[] initialClassObservations) {
      super(initialClassObservations);
    }


    /**
     *
     */
    private static final long serialVersionUID = -2525042202040084035L;

    @Override
    public boolean isRoot() {
      return isRoot;
    }

    @Override
    public void setRoot(boolean isRoot) {
      this.isRoot = isRoot;
    }

    @Override
    public void learnFromInstance(Instance inst, EFDT ht) {
      super.learnFromInstance(inst, ht);

    }

    @Override
    public void learnFromInstance(Instance inst, EFDT ht, EFDTSplitNode parent, int parentBranch) {
      learnFromInstance(inst, ht);

      if (ht.growthAllowed) {
	ActiveLearningNode activeLearningNode = this;
	double weightSeen = activeLearningNode.getWeightSeen();
	if (activeLearningNode.nodeTime % ht.gracePeriodOption.getValue() == 0) {
	  attemptToSplit(activeLearningNode, parent,
	    parentBranch);
	  activeLearningNode.setWeightSeenAtLastSplitEvaluation(weightSeen);
	}
      }
    }

    @Override
    public void setParent(EFDTSplitNode parent) {
      this.parent = parent;
    }

    @Override
    public EFDTSplitNode getParent() {
      return this.parent;
    }

  }

  public static class InactiveLearningNode extends LearningNode {

    private static final long serialVersionUID = 1L;

    public InactiveLearningNode(double[] initialClassObservations) {
      super(initialClassObservations);
    }

    @Override
    public void learnFromInstance(Instance inst, EFDT ht) {
      this.observedClassDistribution.addToValue((int) inst.classValue(),
	inst.weight());
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
      return super.calcByteSize()
	+ (int) (SizeOf.fullSizeOf(this.attributeObservers));
    }

    @Override
    public void learnFromInstance(Instance inst, EFDT ht) {
      nodeTime++;

      if (this.isInitialized) {
	this.attributeObservers = new AutoExpandVector<>(inst.numAttributes());
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
      SplitCriterion criterion, EFDT ht) {
      List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<>();
      double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
      if (!ht.noPrePruneOption.isSet()) {
	// add null split as an option
	bestSuggestions.add(new AttributeSplitSuggestion(null,
	  new double[0][], criterion.getMeritOfSplit(
	  preSplitDist, new double[][]{preSplitDist})));
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

  public static class LearningNodeNB extends ActiveLearningNode {

    private static final long serialVersionUID = 1L;

    public LearningNodeNB(double[] initialClassObservations) {
      super(initialClassObservations);
    }

    @Override
    public double[] getClassVotes(Instance inst, EFDT ht) {
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

    @Override
    public void learnFromInstance(Instance inst, EFDT ht) {
      int trueClass = (int) inst.classValue();
      if (this.observedClassDistribution.maxIndex() == trueClass) {
	this.mcCorrectWeight += inst.weight();
      }
      if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
	this.observedClassDistribution, this.attributeObservers)) == trueClass) {
	this.nbCorrectWeight += inst.weight();
      }
      super.learnFromInstance(inst, ht);
    }

    @Override
    public double[] getClassVotes(Instance inst, EFDT ht) {
      if (this.mcCorrectWeight > this.nbCorrectWeight) {
	return this.observedClassDistribution.getArrayCopy();
      }
      return NaiveBayes.doNaiveBayesPrediction(inst,
	this.observedClassDistribution, this.attributeObservers);
    }
  }

  static class VFDT extends EFDT {

    @Override
    protected void attemptToSplit(ActiveLearningNode node, SplitNode parent,
				  int parentIndex) {
      if (!node.observedClassDistributionIsPure()) {


	SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);
	AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);

	Arrays.sort(bestSplitSuggestions);
	boolean shouldSplit = false;

	for (int i = 0; i < bestSplitSuggestions.length; i++){

	  node.addToSplitAttempts(1); // even if we don't actually attempt to split, we've computed infogains


	  if (bestSplitSuggestions[i].splitTest != null){
	    if (!node.getInfogainSum().containsKey((bestSplitSuggestions[i].splitTest.getAttsTestDependsOn()[0])))
	    {
	      node.getInfogainSum().put((bestSplitSuggestions[i].splitTest.getAttsTestDependsOn()[0]), 0.0);
	    }
	    double currentSum = node.getInfogainSum().get((bestSplitSuggestions[i].splitTest.getAttsTestDependsOn()[0]));
	    node.getInfogainSum().put((bestSplitSuggestions[i].splitTest.getAttsTestDependsOn()[0]), currentSum + bestSplitSuggestions[i].merit);
	  }

	  else { // handle the null attribute
	    double currentSum = node.getInfogainSum().get(-1); // null split
	    node.getInfogainSum().put(-1, currentSum + Math.max(0.0, bestSplitSuggestions[i].merit));
	    assert node.getInfogainSum().get(-1) >= 0.0 : "Negative infogain shouldn't be possible here.";
	  }

	}

	if (bestSplitSuggestions.length < 2) {
	  shouldSplit = bestSplitSuggestions.length > 0;
	}

	else {


	  double hoeffdingBound = computeHoeffdingBound(splitCriterion.getRangeOfMerit(node.getObservedClassDistribution()),
	    this.splitConfidenceOption.getValue(), node.getWeightSeen());

	  AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
	  AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];


	  double bestSuggestionAverageMerit = 0.0;
	  double secondBestSuggestionAverageMerit = 0.0;

	  if(bestSuggestion.splitTest == null){ // if you have a null split
	    bestSuggestionAverageMerit = node.getInfogainSum().get(-1) / node.getNumSplitAttempts();
	  } else{
	    bestSuggestionAverageMerit = node.getInfogainSum().get((bestSuggestion.splitTest.getAttsTestDependsOn()[0])) / node.getNumSplitAttempts();
	  }

	  if(secondBestSuggestion.splitTest == null){ // if you have a null split
	    secondBestSuggestionAverageMerit = node.getInfogainSum().get(-1) / node.getNumSplitAttempts();
	  } else{
	    secondBestSuggestionAverageMerit = node.getInfogainSum().get((secondBestSuggestion.splitTest.getAttsTestDependsOn()[0])) / node.getNumSplitAttempts();
	  }

	  //comment this if statement to get VFDT bug
	  if(bestSuggestion.merit < 1e-10){ // we don't use average here
	    shouldSplit = false;
	  }

	  else
	  if ((bestSuggestionAverageMerit - secondBestSuggestionAverageMerit > hoeffdingBound)
	    || (hoeffdingBound < this.tieThresholdOption.getValue()))
	  {
	    shouldSplit = true;
	  }

	  if(shouldSplit){
	    for(Integer i : node.usedNominalAttributes){
	      if(bestSuggestion.splitTest.getAttsTestDependsOn()[0] == i){
		shouldSplit = false;
		break;
	      }
	    }
	  }

	  // }
	  if ((this.removePoorAttsOption != null)
	    && this.removePoorAttsOption.isSet()) {
	    Set<Integer> poorAtts = new HashSet<Integer>();
	    // scan 1 - add any poor to set
	    for (int i = 0; i < bestSplitSuggestions.length; i++) {
	      if (bestSplitSuggestions[i].splitTest != null) {
		int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
		if (splitAtts.length == 1) {
		  if (bestSuggestion.merit
		    - bestSplitSuggestions[i].merit > hoeffdingBound) {
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
		  if (bestSuggestion.merit
		    - bestSplitSuggestions[i].merit < hoeffdingBound) {
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
	  splitCount++;

	  AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
	  if (splitDecision.splitTest == null) {
	    // preprune - null wins
	    deactivateLearningNode(node, parent, parentIndex);
	  } else {
	    SplitNode newSplit = newSplitNode(splitDecision.splitTest,
	      node.getObservedClassDistribution(), splitDecision.numSplits());
	    for (int i = 0; i < splitDecision.numSplits(); i++) {

	      double[] j = splitDecision.resultingClassDistributionFromSplit(i);

	      Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i));

	      if(splitDecision.splitTest.getClass() == NominalAttributeBinaryTest.class
		||splitDecision.splitTest.getClass() == NominalAttributeMultiwayTest.class){
		newChild.usedNominalAttributes = new ArrayList<Integer>(node.usedNominalAttributes); //deep copy
		newChild.usedNominalAttributes.add(splitDecision.splitTest.getAttsTestDependsOn()[0]);
		// no  nominal attribute should be split on more than once in the path
	      }
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

    @Override
    protected LearningNode newLearningNode() {
      return newLearningNode(new double[0]);
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
      LearningNode ret;
      int predictionOption = this.leafpredictionOption.getChosenIndex();
      if (predictionOption == 0) { //MC
	ret = new ActiveLearningNode(initialClassObservations);
      } else if (predictionOption == 1) { //NB
	ret = new LearningNodeNB(initialClassObservations);
      } else { //NBAdaptive
	ret = new LearningNodeNBAdaptive(initialClassObservations);
      }
      return ret;
    }

    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
				     double[] classObservations, int size) {
      return new SplitNode(splitTest, classObservations, size);
    }

    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
				     double[] classObservations) {
      return new SplitNode(splitTest, classObservations);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
      //System.err.println(i++);
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
	  if (activeLearningNode.nodeTime % this.gracePeriodOption.getValue() == 0) {
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

      numInstances++;
    }
  }
}
