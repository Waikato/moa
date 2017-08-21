/*
 *    CantellisInequality.java
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
package moa.classifiers.rules.core.anomalydetection.probabilityfunctions;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;


/**
 * Returns the probability for anomaly detection according to a Cantelli inequality
 * mean- mean of a  data variable
 * sd- standard deviation of a data variable
 * value- current value of the variable
 */



public class CantellisInequality extends AbstractOptionHandler implements ProbabilityFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public double getProbability(double mean, double sd, double value) {
		if(sd==0)
			sd=10^-9;
		double var=Math.pow(sd, 2);
		double prob=2*var/(var+Math.pow(value - mean, 2));
		if(prob>1)
			prob=1;
		return prob;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {


	}


}
