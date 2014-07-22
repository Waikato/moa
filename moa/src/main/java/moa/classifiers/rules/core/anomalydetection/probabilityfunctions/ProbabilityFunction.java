package moa.classifiers.rules.core.anomalydetection.probabilityfunctions;

import moa.options.OptionHandler;

public interface ProbabilityFunction extends OptionHandler {
	double getProbability(double mean, double sd, double value);
}
