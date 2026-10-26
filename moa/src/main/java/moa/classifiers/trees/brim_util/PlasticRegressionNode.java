package moa.classifiers.trees.brim_util;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PLASTIC node for regression.
 *
 * <p>Instead of class counts, {@code observedClassDistribution} holds the target statistics
 * [weight, sum of targets, sum of squared targets], as expected by
 * {@link moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion}. Numeric attributes are monitored
 * with the Quantization Observer ({@link PlasticQuantizationObserver}) and nominal attributes with {@link PlasticNominalRegressionObserver}.
 * Leaves predict either the mean target or, as in FIMTDD, with a perceptron ({@link PlasticPerceptron}). With
 * adaptive leaves, each node tracks the faded absolute errors of both models and predicts with the better one.</p>
 *
 * <p>Perceptrons are trained in every node an instance passes, not only in the leaves. Nodes that become
 * leaves again (e.g., when restructuring collapses a subtree) and nodes without a successor for an instance
 * thereby have a model trained on their data. New nodes start with a copy of the perceptron of the node they
 * are created from, as the children of a split in FIMTDD.</p>
 *
 * <p>Since the variance reduction is not bounded, merits are divided by the variance of the node's target before
 * comparing them with the Hoeffding bound, i.e., the fraction of the variance that a split explains is used.</p>
 */
public class PlasticRegressionNode extends PlasticNode {

    private static final int NUM_STATS = 3;

    // Estimated size in bytes of a node without its observers and perceptron, see estimateByteSize()
    static final long NODE_BYTES = 1000;

    private final PlasticRegressionContext context;
    // null if the leaves predict the mean target
    private final PlasticPerceptron perceptron;
    // An inactive node has no observers, does not attempt to split and does not re-evaluate its split, see deactivate()
    private boolean active = true;
    // Set once the node was reactivated: its new observers only see part of its instances
    private boolean reactivated = false;
    // Faded absolute errors of the mean target and of the perceptron on the instances of this node,
    // used to choose the leaf model (adaptive leaves)
    private double meanError = 0.0;
    private double perceptronError = 0.0;
    // Attributes that an inner node stopped monitoring to save time and memory (null if none)
    private BitSet ignoredAttributes;
    // Attributes monitored again after being ignored: their observers only see part of the node's instances
    private BitSet partialAttributes;
    private int numReevaluations = 0;
    // The suggestions of the last split evaluation, used to rank the attributes
    private transient AttributeSplitSuggestion[] lastSuggestions;
    private transient boolean cacheSuggestions = false;
    // Every this many re-evaluations, an inner node monitors its ignored attributes again
    static final int ATTRIBUTE_REFRESH_PERIOD = 10;

    public PlasticRegressionNode(
            SplitCriterion splitCriterion, int gracePeriod, Double confidence, Double adaptiveConfidence,
            boolean useAdaptiveConfidence, Integer minSamplesReevaluate, Integer depth,
            Integer maxDepth, Double tau, Double tauReevaluate, Double relMinDeltaG, boolean binaryOnly,
            boolean noPrePrune, NominalAttributeClassObserver nominalObserverBlueprint,
            DoubleVector observedTargetStatistics, List<Integer> usedNominalAttributes,
            int maxBranchLength, double acceptedNumericThresholdDeviation, int blockedAttributeIndex,
            PlasticRegressionContext context, PlasticPerceptron perceptron
    ) {
        // "MC" disables the naive Bayes updates of the classification leaves
        super(splitCriterion, gracePeriod, confidence, adaptiveConfidence, useAdaptiveConfidence, "MC",
                minSamplesReevaluate, depth, maxDepth, tau, tauReevaluate, relMinDeltaG, binaryOnly, noPrePrune,
                nominalObserverBlueprint, padStatistics(observedTargetStatistics), usedNominalAttributes,
                maxBranchLength, acceptedNumericThresholdDeviation, blockedAttributeIndex);
        this.context = context;
        context.numNodesCreated++;
        this.perceptron = context.modelTree ? (perceptron == null ? new PlasticPerceptron() : perceptron) : null;
    }

