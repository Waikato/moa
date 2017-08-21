/*
 *    GaussInequality.java
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
 * Returns the probability for anomaly detection according to a Gauss inequality
 * mean- mean of a  data variable
 * sd- standard deviation of a data variable
 * value- current value of the variable
 */




public class GaussInequality extends AbstractOptionHandler implements ProbabilityFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final double SQRT3=Math.sqrt(3);

	@Override
	public double getProbability(double mean, double sd, double value) {
		/*
		 * From Wikipedia
		 * http://en.wikipedia.org/wiki/Gauss's_inequality
		 */
		/*
		if(sd==0)
			sd=0.0000001;
		double k=Math.abs(value-mean);
		double r=Math.sqrt(Math.pow(k,2)+Math.pow(sd,2));
		if (k>2*r/SQRT3)
			return Math.pow(2*r/(3*k),2);
		else if(r>0)
			return 1-k/(r*SQRT3);
		else
			return 1;
		*/
		
		/*
		 * From "Generalized Gauss Inequalities via Semidefinite Programming 
		 * by Bart P.G. Van Pary, Paul J. Goulart and Daniel Kuhn
		 */
		
		if(sd==0)
			sd=0.0000001;
		double k=Math.abs(value-mean)/sd;
		if (k>2/SQRT3)
			return 4/(9*Math.pow(k,2));
		else 
			return 1-k/SQRT3;
			
		
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {


	}


}
