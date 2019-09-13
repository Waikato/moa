/*
 *    MeanAbsoluteDeviationMT.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package moa.classifiers.rules.multilabel.errormeasurers;

import com.yahoo.labs.samoa.instances.predictions.Prediction;


/**
 * Mean Absolute Deviation for multitarget and with fading factor
 */


public class MeanAbsoluteDeviationMT extends AbstractMultiTargetErrorMeasurer {

	/**
	 * 
	 */
	protected double weightSeen;
	protected double [] sumError;
	private static final long serialVersionUID = 1L;
	protected boolean hasStarted;
	protected int numLearnedOutputs;

	@Override
	public void addPrediction(Prediction prediction, Prediction trueClass, double weight) {
		int numOutputs=prediction.numOutputAttributes();
		if (!hasStarted){
			sumError=new double[numOutputs];
			hasStarted=true;
			for(int i=0; i<numOutputs;i++)
				if(prediction.hasVotesForAttribute(i))
					++numLearnedOutputs;
			hasStarted=true;
		}
		for(int i=0; i<numOutputs;i++){
			if(prediction.hasVotesForAttribute(i))
				sumError[i]=Math.abs(prediction.getVote(i, 0)-trueClass.getVote(i, 0))*weight+fadingErrorFactor*sumError[i];
		}
		weightSeen=weight+fadingErrorFactor*weightSeen;
	}

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
		{
			double sum=0;
			int numOutputs=sumError.length;
			for (int i=0; i<numOutputs; i++)
				sum+=sumError[i];
			return sum/(weightSeen*numLearnedOutputs);
		}
	}

	@Override
	public double getCurrentError(int index) {
		return sumError[index]/weightSeen;
	}

	@Override
	public double[] getCurrentErrors() {
		double [] errors=null;
		if(sumError!=null){
			errors=new double[sumError.length];
			for (int i=0;i<sumError.length; i++)
				errors[i]=sumError[i]/weightSeen;
		}
		return errors;
	}

}
