/*
 *    MinErrorWeightedVote.java
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
 * MinErrorWeightedVote class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (jmduarte@inescporto.pt)
 * @version $Revision: 1 $
 */
public class MinErrorWeightedVote extends AbstractErrorWeightedVote {

	/**
	 * 
	 */
	private static final long serialVersionUID = -205142097727766386L;

	@Override
	public double[] computeWeightedVote() {
		int n=votes.size();
		int min=0;

		double [] weightedVote = null;
		double minError=Double.MAX_VALUE;
		
		if (n>0){
			int d=votes.get(0).length;
			weightedVote=new double[d];	
			for (int i=0; i<n; i++)
			{
				if(errors.get(i)<minError) {
					minError=errors.get(i);
					min=i;
				}
			}
			weights=new double[n];
			weights[min]=1.0;
			weightedVote=votes.get(min);
		}

		return weightedVote;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

}
