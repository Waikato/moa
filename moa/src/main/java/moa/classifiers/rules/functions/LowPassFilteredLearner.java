/*
 *    LowPassFilteredLearner.java
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

package moa.classifiers.rules.functions;

import moa.classifiers.AbstractClassifier;
import moa.core.Measurement;
import moa.options.ClassOption;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

public class LowPassFilteredLearner extends AbstractClassifier implements AMRulesRegressorFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Base learner.", AbstractClassifier.class, AdaptiveNodePredictor.class.getName());
	
	public FloatOption alphaOption = new FloatOption("alpha", 'a',
			"Alpha value. Y=Yold+alpha*(Yold+Prediction)",0.15, 0, 1);
	
	protected AbstractClassifier learner;
	protected boolean hasStarted=false;
	protected double lastX, lastY, alpha;
	public LowPassFilteredLearner() {
		
	}

	@Override
	public double getCurrentError() {
		return 0;
	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return new double[]{lastY};
	}

	@Override
	public void resetLearningImpl() {
		hasStarted=false;
		if(learner!=null)
			learner.resetLearning();
		
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		if(!hasStarted){
			learner=(AbstractClassifier) getPreparedClassOption(this.baseLearnerOption);
			hasStarted=true;
			learner.trainOnInstance(inst);
			lastX=learner.getVotesForInstance(inst)[0];
			lastY=lastX;
			alpha=alphaOption.getValue();
		}
		else{
			learner.trainOnInstance(inst);
			lastX=learner.getVotesForInstance(inst)[0];
			lastY=lastY+alpha*(lastX-lastY);
		}

		
		
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		

	}

	@Override
	public String getPurposeString() {
		return "Low pass filtered output (Y=Yold+alpha*(Yold+Prediction))";
	}
	

}
