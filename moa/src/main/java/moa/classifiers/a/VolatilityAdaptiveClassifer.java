package moa.classifiers.a;

import com.yahoo.labs.samoa.instances.Instance;

import classifiers.selectors.AlwaysFirstClassifierSelector;
import classifiers.selectors.ClassifierSelector;
import classifiers.selectors.NaiveClassifierSelector;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import cutpointdetection.ADWIN;
import moa.core.Measurement;
import moa.options.ClassOption;
import volatilityevaluation.RelativeVolatilityDetector;

public class VolatilityAdaptiveClassifer extends AbstractClassifier
{

	
	
	private static final long serialVersionUID = -220640148754624744L;
	
	
	public ClassOption classifier1Option = new ClassOption("classifier1", 'a', "The classifier used in low volatility mode",
			Classifier.class, "moa.classifiers.trees.HoeffdingTree");
	public ClassOption classifier2Option = new ClassOption("classifier2", 'b', "The classifier used in high volatility mode",
			Classifier.class, "moa.classifiers.trees.HoeffdingAdaptiveTree");
	
	private AbstractClassifier classifier1;
	private AbstractClassifier classifier2;
	private AbstractClassifier activeClassifier;
	private int activeClassifierIndex; 
	
	private RelativeVolatilityDetector volatilityDetector;
	private ClassifierSelector classiferSelector;
	
	private int instanceCount;
	
	
	
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
		
		//selector option
		classiferSelector = new AlwaysFirstClassifierSelector();
		
		
		
		volatilityDetector = new RelativeVolatilityDetector(new ADWIN(), 32);
		instanceCount = 0;
		
		activeClassifierIndex = 1;
		activeClassifier = classifier1;
		
	}
	
	private void initClassifiers()
	{
		//classifier 1
		this.classifier1 = (AbstractClassifier) getPreparedClassOption(this.classifier1Option);
		
		//classifier 2 
		this.classifier2 = (AbstractClassifier) getPreparedClassOption(this.classifier2Option);
	}

	@Override
	public void trainOnInstanceImpl(Instance inst)
	{
		// if there is a volatility shift.
		if(volatilityDetector.setInputVar(correctlyClassifies(inst) ? 0.0 : 1.0))
		{
			
			double avgInterval = volatilityDetector.getBufferMean();
			int decision = classiferSelector.makeDecision(avgInterval);
			
			//test
			System.out.printf("%d, %f \n", instanceCount, avgInterval);
			
			if(activeClassifierIndex!=decision)
			{
				activeClassifier = (decision==1)?classifier1:classifier2;
				//test
				System.out.println(decision);
			}

		}
		instanceCount++;
		activeClassifier.trainOnInstance(inst);
		
	}
	
}
