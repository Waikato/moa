/*
 *    OzaBagAdwin.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet
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

import weka.core.Instance;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.core.SizeOf;
import moa.options.ClassOption;
import moa.options.IntOption;

public class OzaBagAdwin extends AbstractClassifier {

	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Classifier to train.", Classifier.class, "HoeffdingTree");

	public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
			"The number of models in the bag.", 10, 1, Integer.MAX_VALUE);

	protected Classifier[] ensemble;
	protected ADWIN[] ADError;

	@Override
	public int measureByteSize() {
		int size = (int) SizeOf.sizeOf(this);
		for (Classifier classifier : this.ensemble) {
			size += classifier.measureByteSize();
		}
		for (ADWIN adwin : this.ADError) {
			size += adwin.measureByteSize();
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
		this.ADError = new ADWIN[this.ensemble.length];
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ADError[i]=new ADWIN();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		boolean Change=false;
		for (int i = 0; i < this.ensemble.length; i++) {
			int k = MiscUtils.poisson(1.0, this.classifierRandom);
			if (k > 0) {
				Instance weightedInst = (Instance) inst.copy();
				weightedInst.setWeight(inst.weight() * k);
				this.ensemble[i].trainOnInstance(weightedInst);
			}
			boolean correctlyClassifies=this.ensemble[i].correctlyClassifies(inst);
			double ErrEstim=this.ADError[i].getEstimation();
			if (this.ADError[i].setInput(correctlyClassifies ? 0 : 1))
				if (this.ADError[i].getEstimation()> ErrEstim) Change=true;
		}
		if (Change) {
			double max=0.0; int imax=-1;
			for (int i = 0; i < this.ensemble.length; i++) {
				if (max<this.ADError[i].getEstimation()) {
					max=this.ADError[i].getEstimation();
					imax=i;
				}
			}
			if (imax!=-1) {
				this.ensemble[imax].resetLearning();
				//this.ensemble[imax].trainOnInstance(inst);
				this.ADError[imax]=new ADWIN();
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
