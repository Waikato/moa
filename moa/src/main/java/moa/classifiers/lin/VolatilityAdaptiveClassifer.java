package moa.classifiers.lin;

import java.security.AlgorithmConstraints;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import classifiers.selectors.ClassifierSelector;
import classifiers.selectors.NaiveClassifierSelector;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.trees.HoeffdingAdaptiveTree;
import moa.classifiers.trees.HoeffdingTree;
import cutpointdetection.ADWIN;
import moa.core.Measurement;
import volatilityevaluation.RelativeVolatilityDetector;

public class VolatilityAdaptiveClassifer extends AbstractClassifier
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -220640148754624744L;
	
	private AbstractClassifier classifier1;
	private AbstractClassifier classifier2;
	
	private AbstractClassifier activeClassifier;
	private RelativeVolatilityDetector volatilityDetector;
	private ClassifierSelector classiferSelector;
	
	
	@Override
	public boolean isRandomizable()
	{
		return false;
	}

	@Override
	public void getModelDescription(StringBuilder arg0, int arg1)
	{
		
	}

	/**return the information of the current algorithm */
	@Override
	protected Measurement[] getModelMeasurementsImpl()
	{
		/*
        return new Measurement[]{
                new Measurement("tree size (nodes)", this.decisionNodeCount
                + this.activeLeafNodeCount + this.inactiveLeafNodeCount),
                new Measurement("tree size (leaves)", this.activeLeafNodeCount
                + this.inactiveLeafNodeCount),
                new Measurement("active learning leaves",
                this.activeLeafNodeCount),
                new Measurement("tree depth", measureTreeDepth()),
                new Measurement("active leaf byte size estimate",
                this.activeLeafByteSizeEstimate),
                new Measurement("inactive leaf byte size estimate",
                this.inactiveLeafByteSizeEstimate),
                new Measurement("byte size estimate overhead",
                this.byteSizeEstimateOverheadFraction)};
                */
		return null;
	}

	@Override
	public double[] getVotesForInstance(Instance inst)
	{
		return activeClassifier.getVotesForInstance(inst);
	}

	@Override
	public void resetLearningImpl()
	{
		initClassifiers();
		classiferSelector = new NaiveClassifierSelector(100);
		volatilityDetector = new RelativeVolatilityDetector(new ADWIN(), 32);
		
		activeClassifier = classifier1;
	}
	
	private void initClassifiers()
	{
		// classifier 1
		HoeffdingTree ht = new HoeffdingTree();
		classifier1 = ht;
		
		//classifier 2 
		HoeffdingAdaptiveTree hat = new HoeffdingAdaptiveTree();
		classifier2 = hat;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst)
	{
		// if there is a volatility shift.
		if(volatilityDetector.setInputVar(
				correctlyClassifies(inst) ? 0.0 : 1.0))
		{
			
			double avgInterval = volatilityDetector.getBufferMean();
			int decision = classiferSelector.makeDecision(avgInterval);
			
			activeClassifier = (decision==1)?classifier1:classifier2;
		}
		
		activeClassifier.trainOnInstance(inst);
	}
	
}
