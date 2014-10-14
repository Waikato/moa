package moa.classifiers.rules.core.anomalydetection.probabilityfunctions;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.OptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.Options;

public class CantellisInequality extends AbstractOptionHandler implements ProbabilityFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public double getProbability(double mean, double sd, double value) {
		if(sd==0)
			sd=10^-9;
		double var=Math.pow(sd, 2);
		double prob=2*var/(var+Math.pow(value - mean, 2));
		if(prob>1)
			prob=1;
		return prob;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {


	}


}
