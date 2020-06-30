
package moa.classifiers.rules.multilabel.core.voting;

import moa.MOAObject;

import com.yahoo.labs.samoa.instances.Prediction;

/**
 * ErrorWeightedVoteMultiLabel interface for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 * @version $Revision: 1 $
 */
public interface ErrorWeightedVoteMultiLabel {
	
	/**
	 * Adds a vote and the corresponding error for the computation of the weighted vote and respective weighted error.
	 * 
	 * @param  prediction - a vote returned by a classifier
	 * @param  error - the error associated to the vote
	 */
	public void addVote(Prediction prediction, double [] error);
	
	/**
	 * Computes the weighted vote.
	 * Also updates the weights of the votes.
	 * 
	 * @return      the weighted vote
	 */
	public Prediction computeWeightedVote();
	
	/**
	 * Returns the weighted error.
	 * 
	 * @pre computeWeightedVote()
	 * @return      the weighted error
	 */
	public double getWeightedError();
	
	/**
	 * Returns the weighted error.
	 * 
	 * @pre computeWeightedVote()
	 * @return      the weighted error for each output attribute
	 */
	public double[] getOutputAttributesErrors();
	
	/**
	 * Return the weights error.
	 * 
	 * @pre computeWeightedVote()
	 * @return      the weights for each output attribute
	 */
	public double[][]  getWeights();
	
	
	/**
	 * The number of votes added so far.
	 * 
	 * @return      the number of votes
	 */
	public int getNumberVotes();
	
	
	/**
	 * The number of votes for a given output attribute.
	 * 
	 * @param outputAttribute the index of the output attribute
	 * 
	 * @return      the number of votes
	 */
	public int getNumberVotes(int outputAttribute);
	
	/**
	 * Check if vote has a value for each output
	 * 
	 */
	public boolean coversAllOutputs();
	
	/**
	 * Creates a copy of the object
	 * 
	 * @return      copy of the object
	 */
	public MOAObject copy();

	public Prediction getPrediction();
}
