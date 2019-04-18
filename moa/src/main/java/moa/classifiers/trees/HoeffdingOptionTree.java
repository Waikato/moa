/*
 *    HoeffdingOptionTree.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
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
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
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
 * Hoeffding Option Tree.
 *
 * <p>Hoeffding Option Trees are regular Hoeffding trees containing additional
 * option nodes that allow several tests to be applied, leading to multiple
 * Hoeffding trees as separate paths. They consist of a single structure that
 * efﬁciently represents multiple trees. A particular example can travel down
 * multiple paths of the tree, contributing, in different ways, to different
 * options.</p>
 *
 * <p>See for details:</p> <p>B. Pfahringer, G. Holmes, and R. Kirkby. New
 * options for hoeffding trees. In AI, pages 90–99, 2007.</p>
 *
 * <p>Parameters:</p> <ul> <li>-o : Maximum number of option paths per node</li>
 * <li>-m : Maximum memory consumed by the tree</li> <li>-n : Numeric estimator
 * to use :</li> <ul> <li> Gaussian approximation evaluating 10 splitpoints</li>
 * <li> Gaussian approximation evaluating 100 splitpoints</li> <li>
 * Greenwald-Khanna quantile summary with 10 tuples</li> <li> Greenwald-Khanna
 * quantile summary with 100 tuples</li> <li> Greenwald-Khanna quantile summary
 * with 1000 tuples</li> <li> VFML method with 10 bins</li> <li> VFML method
 * with 100 bins</li> <li> VFML method with 1000 bins</li> <li> Exhaustive
 * binary tree</li> </ul> <li>-e : How many instances between memory consumption
 * checks</li> <li>-g : The number of instances a leaf should observe between
 * split attempts</li> <li>-s : Split criterion to use. Example :
 * InfoGainSplitCriterion</li> <li>-c : The allowable error in split decision,
 * values closer to 0 will take longer to decide</li> <li>-w : The allowable
 * error in secondary split decisions, values closer to 0 will take longer to
 * decide</li> <li>-t : Threshold below which a split will be forced to break
 * ties</li> <li>-b : Only allow binary splits</li> <li>-z : Memory strategy to
 * use</li> <li>-r : Disable poor attributes</li> <li>-p : Disable
 * pre-pruning</li> <li>-d : File to append option table to.</li> 
 *  <li> -l : Leaf prediction to use: MajorityClass (MC), Naive Bayes (NB) or NaiveBayes
 * adaptive (NBAdaptive).</li>
 *  <li> -q : The number of instances a leaf should observe before
 * permitting Naive Bayes</li>
 * </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class HoeffdingOptionTree extends AbstractClassifier implements MultiClassClassifier,
                                                                       CapabilitiesHandler {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Hoeffding Option Tree: single tree that represents multiple trees.";
    }

    public IntOption maxOptionPathsOption = new IntOption("maxOptionPaths",
            'o', "Maximum number of option paths per node.", 5, 1,
            Integer.MAX_VALUE);

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

    public FloatOption secondarySplitConfidenceOption = new FloatOption(
            "secondarySplitConfidence",
            'w',
            "The allowable error in secondary split decisions, values closer to 0 will take longer to decide.",
            0.1, 0.0, 1.0);

    public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
            't', "Threshold below which a split will be forced to break ties.",
            0.05, 0.0, 1.0);

    public FlagOption binarySplitsOption = new FlagOption("binarySplits", 'b',
            "Only allow binary splits.");

    public FlagOption removePoorAttsOption = new FlagOption("removePoorAtts",
            'r', "Disable poor attributes.");

    public FlagOption noPrePruneOption = new FlagOption("noPrePrune", 'p',
            "Disable pre-pruning.");

    public FileOption dumpFileOption = new FileOption("dumpFile", 'f',
            "File to append option table to.", null, "csv", true);

    public IntOption memoryStrategyOption = new IntOption("memStrategy", 'z',
            "Memory strategy to use.", 2);

    public static class FoundNode {

        public Node node;

        public SplitNode parent;

        public int parentBranch; // set to -999 for option leaves

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

        public FoundNode[] filterInstanceToLeaves(Instance inst,
                SplitNode parent, int parentBranch, boolean updateSplitterCounts) {
            List<FoundNode> nodes = new LinkedList<FoundNode>();
            filterInstanceToLeaves(inst, parent, parentBranch, nodes,
                    updateSplitterCounts);
            return nodes.toArray(new FoundNode[nodes.size()]);
        }

        public void filterInstanceToLeaves(Instance inst,
                SplitNode splitparent, int parentBranch,
                List<FoundNode> foundNodes, boolean updateSplitterCounts) {
            foundNodes.add(new FoundNode(this, splitparent, parentBranch));
        }

        public double[] getObservedClassDistribution() {
            return this.observedClassDistribution.getArrayCopy();
        }

        public double[] getClassVotes(Instance inst, HoeffdingOptionTree ht) {
            double[] dist = this.observedClassDistribution.getArrayCopy();
            double distSum = Utils.sum(dist);
            if (distSum > 0.0) {
                Utils.normalize(dist, distSum);
            }
            return dist;
        }

        public boolean observedClassDistributionIsPure() {
            return this.observedClassDistribution.numNonZeroEntries() < 2;
        }

        public void describeSubtree(HoeffdingOptionTree ht, StringBuilder out,
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

        public void getDescription(StringBuilder sb, int indent) {
            describeSubtree(null, sb, indent);
        }
    }

    public static class SplitNode extends Node {

        private static final long serialVersionUID = 1L;

        protected InstanceConditionalTest splitTest;

        protected SplitNode parent;

        protected Node nextOption;

        protected int optionCount; // set to -999 for optional splits

        protected AutoExpandVector<Node> children = new AutoExpandVector<Node>();

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
            if (this.nextOption != null) {
                byteSize += this.nextOption.calcByteSizeIncludingSubtree();
            }
            return byteSize;
        }

        public SplitNode(InstanceConditionalTest splitTest,
                double[] classObservations) {
            super(classObservations);
            this.splitTest = splitTest;
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
        public void filterInstanceToLeaves(Instance inst, SplitNode myparent,
                int parentBranch, List<FoundNode> foundNodes,
                boolean updateSplitterCounts) {
            if (updateSplitterCounts) {
                this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
            }
            int childIndex = instanceChildIndex(inst);
            if (childIndex >= 0) {
                Node child = getChild(childIndex);
                if (child != null) {
                    child.filterInstanceToLeaves(inst, this, childIndex,
                            foundNodes, updateSplitterCounts);
                } else {
                    foundNodes.add(new FoundNode(null, this, childIndex));
                }
            }
            if (this.nextOption != null) {
                this.nextOption.filterInstanceToLeaves(inst, this, -999,
                        foundNodes, updateSplitterCounts);
            }
        }

        @Override
        public void describeSubtree(HoeffdingOptionTree ht, StringBuilder out,
                int indent) {
            for (int branch = 0; branch < numChildren(); branch++) {
                Node child = getChild(branch);
                if (child != null) {
                    StringUtils.appendIndented(out, indent, "if ");
                    out.append(this.splitTest.describeConditionForBranch(branch,
                            ht.getModelContext()));
                    out.append(": ");
                    out.append("** option count = " + this.optionCount);
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

        public double computeMeritOfExistingSplit(
                SplitCriterion splitCriterion, double[] preDist) {
            double[][] postDists = new double[this.children.size()][];
            for (int i = 0; i < this.children.size(); i++) {
                postDists[i] = this.children.get(i).getObservedClassDistribution();
            }
            return splitCriterion.getMeritOfSplit(preDist, postDists);
        }

        public void updateOptionCount(SplitNode source, HoeffdingOptionTree hot) {
            if (this.optionCount == -999) {
                this.parent.updateOptionCount(source, hot);
            } else {
                int maxChildCount = -999;
                SplitNode curr = this;
                while (curr != null) {
                    for (Node child : curr.children) {
                        if (child instanceof SplitNode) {
                            SplitNode splitChild = (SplitNode) child;
                            if (splitChild.optionCount > maxChildCount) {
                                maxChildCount = splitChild.optionCount;
                            }
                        }
                    }
                    if ((curr.nextOption != null)
                            && (curr.nextOption instanceof SplitNode)) {
                        curr = (SplitNode) curr.nextOption;
                    } else {
                        curr = null;
                    }
                }
                if (maxChildCount > this.optionCount) { // currently only works
                    // one
                    // way - adding, not
                    // removing
                    int delta = maxChildCount - this.optionCount;
                    this.optionCount = maxChildCount;
                    if (this.optionCount >= hot.maxOptionPathsOption.getValue()) {
                        killOptionLeaf(hot);
                    }
                    curr = this;
                    while (curr != null) {
                        for (Node child : curr.children) {
                            if (child instanceof SplitNode) {
                                SplitNode splitChild = (SplitNode) child;
                                if (splitChild != source) {
                                    splitChild.updateOptionCountBelow(delta,
                                            hot);
                                }
                            }
                        }
                        if ((curr.nextOption != null)
                                && (curr.nextOption instanceof SplitNode)) {
                            curr = (SplitNode) curr.nextOption;
                        } else {
                            curr = null;
                        }
                    }
                    if (this.parent != null) {
                        this.parent.updateOptionCount(this, hot);
                    }
                }
            }
        }

        public void updateOptionCountBelow(int delta, HoeffdingOptionTree hot) {
            if (this.optionCount != -999) {
                this.optionCount += delta;
                if (this.optionCount >= hot.maxOptionPathsOption.getValue()) {
                    killOptionLeaf(hot);
                }
            }
            for (Node child : this.children) {
                if (child instanceof SplitNode) {
                    SplitNode splitChild = (SplitNode) child;
                    splitChild.updateOptionCountBelow(delta, hot);
                }
            }
            if (this.nextOption instanceof SplitNode) {
                ((SplitNode) this.nextOption).updateOptionCountBelow(delta, hot);
            }
        }

        private void killOptionLeaf(HoeffdingOptionTree hot) {
            if (this.nextOption instanceof SplitNode) {
                ((SplitNode) this.nextOption).killOptionLeaf(hot);
            } else if (this.nextOption instanceof ActiveLearningNode) {
                this.nextOption = null;
                hot.activeLeafNodeCount--;
            } else if (this.nextOption instanceof InactiveLearningNode) {
                this.nextOption = null;
                hot.inactiveLeafNodeCount--;
            }
        }

        public int getHeadOptionCount() {
            SplitNode sn = this;
            while (sn.optionCount == -999) {
                sn = sn.parent;
            }
            return sn.optionCount;
        }
    }

    public static abstract class LearningNode extends Node {

        private static final long serialVersionUID = 1L;

        public LearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
        }

        public abstract void learnFromInstance(Instance inst,
                HoeffdingOptionTree ht);
    }

    public static class InactiveLearningNode extends LearningNode {

        private static final long serialVersionUID = 1L;

        public InactiveLearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
        }

        @Override
        public void learnFromInstance(Instance inst, HoeffdingOptionTree ht) {
            this.observedClassDistribution.addToValue((int) inst.classValue(),
                    inst.weight());
        }
    }

    public static class ActiveLearningNode extends LearningNode {

        private static final long serialVersionUID = 1L;

        protected double weightSeenAtLastSplitEvaluation;

        protected AutoExpandVector<AttributeClassObserver> attributeObservers = new AutoExpandVector<AttributeClassObserver>();

        public ActiveLearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
            this.weightSeenAtLastSplitEvaluation = getWeightSeen();
        }

        @Override
        public int calcByteSize() {
            return super.calcByteSize()
                    + (int) (SizeOf.fullSizeOf(this.attributeObservers));
        }

        @Override
        public void learnFromInstance(Instance inst, HoeffdingOptionTree ht) {
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
                SplitCriterion criterion, HoeffdingOptionTree ht) {
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

    protected int maxPredictionPaths;

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
        this.maxPredictionPaths = 0;
        if (this.leafpredictionOption.getChosenIndex() > 0) {
            this.removePoorAttsOption = null;
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            this.activeLeafNodeCount = 1;
        }
        FoundNode[] foundNodes = this.treeRoot.filterInstanceToLeaves(inst,
                null, -1, true);
        for (FoundNode foundNode : foundNodes) {
            // option leaves will have a parentBranch of -999
            // option splits will have an option count of -999
            Node leafNode = foundNode.node;
            if (leafNode == null) {
                leafNode = newLearningNode();
                foundNode.parent.setChild(foundNode.parentBranch, leafNode);
                this.activeLeafNodeCount++;
            }
            if (leafNode instanceof LearningNode) {
                LearningNode learningNode = (LearningNode) leafNode;
                learningNode.learnFromInstance(inst, this);
                if (learningNode instanceof ActiveLearningNode) {
                    ActiveLearningNode activeLearningNode = (ActiveLearningNode) learningNode;
                    double weightSeen = activeLearningNode.getWeightSeen();
                    if (weightSeen
                            - activeLearningNode.getWeightSeenAtLastSplitEvaluation() >= this.gracePeriodOption.getValue()) {
                        attemptToSplit(activeLearningNode, foundNode.parent,
                                foundNode.parentBranch);
                        activeLearningNode.setWeightSeenAtLastSplitEvaluation(weightSeen);
                    }
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
            FoundNode[] foundNodes = this.treeRoot.filterInstanceToLeaves(inst,
                    null, -1, false);
            DoubleVector result = new DoubleVector();
            int predictionPaths = 0;
            for (FoundNode foundNode : foundNodes) {
                if (foundNode.parentBranch != -999) {
                    Node leafNode = foundNode.node;
                    if (leafNode == null) {
                        leafNode = foundNode.parent;
                    }
                    double[] dist = leafNode.getClassVotes(inst, this);
                    //Albert: changed for weights
                    //double distSum = Utils.sum(dist);
                    //if (distSum > 0.0) {
                    //	Utils.normalize(dist, distSum);
                    //}
                    result.addValues(dist);
                    predictionPaths++;
                }
            }
            if (predictionPaths > this.maxPredictionPaths) {
                this.maxPredictionPaths++;
            }
            return result.getArrayRef();
        }
        return new double[0];
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
                    this.byteSizeEstimateOverheadFraction),
                    new Measurement("maximum prediction paths used",
                    this.maxPredictionPaths)};
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

    protected void attemptToSplit(ActiveLearningNode node, SplitNode parent,
            int parentIndex) {
        if (!node.observedClassDistributionIsPure()) {
            SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);
            AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
            Arrays.sort(bestSplitSuggestions);
            boolean shouldSplit = false;
            if (parentIndex != -999) {
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
                                        poorAtts.remove(new Integer(
                                                splitAtts[0]));
                                    }
                                }
                            }
                        }
                        for (int poorAtt : poorAtts) {
                            node.disableAttribute(poorAtt);
                        }
                    }
                }
            } else if (bestSplitSuggestions.length > 0) {
                double hoeffdingBound = computeHoeffdingBound(splitCriterion.getRangeOfMerit(node.getObservedClassDistribution()),
                        this.secondarySplitConfidenceOption.getValue(), node.getWeightSeen());
                AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
                // in option case, scan back through existing options to
                // find best
                SplitNode current = parent;
                double bestPreviousMerit = Double.NEGATIVE_INFINITY;
                double[] preDist = node.getObservedClassDistribution();
                while (true) {
                    double merit = current.computeMeritOfExistingSplit(
                            splitCriterion, preDist);
                    if (merit > bestPreviousMerit) {
                        bestPreviousMerit = merit;
                    }
                    if (current.optionCount != -999) {
                        break;
                    }
                    current = current.parent;
                }

                if (bestSuggestion.merit - bestPreviousMerit > hoeffdingBound) {
                    shouldSplit = true;
                }
            }
            if (shouldSplit) {
                AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
                if (splitDecision.splitTest == null) {
                    // preprune - null wins
                    if (parentIndex != -999) {
                        deactivateLearningNode(node, parent, parentIndex);
                    }
                } else {
                    SplitNode newSplit = new SplitNode(splitDecision.splitTest,
                            node.getObservedClassDistribution());
                    newSplit.parent = parent;
                    // add option procedure
                    SplitNode optionHead = parent;
                    if (parent != null) {
                        while (optionHead.optionCount == -999) {
                            optionHead = optionHead.parent;
                        }
                    }
                    if ((parentIndex == -999) && (parent != null)) {
                        // adding a new option
                        newSplit.optionCount = -999;
                        optionHead.updateOptionCountBelow(1, this);
                        if (optionHead.parent != null) {
                            optionHead.parent.updateOptionCount(optionHead,
                                    this);
                        }
                        addToOptionTable(splitDecision, optionHead.parent);
                    } else {
                        // adding a regular leaf
                        if (optionHead == null) {
                            newSplit.optionCount = 1;
                        } else {
                            newSplit.optionCount = optionHead.optionCount;
                        }
                    }
                    int numOptions = 1;
                    if (optionHead != null) {
                        numOptions = optionHead.optionCount;
                    }
                    if (numOptions < this.maxOptionPathsOption.getValue()) {
                        newSplit.nextOption = node; // preserve leaf
                        // disable attribute just used
                        int[] splitAtts = splitDecision.splitTest.getAttsTestDependsOn();
                        for (int i : splitAtts) {
                            node.disableAttribute(i);
                        }
                    } else {
                        this.activeLeafNodeCount--;
                    }
                    for (int i = 0; i < splitDecision.numSplits(); i++) {
                        Node newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i));
                        newSplit.setChild(i, newChild);
                    }
                    this.decisionNodeCount++;
                    this.activeLeafNodeCount += splitDecision.numSplits();
                    if (parent == null) {
                        this.treeRoot = newSplit;
                    } else {
                        if (parentIndex != -999) {
                            parent.setChild(parentIndex, newSplit);
                        } else {
                            parent.nextOption = newSplit;
                        }
                    }
                }
                // manage memory
                enforceTrackerLimit();
            }
        }
    }

    private void addToOptionTable(AttributeSplitSuggestion bestSuggestion,
            SplitNode parent) {
        File dumpFile = this.dumpFileOption.getFile();
        PrintStream immediateResultStream = null;
        if (dumpFile != null) {
            try {
                if (dumpFile.exists()) {
                    immediateResultStream = new PrintStream(
                            new FileOutputStream(dumpFile, true), true);
                } else {
                    immediateResultStream = new PrintStream(
                            new FileOutputStream(dumpFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Unable to open dump file: "
                        + dumpFile, ex);
            }
            int splitAtt = bestSuggestion.splitTest.getAttsTestDependsOn()[0];
            double splitVal = -1.0;
            if (bestSuggestion.splitTest instanceof NumericAttributeBinaryTest) {
                NumericAttributeBinaryTest test = (NumericAttributeBinaryTest) bestSuggestion.splitTest;
                splitVal = test.getSplitValue();
            }
            int treeDepth = 0;
            while (parent != null) {
                parent = parent.parent;
                treeDepth++;
            }
            immediateResultStream.println(this.trainingWeightSeenByModel + ","
                    + treeDepth + "," + splitAtt + "," + splitVal);
            immediateResultStream.flush();
            immediateResultStream.close();
        }
    }

    public void enforceTrackerLimit() {
        if ((this.inactiveLeafNodeCount > 0)
                || ((this.activeLeafNodeCount * this.activeLeafByteSizeEstimate + this.inactiveLeafNodeCount
                * this.inactiveLeafByteSizeEstimate)
                * this.byteSizeEstimateOverheadFraction > this.maxByteSizeOption.getValue())) {
            FoundNode[] learningNodes = findLearningNodes();
            Arrays.sort(learningNodes, new Comparator<FoundNode>() {

                public int compare(FoundNode fn1, FoundNode fn2) {
                    if (HoeffdingOptionTree.this.memoryStrategyOption.getValue() == 0) {
                        // strategy 1 - every leaf treated equal
                        return Double.compare(fn1.node.calculatePromise(),
                                fn2.node.calculatePromise());
                    } else if (HoeffdingOptionTree.this.memoryStrategyOption.getValue() == 1) {
                        // strategy 2 - internal leaves penalised
                        double p1 = fn1.node.calculatePromise();
                        if (fn1.parentBranch == -999) {
                            p1 /= fn1.parent.getHeadOptionCount();
                        }
                        double p2 = fn2.node.calculatePromise();
                        if (fn2.parentBranch == -999) {
                            p1 /= fn2.parent.getHeadOptionCount();
                        }
                        return Double.compare(p1, p2);
                    } else {
                        // strategy 3 - all true leaves beat internal leaves
                        if (fn1.parentBranch == -999) {
                            if (fn2.parentBranch == -999) {
                                return Double.compare(fn1.node.calculatePromise(), fn2.node.calculatePromise());
                            }
                            return -1; // fn1 < fn2
                        }
                        if (fn2.parentBranch == -999) {
                            return 1; // fn1 > fn2
                        }
                        return Double.compare(fn1.node.calculatePromise(),
                                fn2.node.calculatePromise());
                    }
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
            if (parentBranch != -999) {
                parent.setChild(parentBranch, newLeaf);
            } else {
                parent.nextOption = newLeaf;
            }
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
            if (parentBranch != -999) {
                parent.setChild(parentBranch, newLeaf);
            } else {
                parent.nextOption = newLeaf;
            }
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
                findLearningNodes(splitNode.nextOption, splitNode, -999, found);
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

        @Override
        public double[] getClassVotes(Instance inst, HoeffdingOptionTree hot) {
            if (getWeightSeen() >=  hot.nbThresholdOption.getValue()) {
                return NaiveBayes.doNaiveBayesPrediction(inst,
                        this.observedClassDistribution,
                        this.attributeObservers);
            }
            return super.getClassVotes(inst, hot);
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
        public void learnFromInstance(Instance inst, HoeffdingOptionTree hot) {
            int trueClass = (int) inst.classValue();
            if (this.observedClassDistribution.maxIndex() == trueClass) {
                this.mcCorrectWeight += inst.weight();
            }
            if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
                    this.observedClassDistribution, this.attributeObservers)) == trueClass) {
                this.nbCorrectWeight += inst.weight();
            }
            super.learnFromInstance(inst, hot);
        }

        @Override
        public double[] getClassVotes(Instance inst, HoeffdingOptionTree ht) {
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
            ret = new ActiveLearningNode(initialClassObservations);
        } else if (predictionOption == 1) { //NB
            ret = new LearningNodeNB(initialClassObservations);
        } else { //NBAdaptive
            ret = new LearningNodeNBAdaptive(initialClassObservations);
        }
        return ret;
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == HoeffdingOptionTree.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
