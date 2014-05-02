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

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.Classifier;
import moa.classifiers.Regressor;
import moa.classifiers.meta.RandomRules;
import moa.classifiers.rules.AbstractAMRules;
import moa.classifiers.rules.core.voting.ErrorWeightedVote;
import moa.classifiers.rules.core.voting.Vote;
import moa.options.ClassOption;


public class WeightedRandomRules extends RandomRules implements Regressor {

/*	public IntOption VerbosityOption = new IntOption(
			"verbosity",
			'v',
			"Output Verbosity Control Level. 1 (Less) to 2 (More)",
			1, 1, 2);*/
	
	@Override
	public String getPurposeString() {
		return "WeightedRandomRules";
	}

	private static final long serialVersionUID = 1L;
	
	/*public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Classifier to train.", AMRules.class, "rules.AMRulesRegressor"); 
			*/
	
	public ClassOption votingTypeOption = new ClassOption("votingType",
			'V', "Voting Type.", 
			ErrorWeightedVote.class,
			"InverseErrorWeightedVote");

	/*@Override
	public void resetLearningImpl() {
		this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
		Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.resetLearning();
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
		}
		this.isRegression = (baseLearner instanceof Regressor);
	}*/

	/*@Override
	public void trainOnInstanceImpl(Instance inst) {
		for (int i = 0; i < this.ensemble.length; i++) {
			int k = 1;
			if ( this.useBaggingOption.isSet()) {
				k = MiscUtils.poisson(1.0, this.classifierRandom);
			} 
			if (k > 0) {
				Instance weightedInst = transformInstance(inst,i);
				weightedInst.setWeight(inst.weight() * k);
				this.ensemble[i].trainOnInstance(weightedInst);
			}
		}
	}*/
	

	
	@Override
	public double[] getVotesForInstance(Instance inst) {
		double [] votes=null;
		//ErrorWeightedVote combinedVote = (ErrorWeightedVote)((ErrorWeightedVote) votingTypeOption.getPreMaterializedObject()).copy();
		ErrorWeightedVote combinedVote = (ErrorWeightedVote) getPreparedClassOption(this.votingTypeOption);
		StringBuilder sb = null;
		if (VerbosityOption.getValue()>1)
			sb=new StringBuilder();
		
		for (int i = 0; i < this.ensemble.length; i++) {
			// transformInstance method visibility changed from private to protected in RandomRules
			Vote v = ((AbstractAMRules) this.ensemble[i]).getVotes(transformInstance(inst,i));
			if (VerbosityOption.getValue()>1)
					sb.append(Arrays.toString(v.getVote()) + ", ");
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
	
}