    /**
     * Copies a node during restructuring. The perceptron is deep-copied with its learning rate, and the state of
     * the memory management, of the leaf model choice and of the attribute selection is kept.
     */
    public PlasticRegressionNode(PlasticRegressionNode other) {
        super(other);
        context = other.context;
        context.numNodesCreated++;
        perceptron = other.perceptron == null ? null : new PlasticPerceptron(other.perceptron, true);
        active = other.active;
        reactivated = other.reactivated;
        meanError = other.meanError;
        perceptronError = other.perceptronError;
        ignoredAttributes = other.ignoredAttributes == null ? null : (BitSet) other.ignoredAttributes.clone();
        partialAttributes = other.partialAttributes == null ? null : (BitSet) other.partialAttributes.clone();
        numReevaluations = other.numReevaluations;
    }

    /**
     * @return a copy of this node that keeps its type, see {@link PlasticNode#copyNode()}
     */
    @Override
    protected PlasticNode copyNode() {
        return new PlasticRegressionNode(this);
    }

    /**
     * Creates a successor of this node (for splits and restructuring). It starts with a copy of this node's
     * perceptron (with a restarted learning rate), as the children of a split in FIMTDD.
     */
    @Override
    protected PlasticRegressionNode newNode(int depth, DoubleVector targetStatistics, List<Integer> usedNominalAttributes) {
        PlasticRegressionNode node = new PlasticRegressionNode(
                splitCriterion, gracePeriod, confidence, adaptiveConfidence, useAdaptiveConfidence,
                minSamplesReevaluate, depth, maxDepth,
                tau, tauReevaluate, relMinDeltaG, binaryOnly, noPrePrune, nominalObserverBlueprint,
                targetStatistics, usedNominalAttributes, maxBranchLength, acceptedNumericThresholdDeviation, getSplitAttributeIndex(),
                context, perceptron == null ? null : new PlasticPerceptron(perceptron, false)
        );
        // A new node starts with the leaf model choice of the node it is created from, as with its perceptron
        node.meanError = meanError;
        node.perceptronError = perceptronError;
        return node;
    }

    /**
     * Makes sure the statistics have the [weight, sum, sum of squares] layout, even if no instance was observed yet.
     */
    private static DoubleVector padStatistics(DoubleVector statistics) {
        double[] padded = new double[NUM_STATS];
        if (statistics != null) {
            for (int i = 0; i < NUM_STATS; i++) {
                padded[i] = statistics.getValue(i);
            }
        }
        return new DoubleVector(padded);
    }

    /**
     * Updates the target statistics [weight, sum, sum of squares] and trains the perceptron. The faded errors of
     * the mean and of the perceptron (adaptive leaves) are updated with their predictions made before learning.
     */
    @Override
    protected void updateStatistics(Instance instance) {
        double weight = instance.weight();
        double target = instance.classValue();
        double nodeWeight = observedClassDistribution.getValue(0);
        double meanPrediction = nodeWeight > 0.0 ? observedClassDistribution.getValue(1) / nodeWeight : Double.NaN;
        observedClassDistribution.addToValue(0, weight);
        observedClassDistribution.addToValue(1, weight * target);
        observedClassDistribution.addToValue(2, weight * target * target);
        if (perceptron != null) {
            double perceptronPrediction = perceptron.update(instance, context);
            // Both models are evaluated on the instance before learning it
            if (context.adaptiveLeaves && !Double.isNaN(meanPrediction)) {
                meanError = context.fadingFactor * meanError + weight * Math.abs(target - meanPrediction);
                perceptronError = context.fadingFactor * perceptronError + weight * Math.abs(target - perceptronPrediction);
            }
        }
    }

    /**
     * The statistics of the node were reset during restructuring, so its region of the instance space changed.
     * The perceptron keeps its weights but its learning ratio decay restarts to adapt to the new region.
     */
    @Override
    protected void resetLeafModel() {
        if (perceptron != null)
            perceptron.restartLearningRate();
    }

