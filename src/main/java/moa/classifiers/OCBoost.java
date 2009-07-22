/*
 *    OCBoost.java
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
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.options.IntOption;
import moa.options.FloatOption;
import weka.core.Instance;
import weka.core.Utils;

public class OCBoost extends AbstractClassifier {

	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Classifier to train.", Classifier.class, "HoeffdingTree");

	public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
			"The number of models to boost.", 10, 1, Integer.MAX_VALUE);

	public FloatOption smoothingOption = new FloatOption("smoothingParameter", 'e',
			"Smoothing parameter.", 0.5, 0.0, 100.0);

	protected Classifier[] ensemble;
	
	protected double[] alpha;
	
	protected double[] alphainc;
	
	protected double[] pipos;
	
	protected double[] pineg;
	
	protected double[][] wpos;
	
	protected double[][] wneg;
	

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
		this.alpha = new double[this.ensemble.length];
		this.alphainc = new double[this.ensemble.length];
		this.pipos = new double[this.ensemble.length];
		this.pineg = new double[this.ensemble.length];
		this.wpos = new double[this.ensemble.length][this.ensemble.length];
		this.wneg = new double[this.ensemble.length][this.ensemble.length];
		
		Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.resetLearning();
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
			alpha[i]= 0.0;
			alphainc[i]= 0.0;
			for (int j = 0; j < this.ensemble.length; j++) {
				wpos[i][j] = this.smoothingOption.getValue();
				wneg[i][j] = this.smoothingOption.getValue();
			}
		}
	}


	@Override
	public void trainOnInstanceImpl(Instance inst) {
		double d = 1.0;
		int[] m = new int[this.ensemble.length];
		for (int j = 0; j < this.ensemble.length; j++) {
			int j0 = 0; //max(0,j-K)
			pipos[j] = 1.0;
			pineg[j] = 1.0;
			m[j] = -1;
			if (this.ensemble[j].correctlyClassifies(inst) == true) {
				m[j] = 1;
			}
			for (int k = j0; k <= j-1; k++) {
				pipos[j] *= wpos[j][k]/wpos[j][j] * Math.exp(-alphainc[k]) +
							( 1.0 - wpos[j][k]/wpos[j][j]) * Math.exp(alphainc[k]);
				pineg[j] *= wneg[j][k]/wneg[j][j] * Math.exp(-alphainc[k]) +
							( 1.0 - wneg[j][k]/wneg[j][j]) * Math.exp(alphainc[k]);				
			}
			for (int k = 0; k <= j; k++) {
				wpos[j][k] = wpos[j][k] * pipos[j] + d * ( m[k] == 1 ? 1 : 0) * ( m[j] == 1 ? 1 : 0);
				wneg[j][k] = wneg[j][k] * pineg[j] + d * ( m[k] == -1 ? 1 : 0) * ( m[j] == -1 ? 1 : 0);		
			}
			alphainc[j] = -alpha[j]; 
			alpha[j] = 0.5* Math.log(wpos[j][j]/wneg[j][j]);
			alphainc[j] += alpha[j]; 
			
			d = d * Math.exp(-alpha[j] * m[j]);
			
			if (d > 0.0) {
				Instance weightedInst = (Instance) inst.copy();
				weightedInst.setWeight(inst.weight() * d);
				this.ensemble[j].trainOnInstance(weightedInst);
			}
		}
	}

	protected double getEnsembleMemberWeight(int i) {
		return alpha[i];
	}


	
	public double[] getVotesForInstance(Instance inst) {
		double[] output = new double[2];
		int vote;
		double combinedVote = 0.0;
		for (int i = 0; i < this.ensemble.length; i++) {
			vote = Utils.maxIndex(this.ensemble[i].getVotesForInstance(inst));
			if (vote==0) vote = -1;
			combinedVote += (double) vote * getEnsembleMemberWeight(i);
		}
		output[0]=0;output[1]=0;
		output[combinedVote>0 ? 1:0] = 1;
		return output;
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
