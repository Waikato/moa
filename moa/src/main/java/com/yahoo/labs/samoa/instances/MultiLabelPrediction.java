package com.yahoo.labs.samoa.instances;

import moa.core.DoubleVector;

public class MultiLabelPrediction implements Prediction {
	protected DoubleVector [] prediction;

	public MultiLabelPrediction() {
		this(0);
	}
	
	public MultiLabelPrediction(int numOutputAttributes) {
		prediction=new DoubleVector[numOutputAttributes];
		for (int i=0; i<numOutputAttributes;i++)
			prediction[i]= new DoubleVector();
	}
	
	@Override
	public int numOutputAttributes() {
		return prediction.length;
	}

	@Override
	public int numClasses(int outputAttributeIndex) {
		return prediction[outputAttributeIndex].numValues();
	}

	@Override
	public double[] getVotes(int outputAttributeIndex) {
		return prediction[outputAttributeIndex].getArrayCopy();
	}
	
	@Override
	public double[] getVotes() {
		return getVotes(0);
	}

	@Override
	public double getVote(int outputAttributeIndex, int classIndex) {
		return prediction[outputAttributeIndex].getValue(classIndex);
	}

	@Override
	public void setVotes(int outputAttributeIndex, double[] votes) {
		for(int i=0; i<votes.length; i++)
			prediction[outputAttributeIndex].setValue(i,votes[i]);
	}
	
	@Override
	public void setVotes(double[] votes) {
		setVotes(0, votes);
	}

	@Override
	public void setVote(int outputAttributeIndex, int classIndex, double vote) {
		prediction[outputAttributeIndex].setValue(classIndex, vote);

	}
	
	@Override
	public String toString(){
		StringBuffer sb= new StringBuffer();
		for (int i=0; i<prediction.length; i++){
			sb.append("Out " + i + ": ");
			for (int c=0; c<prediction[i].numValues(); c++)
			{
				sb.append(((int)(prediction[i].getValue(c)*1000)/1000.0)+ " ");
			}
		}
		return sb.toString();
	}

	@Override
	public boolean hasVotesForAttribute(int outputAttributeIndex) {
		if(prediction.length<(outputAttributeIndex+1))
				return false;
		return (prediction[outputAttributeIndex]==null) ? false : true;
	}

}