    /**
     * Updates the observers of the monitored attributes: QOs (or E-BSTs for the ablation) for numeric attributes
     * and {@link PlasticNominalRegressionObserver}s for nominal ones. Inactive nodes have no observers.
     */
    @Override
    protected void updateObservers(Instance instance) {
        if (!active)
            return;
        if (ignoredAttributes != null && isLeaf()) {
            // The node became a leaf again, which can split on any attribute
            monitorIgnoredAttributes();
        }
        for (int i = 0; i < instance.numAttributes() - 1; i++) {
            if (ignoredAttributes != null && ignoredAttributes.get(i))
                continue;
            int instAttIndex = modelAttIndexToInstanceAttIndex(i, instance);
            AttributeClassObserver obs = this.attributeObservers.get(i);
            if (obs == null) {
                obs = instance.attribute(instAttIndex).isNominal() ? newNominalClassObserver() : newNumericClassObserver();
                if (partialAttributes != null && partialAttributes.get(i))
                    useOwnStatistics(obs);
                this.attributeObservers.set(i, obs);
            }
            if (obs instanceof PlasticNominalRegressionObserver) {
                ((PlasticNominalRegressionObserver) obs).observeAttributeTarget(
                        instance.value(instAttIndex), instance.classValue(), instance.weight());
            } else {
                if (obs instanceof PlasticQuantizationObserver)
                    ((PlasticQuantizationObserver) obs).observeAttributeTarget(
                            instance.value(instAttIndex), instance.classValue(), instance.weight(), context.attributeStdDev(i));
                else
                    ((PlasticEBSTObserver) obs).observeAttributeClass(
                            instance.value(instAttIndex), instance.classValue(), instance.weight());
            }
        }
    }

    /**
     * Inactive leaves (deactivated to save memory) do not attempt to split
     */
    @Override
    protected void attemptInitialSplit(Instance instance) {
        if (active)
            super.attemptInitialSplit(instance);
    }

    /**
     * Re-evaluates the split of an active inner node. With {@code maxInnerNodeAttributes > 0}, the attributes that
     * are not among the best ones are then ignored, and ignored attributes are monitored again periodically.
     */
    @Override
    protected void reevaluateSplit(Instance instance) {
        if (!active)
            return;
        int maxAttributes = context.maxInnerNodeAttributes;
        if (maxAttributes > 0 && ++numReevaluations % ATTRIBUTE_REFRESH_PERIOD == 0 && ignoredAttributes != null) {
            // The ignored attributes compete again from the next re-evaluation on
            monitorIgnoredAttributes();
        }
        lastSuggestions = null;
        cacheSuggestions = maxAttributes > 0;
        super.reevaluateSplit(instance);
        cacheSuggestions = false;
        if (maxAttributes > 0 && !isLeaf() && lastSuggestions != null)
            ignoreWorstAttributes(maxAttributes);
        lastSuggestions = null;
    }

