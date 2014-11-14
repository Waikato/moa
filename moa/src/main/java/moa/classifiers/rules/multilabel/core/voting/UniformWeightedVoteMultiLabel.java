/*
 *    UniformWeightedVoteMultiLabel.java
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
 * UniformWeightedVote class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $
 */
public class UniformWeightedVoteMultiLabel extends AbstractErrorWeightedVoteMultiLabel {


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
					//For each vote
					for (int i=0; i<n; i++)
					{
						int numClasses=votes.get(i).numClasses(o);
						if(votes.get(i).hasVotesForAttribute(o))
							weights[i][o]=1.0/outputAttributesCount[o];
						//else takes value 0

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
