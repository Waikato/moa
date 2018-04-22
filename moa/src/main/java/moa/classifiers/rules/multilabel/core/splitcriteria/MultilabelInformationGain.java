/*
 *    MultilabelInformationGain.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author R. Sousa, J. Gama
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
package moa.classifiers.rules.multilabel.core.splitcriteria;

import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Multi-label Information Gain. 
 * Online Multi-label Classification heuristic for AMRules.
 * 
 * @author RSousa
 */
public class MultilabelInformationGain  extends AbstractOptionHandler implements MultiLabelSplitCriterion{
   

    @Override
    public String getPurposeString() {
        return "Online Multi-label Classification heuristic for AMRules.";
    }
	
	
	private static final long serialVersionUID = 1L;

	@Override
	public double getMeritOfSplit(DoubleVector[] preSplitDist, DoubleVector[][] postSplitDists) {
		
            
                double error=0;
		int numOutputs=preSplitDist.length;
		for (int i=0; i<numOutputs; i++)
			error+=getMeritOfSplitForOutput(preSplitDist,postSplitDists,i);
		
                return error/numOutputs;
	}
	
	public double[] getBranchSplitEntropyOutput(DoubleVector[] postSplitDists) {
		double[] entropies = new double[postSplitDists.length];
		for(int i = 0; i < postSplitDists.length; i++){
			entropies[i]=Utils.computeEntropy(postSplitDists[i]);
        }
		return entropies;
	}
	

	@Override
	public double [] getBranchesSplitMerits(DoubleVector[][] postSplitDists){
		
        int numOutputs=postSplitDists.length;
		int numBranches=postSplitDists[0].length; 
		double [] merits=new double[numBranches];
			for(int j=0; j<numOutputs;j++)
			{
				double [] branchMeritsOutput=getBranchSplitEntropyOutput(postSplitDists[j]);
				
				for (int i=0; i<numBranches;i++){
					if (postSplitDists[j][i].getValue(0)>0)
						merits[i]-=branchMeritsOutput[i];
					else
						merits[i]=-Double.MAX_VALUE;
				}
			}
		return merits;
	}
	
	protected double getMeritOfSplitForOutput(DoubleVector[] preSplitDist, DoubleVector[][] postSplitDists, int outputAttributeIndex){
		return getMeritOfSplitForOutput(preSplitDist[outputAttributeIndex],postSplitDists[outputAttributeIndex]);
	}

	protected double getMeritOfSplitForOutput(DoubleVector preSplitDist, DoubleVector[] postSplitDists) {
		double merit=0;
    
		//count number of branches with weightSeen higher than threshold
		int count = 0; 
		for(int i = 0; i < postSplitDists.length; i++)
			if(postSplitDists[i].getValue(0) >=0.05*preSplitDist.getValue(0))
				count = count +1;
                
		//Consider split if all branches have required weight seen
		if(count == postSplitDists.length){
			
            double EntPreSplit=Utils.computeEntropy(preSplitDist);
			double sumEntPostSplit=0;
			double weightTotal=0;
			for (int i=0; i<postSplitDists.length;i++){
				weightTotal+=postSplitDists[i].getValue(0);
			}
			double [] Entropies=getBranchSplitEntropyOutput(postSplitDists);
       
			for(int i = 0; i < Entropies.length; i++)
				if(postSplitDists[i].getValue(0)>0)
					sumEntPostSplit+=(postSplitDists[i].getValue(0)/weightTotal*Entropies[i]);  //weight variance
                    merit=(EntPreSplit-sumEntPostSplit);
		}
		/*if(merit<0 || merit>1)
			System.out.println("out of range");*/
		return merit;
	}

	public double getRangeOfMerit(DoubleVector [] preSplitDist) {
		return 1;
	}



	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {		
	}


    
    
    
    
    
    
}
