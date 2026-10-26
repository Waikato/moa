package moa.classifiers.trees.brim_util;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.NumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.options.AbstractOptionHandler;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.util.Arrays;

/**
 * Quantization Observer (QO) of a numeric attribute for regression.
 *
 * <p>Values are quantized into slots of equal width {@code radius}: value x goes to slot floor(x / radius).
 * Each slot stores the target statistics [weight, sum of targets, sum of squared targets] of its values, as
 * expected by {@link moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion}, and the range of its
 * values. Binary splits are evaluated between consecutive slots, so memory and evaluation costs depend on the
 * number of slots instead of the number of observed values as in the E-BST.</p>
 *
 * <p>The radius is relative: it is {@code radiusFactor} times the standard deviation of the attribute, as the
 * features are standardized before quantization in the paper. The standard deviation is provided with the
 * observations. Until it is available, values are buffered. If a split is evaluated before, the standard deviation
 * of the buffered values is used.</p>
 *
 * <p>The threshold between two slots is the midpoint between the largest value of the left slot and the smallest
 * value of the right slot, so the split statistics are exact for all observed values.</p>
 *
 * <p>An observer created when a node is reactivated after it was deactivated to save memory has seen fewer
 * instances than the node. It is then set to evaluate splits on its own statistics instead of those of the node,
 * see {@link #useOwnStatistics()}.</p>
 *
 * <p>See details in:<br> Saulo Martiello Mastelini, Jacob Montiel, Heitor Murilo Gomes, Albert Bifet,
 * Bernhard Pfahringer, André C. P. L. F. de Carvalho: Fast and lightweight binary and multi-branch Hoeffding Tree
 * Regressors. ICDM Workshops 2021: 380-388</p>
 */
