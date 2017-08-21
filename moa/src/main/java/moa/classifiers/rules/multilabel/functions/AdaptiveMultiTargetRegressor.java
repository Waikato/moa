/*
 *    AdaptiveMultiTargetRegressor.java
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
package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.multilabel.errormeasurers.AbstractMultiTargetErrorMeasurer;
import moa.classifiers.rules.multilabel.errormeasurers.MeanAbsoluteDeviationMT;
import moa.classifiers.rules.multilabel.errormeasurers.MultiTargetErrorMeasurer;
import moa.core.Measurement;
import moa.options.ClassOption;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Adaptive MultiTarget Regressor uses two learner
 * The first is used in first stage when high error are produced(e.g. Target mean)
 * The second is used in a second stage when low error are produced(e.g perceptron)
 * 	baseLearnerOption1- Learner one ;
 *  baseLearnerOption2- learner two
 */


public class AdaptiveMultiTargetRegressor extends AbstractMultiLabelLearner
implements MultiTargetRegressor, AMRulesFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int NUM_LEARNERS=2;

	public ClassOption baseLearnerOption1;
	public ClassOption baseLearnerOption2;

	public ClassOption errorMeasurerOption;
	
	public IntOption randomSeedOption = new IntOption("randomSeedOption",
			'r', "randomSeedOption", 
			1,Integer.MIN_VALUE, Integer.MAX_VALUE);

	protected boolean hasStarted;

	protected MultiTargetRegressor baseLearner[] ;


	protected MultiTargetErrorMeasurer [] errorMeasurer;

	public AdaptiveMultiTargetRegressor(){
		super.randomSeedOption=randomSeedOption;
		baseLearnerOption1 = new ClassOption("baseLearner1", 'l',
				"First base learner.", AMRulesFunction.class, MultiTargetMeanRegressor.class.getName()) ;
		baseLearnerOption2= new ClassOption("baseLearner2", 'm',
				"Second base learner.", AMRulesFunction.class, MultiTargetPerceptronRegressor.class.getName()) ;
		errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
				"Measure of error for deciding which learner should predict.", AbstractMultiTargetErrorMeasurer.class, MeanAbsoluteDeviationMT.class.getName()) ;

	}
	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		if (!this.hasStarted){	
			baseLearner=new MultiTargetRegressor[NUM_LEARNERS];
			errorMeasurer= new MultiTargetErrorMeasurer[NUM_LEARNERS];
			baseLearner[0]=(MultiTargetRegressor) getPreparedClassOption(this.baseLearnerOption1);
			baseLearner[1]=(MultiTargetRegressor) getPreparedClassOption(this.baseLearnerOption2);
			for (int i=0; i<NUM_LEARNERS; i++){
				if(baseLearner[i].isRandomizable())
					baseLearner[i].setRandomSeed(this.randomSeed);
				baseLearner[i].resetLearning();
				errorMeasurer[i]=(MultiTargetErrorMeasurer)((MultiTargetErrorMeasurer) getPreparedClassOption(this.errorMeasurerOption)).copy();
			
			}
			this.hasStarted = true;
		}
		for (int i=0; i<NUM_LEARNERS; i++){
			//Update online errors
			Prediction prediction=baseLearner[i].getPredictionForInstance(instance);
			if(prediction!=null)//should happen only for first instance
				errorMeasurer[i].addPrediction(prediction, instance);
			//Train
			baseLearner[i].trainOnInstanceImpl(instance);
		}

	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		Prediction prediction=null;
		if(hasStarted){
			int bestIndex=0;
			double minError=Double.MAX_VALUE;
			for (int i=0; i<NUM_LEARNERS; i++){
				double error=errorMeasurer[i].getCurrentError();
				if(error<minError)
				{
					minError=error;
					bestIndex=i;
				}
			}
			prediction=baseLearner[bestIndex].getPredictionForInstance(inst);
		}
		return prediction;
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void resetLearningImpl() {
		this.hasStarted = false;
		if(baseLearner!=null){
			for (int i=0; i<baseLearner.length; i++){
				classifierRandom.setSeed(randomSeedOption.getValue());
				baseLearner[i].setRandomSeed(this.randomSeed);
				baseLearner[i].resetLearning();
			}
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
		return "Learns two regressors and uses the regressor with less error to predict.";
	}

	@Override
	public void resetWithMemory() {
		if(errorMeasurer==null)
			errorMeasurer=new MultiTargetErrorMeasurer[NUM_LEARNERS];
		for (int i=0; i<NUM_LEARNERS; i++){
			errorMeasurer[i]=(MultiTargetErrorMeasurer)((MultiTargetErrorMeasurer) getPreparedClassOption(this.errorMeasurerOption)).copy();
			if(baseLearner[i] instanceof AMRulesFunction)
				((AMRulesFunction)baseLearner[i]).resetWithMemory();
		}
	}
	@Override
	public void selectOutputsToLearn(int[] outtputAtributtes) {
		for (int i=0; i<NUM_LEARNERS; i++){
			((AMRulesFunction)baseLearner[i]).selectOutputsToLearn(outtputAtributtes);
		}	
	}




}
