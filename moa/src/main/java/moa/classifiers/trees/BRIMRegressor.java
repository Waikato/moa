package moa.classifiers.trees;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.trees.brim_util.MeasuresNumberOfLeaves;
import moa.classifiers.trees.brim_util.PerformsTreeRevision;
import moa.classifiers.trees.brim_util.PlasticNominalRegressionObserver;
import moa.classifiers.trees.brim_util.PlasticRegressionContext;
import moa.classifiers.trees.brim_util.PlasticRegressionNode;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * PLASTIC for regression
 *
 * <p>Regression version of {@link PLASTIC}: restructures the subtrees that would have been pruned by EFDT.
 * As in FIMTDD, splits are evaluated with the variance reduction
 * ({@link moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion}). Numeric attributes are
 * monitored with the Quantization Observer (QO), whose bounded number of slots keeps the re-evaluation of
 * splits at inner nodes cheap. As in FIMTDD, leaves predict with a perceptron (model tree), or the mean target if
 * {@code -e} (regressionTree) is set. Splits on nominal attributes are binary unless {@code -M} is set. By default, each leaf predicts with whichever of its perceptron and its mean
 * target has the lower faded absolute error (adaptive leaves). The variance reduction of a split is divided by the
 * target variance of the node before it is compared with the Hoeffding bound.</p>
 *
 * <p>Drifts are handled as in {@link moa.classifiers.meta.AdaptiveRandomForestRegressor}: two change detectors
 * monitor the standardized absolute error of the tree (computed before it learns the instance). When the warning
 * detector fires, a background tree starts growing. When the drift detector fires, the background tree replaces
 * the tree as soon as its faded absolute error since it started is lower, checked every grace period for up to 10
 * grace periods after the drift (as FIMTDD does for its alternate trees). If there is no background tree, one is
 * started. A background tree that does not replace the tree within that window, or that no drift follows within
 * that window after a warning, is discarded. Both trees share
 * the memory limit.</p>
 *
 * <p>See details in:<br> Marco Heyden, Heitor Murilo Gomes, Edouard Fouché, Bernhard Pfahringer, Klemens Böhm:
 * Leveraging Plasticity in Incremental Decision Trees. ECML/PKDD (5) 2024: 38-54</p>
 *
 * <p>Saulo Martiello Mastelini, Jacob Montiel, Heitor Murilo Gomes, Albert Bifet, Bernhard Pfahringer,
 * André C. P. L. F. de Carvalho: Fast and lightweight binary and multi-branch Hoeffding Tree Regressors.
 * ICDM Workshops 2021: 380-388</p>
 */
public class BRIMRegressor extends AbstractClassifier implements Regressor, PerformsTreeRevision, MeasuresNumberOfLeaves {

    private static final long serialVersionUID = 1L;

    PlasticRegressionNode root;
    // Grown after a drift warning, replaces the tree when a drift is detected (null if there is none)
    PlasticRegressionNode backgroundRoot;
    PlasticRegressionContext context;
    int seenItems = 0;
    MemoryReservations rootMemory = new MemoryReservations();
    MemoryReservations backgroundMemory;
    ChangeDetector warningDetector;
    ChangeDetector driftDetector;
    int numWarnings = 0;
    int numDrifts = 0;
    int numReplacements = 0;
    int numPrunedNodes = 0;
    // A drift was detected: the background tree replaces the tree once it has seen enough instances, if it is better
    boolean replacementPending = false;
    // Faded absolute errors of both trees since the background tree started, and its number of instances
    double foregroundFadedError = 0.0;
    double backgroundFadedError = 0.0;
    long backgroundInstances = 0;
    long instancesSinceDrift = 0;
    // The background tree is compared with the tree every grace period after a drift, during this many grace periods
    static final int REPLACEMENT_GRACE_PERIODS = 10;
    // The number of nodes created and the estimated size of both trees when the memory was last checked
    int numNodesAtLastCheck = 0;
    long estimatedBytesAtLastCheck = 0;
    // The number of instances at which the memory is checked next
    long nextMemoryCheck = 0;

    /**
     * The memory reserved by the nodes of a tree: the largest average estimated sizes of the observers of active
     * leaves and of active inner nodes seen so far. The observers of young nodes, or of nodes whose observers were
     * reset by restructuring, are small but grow quickly.
     */
    static class MemoryReservations implements Serializable {
        private static final long serialVersionUID = 1L;
        double leafObserversBytes = 0.0;
        double innerObserversBytes = 0.0;

