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
		

}
