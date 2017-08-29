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
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Multi-target regression evaluator that updates evaluation results using a sliding window.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class MultiTargetWindowRegressionPerformanceRelativeMeasuresEvaluator extends AbstractOptionHandler
implements MultiTargetPerformanceEvaluator, RegressionPerformanceEvaluator {

	private static final long serialVersionUID = 1L;

	public IntOption widthOption = new IntOption("width",
			'w', "Size of Window", 1000);

	protected double TotalweightObserved = 0;

	protected Estimator weightObserved;

	protected Estimator [] squareError;

	protected Estimator [] averageError;

	protected Estimator [] averageErrorToTargetMean;

	protected Estimator [] squareErrorToTargetMean;

	protected Estimator [] sumY;

	protected int numClasses;

	protected int numberOutputs;

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
    	this.TotalweightObserved = 0;
    }

    @Override
    public void addResult(Example<Instance> example, double[] prediction) {
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {
    	return new Measurement[]{
    			new Measurement("classified instances",
    					getTotalWeightObserved()),
    					new Measurement("relative mean absolute error",
    							getMeanError()),
    							new Measurement("relative root mean squared error",
    									getSquareError())};
    }

    public double getTotalWeightObserved() {
    	return this.weightObserved.total();
    }

    public double getMeanError() {
    	double error=0;
    	if(this.weightObserved.total() > 0.0 ){
    		for(int i=0; i<this.averageError.length;i++){
    			error+=this.averageError[i].total()/this.averageErrorToTargetMean[i].total();
    		}
    		error/=this.averageError.length;
    	}
    	return error;
    }

    public double getSquareError() {
    	double error=0;
    	if(this.weightObserved.total() > 0.0 ){
    		for(int i=0; i<this.squareError.length;i++){
    			error+=Math.sqrt(this.squareError[i].total()/this.squareErrorToTargetMean[i].total());
    		}
    		error/=this.squareError.length;
    	}
    	return error;
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
    	MultiLabelInstance inst=(MultiLabelInstance) testInst.getData();
    	if(this.squareError==null){
    		this.squareError=new Estimator[inst.numOutputAttributes()];
    		this.averageError=new Estimator[inst.numOutputAttributes()];
    		this.averageErrorToTargetMean=new Estimator[inst.numOutputAttributes()];
    		this.squareErrorToTargetMean=new Estimator[inst.numOutputAttributes()];
    		this.sumY=new Estimator[inst.numOutputAttributes()];

    		for (int i=0; i<inst.numOutputAttributes();i++){
    			this.averageError[i] = new Estimator(this.widthOption.getValue());
    			this.averageErrorToTargetMean[i]=new Estimator(this.widthOption.getValue());
    			this.squareError[i] = new Estimator(this.widthOption.getValue());
    			this.squareErrorToTargetMean[i]=new Estimator(this.widthOption.getValue());
    			this.sumY[i]=new Estimator(this.widthOption.getValue());
    		}
    	}


    	double weight = inst.weight();
    	if (numberOutputs == 0) {
    		numberOutputs = inst.numberOutputTargets();
    	}
    	if (weight > 0.0) {
    		if (TotalweightObserved == 0) {
    			reset(inst.dataset().numClasses());
    		}
    		this.TotalweightObserved += weight;
    		this.weightObserved.add(weight);

    		if (prediction!=null) {
    			for (int i = 0; i< numberOutputs;i++){
    				double error=(inst.valueOutputAttribute(i) - prediction.getVote(i, 0));
    				this.sumY[i].add(inst.valueOutputAttribute(i));
    				double errorTM=(inst.valueOutputAttribute(i) - this.sumY[i].total()/this.weightObserved.total());
    				
    				this.averageErrorToTargetMean[i].add(Math.abs(errorTM));;
        			this.squareErrorToTargetMean[i].add(errorTM*errorTM);;
        			
    				this.averageError[i].add(Math.abs(error));
    				this.squareError[i].add( error*error);
    			}
    		}
    		//System.out.println(inst.classValue()+", "+prediction[0]);
    	}

    }
}
