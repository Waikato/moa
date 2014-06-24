package moa.classifiers.rules.core.voting;

public class Vote {
	double [] vote;
	double error;
	
	
	public Vote(double[] vote, double error) {
		super();
		this.vote = vote;
		this.error = error;
	}
	
	public double[] getVote() {
		return vote;
	}
	
	public void setVote(double[] vote) {
		this.vote = vote;
	}
	
	public double getError() {
		return error;
	}
	
	public void setError(double error) {
		this.error = error;
	}
	
	public double sumVoteDistrib()
	{
		double sum=0;
		for (int i=0; i<vote.length; ++i)
			sum+=vote[i];
		return sum;		
	}
	
	public void normalize()
	{
		double sum=sumVoteDistrib();
		for (int i=0; i<vote.length; ++i)
			vote[i]/=sum;	
	}
	
}
