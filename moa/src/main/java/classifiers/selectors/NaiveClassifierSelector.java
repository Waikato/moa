package classifiers.selectors;

public class NaiveClassifierSelector implements ClassifierSelector
{
	double threshold;
	public NaiveClassifierSelector(double threshold)
	{
		this.threshold = threshold;
	}

	@Override
	public int makeDecision(double avgInterval)
	{
		// if the avgInterval is smaller than the threshold, use classifier 1. Otherwise, use 2. 
		return (avgInterval<threshold)?1:2;
	}

}
