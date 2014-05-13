/*
 *    RandomRules.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers.rules.meta;

import java.util.Arrays;

import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.Regressor;
import moa.classifiers.rules.AMRulesRegressor;
import moa.classifiers.rules.AbstractAMRules;
import moa.classifiers.rules.core.voting.ErrorWeightedVote;
import moa.classifiers.rules.core.voting.Vote;
import moa.core.DoubleVector;
import moa.core.FastVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.streams.InstanceStream;


public class RandomAMRules extends AbstractClassifier implements Regressor {

	public IntOption VerbosityOption = new IntOption(
			"verbosity",
			'v',
			"Output Verbosity Control Level. 1 (Less) to 2 (More)",
			1, 1, 2);
	
	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l', "Classifier to train.", AbstractAMRules.class, "AMRulesRegressor"); 

	public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
			"The number of models in the bag.", 10, 1, Integer.MAX_VALUE);

	public FloatOption numAttributesPercentageOption = new FloatOption("numAttributesPercentage", 'n',
			"The number of attributes to use per model.", 63.2, 0, 100); 

	public FlagOption useBaggingOption = new FlagOption("useBagging", 'p',
			"Use Bagging.");
	
	public ClassOption votingTypeOption = new ClassOption("votingType",
			'V', "Voting Type.", 
			ErrorWeightedVote.class,
			"InverseErrorWeightedVote");


	protected Classifier[] ensemble;

	protected boolean isRegression;

	@Override
	public void resetLearningImpl() {
		this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
		//Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		AbstractAMRules baseLearner = (AbstractAMRules) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.setAttributesPercentage(numAttributesPercentageOption.getValue());
		baseLearner.resetLearning();
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
			this.ensemble[i].setRandomSeed(this.classifierRandom.nextInt());
		}
		this.isRegression = (baseLearner instanceof Regressor);
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		for (int i = 0; i < this.ensemble.length; i++) {
			int k = 1;
			if ( this.useBaggingOption.isSet()) {
				k = MiscUtils.poisson(1.0, this.classifierRandom);
			} 
			if (k > 0) {
				//Instance weightedInst = transformInstance(inst,i);
				inst.setWeight(inst.weight() * k);
				this.ensemble[i].trainOnInstance(inst);
			}
		}
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		double [] votes=null;
		//ErrorWeightedVote combinedVote = (ErrorWeightedVote)((ErrorWeightedVote) votingTypeOption.getPreMaterializedObject()).copy();
		ErrorWeightedVote combinedVote = (ErrorWeightedVote)((ErrorWeightedVote) getPreparedClassOption(this.votingTypeOption)).copy();
		StringBuilder sb = null;
		if (VerbosityOption.getValue()>1)
			sb=new StringBuilder();
		
		for (int i = 0; i < this.ensemble.length; i++) {
			// transformInstance method visibility changed from private to protected in RandomRules
			Vote v = ((AbstractAMRules) this.ensemble[i]).getVotes(inst);
			if (VerbosityOption.getValue()>1)
					sb.append(Arrays.toString(v.getVote()) + ", " + " E: " + v.getError() + " ");
			if (this.isRegression == false && v.sumVoteDistrib() != 0.0){
				v.normalize();
			}
			combinedVote.addVote(v.getVote(), v.getError());
		}
		votes=combinedVote.computeWeightedVote();
		if (VerbosityOption.getValue()>1){
			sb.append(Arrays.toString(votes)  + ", ").append(inst.classValue()); 
			System.out.println(sb.toString());
		}
		return votes; 
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{new Measurement("ensemble size",
				this.ensemble != null ? this.ensemble.length : 0)};
	}

	@Override
	public Classifier[] getSubClassifiers() {
		return this.ensemble; //.clone();
	}

	protected int[][] listAttributes;
	protected int numAttributes;
	protected InstancesHeader[] dataset;


	
	public String getPurposeString() {
		return "WeightedRandomRules";
	}
}
