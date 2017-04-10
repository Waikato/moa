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
import moa.evaluation.MultiTargetWindowRegressionPerformanceRelativeMeasuresEvaluator.Estimator;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.DenseInstanceData;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Regression evaluator that performs basic incremental evaluation.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class BasicMultiTargetPerformanceRelativeMeasuresEvaluator extends AbstractMOAObject
implements MultiTargetPerformanceEvaluator, RegressionPerformanceEvaluator{

	private static final long serialVersionUID = 1L;

	protected double weightObserved;

	protected double [] squareError;

	protected double [] averageError;

	protected double [] averageErrorToTargetMean;

	protected double [] squareErrorToTargetMean;

	protected double [] sumY;


	protected int numberOutputs;

	@Override
	public void reset() {
		this.weightObserved = 0.0;
		this.squareError = null;
		this.averageError = null;
		this.averageErrorToTargetMean=null;
		this.squareErrorToTargetMean=null;
		this.sumY=null;
	}

	@Override
	public void addResult(Example<Instance> example, Prediction prediction) {
		MultiLabelInstance inst = (MultiLabelInstance) example.getData();
		if (numberOutputs == 0) {
			numberOutputs = inst.numberOutputTargets();
		}
		if(this.squareError==null){
			this.squareError = new double[numberOutputs];
			this.averageError = new double[numberOutputs];
			this.averageErrorToTargetMean=new double[numberOutputs];
			this.squareErrorToTargetMean=new double[numberOutputs];
			this.sumY=new double[numberOutputs];
		}



		if (inst.weight() > 0.0) {
			this.weightObserved += inst.weight();
			if (prediction != null && prediction.numOutputAttributes()>0) {
				for (int i = 0; i< numberOutputs;i++){
					double error=(inst.valueOutputAttribute(i) - prediction.getVote(i, 0));
					
					this.sumY[i]+=inst.valueOutputAttribute(i);
					double errorTM=(inst.valueOutputAttribute(i) - this.sumY[i]/this.weightObserved);

					this.averageErrorToTargetMean[i]+=Math.abs(errorTM);
					this.squareErrorToTargetMean[i]+=errorTM*errorTM;

					this.averageError[i]+=Math.abs(error);
					this.squareError[i]+=error*error;

				}
			}
			//System.out.println(inst.classValue()+", "+prediction);
		}
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
		return this.weightObserved;
	}

	public double getMeanError() {
		double error=0;
		if(this.weightObserved > 0.0 ){
			for(int i=0; i<this.averageError.length;i++){
				error+=this.averageError[i]/this.averageErrorToTargetMean[i];
			}
			error/=this.numberOutputs;
		}
		return error;
	}

	public double getSquareError() {
		double error=0;
		if(this.weightObserved > 0.0 ){
			for(int i=0; i<this.squareError.length;i++){
				error+=Math.sqrt(this.squareError[i]/this.squareErrorToTargetMean[i]);
			}
			error/=this.numberOutputs;
		}
		return error;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
				sb, indent);
	}

	//only for one output
	@Override
	public void addResult(Example<Instance> example, double[] classVotes) {
		Prediction p=new MultiLabelPrediction(1);
		p.setVotes(classVotes);
		addResult(example, p);
	}

}
