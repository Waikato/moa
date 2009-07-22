/*
 *    DecisionStump.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.options.IntOption;
import weka.core.Instance;

public class DecisionStump extends AbstractClassifier {

	private static final long serialVersionUID = 1L;

	public IntOption gracePeriodOption = new IntOption("gracePeriod", 'g',
			"The number of instances to observe between model changes.", 1000,
			0, Integer.MAX_VALUE);

	public FlagOption binarySplitsOption = new FlagOption("binarySplits", 'b',
			"Only allow binary splits.");

	public ClassOption splitCriterionOption = new ClassOption("splitCriterion",
			'c', "Split criterion to use.", SplitCriterion.class,
			"InfoGainSplitCriterion");

	protected AttributeSplitSuggestion bestSplit;

	protected DoubleVector observedClassDistribution;

	protected AutoExpandVector<AttributeClassObserver> attributeObservers;

	protected double weightSeenAtLastSplit;

	@Override
	public void resetLearningImpl() {
		this.bestSplit = null;
		this.observedClassDistribution = new DoubleVector();
		this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
		this.weightSeenAtLastSplit = 0.0;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		this.observedClassDistribution.addToValue((int) inst.classValue(), inst
				.weight());
		for (int i = 0; i < inst.numAttributes() - 1; i++) {
			int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
			AttributeClassObserver obs = this.attributeObservers.get(i);
			if (obs == null) {
				obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver()
						: newNumericClassObserver();
				this.attributeObservers.set(i, obs);
			}
			obs.observeAttributeClass(inst.value(instAttIndex), (int) inst
					.classValue(), inst.weight());
		}
		if (this.trainingWeightSeenByModel - this.weightSeenAtLastSplit >= this.gracePeriodOption
				.getValue()) {
			this.bestSplit = findBestSplit((SplitCriterion) getPreparedClassOption(this.splitCriterionOption));
			this.weightSeenAtLastSplit = this.trainingWeightSeenByModel;
		}
	}

	public double[] getVotesForInstance(Instance inst) {
		if (this.bestSplit != null) {
			int branch = this.bestSplit.splitTest.branchForInstance(inst);
			if (branch >= 0) {
				return this.bestSplit
						.resultingClassDistributionFromSplit(branch);
			}
		}
		return this.observedClassDistribution.getArrayCopy();
	}

	public boolean isRandomizable() {
		return false;
	}

	protected AttributeClassObserver newNominalClassObserver() {
		return new NominalAttributeClassObserver();
	}

	protected AttributeClassObserver newNumericClassObserver() {
		return new GaussianNumericAttributeClassObserver();
	}

	protected AttributeSplitSuggestion findBestSplit(SplitCriterion criterion) {
		AttributeSplitSuggestion bestFound = null;
		double bestMerit = Double.NEGATIVE_INFINITY;
		double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
		for (int i = 0; i < this.attributeObservers.size(); i++) {
			AttributeClassObserver obs = this.attributeObservers.get(i);
			if (obs != null) {
				AttributeSplitSuggestion suggestion = obs
						.getBestEvaluatedSplitSuggestion(criterion,
								preSplitDist, i, this.binarySplitsOption
										.isSet());
				if (suggestion.merit > bestMerit) {
					bestMerit = suggestion.merit;
					bestFound = suggestion;
				}
			}
		}
		return bestFound;
	}

}
