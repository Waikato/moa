/*
 *    RuleClassifierNBayes.java
 *    Copyright (C) 2012 University of Porto, Portugal
 *    @author P. Kosina, E. Almeida, J. Gama
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

package moa.classifiers.rules;

import java.util.ArrayList;
import java.util.Collections;

import moa.classifiers.bayes.NaiveBayes;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * This classifier learn ordered and unordered rule set from data stream with naive Bayes learners.
 * <p> This algorithm also does the detection of anomalies.
 * 
 * <p>Learning Decision RuleClassifications from Data Streams, IJCAI 2011, J. Gama,  P. Kosina </p>
 * 
 * <p>Parameters:</p>
 * <ul>
 * <li> -q: The number of instances a leaf should observe before permitting Naive Bayes.</li>
 * <li> -p: Minimum value of p </li>
 * <li> -t: Tie Threshold </li>
 * <li> -c: Split Confidence </li>
 * <li> -g: GracePeriod, the number of instances a leaf should observe between split attempts </li>
 * <li> -o: Prediction function to use. Ex:FirstHit </li>
 * <li> -r: Learn ordered or unordered rule </li>
 * </ul>
 * 
 * @author P. Kosina, E. Almeida, J. Gama
 * @version $Revision: 2 $
 */
public class RuleClassifierNBayes extends RuleClassifier {
	
	private static final long serialVersionUID = 1L;
	
