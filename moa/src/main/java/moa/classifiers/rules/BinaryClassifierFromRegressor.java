package moa.classifiers.rules;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.Regressor;
import moa.core.Measurement;
import moa.options.ClassOption;

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