        /**
         * A new tree starts with the reservations of the current tree, since its observers will grow similarly
         * @return a copy of these reservations
         */
        MemoryReservations copy() {
            MemoryReservations copy = new MemoryReservations();
            copy.leafObserversBytes = leafObserversBytes;
            copy.innerObserversBytes = innerObserversBytes;
            return copy;
        }
    }

    public IntOption gracePeriodOption = new IntOption(
            "gracePeriod",
            'g',
            "The number of instances a leaf should observe between split attempts.",
            200, 0, Integer.MAX_VALUE);

    public IntOption reEvalPeriodOption = new IntOption(
            "reevaluationPeriod",
            'R',
            "The number of instances an internal node should observe between re-evaluation attempts.",
            200, 0, Integer.MAX_VALUE);

    public ClassOption splitCriterionOption = new ClassOption("splitCriterion",
            's', "Split criterion to use.", SplitCriterion.class,
            "moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion");

    public FloatOption splitConfidenceOption = new FloatOption(
            "splitConfidence",
            'c',
            "The allowable error in split decision when using fixed confidence. Values closer to 0 will take longer to decide.",
            0.0000001, 0.0, 1.0);

    public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
            't', "Threshold below which a split will be forced to break ties.",
            0.05, 0.0, 1.0);

    public FloatOption tieThresholdReevalOption = new FloatOption("tieThresholdReevaluation",
            'T', "Threshold below which a split will be forced to break ties during reevaluation.",
            0.05, 0.0, 1.0);

    public FloatOption relMinDeltaG = new FloatOption("relMinDeltaG",
            'G', "Relative minimum variance reduction to split a tie during reevaluation.",
            0.5, 0.0, 1.0);

    public FlagOption multiwaySplitsOption = new FlagOption("multiwaySplits", 'M',
            "Allow multiway splits on nominal attributes (one branch per value). By default, nominal splits are binary (one value against the rest), which keeps more data in each branch.");

    public IntOption maxDepthOption = new IntOption(
            "maxDepth",
            'D',
            "Maximum allowed depth of tree.",
            20, 0, Integer.MAX_VALUE);

    public IntOption maxBranchLengthOption = new IntOption(
            "maxBranchLength",
            'B',
            "Maximum allowed length of branches during restructuring.",
            5, 1, Integer.MAX_VALUE);

    public FloatOption quantizationRadiusOption = new FloatOption(
            "quantizationRadius",
            'q',
            "Width of the intervals into which the Quantization Observer divides a numeric attribute, relative to the standard deviation of the attribute.",
            0.25, Double.MIN_VALUE, Double.MAX_VALUE);

    public IntOption maxByteSizeOption = new IntOption(
            "maxByteSize",
            'm',
            "Maximum memory consumed by the tree and its background tree, estimated from their structure (0 for no limit). Leaves and then inner nodes are deactivated to stay below it.",
            33554432, 0, Integer.MAX_VALUE);

    public IntOption memoryEstimatePeriodOption = new IntOption(
            "memoryEstimatePeriod",
            'E',
            "How many instances between memory consumption checks. Checks are less frequent while the tree is far below maxByteSize. Once it uses more than half of it, the memory is also checked whenever nodes are created.",
            100, 1, Integer.MAX_VALUE);

    public FlagOption disableDriftDetectionOption = new FlagOption(
            "disableDriftDetection", 'u', "Disable drift detection, and thereby the background tree.");

    public ClassOption warningDetectionMethodOption = new ClassOption("warningDetectionMethod", 'w',
            "Change detector for warnings on the standardized absolute error (starts a background tree).",
            ChangeDetector.class, "ADWINChangeDetector -a 1.0E-2");

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
            "Change detector for drifts on the standardized absolute error (the background tree replaces the tree).",
            ChangeDetector.class, "ADWINChangeDetector -a 1.0E-3");

    public FloatOption backgroundMemoryFractionOption = new FloatOption(
            "backgroundMemoryFraction", 'F',
            "Fraction of maxByteSize that the background tree can use. The tree uses the memory that the background tree does not reserve.",
            0.5, 0.0, 1.0);

    // AbstractClassifier's -r is not settable from the command line (its option field is not public)
    public IntOption randomSeedOption = new IntOption("randomSeed", 'Z', "The random seed", 1);

    public FlagOption noAdaptiveLeavesOption = new FlagOption("noAdaptiveLeaves", 'N',
            "Always predict with the perceptron in model trees, instead of choosing in each leaf between the perceptron and the mean target by their faded absolute errors.");

    public FloatOption fadingFactorOption = new FloatOption("fadingFactor", 'f',
            "Fading factor of the absolute errors used to choose the leaf model and to decide if the background tree replaces the tree.",
            0.995, 0.0, 1.0);

    public IntOption maxInnerNodeAttributesOption = new IntOption("maxInnerNodeAttributes", 'k',
            "Maximum number of attributes monitored by an inner node to re-evaluate its split: the split attribute and the best ones in the last evaluation. The others are monitored again every 10 re-evaluations. 0 for all attributes.",
            5, 0, Integer.MAX_VALUE);

    // Ablation switches: each disables one component of the method
    public FlagOption ebstObserverOption = new FlagOption("ebstObserver", 'O',
            "Ablation: monitor numeric attributes with the E-BST of FIMTDD instead of the Quantization Observer.");

    public FlagOption noClippingOption = new FlagOption("noClipping", 'C',
            "Ablation: do not clip the normalized inputs and errors of the perceptrons.");

    public FlagOption immediateReplacementOption = new FlagOption("immediateReplacement", 'I',
            "Ablation: replace the tree with the background tree as soon as a drift is detected (as AdaptiveRandomForestRegressor does), instead of only if the background tree is more accurate.");

    public FlagOption regressionTreeOption = new FlagOption(
            "regressionTree", 'e', "Build a regression tree (leaves predict the mean target) instead of a model tree (leaves predict with a perceptron).");

    public FloatOption learningRatioOption = new FloatOption(
            "learningRatio", 'l', "Learning ratio to used for training the Perceptrons in the leaves.",
            0.02, 0, 1.00);

    public FloatOption learningRateDecayFactorOption = new FloatOption(
            "learningRatioDecayFactor", 'd', "Learning rate decay factor (not used when learning rate is constant).",
            0.001, 0, 1.00);

    public FlagOption learningRatioConstOption = new FlagOption(
            "learningRatioConst", 'p', "Keep learning rate constant instead of decaying.");

    @Override
    public String getPurposeString() {
        return "PLASTIC for regression, using the variance reduction split criterion of FIMTDD.";
    }

    /**
     * Creates the tree, its context and the change detectors
     **/
    void initialize() {
        context = new PlasticRegressionContext(
                !regressionTreeOption.isSet(),
                learningRatioOption.getValue(),
                learningRateDecayFactorOption.getValue(),
                learningRatioConstOption.isSet(),
                classifierRandom,
                quantizationRadiusOption.getValue(),
                !noAdaptiveLeavesOption.isSet(),
                fadingFactorOption.getValue(),
                maxInnerNodeAttributesOption.getValue()
        );
        context.setAblation(ebstObserverOption.isSet(), !noClippingOption.isSet());
        root = newRoot();
        rootMemory = new MemoryReservations();
        warningDetector = newChangeDetector(warningDetectionMethodOption);
        driftDetector = newChangeDetector(driftDetectionMethodOption);
        nextMemoryCheck = memoryEstimatePeriodOption.getValue();
    }

    /**
     * @param option the option holding the change detector and its parameters
     * @return a new, prepared change detector
     */
    ChangeDetector newChangeDetector(ClassOption option) {
        return ((ChangeDetector) getPreparedClassOption(option)).copy();
    }

    /**
     * Creates and configures the root node of a new tree
     *
     * @return the created root node
     **/
    PlasticRegressionNode newRoot() {
        return new PlasticRegressionNode(
                (SplitCriterion) getPreparedClassOption(splitCriterionOption),
                gracePeriodOption.getValue(),
                splitConfidenceOption.getValue(),
                0.2,
                false,
                reEvalPeriodOption.getValue(),
                0,
                maxDepthOption.getValue(),
                tieThresholdOption.getValue(),
                tieThresholdReevalOption.getValue(),
                relMinDeltaG.getValue(),
                !multiwaySplitsOption.isSet(),
                true,
                new PlasticNominalRegressionObserver(),
                new DoubleVector(),
                new ArrayList<>(),
                maxBranchLengthOption.getValue(),
                0.05,
                -1,
                context,
                null
        );
    }

    /**
     * Trains the tree on the provided instance
     * @param inst The instance to train on
     **/
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (root == null)
            initialize();
        context.update(inst);
        // The errors of the trees on the instance before learning it, as in prequential evaluation
        double error = Double.NaN;
        if (!disableDriftDetectionOption.isSet()) {
            double absoluteError = Math.abs(inst.classValue() - root.predict(inst)[0]);
            if (backgroundRoot != null) {
                double fadingFactor = fadingFactorOption.getValue();
                foregroundFadedError = fadingFactor * foregroundFadedError + absoluteError;
                backgroundFadedError = fadingFactor * backgroundFadedError
                        + Math.abs(inst.classValue() - backgroundRoot.predict(inst)[0]);
                backgroundInstances++;
            }
            double stdDev = context.targetStdDev();
            error = stdDev > 0.0 ? absoluteError / stdDev : Double.NaN;
        }
        root.learnInstance(inst, seenItems);
        if (backgroundRoot != null)
            backgroundRoot.learnInstance(inst, seenItems);
        seenItems++;
        if (!Double.isNaN(error))
            detectDrift(error);
        if (replacementPending) {
            instancesSinceDrift++;
            int gracePeriod = Math.max(gracePeriodOption.getValue(), 1);
            if (backgroundInstances >= gracePeriod && instancesSinceDrift % gracePeriod == 0)
                decideReplacement();
        } else if (backgroundRoot != null && backgroundInstances >= replacementWindow()) {
            // No drift confirmed the warning within the window: the background tree only costs time and memory
            discardBackgroundTree();
        }
        // The memory is checked periodically, and as soon as nodes were created (new nodes start active) unless the
        // tree was far below the limit at the last check
        boolean nodesCreated = context.getNumNodesCreated() != numNodesAtLastCheck
                && 2 * estimatedBytesAtLastCheck > maxByteSizeOption.getValue();
        if (maxByteSizeOption.getValue() > 0 && (seenItems >= nextMemoryCheck || nodesCreated)) {
            enforceMemoryLimit();
            numNodesAtLastCheck = context.getNumNodesCreated();
            // Far below the limit, the next check waits until the tree could have reached the limit
            double maxGrowth = (backgroundRoot == null ? 1 : 2) * PlasticRegressionNode.maxGrowthPerInstance(
                    inst.numAttributes() - 1, maxDepthOption.getValue(), maxBranchLengthOption.getValue());
            long instancesToLimit = (long) ((maxByteSizeOption.getValue() - estimatedBytesAtLastCheck) / maxGrowth);
            nextMemoryCheck = seenItems + Math.max(memoryEstimatePeriodOption.getValue(), instancesToLimit);
        }
    }

    /**
     * Updates the change detectors with the standardized absolute error of the tree (its absolute error divided by
     * the standard deviation of the target). A warning starts a new background tree, replacing the current one
     * unless a replacement is pending. A drift makes the replacement pending, starting a background tree if there
     * is none.
     * @param error the standardized absolute error of the tree on the current instance
     */
    void detectDrift(double error) {
        warningDetector.input(error);
        if (warningDetector.getChange()) {
            numWarnings++;
            if (!replacementPending)
                startBackgroundTree();
            warningDetector = newChangeDetector(warningDetectionMethodOption);
        }
        driftDetector.input(error);
        if (driftDetector.getChange() && immediateReplacementOption.isSet()) {
            // Ablation: the background tree (or a new tree) replaces the tree right away
            numDrifts++;
            if (backgroundRoot == null) {
                root = newRoot();
                rootMemory = rootMemory.copy();
            } else {
                root = backgroundRoot;
                rootMemory = backgroundMemory;
                numReplacements++;
            }
            discardBackgroundTree();
            replacementPending = false;
            warningDetector = newChangeDetector(warningDetectionMethodOption);
            driftDetector = newChangeDetector(driftDetectionMethodOption);
        } else if (driftDetector.getChange()) {
            numDrifts++;
            // Further drifts while a replacement is pending do not extend the comparison window: otherwise, frequent
            // drift signals keep a background tree that never becomes better (and grows) forever
            if (!replacementPending) {
                if (backgroundRoot == null)
                    startBackgroundTree();
                replacementPending = true;
                instancesSinceDrift = 0;
            }
            warningDetector = newChangeDetector(warningDetectionMethodOption);
            driftDetector = newChangeDetector(driftDetectionMethodOption);
        }
    }

    /**
     * Starts a new background tree, replacing the current one if there is one. It starts with the memory
     * reservations of the tree, and the comparison of both trees restarts.
     */
    void startBackgroundTree() {
        backgroundRoot = newRoot();
        backgroundMemory = rootMemory.copy();
        foregroundFadedError = 0.0;
        backgroundFadedError = 0.0;
        backgroundInstances = 0;
    }

    /**
     * The background tree replaces the tree as soon as its faded absolute error since it started is lower, which is
     * checked every grace period after a drift. If it is not better within {@link #REPLACEMENT_GRACE_PERIODS} grace
     * periods, the tree is kept and the background tree is discarded, as FIMTDD discards its alternate trees.
     */
    void decideReplacement() {
        if (backgroundFadedError < foregroundFadedError) {
            root = backgroundRoot;
            rootMemory = backgroundMemory;
            backgroundRoot = null;
            backgroundMemory = null;
            numReplacements++;
            replacementPending = false;
        } else if (instancesSinceDrift >= replacementWindow()) {
            replacementPending = false;
            discardBackgroundTree();
        }
    }

    /**
     * @return the number of instances during which a background tree can replace the tree after a drift, and the
     *         maximum age of a background tree without a drift
     */
    long replacementWindow() {
        return (long) REPLACEMENT_GRACE_PERIODS * Math.max(gracePeriodOption.getValue(), 1);
    }

    /**
     * Discards the background tree, freeing its memory
     */
    void discardBackgroundTree() {
        backgroundRoot = null;
        backgroundMemory = null;
    }

    /**
     * Keeps the estimated size of both trees below {@code maxByteSize}. The background tree can use up to
     * {@code backgroundMemoryFraction} of it, and the tree uses what the background tree does not reserve.
     */
    void enforceMemoryLimit() {
        long maxByteSize = maxByteSizeOption.getValue();
        if (backgroundRoot == null) {
            estimatedBytesAtLastCheck = enforceMemoryLimit(root, rootMemory, maxByteSize)[0];
        } else {
            long[] background = enforceMemoryLimit(backgroundRoot, backgroundMemory,
                    (long) (backgroundMemoryFractionOption.getValue() * maxByteSize));
            long[] foreground = enforceMemoryLimit(root, rootMemory, maxByteSize - background[1]);
            estimatedBytesAtLastCheck = foreground[0] + background[0];
        }
    }

    /**
     * Keeps the estimated size of a tree below a budget. Nodes without observers are always kept, and the
     * remaining memory goes to the observers of the nodes in this order: inner nodes before leaves, and, within
     * each group, the most promising nodes first. Nodes whose observers do not fit are deactivated: they discard
     * their observers, leaves stop splitting (as in {@link HoeffdingTree}) and inner nodes stop re-evaluating their
     * split. So leaves are deactivated first, and inner nodes only when all leaves are inactive and the tree is
     * still too large. Deactivated nodes are activated again when memory becomes available. An active leaf also
     * reserves the nodes of a split. If the nodes alone exceed the budget (e.g., after restructuring), the least
     * promising subtrees are pruned first.
     * @param treeRoot the root of the tree
     * @param reservations the memory reservations of the tree
     * @param budget the budget in bytes
     * @return the estimated size of the tree and the memory it reserves, in bytes
     */
    long[] enforceMemoryLimit(PlasticRegressionNode treeRoot, MemoryReservations reservations, long budget) {
        pruneToBudget(treeRoot, budget);
        List<PlasticRegressionNode> nodes = new ArrayList<>();
        treeRoot.collectSubtree(nodes);
        List<PlasticRegressionNode> innerNodes = new ArrayList<>();
        List<PlasticRegressionNode> leaves = new ArrayList<>();
        long nodesBytes = 0;
        long innerObserversBytes = 0, leafObserversBytes = 0;
        int numActiveInner = 0, numActiveLeaves = 0;
        for (PlasticRegressionNode node : nodes) {
            nodesBytes += node.estimateByteSize(false);
            if (node.isLeafNode()) {
                leaves.add(node);
                if (node.isActive()) {
                    leafObserversBytes += node.estimateObserversByteSize();
                    numActiveLeaves++;
                }
            } else {
                innerNodes.add(node);
                if (node.isActive()) {
                    innerObserversBytes += node.estimateObserversByteSize();
                    numActiveInner++;
                }
            }
        }
        // Without any active node of a kind, the other kind's reservation is used
        if (numActiveLeaves > 0)
            reservations.leafObserversBytes = Math.max(reservations.leafObserversBytes, (double) leafObserversBytes / numActiveLeaves);
        if (numActiveInner > 0)
            reservations.innerObserversBytes = Math.max(reservations.innerObserversBytes, (double) innerObserversBytes / numActiveInner);
        double leafReservation = reservations.leafObserversBytes > 0.0 ? reservations.leafObserversBytes : reservations.innerObserversBytes;
        double innerReservation = reservations.innerObserversBytes > 0.0 ? reservations.innerObserversBytes : reservations.leafObserversBytes;

        Comparator<PlasticRegressionNode> byPromise = Comparator.comparingDouble(PlasticRegressionNode::getPromise).reversed();
        innerNodes.sort(byPromise);
        leaves.sort(byPromise);
        List<PlasticRegressionNode> byPriority = new ArrayList<>(innerNodes);
        byPriority.addAll(leaves);

        double remaining = budget - nodesBytes;
        boolean full = false;
        long estimatedBytes = nodesBytes;
        double reservedBytes = nodesBytes;
        // An active leaf can split, so it also reserves the nodes (without observers) of a binary split. Otherwise
        // the nodes alone could grow beyond the budget, since nodes are never removed to save memory.
        double splitBytes = 2.0 * nodesBytes / nodes.size();
        for (PlasticRegressionNode node : byPriority) {
            double reservation = node.isLeafNode() ? leafReservation : innerReservation;
            double cost = node.isActive() ? Math.max(node.estimateObserversByteSize(), reservation) : reservation;
            if (node.isLeafNode())
                cost += splitBytes;
            if (!full && cost <= remaining) {
                remaining -= cost;
                reservedBytes += cost;
                if (!node.isActive())
                    node.activate();
            } else {
                full = true;
                if (node.isActive())
                    node.deactivate();
            }
            estimatedBytes += node.estimateObserversByteSize();
        }
        return new long[]{estimatedBytes, (long) reservedBytes};
    }

    /**
     * Removes nodes if the nodes of a tree alone (without observers) exceed its budget, which restructuring can cause
     * because it can duplicate parts of subtrees. The least promising inner nodes whose successors are all leaves
     * become leaves, until the nodes fit.
     * @param treeRoot the root of the tree
     * @param budget the budget in bytes
     */
    void pruneToBudget(PlasticRegressionNode treeRoot, long budget) {
        while (true) {
            List<PlasticRegressionNode> nodes = new ArrayList<>();
            treeRoot.collectSubtree(nodes);
            long nodesBytes = 0;
            for (PlasticRegressionNode node : nodes)
                nodesBytes += node.estimateByteSize(false);
            if (nodesBytes <= budget)
                return;
            List<PlasticRegressionNode> candidates = new ArrayList<>();
            for (PlasticRegressionNode node : nodes) {
                if (node.isLeafNode())
                    continue;
                boolean allLeaves = true;
                for (PlasticRegressionNode successor : node.getSuccessorNodes())
                    allLeaves &= successor.isLeafNode();
                if (allLeaves)
                    candidates.add(node);
            }
            if (candidates.isEmpty())
                return;
            candidates.sort(Comparator.comparingDouble(PlasticRegressionNode::getPromise));
            double bytesPerNode = (double) nodesBytes / nodes.size();
            for (PlasticRegressionNode candidate : candidates) {
                int removed = candidate.collapseToLeaf();
                numPrunedNodes += removed;
                nodesBytes -= (long) (removed * bytesPerNode);
                if (nodesBytes <= budget)
                    return;
            }
        }
    }

    /**
     * @return the estimated size of the tree and its background tree in bytes
     */
    public long estimateByteSize() {
        return estimateByteSize(root) + estimateByteSize(backgroundRoot);
    }

    /**
     * @param treeRoot the root of a tree (or null)
     * @return the estimated size of the tree in bytes
     */
    static long estimateByteSize(PlasticRegressionNode treeRoot) {
        if (treeRoot == null)
            return 0;
        List<PlasticRegressionNode> nodes = new ArrayList<>();
        treeRoot.collectSubtree(nodes);
        long size = 0;
        for (PlasticRegressionNode node : nodes)
            size += node.estimateByteSize(true);
        return size;
    }

    /**
     * Predicts with the tree. The background tree is never used for predictions.
     */
    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (root == null) {
            initialize();
            return new double[]{0.0};
        }
        return root.predict(inst);
    }

    /**
     * Resets the tree, the background tree, the change detectors and the memory management state. The random seed
     * is taken from -Z, or from the inherited -r option if it was set programmatically (e.g., by setRandomSeed).
     */
    @Override
    public void resetLearningImpl() {
        if (super.randomSeedOption.getValue() != Integer.parseInt(super.randomSeedOption.getDefaultCLIString())) // super random set
            this.randomSeedOption.setValue(super.randomSeedOption.getValue()); // override current random
        this.classifierRandom = new Random(randomSeedOption.getValue());
        root = null;
        backgroundRoot = null;
        context = null;
        seenItems = 0;
        rootMemory = new MemoryReservations();
        backgroundMemory = null;
        warningDetector = null;
        driftDetector = null;
        numWarnings = 0;
        numDrifts = 0;
        numReplacements = 0;
        numPrunedNodes = 0;
        replacementPending = false;
        instancesSinceDrift = 0;
        foregroundFadedError = 0.0;
        backgroundFadedError = 0.0;
        backgroundInstances = 0;
        numNodesAtLastCheck = 0;
        estimatedBytesAtLastCheck = 0;
        nextMemoryCheck = 0;
    }

    /**
     * Checks if a tree revision (e.g., pruning or restructuring) was performed at the current time step
     *
     * @return if a tree revision was performed
     **/
    @Override
    public boolean didPerformTreeRevision() {
        return root != null && root.didPerformTreeRevision();
    }

    /**
     * Returns the number of leaves of the tree.
     *
     * <p>
     *     Note that calling this function repeatedly causes some overhead
     *     as the function traverses the tree recursively.
     * </p>
     *
     * @return the number of leaves
     **/
    @Override
    public int getLeafNumber() {
        return root == null ? 0 : root.getLeafNumber();
    }

    /**
     * Reports the structure of the tree (leaves are counted by state) and of the background tree, the drift handling
     * counters and the estimated size of both trees.
     */
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        int numNodes = 0, numActive = 0, numInactive = 0, numInactiveInner = 0, numPerceptronLeaves = 0;
        long byteSize = 0;
        if (root != null) {
            List<PlasticRegressionNode> nodes = new ArrayList<>();
            root.collectSubtree(nodes);
            numNodes = nodes.size();
            for (PlasticRegressionNode node : nodes) {
                byteSize += node.estimateByteSize(true);
                if (node.isLeafNode()) {
                    if (node.isActive())
                        numActive++;
                    else
                        numInactive++;
                    if (node.usesPerceptron())
                        numPerceptronLeaves++;
                } else if (!node.isActive()) {
                    numInactiveInner++;
                }
            }
        }
        int numBackgroundNodes = 0;
        if (backgroundRoot != null) {
            List<PlasticRegressionNode> backgroundNodes = new ArrayList<>();
            backgroundRoot.collectSubtree(backgroundNodes);
            numBackgroundNodes = backgroundNodes.size();
        }
        return new Measurement[]{
                new Measurement("tree size (nodes)", numNodes),
                new Measurement("active learning leaves", numActive),
                new Measurement("inactive learning leaves", numInactive),
                new Measurement("inactive inner nodes", numInactiveInner),
                new Measurement("leaves predicting with the perceptron", numPerceptronLeaves),
                new Measurement("background tree size (nodes)", numBackgroundNodes),
                new Measurement("warnings detected", numWarnings),
                new Measurement("drifts detected", numDrifts),
                new Measurement("background tree replacements", numReplacements),
                new Measurement("nodes pruned to save memory", numPrunedNodes),
                new Measurement("byte size estimate", byteSize + estimateByteSize(backgroundRoot))
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {}

    @Override
    public boolean isRandomizable() {
        return true;
    }
}
