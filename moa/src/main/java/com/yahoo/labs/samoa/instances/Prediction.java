/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */
package com.yahoo.labs.samoa.instances;

public interface Prediction {
	
    /**
     * Number of output attributes.
     *
     * @return the number of output attributes
     */
	public int numOutputAttributes();
	
    /**
     * Different output attributes may have different number of classes.
     * Regressors have one class per output attribute.
     *
     * @return the number of classes for attribute attributeIndex
     */
	public int numClasses(int outputAttributeIndex);
	
    /*
     * The predictions for each output attribute.
     *
     * @return the classes for each output attribute
     *//*
	public double [] getPrediction();
	*/
	
    /**
     * The votes for a given output attribute
     *
     * @return the votes for a given output attribute outputAttributeIndex.
     */
	public double [] getVotes(int outputAttributeIndex);
	
    /**
     * The vote assigned to a class of an output attribute
     *
     * @return the vote for an output attribute outputAttributeIndex and a class classIndex.
     */
	public double getVote(int outputAttributeIndex, int classIndex);
	
    /**
     * Sets the votes for a given output attribute
     *
     */
	public void setVotes(int outputAttributeIndex, double [] votes);
	
    /**
     * Sets the votes for the first output attribute
     *
     */
	public void setVotes(double[] votes);
	
    /**
     * Sets the vote for class of a given output attribute
     *
     */
	public void setVote(int outputAttributeIndex, int classIndex, double vote);

    /**
     * The votes for the first output attribute
     *
     * @return the votes for the first output attribute outputAttributeIndex.
     */
	double[] getVotes();
	
    /**
     * Checks if there are votes for a given output attribute
     *
     * @return the votes for the first output attribute outputAttributeIndex.
     */
	boolean hasVotesForAttribute(int outputAttributeIndex);


     /**
     * The size of the prediction, that is the number of output attributes
     *
     * @return the votes for the first output attribute outputAttributeIndex.
     */
     public int size();

    /**
     * The text of the prediction, that is the description of the values of the prediction
     *
     * @return the text
     */
     public String toString();
		

}
