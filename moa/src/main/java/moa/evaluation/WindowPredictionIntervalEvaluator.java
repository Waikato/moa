/*
 *    WindowRegressionPerformanceEvaluator.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
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

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Regression evaluator that updates evaluation results using a sliding window.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class WindowPredictionIntervalEvaluator extends AbstractOptionHandler
        implements PredictionIntervalPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    public IntOption widthOption = new IntOption("width",
            'w', "Size of Window", 1000);

    protected double TotalweightObserved = 0;
    protected Estimator weightObserved;
    protected Estimator squareError;
    protected Estimator averageError;

    protected Estimator lower;

    protected Estimator upper;
    protected Estimator counterCorrect;

    protected Estimator truth;

    protected int numClasses;

    public class Estimator {

        protected double[] window;

        protected int posWindow;

        protected int lenWindow;

        protected int SizeWindow;

        protected double sum;

        public Estimator(int sizeWindow) {
            window = new double[sizeWindow];
            SizeWindow = sizeWindow;
            posWindow = 0;
        }

        public void add(double value) {
            sum -= window[posWindow];
            sum += value;
            window[posWindow] = value;
            posWindow++;
            if (posWindow == SizeWindow) {
                posWindow = 0;
            }
        }

        public double total() {
            return sum;
        }

        public double max() {
            double max = Double.MIN_VALUE;
            for (double v : window) max = Math.max(max, v);
            return max;
        }

        public double min() {
            double min = Double.MAX_VALUE;
            for (double v : window) min = Math.min(min, v);
            return min;
        }
    }

    /*   public void setWindowWidth(int w) {
    this.width = w;
    reset();
    }*/
    @Override
    public void reset() {
        reset(this.numClasses);
    }

    public void reset(int numClasses) {
        this.numClasses = numClasses;
        this.weightObserved = new Estimator(this.widthOption.getValue());
        this.squareError = new Estimator(this.widthOption.getValue());
        this.averageError = new Estimator(this.widthOption.getValue());
        this.lower = new Estimator(this.widthOption.getValue());
        this.upper = new Estimator(this.widthOption.getValue());
        this.counterCorrect = new Estimator(this.widthOption.getValue());
        this.truth = new Estimator(this.widthOption.getValue());
        this.TotalweightObserved = 0;
    }

    @Override
    public void addResult(Example<Instance> example, double[] prediction) {
        Instance inst = example.getData();
        double weight = inst.weight();
        if (weight > 0.0) {
            if (TotalweightObserved == 0) {
                reset(inst.dataset().numClasses());
            }
            this.TotalweightObserved += weight;
            this.weightObserved.add(weight);

            if (prediction.length == 3) {
                this.squareError.add((inst.classValue() - prediction[1]) * (inst.classValue() - prediction[1]));
                this.averageError.add(Math.abs(inst.classValue() - prediction[1]));
                this.lower.add(prediction[0]);
                this.upper.add(prediction[2]);
                this.counterCorrect.add( inst.classValue() >= prediction[0] && inst.classValue() <= prediction[2]? 1 : 0);
                this.truth.add(inst.classValue());
            }
            //System.out.println(inst.classValue()+", "+prediction[0]);
        }
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {
        return new Measurement[]{
                    new Measurement("classified instances",
                    getTotalWeightObserved()),
                    new Measurement("mean absolute error",
                    getMeanError()),
                    new Measurement("root mean squared error",
                    getSquareError()),
                new Measurement("coverage",
                        getCoverage()),
                new Measurement("average length",
                        getAverageLength()),
                new Measurement("NMPIW",
                        getNMPIW())
        };
    }

    public double getTotalWeightObserved() {
        return this.weightObserved.total();
    }

    public double getMeanError() {
        return this.weightObserved.total() > 0.0 ? this.averageError.total()
                / this.weightObserved.total() : 0.0;
    }

    public double getSquareError() {
        return Math.sqrt(this.weightObserved.total() > 0.0 ? this.squareError.total()
                / this.weightObserved.total() : 0.0);
    }

    public double getCoverage(){
        return Math.round(this.counterCorrect.sum / this.widthOption.getValue() * 10000.0) / 100.0;
    }
    public double getAverageLength(){
        return (this.upper.sum - this.lower.sum) / this.upper.SizeWindow;
    }

    public double getNMPIW(){
      return Math.round(getAverageLength() / (this.truth.max() - this.truth.min()) * 10000.0) / 100.0;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
    }
    

	@Override
	public void addResult(Example<Instance> testInst, Prediction prediction) {
		double votes[];
		if(prediction==null)
			votes = new double[0];
		else
			votes=prediction.getVotes();
		addResult(testInst, votes);
		
	}
}
