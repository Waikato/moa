/*
 *    InverseErrorWeightedVoteMultiLabel.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
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

import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * InverseErrorWeightedVoteMuliLabel class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $
 */
public class InverseErrorWeightedVoteMultiLabel extends AbstractErrorWeightedVoteMultiLabel {

	/**
	 * 
	 */
	private static final double EPS = 0.000000001; //just to prevent divide by 0 in 1/X -> 1/(x+EPS)
	private static final long serialVersionUID = 6359349250620616482L;

	@Override
	public Prediction computeWeightedVote() {
		int n=votes.size();
		if (n>0){
			int numOutputs=outputAttributesCount.length;

			weights=new double[n][numOutputs];


			weightedVote=new MultiLabelPrediction(numOutputs);

			double [] sumError= new double[numOutputs];
			//For each output attribute
			for (int o=0;o<numOutputs;o++)
			{
				for (int i=0; i<n; i++)
				{
					if(votes.get(i).hasVotesForAttribute(o) && errors.get(i)!=null){
						weights[i][o]=1.0/(errors.get(i)[o]+EPS);
						sumError[o]+=weights[i][o];
					}
				}

				//For each vote
				for (int i=0; i<n; i++)
				{
					int numClasses=votes.get(i).numClasses(o);
					if(votes.get(i).hasVotesForAttribute(o)){
						if(sumError[o]>0)
							weights[i][o]/=sumError[o];
						else
							weights[i][o]=1.0/outputAttributesCount[o];
					}
					//For each class
					for(int j=0; j<numClasses; j++){
						weightedVote.setVote(o, j, weightedVote.getVote(o, j)+votes.get(i).getVote(o, j)*weights[i][o]);
					}


				}	

			}

		}
		return weightedVote;
	}


}
