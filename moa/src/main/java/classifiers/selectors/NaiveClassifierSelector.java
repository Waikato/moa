package classifiers.selectors;

import com.github.javacliparser.IntOption;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class NaiveClassifierSelector extends AbstractOptionHandler implements ClassifierSelector
{

	private static final long serialVersionUID = 3866059530234889292L;
	
	public double threshold;
	public IntOption thresholdOption = new IntOption("threshold", 't',
			"threshold", 200, 0, Integer.MAX_VALUE);
	
	public NaiveClassifierSelector()
	{
		
	}
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
	
	@Override
	public void getDescription(StringBuilder sb, int indent)
	{
		
	}
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository)
	{
		this.threshold = thresholdOption.getValue();
	}

}
