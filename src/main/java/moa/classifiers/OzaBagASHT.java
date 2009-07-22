/*
 *    OzaBagASHT.java
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

import sizeof.agent.SizeOfAgent;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.IntOption;
import moa.options.FlagOption;
import weka.core.Instance;
import weka.core.Utils;

public class OzaBagASHT extends OzaBag {

	private static final long serialVersionUID = 1L;
	
	public IntOption firstClassifierSizeOption = new IntOption("firstClassifierSize", 'f',
			"The size of first classifier in the bag.", 1, 1, Integer.MAX_VALUE);
			
	public FlagOption useWeightOption = new FlagOption("useWeight",
			'u', "Enable weight classifiers.");
	
	public FlagOption resetTreesOption = new FlagOption("resetTrees",
			'r', "Reset trees when size is higher than the max.");
	
	protected double[] error;

	protected double alpha = 0.01;

	@Override
	public void resetLearningImpl() {
		this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
		this.error = new double[this.ensembleSizeOption.getValue()];
		Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.resetLearning();
		int pow = this.firstClassifierSizeOption.getValue(); //EXTENSION TO ASHT
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
			this.error[i] = 0.0;
			((ASHoeffdingTree) this.ensemble[i]).setMaxSize(pow); //EXTENSION TO ASHT 
			if ((this.resetTreesOption != null)
						&& this.resetTreesOption.isSet()) {
							((ASHoeffdingTree) this.ensemble[i]).setResetTree();  
						}
			pow *= 2; //EXTENSION TO ASHT
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		int trueClass = (int) inst.classValue();
		for (int i = 0; i < this.ensemble.length; i++) {
			int k = MiscUtils.poisson(1.0, this.classifierRandom);
			if (k > 0) {
				Instance weightedInst = (Instance) inst.copy();
				weightedInst.setWeight(inst.weight() * k);
				if (Utils.maxIndex(this.ensemble[i].getVotesForInstance(inst)) == trueClass) {
					this.error[i] += alpha * (0.0 - this.error[i]); //EWMA
				} else {
					this.error[i] += alpha * (1.0 - this.error[i]); //EWMA
				}
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
				if ((this.useWeightOption != null)
						&& this.useWeightOption.isSet()) {
					vote.scaleValues( 1.0 / (this.error[i]*this.error[i]));
				}
				combinedVote.addValues(vote);
			}
		}
		return combinedVote.getArrayRef();
	}


	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub

	}



}
