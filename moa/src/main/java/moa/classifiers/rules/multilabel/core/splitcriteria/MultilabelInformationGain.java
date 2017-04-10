/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.classifiers.rules.multilabel.core.splitcriteria;

import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 *
 * @author RSousa
 */
public class MultilabelInformationGain  extends AbstractOptionHandler implements MultiLabelSplitCriterion{
   

	private static final long serialVersionUID = 1L;

	@Override
	public double getMeritOfSplit(DoubleVector[] preSplitDist, DoubleVector[][] postSplitDists) {
		
            
                double error=0;
		int numOutputs=preSplitDist.length;
		for (int i=0; i<numOutputs; i++)
			error+=getMeritOfSplitForOutput(preSplitDist,postSplitDists,i);
		
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                //System.out.print("MultilabelInformationGain.getMeritOfSplit: merit " +  error + "  " + numOutputs +  "\n\n");
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                
                
                return error/numOutputs;
	}
	
	public double[] getBranchSplitEntropyOutput(DoubleVector[] postSplitDists) {
		double[] entropies = new double[postSplitDists.length];
		for(int i = 0; i < postSplitDists.length; i++){
                        //====================================================
			entropies[i]=Utils.computeEntropy(postSplitDists[i]);
                        //====================================================
                }
		return entropies;
	}
	

	@Override
	public double [] getBranchesSplitMerits(DoubleVector[][] postSplitDists){
		
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                //System.out.print("MultilabelInformationGain.getBranchesSplitMerits: " +  postSplitDists.length +  "\n\n");
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
            
                int numOutputs=postSplitDists.length;
		int numBranches=postSplitDists[0].length;
                
                
		double [] merits=new double[numBranches];
			for(int j=0; j<numOutputs;j++)
			{
				double [] branchMeritsOutput=getBranchSplitEntropyOutput(postSplitDists[j]);
				/* double weightTotal=0;
				for (int i=0; i<numBranches;i++){
					weightTotal+=postSplitDists[j][i].getValue(0)/weightTotal*postSplitDists[j][i].getValue(0);
				}
				for (int i=0; i<numBranches;i++){
					merits[i]+=postSplitDists[j][i].getValue(0)/weightTotal*branchMeritsOutput[i];
				}*/
				
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
                
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                //System.out.print("MultilabelInformation.getMeritOfSplitForOutput: " + "\n\n");
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                
                
		//count number of branches with weightSeen higher than threshold
		int count = 0; 
		for(int i = 0; i < postSplitDists.length; i++)
			if(postSplitDists[i].getValue(0) >=0.05*preSplitDist.getValue(0))
				count = count +1;
                
		//Consider split if all branches have required weight seen
		if(count == postSplitDists.length){
			
                        //===================================================
                        double EntPreSplit=Utils.computeEntropy(preSplitDist);
			double sumEntPostSplit=0;
			double weightTotal=0;
			for (int i=0; i<postSplitDists.length;i++){
				weightTotal+=postSplitDists[i].getValue(0);
			}
			double [] Entropies=getBranchSplitEntropyOutput(postSplitDists);
                        
                        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        //for(int ii=0; ii<Entropies.length ; ii++)
                            //System.out.print("MultilabelInformation.getMeritOfSplitForOutput: Entropies[i]" + Entropies[ii] +  "\n\n");
                       //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        
                        
			for(int i = 0; i < Entropies.length; i++)
				if(postSplitDists[i].getValue(0)>0)
					sumEntPostSplit+=(postSplitDists[i].getValue(0)/weightTotal*Entropies[i]);  //weight variance
			
                        
                        merit=(EntPreSplit-sumEntPostSplit);
                        
                        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        //System.out.print("MultilabelInformation.getMeritOfSplitForOutput: EntPreSplit,sumEntPostSplit " + EntPreSplit + "  " + sumEntPostSplit +  "\n\n");
                       //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                
                        
                        //===================================================
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
