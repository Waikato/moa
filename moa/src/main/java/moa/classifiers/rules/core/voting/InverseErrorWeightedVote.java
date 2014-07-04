/*
 *    InverseErrorWeightedVoteMultiLabel.java
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
package moa.classifiers.rules.core.voting;

/**
 * InverseErrorWeightedVoteMultiLabel class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (jmduarte@inescporto.pt)
 * @version $Revision: 1 $
 */
public class InverseErrorWeightedVote extends AbstractErrorWeightedVote {

	/**
	 * 
	 */
	private static final double EPS = 0.000000001; //just to prevent divide by 0 in 1/X -> 1/(x+EPS)
	private static final long serialVersionUID = 6359349250620616482L;

	@Override
	public double[] computeWeightedVote() {
		int n=votes.size();
		weights=new double[n];
		double [] weightedVote=null;
		if (n>0){
			int d=votes.get(0).length;
			weightedVote=new double[d];
			double sumError=0;
			//weights are 1/(error+eps)
			for (int i=0; i<n; ++i){
				if(errors.get(i)<Double.MAX_VALUE){
					weights[i]=1.0/(errors.get(i)+EPS);
					sumError+=weights[i];
				}
				else
					weights[i]=0;
					
			}
			
			if(sumError>0)
			for (int i=0; i<n; ++i)
			{
				//normalize so that weights sum 1
				weights[i]/=sumError;
				//compute weighted vote
				for(int j=0; j<d; j++)
					weightedVote[j]+=votes.get(i)[j]*weights[i];
			}
			//Only occurs if all errors=Double.MAX_VALUE
			else
			{
				//compute arithmetic vote
				for (int i=0; i<n; ++i)
				{
					for(int j=0; j<d; j++)
						weightedVote[j]+=votes.get(i)[j]/n;
				}
			}
		}
		return weightedVote;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
