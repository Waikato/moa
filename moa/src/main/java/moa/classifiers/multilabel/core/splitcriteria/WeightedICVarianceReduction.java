package moa.classifiers.multilabel.core.splitcriteria;

import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/**
 * Weighted intra cluster variance reduction split criterion
 * 
 * @author Aljaž Osojnik <aljaz.osojnik@ijs.si>
 *
 */

public class WeightedICVarianceReduction extends ICVarianceReduction {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public DoubleVector weights;

	public WeightedICVarianceReduction(DoubleVector targetWeights) {
		super();
		this.weights = targetWeights;
	}

	@Override
	public double getMeritOfSplit(DoubleVector[] preSplitDist, DoubleVector[][] postSplitDists) {
		double error = 0;
		int numOutputs = preSplitDist.length;
		for (int i = 0; i < numOutputs; i++)
			// TEMPORARY
			// error += (1 / weights.getValue(i)) *
			// getMeritOfSplitForOutput(preSplitDist,postSplitDists,i);
			error += weights.getValue(i) * getMeritOfSplitForOutput(preSplitDist, postSplitDists, i);
		return error / weights.sumOfValues();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
	}

}
