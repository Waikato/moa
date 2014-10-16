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

package moa.classifiers.rules.multilabel.core.voting;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;
import moa.core.DoubleVector;

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
	protected List<double[]> errors;
	protected double[][] weights;
	protected int [] outputAttributesCount;
	protected Prediction weightedVote=null;


	public AbstractErrorWeightedVoteMultiLabel() {
		super();
		votes = new ArrayList<Prediction>();
		errors = new ArrayList<double[]>();
	}


	@Override
	public void addVote(Prediction vote, double [] error) {
		int numOutputs=vote.numOutputAttributes();
		if(outputAttributesCount==null)
			outputAttributesCount=new int[numOutputs];

		for(int i=0; i<numOutputs; i++)
			if(vote.hasVotesForAttribute(i))
				outputAttributesCount[i]++;

		votes.add(vote);
		errors.add(error);
	}


	@Override
	abstract public Prediction computeWeightedVote();

	@Override
	public double getWeightedError()
	{
		double [] errors=getOutputAttributesErrors();
		if(errors!=null){
			int numOutputs=errors.length;
			double error=0;
			
			for (int i=0; i<numOutputs;i++)
				error+=errors[i];
			
			return error/numOutputs;
		}
		else
			return Double.MAX_VALUE; 
		
	}


	@Override
	public double[][] getWeights() {
		return weights;
	}

	@Override
	public int getNumberVotes() {
		return votes.size();
	}


	@Override
	public int getNumberVotes(int outputAttribute) {
		return outputAttributesCount[outputAttribute];
	}


	@Override
	public double[] getOutputAttributesErrors() {
		double [] weightedError;
		if (weights!=null && weights.length==errors.size())
		{
			int numOutputs=outputAttributesCount.length;
			int numVotes=weights.length;
			weightedError=new double[numOutputs];
			
			//For all votes
			for (int i=0; i<numVotes; ++i){
				//For each output attribute
				for (int j=0; j<numOutputs; j++){
					if(errors.get(i)!=null && errors.get(i)[j]!=Double.MAX_VALUE)
						weightedError[j]+=errors.get(i)[j]*weights[i][j];
				}
			}
			return weightedError;
		}
		else
			//weightedError=-1; 
			return null;
	}
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}
	
	public Prediction getPrediction(){
		if (this.weightedVote==null)
			weightedVote=computeWeightedVote();
		return weightedVote;
	}
	
}
