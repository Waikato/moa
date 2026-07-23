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
        this.weightCorrect = newEstimator();
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
            }
            this.weightCorrectNoChangeClassifier.add(this.lastSeenClass == trueClass ? weight : 0);
            this.weightMajorityClassifier.add(getMajorityClass() == trueClass ? weight : 0);
            this.lastSeenClass = trueClass;
        }
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


    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == BasicClassificationPerformanceEvaluator.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
