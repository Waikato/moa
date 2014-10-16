package moa.classifiers.rules.core.anomalydetection.probabilityfunctions;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.OptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.Options;

public class ChebyshevInequality extends AbstractOptionHandler implements ProbabilityFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
		
	}

	@Override
	public double getProbability(double mean, double sd, double value) {
		{
			double probability = 0.0;
		//	double diff = value - mean;
	        if (sd > 0.0) {
	            double k = (Math.abs(value - mean) / sd);
	            if (k > 1.0) {
	                probability = 1.0 / (k * k); // Chebyshev's inequality
	            } else {
	                //probability = Math.exp(-(diff * diff / (2.0 * sd * sd)));
	            	probability=1;
	            }
	        }
			return probability;
		}
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
		
	}


}
