package moa.classifiers.rules.multilabel.functions;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.multilabel.errormeasurers.AbstractMultiTargetErrorMeasurer;
import moa.classifiers.rules.multilabel.errormeasurers.MultiTargetErrorMeasurer;
import moa.core.Measurement;
import moa.learners.Learner;
import moa.options.ClassOption;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

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

	protected boolean hasStarted;

	protected MultiTargetRegressor baseLearner[] ;


	protected MultiTargetErrorMeasurer [] errorMeasurer;

	public AdaptiveMultiTargetRegressor(){
		 baseLearnerOption1 = new ClassOption("baseLearner1", 'l',
					"First base learner.", MultiTargetRegressor.class, "moa.classifiers.rules.multilabel.functions.MultiLabelTargetMeanRegressor") ;
		 baseLearnerOption2= new ClassOption("baseLearner2", 'm',
					"Second base learner.", MultiTargetRegressor.class, "moa.classifiers.rules.multilabel.functions.MultiLabelPerceptronRegressor") ;
		 errorMeasurerOption = new ClassOption("errorMeasurer", 'e',
					"Measure of error for deciding which learner should predict.", AbstractMultiTargetErrorMeasurer.class, "MeanAbsoluteDeviationMT") ;

	}
	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		if (!this.hasStarted){	
			baseLearner=new MultiTargetRegressor[NUM_LEARNERS];
			errorMeasurer= new MultiTargetErrorMeasurer[NUM_LEARNERS];
			baseLearner[0]=(MultiTargetRegressor) getPreparedClassOption(this.baseLearnerOption1);
			baseLearner[1]=(MultiTargetRegressor) getPreparedClassOption(this.baseLearnerOption2);
			for (int i=0; i<NUM_LEARNERS; i++){
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
	



}
