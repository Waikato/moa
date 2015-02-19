
package moa.classifiers.rules.multilabel.core.voting;

import java.util.LinkedList;
import java.util.List;

import moa.AbstractMOAObject;

import com.yahoo.labs.samoa.instances.Prediction;

/**
 * AbstractErrorWeightedVote class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (jmduarte@inescporto.pt)
 * @version $Revision: 1 $
 */
public abstract class AbstractErrorWeightedVoteMultiLabel extends AbstractMOAObject implements ErrorWeightedVoteMultiLabel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1;
	protected List<Prediction> votes;
	protected List<double[]> errors;
	protected double[][] weights;
	protected int [] outputAttributesCount;
	protected Prediction weightedVote=null;


	public AbstractErrorWeightedVoteMultiLabel() {
		super();
		votes = new LinkedList<Prediction>();
		errors = new LinkedList<double[]>();
	}


	@Override
	public void addVote(Prediction vote, double [] error) {
		int numOutputs=vote.numOutputAttributes();
		if(outputAttributesCount==null)
			outputAttributesCount=new int[numOutputs];

		for(int i=0; i<numOutputs; i++)
			if(vote.hasVotesForAttribute(i))
				outputAttributesCount[i]++;
		votes.add(vote);
		errors.add(error);
	}


	@Override
	abstract public Prediction computeWeightedVote();

	@Override
	public double getWeightedError()
	{
		double [] errors=getOutputAttributesErrors();
		if(errors!=null){
			int numOutputs=errors.length;
			double error=0;
			
			for (int i=0; i<numOutputs;i++)
				error+=errors[i];
			
			return error/numOutputs;
		}
		else
			return Double.MAX_VALUE; 
		
	}


	@Override
	public double[][] getWeights() {
		return weights;
	}

	@Override
	public int getNumberVotes() {
		return votes.size();
	}


	@Override
	public int getNumberVotes(int outputAttribute) {
		return outputAttributesCount[outputAttribute];
	}


	@Override
	public double[] getOutputAttributesErrors() {
		double [] weightedError;
		if (weights!=null && weights.length==errors.size())
		{
			int numOutputs=outputAttributesCount.length;
			int numVotes=weights.length;
			weightedError=new double[numOutputs];
			
			//For all votes
			for (int i=0; i<numVotes; ++i){
				//For each output attribute
				for (int j=0; j<numOutputs; j++){
					if(errors.get(i)!=null && errors.get(i)[j]!=Double.MAX_VALUE)
						weightedError[j]+=errors.get(i)[j]*weights[i][j];
				}
			}
			return weightedError;
		}
		else
			//weightedError=-1; 
			return null;
	}
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}
	
	public Prediction getPrediction(){
		if (this.weightedVote==null)
			weightedVote=computeWeightedVote();
		return weightedVote;
	}
	
	public boolean coversAllOutputs(){
		int i=0;
		boolean flag=false;
		
		if(outputAttributesCount!=null){
			while( i<outputAttributesCount.length && outputAttributesCount[i]>0)
				i++;
			flag=i==outputAttributesCount.length;
		}
		return flag;
	}
	
}
