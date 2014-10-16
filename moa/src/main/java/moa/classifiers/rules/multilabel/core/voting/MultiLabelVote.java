package moa.classifiers.rules.multilabel.core.voting;

import com.yahoo.labs.samoa.instances.Prediction;

public class MultiLabelVote {
	protected Prediction vote;
	protected double error;
	
	
	public MultiLabelVote(Prediction vote, double error) {
		super();
		this.vote = vote;
		this.error = error;
	}
	
	public Prediction getVote() {
		return vote;
	}
	
	public void setVote(Prediction vote) {
		this.vote = vote;
	}
	
	public double getError() {
		return error;
	}
	
	public void setError(double error) {
		this.error = error;
	}
	
	public double [] sumVoteDistrib()
	{
		int numOutputs=vote.numOutputAttributes();
		double [] sum= new double[numOutputs];
		for (int j=0; j<numOutputs;j++){
			for (int i=0; i<vote.numClasses(j); ++i)
				sum[j]+=vote.getVote(j, i);
		}
		return sum;		
	}
	
	public void normalize()
	{
		double [] sum=sumVoteDistrib();
		for (int j=0; j<vote.numOutputAttributes();j++)
			for (int i=0; i<vote.numClasses(j); ++i)
				if(sum[j]!=0)
					vote.setVote(j, i, vote.getVote(j, i)/sum[j]);
	}
}