    /**
     * Keeps the suggestions of the re-evaluation in progress, to rank the attributes afterwards
     */
    @Override
    public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion) {
        AttributeSplitSuggestion[] suggestions = super.getBestSplitSuggestions(criterion);
        if (cacheSuggestions)
            lastSuggestions = suggestions;
        return suggestions;
    }

    /**
     * Stops monitoring the attributes that are not among the best ones in the last split evaluation, keeping the
     * split attribute. An inner node only needs its observers to re-evaluate its split, and the attributes that are
     * far from the best split rarely matter. With many attributes, this saves most of the time and memory of the
     * observers of inner nodes.
     * @param maxAttributes the number of attributes to keep monitoring
     */
    private void ignoreWorstAttributes(int maxAttributes) {
        int splitAttribute = getSplitAttributeIndex();
        Map<Integer, Double> merits = new HashMap<>();
        for (AttributeSplitSuggestion suggestion : lastSuggestions) {
            if (suggestion.splitTest != null)
                merits.merge(suggestion.splitTest.getAttsTestDependsOn()[0], suggestion.merit, Math::max);
        }
        List<Integer> monitored = new ArrayList<>();
        for (int i = 0; i < attributeObservers.size(); i++) {
            if (attributeObservers.get(i) != null && i != splitAttribute)
                monitored.add(i);
        }
        if (monitored.size() + 1 <= maxAttributes)
            return;
        monitored.sort((a, b) -> Double.compare(merits.getOrDefault(b, Double.NEGATIVE_INFINITY),
                merits.getOrDefault(a, Double.NEGATIVE_INFINITY)));
        for (int i = maxAttributes - 1; i < monitored.size(); i++) {
            int attribute = monitored.get(i);
            attributeObservers.set(attribute, null);
            if (ignoredAttributes == null)
                ignoredAttributes = new BitSet();
            ignoredAttributes.set(attribute);
            if (partialAttributes != null)
                partialAttributes.clear(attribute);
        }
    }

    /**
     * Monitors the ignored attributes again. Their new observers only see the instances from now on, so they
     * evaluate splits on their own statistics.
     */
    private void monitorIgnoredAttributes() {
        if (partialAttributes == null)
            partialAttributes = new BitSet();
        partialAttributes.or(ignoredAttributes);
        ignoredAttributes = null;
    }

    /**
     * Makes an observer evaluate splits on its own statistics (E-BSTs of the ablation do not support it)
     */
    private static void useOwnStatistics(AttributeClassObserver observer) {
        if (observer instanceof PlasticQuantizationObserver)
            ((PlasticQuantizationObserver) observer).useOwnStatistics();
        else if (observer instanceof PlasticNominalRegressionObserver)
            ((PlasticNominalRegressionObserver) observer).useOwnStatistics();
    }

    /**
     * Resets the observers after restructuring, as {@link PlasticNode} does, but skipping the attributes without an
     * observer. Attributes that were ignored are monitored again.
     */
    @Override
    protected void resetObservers() {
        AutoExpandVector<AttributeClassObserver> newObservers = new AutoExpandVector<>();
        for (int i = 0; i < attributeObservers.size(); i++) {
            AttributeClassObserver observer = attributeObservers.get(i);
            if (observer != null) {
                newObservers.set(i, observer.getClass() == nominalObserverBlueprint.getClass()
                        ? newNominalClassObserver() : newNumericClassObserver());
            }
        }
        attributeObservers = newObservers;
        ignoredAttributes = null;
        partialAttributes = null;
    }

    /**
     * Turns the node into a leaf, discarding its subtree. Used when the nodes of a tree alone exceed its memory
     * budget. The node keeps its statistics and its perceptron, which learned from all instances of the subtree.
     * @return the number of nodes removed
     */
    public int collapseToLeaf() {
        List<PlasticRegressionNode> subtree = new ArrayList<>();
        collectSubtree(subtree);
        killSubtree();
        resetSplitAttribute();
        return subtree.size() - 1;
    }

    /**
     * @return true if the node has no successors
     */
    public boolean isLeafNode() {
        return isLeaf();
    }

    /**
     * @return false if the node was deactivated to save memory
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Deactivates the node to save memory: its observers are discarded. A leaf stops attempting to split, as in
     * {@link moa.classifiers.trees.HoeffdingTree}, and an inner node stops re-evaluating its split, which freezes it.
     * The node still updates its statistics and its perceptron, so its predictions keep improving.
     */
    public void deactivate() {
        active = false;
        attributeObservers = new AutoExpandVector<>();
    }

    /**
     * Activates a deactivated node. Its observers start empty, so they evaluate splits on their own statistics
     * and the counters behind its split decisions (e.g., the number of instances in the Hoeffding bound) restart.
     */
    public void activate() {
        active = true;
        reactivated = true;
        seenWeight = 0.0;
        nodeTime = 0;
        numSplitAttempts = 0;
    }

    /**
     * The promise of a node, used to decide which nodes stay active when memory is limited:
     * the sum of squared deviations from its mean target, i.e., the variance that splits of the node could explain.
     * @return the promise
     */
    public double getPromise() {
        double weight = observedClassDistribution.getValue(0);
        return weight > 0.0 ? weight * getTargetVariance() : 0.0;
    }

    /**
     * @return the estimated size of the observers of this node in bytes
     */
    public long estimateObserversByteSize() {
        long size = 0;
        for (AttributeClassObserver obs : attributeObservers) {
            if (obs instanceof PlasticQuantizationObserver)
                size += ((PlasticQuantizationObserver) obs).estimateByteSize();
            else if (obs instanceof PlasticEBSTObserver)
                size += ((PlasticEBSTObserver) obs).estimateByteSize();
            else if (obs instanceof PlasticNominalRegressionObserver)
                size += ((PlasticNominalRegressionObserver) obs).estimateByteSize();
        }
        return size;
    }

    /**
     * Estimates the size of this node in bytes, without its successors. The estimate is computed from the structure
     * of the node (e.g., the number of quantization slots), so it is cheap and does not need the SizeOf agent.
     * @param includeObservers whether the size of the observers is included
     * @return the estimated size in bytes
     */
    public long estimateByteSize(boolean includeObservers) {
        long size = NODE_BYTES;
        if (perceptron != null)
            size += perceptron.estimateByteSize();
        if (includeObservers)
            size += estimateObserversByteSize();
        return size;
    }

    /**
     * An upper bound of the growth of the estimated size of a tree per instance: the instance can add a slot (or a
     * nominal value) to each observer of each node on its path, and a split or a restructuring can create nodes.
     * @param numAttributes the number of attributes (without the target)
     * @param maxDepth the maximum depth of the tree
     * @param maxBranchLength the maximum length of the branches created by restructuring
     * @return the maximum growth in bytes
     */
    public static double maxGrowthPerInstance(int numAttributes, int maxDepth, int maxBranchLength) {
        double newNodeBytes = NODE_BYTES + PlasticPerceptron.BASE_BYTES + 8.0 * (numAttributes + 1);
        // A new slot of a QO, or a new node of an E-BST (ablation), for each attribute of each node of the path
        long valueBytes = Math.max(PlasticQuantizationObserver.SLOT_BYTES, PlasticEBSTObserver.NODE_BYTES);
        return (maxDepth + 1.0) * numAttributes * valueBytes
                + (maxBranchLength + 2.0) * newNodeBytes;
    }

    /**
     * @return the successors of this node (empty for a leaf)
     */
    public List<PlasticRegressionNode> getSuccessorNodes() {
        List<PlasticRegressionNode> nodes = new ArrayList<>();
        if (!isLeaf()) {
            for (CustomEFDTNode s : getSuccessors().getAllSuccessors())
                nodes.add((PlasticRegressionNode) s);
        }
        return nodes;
    }

    /**
     * Adds this node and all nodes of its subtree to the list
     * @param nodes the list to add the nodes to
     */
    public void collectSubtree(List<PlasticRegressionNode> nodes) {
        nodes.add(this);
        if (!isLeaf()) {
            for (CustomEFDTNode s : getSuccessors().getAllSuccessors())
                ((PlasticRegressionNode) s).collectSubtree(nodes);
        }
    }

    /**
     * @return a new Quantization Observer, or an E-BST for the ablation. The observers of a reactivated node only
     *         see part of its instances, so they evaluate splits on their own statistics.
     */
    @Override
    protected NumericAttributeClassObserver newNumericClassObserver() {
        if (context.usesEBST())
            return new PlasticEBSTObserver();
        PlasticQuantizationObserver observer = new PlasticQuantizationObserver(context.quantizationRadius);
        if (reactivated)
            observer.useOwnStatistics();
        return observer;
    }

    /**
     * @return a new nominal observer, which evaluates splits on its own statistics if the node was reactivated
     */
    @Override
    protected NominalAttributeClassObserver newNominalClassObserver() {
        NominalAttributeClassObserver observer = super.newNominalClassObserver();
        if (reactivated && observer instanceof PlasticNominalRegressionObserver)
            ((PlasticNominalRegressionObserver) observer).useOwnStatistics();
        return observer;
    }

    /**
     * Evaluates the current threshold of the split (re-evaluation) or a threshold given by restructuring
     */
    @Override
    protected AttributeSplitSuggestion forceNumericSplit(AttributeClassObserver observer, double[] preSplitDist, int attIndex, double threshold) {
        if (observer instanceof PlasticEBSTObserver)
            return ((PlasticEBSTObserver) observer).forceSplit(splitCriterion, preSplitDist, attIndex, threshold);
        return ((PlasticQuantizationObserver) observer).forceSplit(splitCriterion, preSplitDist, attIndex, threshold);
    }

    /**
     * The variance reduction is divided by the target variance of the node: the fraction of the variance that a
     * split explains is compared with the Hoeffding bound, whose range is then 1.
     */
    @Override
    protected double normalizeMerit(double merit) {
        double variance = getTargetVariance();
        return variance > 0.0 ? merit / variance : 0.0;
    }

    /**
     * The node is pure if the target has (numerically) zero variance.
     * @return true if all observed targets are equal
     */
    @Override
    boolean isPure() {
        double weight = observedClassDistribution.getValue(0);
        if (weight <= 0.0)
            return true;
        double secondMoment = observedClassDistribution.getValue(2) / weight;
        return getTargetVariance() <= 1e-10 * secondMoment;
    }

    /**
     * @return the variance of the target in this node
     */
    private double getTargetVariance() {
        double weight = observedClassDistribution.getValue(0);
        if (weight <= 0.0)
            return 0.0;
        double sum = observedClassDistribution.getValue(1);
        double sumSq = observedClassDistribution.getValue(2);
        return Math.max(0.0, (sumSq - sum * sum / weight) / weight);
    }

    /**
     * Predicts with the leaf the instance falls into, or with the deepest node on its path if a successor is missing.
     * The node predicts with its perceptron or, if its faded absolute error is lower (adaptive leaves) or if the
     * tree is a regression tree, with the mean target. Nodes that have not observed any instance yet (e.g., after
     * restructuring) use the mean target of their parent, or their copy of the parent's perceptron.
     * @param instance the instance to predict
     * @return an array containing the predicted target
     */
    @Override
    public double[] predict(Instance instance) {
        return new double[]{predictTarget(instance, 0.0)};
    }

    /**
     * @param fallback the mean target of the closest ancestor with instances, used by nodes without instances
     */
    private double predictTarget(Instance instance, double fallback) {
        double weight = observedClassDistribution.getValue(0);
        double mean = weight > 0.0 ? observedClassDistribution.getValue(1) / weight : fallback;
        if (!isLeaf()) {
            CustomEFDTNode successor = getSuccessor(instance);
            if (successor != null)
                return ((PlasticRegressionNode) successor).predictTarget(instance, mean);
        }
        if (perceptron != null && (!context.adaptiveLeaves || perceptronError <= meanError))
            return perceptron.predict(instance, context);
        return mean;
    }

    /**
     * @return true if the node predicts with its perceptron (if it is reached by an instance)
     */
    public boolean usesPerceptron() {
        return perceptron != null && (!context.adaptiveLeaves || perceptronError <= meanError);
    }

    /**
     * Used during restructuring to collapse subtrees whose pure leaves all predict the same value.
     * @return the set of mean targets of the leaves
     */
    @Override
    protected Set<Double> getMajorityVotesOfLeaves() {
        Set<Double> leafPredictions = new HashSet<>();
        if (isLeaf()) {
            double weight = observedClassDistribution.getValue(0);
            if (weight > 0.0)
                leafPredictions.add(observedClassDistribution.getValue(1) / weight);
            return leafPredictions;
        }
        for (CustomEFDTNode s : getSuccessors().getAllSuccessors()) {
            leafPredictions.addAll(((PlasticNode) s).getMajorityVotesOfLeaves());
        }
        return leafPredictions;
    }

    /**
     * Keeps the [weight, sum, sum of squares] layout when restructuring sets the statistics
     */
    @Override
    protected void setObservedClassDistribution(double[] newDistribution) {
        super.setObservedClassDistribution(padStatistics(new DoubleVector(newDistribution)).getArrayRef());
    }

    /**
     * Resets the target statistics, keeping their layout
     */
    @Override
    protected void resetObservedClassDistribution() {
        super.resetObservedClassDistribution();
        observedClassDistribution = padStatistics(null);
        classDistributionAtTimeOfCreation = padStatistics(null);
    }
}
