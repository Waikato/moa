/*
 *    FirstHitVoteMultiLabel.java
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
 * FirstHitVoteMultiLabel class for weighted votes based on estimates of errors.
 * Returns the first vote for each output attribute as the weighted vote.
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $
 */
public class FirstHitVoteMultiLabel extends AbstractErrorWeightedVoteMultiLabel {


	private static final long serialVersionUID = 1;
	@Override
	public Prediction computeWeightedVote() {
		int n=votes.size();
		if (n>0){
			int numOutputs=outputAttributesCount.length;
			weights=new double[n][numOutputs];
			weightedVote=new MultiLabelPrediction(numOutputs);

			//For each output attribute
			for (int o=0;o<numOutputs;o++)
			{
				//int numClasses=votes.get(0).numClasses(o);
				//For each vote
				for (int i=0; i<n; i++)
				{
					if(votes.get(i).hasVotesForAttribute(o)){
						//set as weighted vote and set weight to 1	
						weights[i][o]=1;
						/*for(int j=0; j<numClasses; j++){
							weightedVote.setVote(o, j, votes.get(i).getVote(o, j));
						}*/
						weightedVote.setVotes(o, votes.get(i).getVotes(o));
						break;
					}
				}
			}
		}
		return weightedVote;
	}
}
