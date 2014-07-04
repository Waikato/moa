/*
 *    UniformWeightedVote.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.classifiers.rules.core.voting.multilabel;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;

/**
 * AbstractErrorWeightedVote class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (jmduarte@inescporto.pt)
 * @version $Revision: 1 $
 */
public abstract class AbstractErrorWeightedVoteMultiLabel extends AbstractMOAObject implements ErrorWeightedVoteMultiLabel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1;
	protected List<Prediction> votes;
	protected List<Double> errors;
	protected double[] weights;
	
	

	public AbstractErrorWeightedVoteMultiLabel() {
		super();
		votes = new ArrayList<Prediction>();
		errors = new ArrayList<Double>();
	}


	@Override
	public void addVote(Prediction vote, double error) {
		votes.add(vote);
		errors.add(error);
	}


	@Override
	abstract public Prediction computeWeightedVote();

	@Override
	public double getWeightedError()
	{
		double weightedError=0;
		if (weights!=null && weights.length==errors.size())
		{
			for (int i=0; i<weights.length; ++i)
				weightedError+=errors.get(i)*weights[i];
		}
		else
			weightedError=-1;
		return weightedError;
	}
	
	
	@Override
	public double [] getWeights() {
		return weights;
	}

	@Override
	public int getNumberVotes() {
		return votes.size();
	}
	
}
