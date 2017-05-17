/*
 *    SDRSplitCriterionAMRulesNode.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
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
package moa.classifiers.rules.core.splitcriteria;


public class SDRSplitCriterionAMRulesNode extends SDRSplitCriterionAMRules implements AMRulesSplitCriterion {

    private static final long serialVersionUID = 1L;

    @Override
    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
        double SDR=0.0;
    	int count = 0; 
    	
    	for(int i = 0; i < postSplitDists.length; i++)
    	{
    		double Ni = postSplitDists[i][0];
    		if(Ni >=0.05*preSplitDist[0]){
    			count = count +1;
    		}
    	}
    	if(count == postSplitDists.length){
    		SDR = computeSD(preSplitDist);
    		double min=Double.MAX_VALUE;
    		for(int i = 0; i < postSplitDists.length; i++)
        	{
        		double val=computeSD(postSplitDists[i]);
        		if (val<min)
        			min=val;
        	}
    		SDR-=min;
    	}
    	return SDR;
    }

    public double[] computeBranchSplitMerits(double[][] postSplitDists) {
    	double[] SDR = new double[postSplitDists.length];
    	for(int i = 0; i < postSplitDists.length; i++)
    		SDR[i] = computeSD(postSplitDists[i]);
    	return SDR;

    }


    @Override
    public double getRangeOfMerit(double[] preSplitDist) {
        return 1;
    }


}
