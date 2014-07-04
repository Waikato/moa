/*
 *    UniformWeightedVote.java
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
 * UniformWeightedVote class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (jmduarte@inescporto.pt)
 * @version $Revision: 1 $
 */
public class UniformWeightedVote extends AbstractErrorWeightedVote {


	private static final long serialVersionUID = 6359349250620616482L;

	@Override
	public double[] computeWeightedVote() {
		int n=votes.size();
		weights=new double[n];
		double [] weightedVote=null;
		if (n>0){
			int d=votes.get(0).length;
			weightedVote=new double[d];					
			for (int i=0; i<n; i++)
			{
				weights[i]=1.0/n;
				for(int j=0; j<d; j++)
					weightedVote[j]+=(votes.get(i)[j]*weights[i]);
			}
			
		}
		return weightedVote;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
