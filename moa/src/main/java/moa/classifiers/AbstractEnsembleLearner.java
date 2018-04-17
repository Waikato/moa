package moa.classifiers;

import java.util.ArrayList;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.Measurement;
import moa.learners.InstanceLearner;
import moa.options.ClassOption;

public abstract class AbstractEnsembleLearner<MLTask extends InstanceLearner> extends AbstractInstanceLearner<MLTask> {

	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption;

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models in the ensemble.", 10, 1, Integer.MAX_VALUE);
    
    public ArrayList<MLTask> ensemble;

    public long measureByteSize() {
    	long size = 0;
    	for (MLTask c : ensemble) 
    		size += c.measureByteSize();
    	return size;
    }

    @SuppressWarnings("unchecked")
	@Override
    public void resetLearningImpl() {
        this.ensemble = new ArrayList<>(this.ensembleSizeOption.getValue());
        MLTask baseLearner = (MLTask) getPreparedClassOption(this.baseLearnerOption);
        for (int i = 0; i < this.ensembleSizeOption.getValue(); i++) {
        	MLTask learner = (MLTask) baseLearner.copy();
        	learner.setRandomSeed(this.randomSeed + i + 1);
        	learner.prepareForUse();
            this.ensemble.add(learner);
        }
    }
   
    public void setRandomSeed(int seed) {
    	super.setRandomSeed(seed);
    	if (this.ensemble != null) 
    		for (int i = 0; i < this.ensembleSizeOption.getValue(); i++) {
    			if (this.ensemble.get(i) != null)
    				this.ensemble.get(i).setRandomSeed(this.randomSeed + i + 1);
    		}	
    }
    
	public abstract Prediction combinePredictions(Prediction[] predictions);
	
	public Prediction getPredictionForInstance(Instance inst) {
		Prediction[] predictions = new Prediction[this.ensemble.size()];
		for (int i = 0; i < this.ensemble.size(); i++) {
			predictions[i] = this.ensemble.get(i).getPredictionForInstance(inst);
		}
		return combinePredictions(predictions);
	}

	public Prediction getPredictionForInstanceUsingN(Instance inst, int n) {
		Prediction[] predictions = new Prediction[this.ensemble.size()];
		for (int i = 0; i < n; i++) {
			predictions[i] = this.ensemble.get(i).getPredictionForInstance(inst);
		}
		return combinePredictions(predictions);
	}
	
    @Override    
    public void modelContextSet() {
        for (int i = 0; i < this.ensemble.size(); i++) {
            this.ensemble.get(i).setModelContext(this.getModelContext());;
        }
    }
    
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{new Measurement("ensemble size",
                    this.ensemble != null ? this.ensemble.size() : 0)};
    }
    
    @SuppressWarnings("unchecked")
	public MLTask[] getSubClassifiers() {
        return (MLTask[]) this.ensemble.clone();
    }
}