public class PlasticQuantizationObserver extends AbstractOptionHandler implements NumericAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    // Layout of the statistics of a slot
    private static final int WEIGHT = 0, SUM = 1, SUM_SQ = 2, MIN = 3, MAX = 4, SUM_X = 5, SLOT_SIZE = 6;

    private final double radiusFactor;
    // NaN until the radius is set
    private double radius = Double.NaN;
    // The slots, sorted by key: the statistics of the slot with key keys[i] are slotStats[SLOT_SIZE * i, ...].
    // Primitive arrays instead of a map keep a slot at 56 bytes.
    private long[] keys = new long[0];
    private double[] slotStats = new double[0];
    // Observations [value, target, weight] received before the radius was set (null once it is set)
    private double[] buffer;
    private int bufferSize = 0;
    // Whether splits are evaluated on the statistics of the observer instead of those of the node
    private boolean ownStatistics = false;

    /**
     * @param radiusFactor the width of the slots, relative to the standard deviation of the attribute
     */
    public PlasticQuantizationObserver(double radiusFactor) {
        this.radiusFactor = radiusFactor;
    }

    /**
     * Copies the observer without serialization, which is much faster. Copies are made when nodes are restructured.
     * @return a deep copy of this observer
     */
    @Override
    public PlasticQuantizationObserver copy() {
        PlasticQuantizationObserver copy = new PlasticQuantizationObserver(radiusFactor);
        copy.radius = radius;
        copy.keys = keys.clone();
        copy.slotStats = slotStats.clone();
        copy.buffer = buffer == null ? null : buffer.clone();
        copy.bufferSize = bufferSize;
        copy.ownStatistics = ownStatistics;
        return copy;
    }

    /**
     * Observes a value of the attribute.
     * @param attVal the value of the attribute
     * @param target the target of the instance
     * @param weight the weight of the instance
     * @param attributeStdDev the standard deviation of the attribute, or 0 if it is not known yet.
     *                        Only used until the radius is set.
     */
    public void observeAttributeTarget(double attVal, double target, double weight, double attributeStdDev) {
        if (Double.isNaN(attVal))
            return;
        if (Double.isNaN(radius)) {
            if (attributeStdDev <= 0.0) {
                addToBuffer(attVal, target, weight);
                return;
            }
            setRadius(attributeStdDev);
        }
        addToSlot(attVal, target, weight);
    }

    @Override
    public void observeAttributeTarget(double attVal, double target) {
        observeAttributeTarget(attVal, target, 1.0, 0.0);
    }

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        observeAttributeTarget(attVal, classVal, weight, 0.0);
    }

    /**
     * Buffers an observation until the radius is set
     */
    private void addToBuffer(double attVal, double target, double weight) {
        if (buffer == null)
            buffer = new double[3 * 16];
        else if (3 * (bufferSize + 1) > buffer.length)
            buffer = Arrays.copyOf(buffer, 2 * buffer.length);
        buffer[3 * bufferSize] = attVal;
        buffer[3 * bufferSize + 1] = target;
        buffer[3 * bufferSize + 2] = weight;
        bufferSize++;
    }

    /**
     * Sets the radius and moves the buffered observations into slots
     */
    private void setRadius(double attributeStdDev) {
        radius = attributeStdDev > 0.0 ? radiusFactor * attributeStdDev : radiusFactor;
        for (int i = 0; i < bufferSize; i++)
            addToSlot(buffer[3 * i], buffer[3 * i + 1], buffer[3 * i + 2]);
        buffer = null;
        bufferSize = 0;
    }

    /**
     * Sets the radius from the buffered values if it was not set yet
     */
    private void ensureRadius() {
        if (!Double.isNaN(radius) || bufferSize == 0)
            return;
        double weight = 0.0, sum = 0.0, sumSq = 0.0;
        for (int i = 0; i < bufferSize; i++) {
            double value = buffer[3 * i], w = buffer[3 * i + 2];
            weight += w;
            sum += w * value;
            sumSq += w * value * value;
        }
        setRadius(Math.sqrt(Math.max(0.0, (sumSq - sum * sum / weight) / weight)));
    }

    /**
     * Adds an observation to its slot, creating the slot if needed (binary search on the sorted keys)
     */
    private void addToSlot(double attVal, double target, double weight) {
        long key = (long) Math.floor(attVal / radius);
        int index = Arrays.binarySearch(keys, key);
        if (index < 0) {
            index = -index - 1;
            insertSlot(index, key, attVal);
        }
        int offset = SLOT_SIZE * index;
        slotStats[offset + WEIGHT] += weight;
        slotStats[offset + SUM] += weight * target;
        slotStats[offset + SUM_SQ] += weight * target * target;
        slotStats[offset + MIN] = Math.min(slotStats[offset + MIN], attVal);
        slotStats[offset + MAX] = Math.max(slotStats[offset + MAX], attVal);
        slotStats[offset + SUM_X] += weight * attVal;
    }

    /**
     * Inserts an empty slot at the given position of the sorted arrays
     */
    private void insertSlot(int index, long key, double attVal) {
        int numSlots = keys.length;
        long[] newKeys = new long[numSlots + 1];
        System.arraycopy(keys, 0, newKeys, 0, index);
        System.arraycopy(keys, index, newKeys, index + 1, numSlots - index);
        newKeys[index] = key;
        double[] newStats = new double[SLOT_SIZE * (numSlots + 1)];
        System.arraycopy(slotStats, 0, newStats, 0, SLOT_SIZE * index);
        System.arraycopy(slotStats, SLOT_SIZE * index, newStats, SLOT_SIZE * (index + 1), SLOT_SIZE * (numSlots - index));
        newStats[SLOT_SIZE * index + MIN] = attVal;
        newStats[SLOT_SIZE * index + MAX] = attVal;
        keys = newKeys;
        slotStats = newStats;
    }

    /**
     * Evaluates the binary splits between consecutive slots and returns the best one (null with fewer than two slots)
     */
    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(SplitCriterion criterion, double[] preSplitDist, int attIndex, boolean binaryOnly) {
        ensureRadius();
        preSplitDist = consistentPreSplitDist(preSplitDist);
        int numSlots = keys.length;
        if (numSlots < 2)
            return null;
        double[] left = new double[3];
        double[] bestLeft = null;
        double bestThreshold = 0.0;
        double bestMerit = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < numSlots; k++) {
            int offset = SLOT_SIZE * k;
            if (k > 0) {
                double[][] postSplitDists = {left, rightOf(preSplitDist, left)};
                double merit = criterion.getMeritOfSplit(preSplitDist, postSplitDists);
                if (bestLeft == null || merit > bestMerit) {
                    bestMerit = merit;
                    bestLeft = left.clone();
                    bestThreshold = (slotStats[offset - SLOT_SIZE + MAX] + slotStats[offset + MIN]) / 2.0;
                }
            }
            for (int i = 0; i < 3; i++)
                left[i] += slotStats[offset + i];
        }
        return new AttributeSplitSuggestion(new NumericAttributeBinaryTest(attIndex, bestThreshold, true),
                new double[][]{bestLeft, rightOf(preSplitDist, bestLeft)}, bestMerit);
    }

    /**
     * Evaluates the binary split at the given threshold. Values of a slot that contains the threshold are
     * assigned to the side of the slot's mean value.
     */
    public AttributeSplitSuggestion forceSplit(SplitCriterion criterion, double[] preSplitDist, int attIndex, double threshold) {
        ensureRadius();
        preSplitDist = consistentPreSplitDist(preSplitDist);
        double[] left = new double[3];
        for (int k = 0; k < keys.length; k++) {
            int offset = SLOT_SIZE * k;
            if (slotStats[offset + MIN] > threshold)
                break;
            if (slotStats[offset + MAX] <= threshold || slotStats[offset + SUM_X] / slotStats[offset + WEIGHT] <= threshold) {
                for (int i = 0; i < 3; i++)
                    left[i] += slotStats[offset + i];
            }
        }
        double[][] postSplitDists = {left, rightOf(preSplitDist, left)};
        return new AttributeSplitSuggestion(new NumericAttributeBinaryTest(attIndex, threshold, true),
                postSplitDists, criterion.getMeritOfSplit(preSplitDist, postSplitDists));
    }

    /**
     * Evaluates splits on the statistics of this observer instead of the statistics of the node. Used when the
     * observer has only seen part of the node's instances.
     */
    public void useOwnStatistics() {
        ownStatistics = true;
    }

    /**
     * @return the statistics of the node, or those of the observer if set by {@link #useOwnStatistics()}
     */
    private double[] consistentPreSplitDist(double[] preSplitDist) {
        if (!ownStatistics)
            return preSplitDist;
        double[] own = new double[3];
        for (int k = 0; k < keys.length; k++) {
            for (int i = 0; i < 3; i++)
                own[i] += slotStats[SLOT_SIZE * k + i];
        }
        return own[0] > 0.0 ? own : preSplitDist;
    }

    /**
     * @return the statistics of the right branch: the node statistics minus those of the left branch
     */
    private static double[] rightOf(double[] preSplitDist, double[] left) {
        double[] right = new double[3];
        for (int i = 0; i < 3; i++)
            right[i] = (i < preSplitDist.length ? preSplitDist[i] : 0.0) - left[i];
        return right;
    }

    // Estimated sizes in bytes of the observer without slots and of a slot (key and statistics)
    static final long BASE_BYTES = 110, SLOT_BYTES = 56;

    /**
     * @return the estimated size of this observer in bytes
     */
    public long estimateByteSize() {
        return BASE_BYTES + keys.length * SLOT_BYTES + (buffer == null ? 0 : buffer.length * 8L);
    }

    /**
     * @return the number of slots, i.e., of intervals of the attribute that are monitored
     */
    public int getNumSlots() {
        return keys.length;
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal, int classVal) {
        return 0.0;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {}

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {}
}
