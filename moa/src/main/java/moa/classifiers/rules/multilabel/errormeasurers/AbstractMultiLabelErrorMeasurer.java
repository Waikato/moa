/*
 *    AbstractMultiLabelErrorMeasurer.java
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

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;





public abstract class AbstractMultiLabelErrorMeasurer  extends AbstractOptionHandler implements MultiTargetErrorMeasurer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FloatOption fadingErrorFactorOption = new FloatOption(
			"fadingErrorFactor", 'f', 
			"Fading factor for the error", 0.99, 0, 1);
	
	
	
	protected double fadingErrorFactor;
	
	abstract public void addPrediction(Prediction prediction, Prediction trueClass, double weight);
	
	public void addPrediction(Prediction prediction, Prediction trueClass){
		addPrediction(prediction, trueClass,1.0);
	}
	
	abstract public void addPrediction(Prediction prediction, MultiLabelInstance inst);
	
	abstract public double getCurrentError();
	
	abstract public double getCurrentError(int index);
	
	abstract public double [] getCurrentErrors();
	
	public AbstractMultiLabelErrorMeasurer() {
		fadingErrorFactor=fadingErrorFactorOption.getValue();
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,ObjectRepository repository) {
		
	}

}
