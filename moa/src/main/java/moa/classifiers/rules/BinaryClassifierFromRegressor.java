/*
 *    BinaryClassifierFromRegressor.java
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

package moa.classifiers.rules;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.Regressor;
import moa.core.Measurement;
import moa.options.ClassOption;

/**
 * Function that convertes a regressor into a binary classifier
 * baseLearnerOption- regressor learner selection
 */

public class BinaryClassifierFromRegressor extends AbstractClassifier {

	/**
	 * 
	 */
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'r',
            "Regressor to train.", Regressor.class, "rules.AMRulesRegressor");
    
	private static final long serialVersionUID = 8271864290912280188L;
	private Classifier regressor;
	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		//TODO: use a parameter determining the function to use to return the output.
		//Current function is the step function. By default should return regressor values
		double vote=this.regressor.getVotesForInstance(inst)[0]; //Maybe pass the value through a sigmoid function
		double [] ret = new double[2];
		if (vote < 0.5){
			ret[0]=1;
			ret[1]=0;
		}
		else{
			ret[0]=0;
			ret[1]=1;
		}
		return ret;
	}

	@Override
	public void resetLearningImpl() {
        this.regressor = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        this.regressor.resetLearning();
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		this.regressor.trainOnInstance(inst);

	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return this.regressor.getModelMeasurements();
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		
	}

}