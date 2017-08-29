/*
 *    MeanAbsoluteDeviation.java
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
package moa.classifiers.rules.errormeasurers;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes the Mean Absolute Deviation for single target regression problems
 */
public class MeanAbsoluteDeviation extends ErrorMeasurement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private double weightSeen=0;
	private double sumError=0;

	@Override
	public double getCurrentError() {
		if(weightSeen==0)
			return Double.MAX_VALUE;
		else
			return sumError/weightSeen;
	}

	@Override
	public void addPrediction(double[] prediction, Instance inst) {
		sumError=Math.abs(prediction[0]-inst.classValue())*inst.weight()+fadingErrorFactor*sumError;
		weightSeen=inst.weight()+fadingErrorFactor*weightSeen;
	}

}
