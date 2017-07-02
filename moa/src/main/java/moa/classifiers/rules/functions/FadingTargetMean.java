/*
 *    FadingTargetMean.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author J. Duarte, A. Bifet, J. Gama
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
package moa.classifiers.rules.functions;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;
import com.yahoo.labs.samoa.instances.predictions.RegressionPrediction;

public class FadingTargetMean extends TargetMean implements AMRulesRegressorFunction{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1383391769242905972L;

	public FloatOption fadingFactorOption = new FloatOption(
			"fadingFactor", 'f', 
			"Fading factor for the FadingTargetMean accumulated error", 0.99, 0, 1);

	private double nD;
	private double fadingFactor;
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		updateAccumulatedError(inst);
		nD=inst.weight()+fadingFactor*nD;
		sum=inst.classValue()*inst.weight()+fadingFactor*sum;	
	}	
	@Override
	public void resetLearningImpl() {
		super.resetLearningImpl();
		this.fadingFactor=fadingFactorOption.getValue();
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		double currentMean = 0;
		if (nD>0)
			currentMean = sum/nD;
		return new RegressionPrediction(currentMean);
	}

}