	public IntOption nbThresholdOption = new IntOption(
			"nbThreshold",
            'q',
            "The number of instances a leaf should observe before permitting Naive Bayes.",
            0, 0, Integer.MAX_VALUE);
	
	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] votes = new double[numClass];
		switch (super.predictionFunctionOption.getChosenIndex()) {
        case 0:
        	votes = firstHitNB(inst);
        	break;
        case 1:
        	votes = weightedSumNB(inst);
        	break;
        case 2:
        	votes = weightedMaxNB(inst);
        	break;
        	}
		return votes; 
		}
	
	// The following three functions are used for the prediction 
	protected double[] firstHitNB(Instance inst) {
		int countFired = 0;
		boolean fired = false;
		double[] votes = new double[this.numClass];
		for (int j = 0; j < this.ruleSet.size(); j++) {
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				if (this.ruleSet.get(j).obserClassDistrib.sumOfValues() >= this.nbThresholdOption.getValue()) {
					votes = NaiveBayes.doNaiveBayesPredictionLog(inst, this.ruleSet.get(j).obserClassDistrib, this.ruleSet.get(j).observers, this.ruleSet.get(j).observersGauss);
		    	    votes = exponential(votes);
		    	    votes = normalize(votes);
		    	    } else {
		    	    	for (int z = 0; z < this.numClass; z++) {
		    	    		votes[z] = this.ruleSet.get(j).obserClassDistrib.getValue(z) 
		    	    				/ this.ruleSet.get(j).obserClassDistrib.sumOfValues();
		    	    		}
		    	    	}
				break;
				}
			}
		if (countFired > 0) {
			fired = true;
			} else {
				fired = false;
				}
		if (fired == false) {
			if (super.getWeightSeen() >= this.nbThresholdOption.getValue()) {
				votes = NaiveBayes.doNaiveBayesPredictionLog(inst, this.observedClassDistribution, this.attributeObservers, this.attributeObserversGauss);
				votes = exponential(votes);
				votes = normalize(votes);
				} else {
					votes = super.oberversDistribProb(inst, this.observedClassDistribution);
					}
			}
		return votes;
		}
	
	protected double[] weightedMaxNB(Instance inst) {
		int countFired = 0;
		int count = 0;
		boolean fired = false;
		double highest = 0.0;
		double[] votes = new double[this.numClass];
		ArrayList<Double> ruleSetVotes = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> majorityProb = new ArrayList<ArrayList<Double>>();
		for (int j = 0; j < this.ruleSet.size(); j++) {
			ArrayList<Double> ruleClassDistribProb=new ArrayList<Double>();
			if(this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				if (this.ruleSet.get(j).obserClassDistrib.sumOfValues() >= this.nbThresholdOption.getValue()) {
					votes = NaiveBayes.doNaiveBayesPredictionLog(inst, this.ruleSet.get(j).obserClassDistrib, this.ruleSet.get(j).observers, this.ruleSet.get(j).observersGauss);
					votes = exponential(votes);
		    	    votes = normalize(votes);
		    	    } else {
		    	    	count = count + 1;
		    	    	for (int z = 0; z < this.numClass; z++){
		    	    		ruleSetVotes.add(this.ruleSet.get(j).obserClassDistrib.getValue(z) / this.ruleSet.get(j).obserClassDistrib.sumOfValues());
		    	    		ruleClassDistribProb.add(this.ruleSet.get(j).obserClassDistrib.getValue(z) / this.ruleSet.get(j).obserClassDistrib.sumOfValues());
		    	    		}
		    	    	majorityProb.add(ruleClassDistribProb);
		    	    	}
				}
			}
		if (count > 0) {
			Collections.sort(ruleSetVotes); 
		    highest = ruleSetVotes.get(ruleSetVotes.size() - 1);
		    for (int t = 0; t < majorityProb.size(); t++) {
		    	for (int m = 0; m < majorityProb.get(t).size(); m++) {
		    		if(majorityProb.get(t).get(m) == highest){
		    			for(int h = 0; h < majorityProb.get(t).size(); h++){
		    				votes[h]=majorityProb.get(t).get(h);
		    				}
 		    			break;
		    			}
		    		}
		    	}
		    }
		if (countFired > 0) {
			fired=true;
			}  else {
				fired=false;
				}
		if (fired == false) {
			if(super.getWeightSeen() >= this.nbThresholdOption.getValue()) {
				votes = NaiveBayes.doNaiveBayesPredictionLog(inst, this.observedClassDistribution, this.attributeObservers, this.attributeObserversGauss);
				votes = exponential(votes);
				votes = normalize(votes);
				} else {
					votes = super.oberversDistribProb(inst, this.observedClassDistribution);
					}
			}
		return votes;
		}
	
	protected double[] weightedSumNB(Instance inst) {
		int countFired = 0;
		int count = 0;
		boolean fired = false;
		double[] votes = new double[this.numClass];
		ArrayList<Double> weightSum = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> majorityProb = new ArrayList<ArrayList<Double>>();
		for ( int j = 0; j < this.ruleSet.size(); j++) {
			ArrayList<Double> ruleClassDistribProb=new ArrayList<Double>();
			if (this.ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				if (this.ruleSet.get(j).obserClassDistrib.sumOfValues() >= this.nbThresholdOption.getValue()) {
					votes = NaiveBayes.doNaiveBayesPredictionLog(inst, this.ruleSet.get(j).obserClassDistrib, ruleSet.get(j).observers, this.ruleSet.get(j).observersGauss);
	    	        votes = exponential(votes);
	    	        votes = normalize(votes);
	    	        } else {
	    	        	count=count+1;
	    	        	for (int z = 0; z < this.numClass; z++) {
	    	        		ruleClassDistribProb.add(this.ruleSet.get(j).obserClassDistrib.getValue(z) / this.ruleSet.get(j).obserClassDistrib.sumOfValues());
	    	        		}
	    	        	majorityProb.add(ruleClassDistribProb);
	    	        	}
				}
			}
		 if(count > 0) {
			 for (int m = 0; m < majorityProb.get(0).size(); m++) {
				 double sum = 0.0;
				 for (int t = 0; t < majorityProb.size(); t++){
					 sum = sum + majorityProb.get(t).get(m);
					 }
				 weightSum.add(sum);
				 }
			 for (int h = 0; h < weightSum.size(); h++) {
				 votes[h] = weightSum.get(h) / majorityProb.size();
				 }
			 }
		 if(countFired>0){
			 fired = true;
			 } else {
				 fired=false;
				 }
		 if (fired == false) {
			 if (super.getWeightSeen() >= this.nbThresholdOption.getValue()) {
				 votes = NaiveBayes.doNaiveBayesPredictionLog(inst, this.observedClassDistribution, this.attributeObservers, this.attributeObserversGauss);
			  	 votes = exponential(votes);
	  		 	 votes = normalize(votes);
	  		 	 } else {
	  		 		 votes = super.oberversDistribProb(inst, this.observedClassDistribution);
	  		 		 }
			 }
		 return votes;
		 }
	
	protected double[] normalize(double[] votes) {
		double sum=0;
		for (int i = 0; i < votes.length; i++) {
			sum = sum + votes[i];
			}
		for (int j = 0; j < votes.length; j++) {
			votes[j] = votes[j] / sum;
			}
		return votes;
		}
	
	protected double[] exponential(double[] votes) {
		for (int i = 0; i < votes.length; i++) {
			votes[i] = Math.exp(votes[i]);
			}
		return votes;
		}


  
}
