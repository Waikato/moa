/*
 *    BasicClassificationPerformanceEvaluator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
package moa.evaluation;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Classification evaluator that performs basic incremental evaluation.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * 
 * Updates in September 15th 2017 to include precision, recall and F1 scores.
 * @author Jean Karax (karaxjr@gmail.com)
 * @author Jean Paul Barddal (jean.barddal@ppgia.pucpr.br)
 * @author Wilson Sasaki Jr (sasaki.wilson.jr@gmail.com)
 * Updates in July 23rd 2026 to fix precision, recall and F1 scores zero handling and macro (per class) and micro statistics,
 * and to add log loss and the binary prequential ROC AUC.
 * @author Daniel Nowak Assis (daniel dot nowak-assis at lip6 dot fr)
 * @version $Revision: 8 $
 */
public class BasicClassificationPerformanceEvaluator extends AbstractOptionHandler
        implements ClassificationPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    protected Estimator weightCorrect;

    protected Estimator[] columnKappa;

    protected Estimator[] rowKappa;

    protected Estimator[] precision;

    protected Estimator[] recall;

    protected Estimator logLoss;

    /** Prequential AUC; null while it is not being reported. */
    protected AucEstimator auc;

    /** Class index treated as positive by the ROC AUC. */
    protected int positiveClass;

    protected int numClasses;

    private Estimator weightCorrectNoChangeClassifier;

    private Estimator weightMajorityClassifier;

    private int lastSeenClass;

    private double totalWeightObserved;

    public FlagOption precisionRecallOutputOption = new FlagOption("precisionRecallOutput",
            'o',
            "Outputs average precision, recall and F1 scores.");
    
    public FlagOption precisionPerClassOption = new FlagOption("precisionPerClass",
            'p',
            "Report precision per class.");

    public FlagOption recallPerClassOption = new FlagOption("recallPerClass",
            'r',
            "Report recall per class.");

    public FlagOption f1PerClassOption = new FlagOption("f1PerClass", 'f',
            "Report F1 per class.");

    public FlagOption logLossOption = new FlagOption("logLoss", 'l',
            "Report logarithmic loss (cross-entropy) of the predicted class distribution.");

    public FlagOption rocAucOption = new FlagOption("rocAUC", 'a',
            "Report the prequential ROC AUC. Binary problems only; reported as "
                    + "NaN otherwise. Every score seen has to be remembered, so "
                    + "setting this option for large streams can cause substantial "
                    + "memory usage.");

    public IntOption positiveClassOption = new IntOption("positiveClass", 'c',
            "Index of the class treated as positive by the ROC AUC.",
            1, 0, 1);

    /**
     * Predicted probabilities are clamped to this floor before taking the
     * logarithm, so that a confident-and-wrong prediction contributes a large
     * but finite loss instead of infinity.
     */
    protected static final double MIN_PROBABILITY = 1e-15;

    @Override
    public void reset() {
        reset(this.numClasses);
    }

    public void reset(int numClasses) {
        this.numClasses = numClasses;
        this.rowKappa = new Estimator[numClasses];
        this.columnKappa = new Estimator[numClasses];
        this.precision = new Estimator[numClasses];
        this.recall = new Estimator[numClasses];
        for (int i = 0; i < this.numClasses; i++) {
            this.rowKappa[i] = newEstimator();
            this.columnKappa[i] = newEstimator();
            this.precision[i] = newEstimator();
            this.recall[i] = newEstimator();
        }
        this.positiveClass = this.positiveClassOption.getValue();
        this.auc = this.rocAucOption.isSet() ? new AucEstimator() : null;
        this.weightCorrect = newEstimator();
        this.logLoss = newEstimator();
        this.weightCorrectNoChangeClassifier = newEstimator();
        this.weightMajorityClassifier = newEstimator();
        this.lastSeenClass = 0;
        this.totalWeightObserved = 0;
    }

    @Override
    public void addResult(Example<Instance> example, double[] classVotes) {
        Instance inst = example.getData();
        double weight = inst.weight();
        if (inst.classIsMissing() == false) {
            int trueClass = (int) inst.classValue();
            int predictedClass = Utils.maxIndex(classVotes);
            if (weight > 0.0) {
                if (this.totalWeightObserved == 0) {
                    reset(inst.dataset().numClasses());
                }
                this.totalWeightObserved += weight;
                this.weightCorrect.add(predictedClass == trueClass ? weight : 0);
                this.logLoss.add(weight * -Math.log(getProbabilityOfTrueClass(classVotes, trueClass)));
                for (int i = 0; i < this.numClasses; i++) {
                    this.rowKappa[i].add(predictedClass == i ? weight : 0);
                    this.columnKappa[i].add(trueClass == i ? weight : 0);
                    // for both precision and recall, NaN values are used to 'balance' the number
                    // of instances seen across classes
                    if (predictedClass == i) {
                        precision[i].add(predictedClass == trueClass ? weight : 0.0);
                    } else precision[i].add(Double.NaN);
                    if (trueClass == i) {
                        recall[i].add(predictedClass == trueClass ? weight : 0.0);
                    } else recall[i].add(Double.NaN);
                }
                addAucResult(classVotes, trueClass, weight);
            }
            this.weightCorrectNoChangeClassifier.add(this.lastSeenClass == trueClass ? weight : 0);
            this.weightMajorityClassifier.add(getMajorityClass() == trueClass ? weight : 0);
            this.lastSeenClass = trueClass;
        }
    }

    /**
     * Probability the learner assigned to the given class.
     *
     * Vote arrays in MOA are not required to be normalised, nor to cover every
     * class: a learner may return a shorter array, all-zero votes, or scores
     * that do not sum to one. Votes are therefore normalised here, negative
     * entries are clipped away, and a degenerate (empty or non-positive) vote
     * array falls back to the uniform distribution, which is what an
     * abstaining learner effectively predicts. The result is clamped to
     * {@link #MIN_PROBABILITY} so that the log loss stays finite.
     */
    protected double getNormalizedProbability(double[] classVotes, int classIndex) {
        double sum = 0.0;
        for (double vote : classVotes) {
            if (vote > 0.0) {
                sum += vote;
            }
        }
        double p;
        if (sum > 0.0) {
            p = classIndex < classVotes.length && classVotes[classIndex] > 0.0
                    ? classVotes[classIndex] / sum : 0.0;
        } else {
            p = this.numClasses > 0 ? 1.0 / this.numClasses : 0.0;
        }
        return Math.max(p, MIN_PROBABILITY);
    }

    /** Probability the learner assigned to the true class, used by the log loss. */
    protected double getProbabilityOfTrueClass(double[] classVotes, int trueClass) {
        return getNormalizedProbability(classVotes, trueClass);
    }

    /**
     * Feeds one prediction to the AUC estimator. AUC is a binary measure, so
     * nothing is recorded on a multi-class stream and {@link #getROCAUC()}
     * reports NaN there.
     */
    protected void addAucResult(double[] classVotes, int trueClass, double weight) {
        if (this.auc == null || this.numClasses != 2) {
            return;
        }
        this.auc.add(getNormalizedProbability(classVotes, this.positiveClass),
                trueClass == this.positiveClass);
    }

    private int getMajorityClass() {
        int majorityClass = 0;
        double maxProbClass = 0.0;
        for (int i = 0; i < this.numClasses; i++) {
            if (this.columnKappa[i].estimation() > maxProbClass) {
                majorityClass = i;
                maxProbClass = this.columnKappa[i].estimation();
            }
        }
        return majorityClass;
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {
        ArrayList<Measurement> measurements = new ArrayList<Measurement>();
        measurements.add(new Measurement("classified instances", this.getTotalWeightObserved()));
        measurements.add(new Measurement("classifications correct (percent)", this.getFractionCorrectlyClassified() * 100.0));
        measurements.add(new Measurement("Kappa Statistic (percent)", this.getKappaStatistic() * 100.0));
        measurements.add(new Measurement("Kappa Temporal Statistic (percent)", this.getKappaTemporalStatistic() * 100.0));
        measurements.add(new Measurement("Kappa M Statistic (percent)", this.getKappaMStatistic() * 100.0));
        if (precisionRecallOutputOption.isSet())
            measurements.add(new Measurement("F1 Score (percent)",
                    this.getMicroF1Statistic() * 100.0));
        if (f1PerClassOption.isSet()) {
            measurements.add(new Measurement("F1 Score macro (percent)",
                    this.getF1Statistic() * 100.0));
            for (int i = 0; i < this.numClasses; i++) {
                measurements.add(new Measurement("F1 Score for class " + i +
                        " (percent)", 100.0 * this.getF1Statistic(i)));
            }
        }
        if (precisionRecallOutputOption.isSet())
            measurements.add(new Measurement("Precision (percent)",
                this.getMicroPrecisionStatistic() * 100.0));
        if (precisionPerClassOption.isSet()) {
            measurements.add(new Measurement("Precision macro (percent)",
                    this.getPrecisionStatistic() * 100.0));
            for (int i = 0; i < this.numClasses; i++) {
                measurements.add(new Measurement("Precision for class " + i +
                        " (percent)", 100.0 * this.getPrecisionStatistic(i)));
            }
        }
        if (precisionRecallOutputOption.isSet())
            measurements.add(new Measurement("Recall (percent)",
                this.getMicroRecallStatistic() * 100.0));
        if (recallPerClassOption.isSet()) {
            measurements.add(new Measurement("Recall macro (percent)",
                    this.getRecallStatistic() * 100.0));
            for (int i = 0; i < this.numClasses; i++) {
                measurements.add(new Measurement("Recall for class " + i +
                        " (percent)", 100.0 * this.getRecallStatistic(i)));
            }
        }

        if (logLossOption.isSet())
            measurements.add(new Measurement("Log Loss", this.getLogLoss()));

        if (rocAucOption.isSet())
            measurements.add(new Measurement("ROC AUC (cumulative)", this.getROCAUC()));

        Measurement[] result = new Measurement[measurements.size()];

        return measurements.toArray(result);

    }

    public double getTotalWeightObserved() {
        return this.totalWeightObserved;
    }

    public double getFractionCorrectlyClassified() {
        return this.weightCorrect.estimation();
    }

    public double getFractionIncorrectlyClassified() {
        return 1.0 - getFractionCorrectlyClassified();
    }

    /**
     * Mean logarithmic loss (cross-entropy) of the predicted class
     * distributions. Like every other measure here it is produced by an
     * {@link Estimator}, so subclasses that decay or window their estimators
     * report a decayed or windowed log loss without further work.
     */
    public double getLogLoss() {
        return this.getTotalWeightObserved() > 0.0 ? this.logLoss.estimation() : 0.0;
    }

    public double getKappaStatistic() {
        if (this.getTotalWeightObserved() > 0.0) {
            double p0 = getFractionCorrectlyClassified();
            double pc = 0.0;
            for (int i = 0; i < this.numClasses; i++) {
                pc += this.rowKappa[i].estimation()
                        * this.columnKappa[i].estimation();
            }
            return (p0 - pc) / (1.0 - pc);
        } else {
            return 0;
        }
    }

    public double getKappaTemporalStatistic() {
        if (this.getTotalWeightObserved() > 0.0) {
            double p0 = getFractionCorrectlyClassified();
            double pc = this.weightCorrectNoChangeClassifier.estimation();

            return (p0 - pc) / (1.0 - pc);
        } else {
            return 0;
        }
    }

    private double getKappaMStatistic() {
        if (this.getTotalWeightObserved() > 0.0) {
            double p0 = getFractionCorrectlyClassified();
            double pc = this.weightMajorityClassifier.estimation();

            return (p0 - pc) / (1.0 - pc);
        } else {
            return 0;
        }
    }

    /**
     * Macro-averaged precision: the unweighted mean of the per-class precisions.
     * A class that was never predicted has an undefined precision; as in
     * scikit-learn's default zero_division it counts as 0 and still occupies a
     * slot in the average, so the denominator is always the full label set.
     */
    public double getPrecisionStatistic() {
        double total = 0;
        for (int i = 0; i < this.numClasses; i++) {
            total += getPrecisionStatistic(i);
        }
        return this.numClasses == 0 ? 0.0 : total / this.numClasses;
    }

    public double getPrecisionStatistic(int numClass) {
        double v = this.precision[numClass].estimation();
        return Double.isNaN(v) ? 0.0 : v;
    }

    /**
     * Macro-averaged recall, with undefined classes handled as in
     * {@link #getPrecisionStatistic()}.
     */
    public double getRecallStatistic() {
        double total = 0;
        for (int i = 0; i < this.numClasses; i++) {
            total += getRecallStatistic(i);
        }
        return this.numClasses == 0 ? 0.0 : total / this.numClasses;
    }

    public double getRecallStatistic(int numClass) {
        double v = this.recall[numClass].estimation();
        return Double.isNaN(v) ? 0.0 : v;
    }

    public double getF1Statistic() {
        double total = 0;
        for (int i = 0; i < this.numClasses; i++) {
            total += getF1Statistic(i);
        }
        return total / this.numClasses;
    }

    public double getF1Statistic(int numClass) {
        double p = this.getPrecisionStatistic(numClass);
        double r = this.getRecallStatistic(numClass);
        if (Double.isNaN(p)) p = 0.0;
        if (Double.isNaN(r)) r = 0.0;
        double denom = p + r;
        return denom == 0.0 ? 0.0 : 2.0 * p * r / denom;
    }

    /**
     * Micro-averaged precision, recall and F1: sum the true/false positives and
     * negatives over all classes first, then divide.
     *
     * micro precision = sum(TP) / sum(TP + FP)
     * micro recall    = sum(TP) / sum(TP + FN)
     * micro F1        = 2*sum(TP) / (2*sum(TP) + sum(FP) + sum(FN))
     *
     * This evaluator scores single-label predictions over the full label set, so
     * every instance contributes exactly one entry to sum(TP)+sum(FP) (the class
     * it was predicted as) and exactly one to sum(TP)+sum(FN) (its true class).
     * Both denominators therefore equal the total weight seen, all three measures
     * collapse to sum(TP)/total, and each equals the accuracy. Deriving them from
     * weightCorrect keeps that identity exact under every weighting scheme
     * (cumulative, sliding window, EWMA, fading factor), which pooling the
     * per-class estimator ratios could not: Estimator exposes only the ratio, and
     * the decayed estimators hold no counts to pool.
     */
    public double getMicroPrecisionStatistic() {
        return getFractionCorrectlyClassified();
    }

    public double getMicroRecallStatistic() {
        return getFractionCorrectlyClassified();
    }

    public double getMicroF1Statistic() {
        return getFractionCorrectlyClassified();
    }

    /**
     * ROC AUC, computed from every score seen as in D. Brzezinski and
     * J. Stefanowski, "Prequential AUC: Properties of the Area Under the ROC
     * Curve for Data Streams with Concept Drift", Knowledge and Information
     * Systems, 2017, and as implemented by
     * {@link BasicAUCImbalancedPerformanceEvaluator}.
     *
     * Unlike every other measure here this one does not go through
     * {@link Estimator}: it is always cumulative over the whole stream, so a
     * windowed or fading subclass still reports the all-time AUC. Use
     * {@link WindowAUCImbalancedPerformanceEvaluator} for a windowed one.
     * Instance weights are ignored, as in the reference implementation.
     * Returns NaN unless the problem is binary, the measure is switched on,
     * and both classes have been seen.
     */
    public double getROCAUC() {
        return this.auc == null || this.numClasses != 2
                ? Double.NaN : this.auc.getAUC();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
    }

    @Override
    public void addResult(Example<Instance> testInst, Prediction prediction) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {

    }

    public interface Estimator extends Serializable {

        void add(double value);

        double estimation();
    }

    public class BasicEstimator implements Estimator {

        protected double len;

        protected double sum;

        @Override
        public void add(double value) {
            if(!Double.isNaN(value)) {
                sum += value;
                len++;
            }
        }

        @Override
        public double estimation() {
            return sum / len;
        }

    }

    protected Estimator newEstimator() {
        return new BasicEstimator();
    }

    /**
     * ROC AUC over every score seen so far. The scores are kept sorted
     * descending, and a single sweep counts, for each negative, how many
     * positives outrank it, ties counted as half; that count over all
     * positive-negative pairs is the AUC.
     */
    public static class AucEstimator implements Serializable {

        private static final long serialVersionUID = 1L;

        /** One scored instance, ordered by descending score. */
        protected static class Score implements Comparable<Score>, Serializable {

            private static final long serialVersionUID = 1L;

            protected final double value;

            /** Position in the stream, kept so that equal scores stay distinct. */
            protected final int position;

            protected final boolean isPositive;

            public Score(double value, int position, boolean isPositive) {
                this.value = value;
                this.position = position;
                this.isPositive = isPositive;
            }

            @Override
            public int compareTo(Score o) {
                if (o.value != this.value) {
                    return o.value < this.value ? -1 : 1;
                }
                // on a tie, positives come first so that the sweep below can
                // recognise the tied block, then the stream order breaks the rest
                if (o.isPositive != this.isPositive) {
                    return this.isPositive ? -1 : 1;
                }
                return Integer.compare(this.position, o.position);
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Score) && ((Score) o).position == this.position;
            }

            @Override
            public int hashCode() {
                return this.position;
            }
        }

        protected final TreeSet<Score> sortedScores = new TreeSet<Score>();

        protected int position;

        protected double numPos;

        protected double numNeg;

        public void add(double score, boolean isPositive) {
            this.sortedScores.add(new Score(score, this.position++, isPositive));
            if (isPositive) {
                this.numPos++;
            } else {
                this.numNeg++;
            }
        }

        public double getAUC() {
            if (this.numPos == 0 || this.numNeg == 0) {
                return Double.NaN;
            }
            double auc = 0.0;
            double positivesSoFar = 0.0;
            double positivesBeforeTie = 0.0;
            double lastPositiveScore = Double.MAX_VALUE;
            for (Score s : this.sortedScores) {
                if (s.isPositive) {
                    if (s.value != lastPositiveScore) {
                        positivesBeforeTie = positivesSoFar;
                        lastPositiveScore = s.value;
                    }
                    positivesSoFar += 1.0;
                } else if (s.value == lastPositiveScore) {
                    auc += (positivesSoFar + positivesBeforeTie) / 2.0;
                } else {
                    auc += positivesSoFar;
                }
            }
            return auc / (this.numPos * this.numNeg);
        }
    }


    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == BasicClassificationPerformanceEvaluator.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
