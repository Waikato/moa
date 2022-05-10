package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Measurement;
import moa.options.ClassOption;

public class MLCviaMTR extends AbstractMultiLabelLearner implements MultiLabelClassifier {

	protected InstancesHeader header;
	
	protected MultiTargetRegressor learner;
	
    public ClassOption regressorOption = new ClassOption("regressor", 'l',
            "Multi-target regressor to use for MLC .", MultiTargetRegressor.class, "multilabel.trees.ISOUPTree");

	private void makeHeader() {
		if (header == null) {
				header = getModelContext();
		}
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		learner.trainOnInstance(instance);
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		int n = header.numOutputAttributes();
		Prediction p = learner.getPredictionForInstance(inst);
        MultiLabelPrediction y = new MultiLabelPrediction(n);
		
        for (int i = 0; i < n; i++)
        	// Correctly set values for an MLC prediction
    		y.setVotes(i, new double[]{1 - p.getVote(i, 1), p.getVote(i, 1)});
		
		return y;
	}

	@Override
	public void resetLearningImpl() {
		learner = (MultiTargetRegressor) ((MultiTargetRegressor) getPreparedClassOption(this.regressorOption)).copy();
		learner.resetLearning();
	}
	
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		makeHeader();
		learner.setModelContext(ih);
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return learner.getModelMeasurements();
	}


	@Override
	public void setRandomSeed(int i) {
		super.setRandomSeed(i);
		learner.setRandomSeed(i);
	}

	
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO This needs to be cleaned up, currently MultiTargetRegressor does not contain getModelDescription
		// These kinds of methods should be in the interfaces and not the abstract classes
		// learner.getModelDescription(out, indent);
	}
    
	
}
