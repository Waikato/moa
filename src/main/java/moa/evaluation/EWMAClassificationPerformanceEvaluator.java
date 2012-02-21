/*
 *    EWMAClassificationPerformanceEvaluator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
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

import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.FloatOption;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import weka.core.Instance;
import weka.core.Utils;

/**
 * Classification evaluator that updates evaluation results using an Exponential Weighted Moving Average.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EWMAClassificationPerformanceEvaluator extends AbstractOptionHandler
        implements ClassificationPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    protected double TotalweightObserved;

    public FloatOption alphaOption = new FloatOption("alpha",
            'a', "Fading factor or exponential smoothing factor", .01);

    protected Estimator weightCorrect;

    protected class Estimator {

        protected double alpha;

        protected double estimation;

        public Estimator(double a) {
            alpha = a;
            estimation = 0;
        }

        public void add(double value) {
            estimation += alpha * (value - estimation);
        }

        public double estimation() {
            return estimation;
        }
    }

    /*   public void setalpha(double a) {
    this.alpha = a;
    reset();
    }*/
    
    @Override
    public void reset() {
        weightCorrect = new Estimator(this.alphaOption.getValue());
    }

    @Override
    public void addResult(Instance inst, double[] classVotes) {
        double weight = inst.weight();
        int trueClass = (int) inst.classValue();
        if (weight > 0.0) {
            this.TotalweightObserved += weight;
            if (Utils.maxIndex(classVotes) == trueClass) {
                this.weightCorrect.add(1);
            } else {
                this.weightCorrect.add(0);
            }
        }
    }
    /*public void addClassificationAttempt(int trueClass, double[] classVotes,
    double weight) {
    if (weight > 0.0) {
    this.TotalweightObserved += weight;
    if (Utils.maxIndex(classVotes) == trueClass) {
    this.weightCorrect.add(1);
    } else
    this.weightCorrect.add(0);
    }
    }*/

    @Override
    public Measurement[] getPerformanceMeasurements() {
        return new Measurement[]{
                    new Measurement("classified instances",
                    this.TotalweightObserved),
                    new Measurement("classifications correct (percent)",
                    getFractionCorrectlyClassified() * 100.0)};
    }

    public double getTotalWeightObserved() {
        return this.TotalweightObserved;
    }

    public double getFractionCorrectlyClassified() {
        return this.weightCorrect.estimation();
    }

    public double getFractionIncorrectlyClassified() {
        return 1.0 - getFractionCorrectlyClassified();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        reset();
    }
}
