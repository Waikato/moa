/*
 *    ErrorWeightedVoteMultiLabel.java
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

package moa.learners.predictors.core.voting;

import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.MOAObject;

/**
 * ErrorWeightedVoteMultiLabel interface for weighted votes based on estimates
 * of errors.
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $
 */
public interface ErrorWeightedVoteMultiLabel {

	/**
	 * Adds a vote and the corresponding error for the computation of the weighted
	 * vote and respective weighted error.
	 *
	 * @param vote  - a vote returned by a classifier
	 * @param error - the error associated to the vote
	 */
	void addVote(Prediction prediction, double[] error);

	/**
	 * Computes the weighted vote. Also updates the weights of the votes.
	 *
	 * @return the weighted vote
	 */
	Prediction computeWeightedVote();

	/**
	 * Returns the weighted error.
	 *
	 * @pre computeWeightedVote()
	 * @return the weighted error
	 */
	double getWeightedError();

	/**
	 * Returns the weighted error.
	 *
	 * @pre computeWeightedVote()
	 * @return the weighted error for each output attribute
	 */
	double[] getOutputAttributesErrors();

	/**
	 * Return the weights error.
	 *
	 * @pre computeWeightedVote()
	 * @return the weights for each output attribute
	 */
	double[][] getWeights();

	/**
	 * The number of votes added so far.
	 *
	 * @return the number of votes
	 */
	int getNumberVotes();

	/**
	 * The number of votes for a given output attribute.
	 *
	 * @param outputAttribute the index of the output attribute
	 *
	 * @return the number of votes
	 */
	int getNumberVotes(int outputAttribute);

	/**
	 * Check if vote has a value for each output
	 *
	 */
	boolean coversAllOutputs();

	/**
	 * Creates a copy of the object
	 *
	 * @return copy of the object
	 */
	MOAObject copy();

	Prediction getPrediction();
}
