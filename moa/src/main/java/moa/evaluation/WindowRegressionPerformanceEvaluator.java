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

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;

import com.github.javacliparser.IntOption;

import moa.tasks.TaskMonitor;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Regression evaluator that updates evaluation results using a sliding window.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class WindowRegressionPerformanceEvaluator extends AbstractOptionHandler
        implements RegressionPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    public IntOption widthOption = new IntOption("width",
            'w', "Size of Window", 1000);

    protected double TotalweightObserved = 0;

    protected Estimator weightObserved;

    protected Estimator squareError;

    protected Estimator averageError;

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

            if (prediction.length > 0) {
                this.squareError.add((inst.classValue() - prediction[0]) * (inst.classValue() - prediction[0]));
                this.averageError.add(Math.abs(inst.classValue() - prediction[0]));
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
                    getSquareError())};
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
