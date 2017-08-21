/*
 *    ErrorMeasurement.java
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

import moa.AbstractMOAObject;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Computes error measures with a fading factor
 * fadingErrorFactorOption - Fading factor
 */



abstract public class ErrorMeasurement extends AbstractMOAObject {
	
	public FloatOption fadingErrorFactorOption = new FloatOption(
			"fadingErrorFactor", 'e', 
			"Fading factor for the error", 0.99, 0, 1);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected double fadingErrorFactor;
	abstract public void addPrediction(double [] prediction, Instance inst);
	
	abstract public double getCurrentError();

	public ErrorMeasurement(){
		fadingErrorFactor=fadingErrorFactorOption.getValue();
	}
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
