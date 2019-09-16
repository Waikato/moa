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

package moa.learners.predictors.core.voting;

import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

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
		int n = votes.size();
		if (n > 0) {
			int numOutputs = outputAttributesCount.length;
			weights = new double[n][numOutputs];
			weightedVote = new MultiLabelClassificationPrediction(numOutputs);

			// For each vote
			for (int i = 0; i < n; i++) {

				// For each output attribute
				for (int o = 0; o < numOutputs; o++) {
					// int numClasses=2;
					if (votes.get(i).hasVotesForAttribute(o))
						weights[i][o] = 1.0 / outputAttributesCount[o];
					else
						weights[i][o] = 0.0;
					// else takes value 0

					// In multi-label classification there are only two classes for
					// For each class
					// for(int j=0; j<numClasses; j++){
					weightedVote.setVote(o, 0, weightedVote.getVote(o, 0) + votes.get(i).getVote(o, 0) * weights[i][o]);
					// }
				}
			}

		}
		return weightedVote;
	}

}
