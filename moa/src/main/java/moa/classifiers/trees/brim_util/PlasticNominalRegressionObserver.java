package moa.classifiers.trees.brim_util;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Utils;

/**
 * Observer of a nominal attribute for regression.
 *
 * <p>For each attribute value, it stores the target statistics [weight, sum of targets, sum of squared targets],
 * which is the format expected by {@link moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion}.
 * Split evaluation and {@link #forceSplit} are inherited from {@link NominalAttributeClassObserver}, which only
 * relies on the resulting distributions computed here.</p>
 */
public class PlasticNominalRegressionObserver extends NominalAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    protected AutoExpandVector<DoubleVector> targetStatsPerValue = new AutoExpandVector<>();
    // Whether splits are evaluated on the statistics of the observer instead of those of the node
    private boolean ownStatistics = false;

    /**
     * Adds an observation to the target statistics of its attribute value
     */
    public void observeAttributeTarget(double attVal, double target, double weight) {
        if (Utils.isMissingValue(attVal)) {
            this.missingWeightObserved += weight;
        } else {
            int attValInt = (int) attVal;
            DoubleVector stats = this.targetStatsPerValue.get(attValInt);
            if (stats == null) {
                stats = new DoubleVector(new double[3]);
                this.targetStatsPerValue.set(attValInt, stats);
            }
            stats.addToValue(0, weight);
            stats.addToValue(1, weight * target);
            stats.addToValue(2, weight * target * target);
        }
        this.totalWeightObserved += weight;
    }

    /**
     * Copies the observer without serialization, which is much faster. Copies are made when nodes are restructured.
     * @return a deep copy of this observer
     */
    @Override
    public PlasticNominalRegressionObserver copy() {
        PlasticNominalRegressionObserver copy = new PlasticNominalRegressionObserver();
        for (int i = 0; i < targetStatsPerValue.size(); i++) {
            DoubleVector stats = targetStatsPerValue.get(i);
            if (stats != null)
                copy.targetStatsPerValue.set(i, new DoubleVector(stats.getArrayCopy()));
        }
        copy.totalWeightObserved = totalWeightObserved;
        copy.missingWeightObserved = missingWeightObserved;
        copy.ownStatistics = ownStatistics;
        return copy;
    }

    @Override
    public void observeAttributeTarget(double attVal, double target) {
        observeAttributeTarget(attVal, target, 1.0);
    }

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        observeAttributeTarget(attVal, classVal, weight);
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal, int classVal) {
        return 0.0;
    }

    /**
     * @return the number of attribute values with statistics (including values without observations below the largest)
     */
    @Override
    public int getMaxAttValsObserved() {
        return this.targetStatsPerValue.size();
    }

    /**
     * @return the target statistics of each attribute value (one branch per value)
     */
    @Override
    public double[][] getClassDistsResultingFromMultiwaySplit(int maxAttValsObserved) {
        double[][] distributions = new double[maxAttValsObserved][];
        for (int i = 0; i < distributions.length; i++) {
            distributions[i] = getStats(i);
        }
        return distributions;
    }

    /**
     * @return the target statistics of the given value and of all other values
     */
    @Override
    public double[][] getClassDistsResultingFromBinarySplit(int valIndex) {
        double[] equalsDist = getStats(valIndex);
        double[] notEqualDist = new double[3];
        for (int i = 0; i < this.targetStatsPerValue.size(); i++) {
            if (i == valIndex) {
                continue;
            }
            double[] stats = getStats(i);
            for (int j = 0; j < 3; j++) {
                notEqualDist[j] += stats[j];
            }
        }
        return new double[][]{equalsDist, notEqualDist};
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(SplitCriterion criterion, double[] preSplitDist, int attIndex, boolean binaryOnly) {
        return super.getBestEvaluatedSplitSuggestion(criterion, consistentPreSplitDist(preSplitDist), attIndex, binaryOnly);
    }

    @Override
    public AttributeSplitSuggestion forceSplit(SplitCriterion criterion, double[] preSplitDist, int attIndex, boolean binary, Double splitValue) {
        return super.forceSplit(criterion, consistentPreSplitDist(preSplitDist), attIndex, binary, splitValue);
    }

    /**
     * Evaluates splits on the statistics of this observer instead of the statistics of the node. Used when the
     * observer has only seen part of the node's instances, i.e., when a node is reactivated after it was
     * deactivated to save memory.
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
        for (int i = 0; i < targetStatsPerValue.size(); i++) {
            double[] stats = getStats(i);
            for (int j = 0; j < 3; j++)
                own[j] += stats[j];
        }
        return own[0] > 0.0 ? own : preSplitDist;
    }

    private double[] getStats(int valIndex) {
        DoubleVector stats = this.targetStatsPerValue.get(valIndex);
        return stats == null ? new double[3] : stats.getArrayCopy();
    }

    // Estimated sizes in bytes of the observer without values and of the statistics of a value
    static final long BASE_BYTES = 90, VALUE_BYTES = 65;

    /**
     * @return the estimated size of this observer in bytes
     */
    public long estimateByteSize() {
        return BASE_BYTES + targetStatsPerValue.size() * VALUE_BYTES;
    }
}
