/*
 *    Vote.java
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
 * Vote class for weighted votes based on estimates of errors. 
 *
 * @author João Duarte (jmduarte@inescporto.pt)
 * @version $Revision: 1 $
 */
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
