package classifiers.selectors;

public class AlwaysFirstClassifierSelector implements ClassifierSelector
{

	@Override
	public int makeDecision(double avgInterval)
	{
		return 1;
	}

}
