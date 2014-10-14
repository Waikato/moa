/*
 *    BasicRegressionPerformanceEvaluator.java
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

import moa.AbstractMOAObject;
import moa.core.Example;
import moa.core.Measurement;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Regression evaluator that performs basic incremental evaluation.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class BasicRegressionPerformanceEvaluator extends AbstractMOAObject
        implements RegressionPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    protected double weightObserved;

    protected double squareError;

    protected double averageError;

    protected double sumTarget;
    
    protected double squareTargetError;
    
    protected double averageTargetError;

    @Override
    public void reset() {
        this.weightObserved = 0.0;
        this.squareError = 0.0;
        this.averageError = 0.0;
        this.sumTarget = 0.0;
        this.averageTargetError = 0.0;
        this.squareTargetError = 0.0;
        
    }

    @Override
    public void addResult(Example<Instance> example, double[] prediction) {
	Instance inst = example.getData();
        if (inst.weight() > 0.0) {
            if (prediction.length > 0) {
                double meanTarget = this.weightObserved != 0 ? 
                            this.sumTarget / this.weightObserved : 0.0;
                this.squareError += (inst.classValue() - prediction[0]) * (inst.classValue() - prediction[0]);
                this.averageError += Math.abs(inst.classValue() - prediction[0]);
                this.squareTargetError += (inst.classValue() - meanTarget) * (inst.classValue() - meanTarget);
                this.averageTargetError += Math.abs(inst.classValue() - meanTarget);
                this.sumTarget += inst.classValue();
                this.weightObserved += inst.weight();
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
                    new Measurement("relative mean absolute error",
                    getRelativeMeanError()),
                    new Measurement("relative root mean squared error",
                    getRelativeSquareError())
        };
    }

    public double getTotalWeightObserved() {
        return this.weightObserved;
    }

    public double getMeanError() {
        return this.weightObserved > 0.0 ? this.averageError
                / this.weightObserved : 0.0;
    }

    public double getSquareError() {
        return Math.sqrt(this.weightObserved > 0.0 ? this.squareError
                / this.weightObserved : 0.0);
    }

    public double getTargetMeanError() {
        return this.weightObserved > 0.0 ? this.averageTargetError
                / this.weightObserved : 0.0;
    }

    public double getTargetSquareError() {
        return Math.sqrt(this.weightObserved > 0.0 ? this.squareTargetError
                / this.weightObserved : 0.0);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
    }

    private double getRelativeMeanError() {
        //double targetMeanError = getTargetMeanError();
        //return targetMeanError > 0 ? getMeanError()/targetMeanError : 0.0;
        return this.averageTargetError> 0 ?
                this.averageError/this.averageTargetError : 0.0;
}

    private double getRelativeSquareError() {
        //double targetSquareError = getTargetSquareError();
        //return targetSquareError > 0 ? getSquareError()/targetSquareError : 0.0;
    return Math.sqrt(this.squareTargetError> 0 ?
                this.squareError/this.squareTargetError : 0.0);
    }
    
    @Override
    public void addResult(Example<Instance> example, Prediction prediction) {
    	if(prediction!=null)
    		addResult(example,prediction.getVotes(0));
    }
}
