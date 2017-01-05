/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.classifiers.rules.multilabel.outputselectors;

import com.github.javacliparser.FloatOption;
import java.util.LinkedList;
import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 *
 * @author RSousa
 */
public class EntropyThreshold extends AbstractOptionHandler implements
OutputAttributesSelector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FloatOption thresholdOption = new FloatOption("Threshold",
			'p', "Maximum allowed Entropy (entropy(new)/entropy(old)).",
			1.0, 0.5, 2.0);


	public int[] getNextOutputIndices(DoubleVector[] resultingStatistics, DoubleVector[] currentLiteralStatistics, int[] currentIndices) {
            
		int numCurrentOutputs=resultingStatistics.length;
		double threshold=thresholdOption.getValue();
		
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx    
                /*System.out.print("\n");
                 System.out.print( "Antes\n");
                for(int ii=0 ; ii< numCurrentOutputs ; ii++){
                      System.out.format( "     Output " + ii +"   '1':" + currentLiteralStatistics[ii].getValue(1) + " (" + "%f" + ")    '0':" + (currentLiteralStatistics[ii].getValue(0)-currentLiteralStatistics[ii].getValue(1)) + " (" + "%f"  + ")     H=" + Utils.computeEntropy(currentLiteralStatistics[ii].getValue(0),currentLiteralStatistics[ii].getValue(1))+ "\n", currentLiteralStatistics[ii].getValue(1)/currentLiteralStatistics[ii].getValue(0),(currentLiteralStatistics[ii].getValue(0)-currentLiteralStatistics[ii].getValue(1))/currentLiteralStatistics[ii].getValue(0));
                }
                System.out.print( "Depois\n");
                for(int ii=0 ; ii< numCurrentOutputs ; ii++){
                      System.out.format( "     Output " + ii +"   '1':" + resultingStatistics[ii].getValue(1) + " (" + "%f" + ")    '0':" + (resultingStatistics[ii].getValue(0)-resultingStatistics[ii].getValue(1)) + "  (" + "%f" + ")     H=" + Utils.computeEntropy(resultingStatistics[ii].getValue(0),resultingStatistics[ii].getValue(1))+ "\n",resultingStatistics[ii].getValue(1)/resultingStatistics[ii].getValue(0),(resultingStatistics[ii].getValue(0)-resultingStatistics[ii].getValue(1))/resultingStatistics[ii].getValue(0));  
                }*/
                
                
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx 

                //get new outputs
		LinkedList<Integer> newOutputsList= new LinkedList<Integer>();
		for(int i=0; i<numCurrentOutputs;i++){
			
                        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        //System.out.print("EntropyThreshold.getNextOutputIndices:sumShifts " + resultingStatistics[i].getValue(3) + " "+ currentLiteralStatistics[i].getValue(3) + "\n\n");
                        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                    

                        double EntRes=Utils.computeEntropy(resultingStatistics[i].getValue(0),resultingStatistics[i].getValue(1));
			double EntCur=Utils.computeEntropy(currentLiteralStatistics[i].getValue(0),currentLiteralStatistics[i].getValue(1));
	
			
                        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        //System.out.print("EntropyThreshold.getNextOutputIndices: " + EntRes + " "+ EntCur + " " + threshold + "\n\n");
                        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        
                        if( (EntCur-EntRes) > 0 || EntCur==0) 
				newOutputsList.add(currentIndices[i]);
                        
		}
		//list to array
		int [] newOutputs=new int[newOutputsList.size()];
		int ct=0;
		for(int outIndex : newOutputsList){
			newOutputs[ct]=outIndex;
			++ct;
		}
		return newOutputs;
	}


	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}



	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

}

    
