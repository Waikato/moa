/*
 *    OzaBag.java
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

import sizeof.agent.SizeOfAgent;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;
import moa.options.IntOption;
import weka.core.Instance;

public class OzaBag extends AbstractClassifier {

	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Classifier to train.", Classifier.class, "HoeffdingTree");

	public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
			"The number of models in the bag.", 10, 1, Integer.MAX_VALUE);

	protected Classifier[] ensemble;

	@Override
	public int measureByteSize() {
		int size = (int) SizeOfAgent.sizeOf(this);
		for (Classifier classifier : this.ensemble) {
			size += classifier.measureByteSize();
		}
		return size;
	}

	@Override
	public void resetLearningImpl() {
		this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
		Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.resetLearning();
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		for (int i = 0; i < this.ensemble.length; i++) {
			int k = MiscUtils.poisson(1.0, this.classifierRandom);
			if (k > 0) {
				Instance weightedInst = (Instance) inst.copy();
				weightedInst.setWeight(inst.weight() * k);
				this.ensemble[i].trainOnInstance(weightedInst);
			}
		}
	}

	public double[] getVotesForInstance(Instance inst) {
		DoubleVector combinedVote = new DoubleVector();
		for (int i = 0; i < this.ensemble.length; i++) {
			DoubleVector vote = new DoubleVector(this.ensemble[i]
					.getVotesForInstance(inst));
			if (vote.sumOfValues() > 0.0) {
				vote.normalize();
				combinedVote.addValues(vote);
			}
		}
		return combinedVote.getArrayRef();
	}

	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[] { new Measurement("ensemble size",
				this.ensemble != null ? this.ensemble.length : 0) };
	}

	@Override
	public Classifier[] getSubClassifiers() {
		return this.ensemble.clone();
	}

}
