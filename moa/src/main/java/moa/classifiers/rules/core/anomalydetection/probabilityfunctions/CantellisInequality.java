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
		return sd/(sd+Math.abs(value - mean));
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {


	}


}
