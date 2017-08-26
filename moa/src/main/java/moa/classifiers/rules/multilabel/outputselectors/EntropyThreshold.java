/*
 *    EntropyThreshold.java
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
package moa.classifiers.rules.multilabel.outputselectors;

import com.github.javacliparser.FloatOption;
import java.util.LinkedList;
import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Entropy measure use by online multi-label AMRules for heuristics computation.
 * @author RSousa
 */
public class EntropyThreshold extends AbstractOptionHandler implements
OutputAttributesSelector {

	/**
	 * 
	 */
	
    @Override
    public String getPurposeString() {
        return "Entropy measure use by online multi-label AMRules for heuristics computation.";
    }
	
	private static final long serialVersionUID = 1L;

	public FloatOption thresholdOption = new FloatOption("Threshold",
			'p', "Maximum allowed Entropy (entropy(new)/entropy(old)).",
			1.0, 0.5, 2.0);


	public int[] getNextOutputIndices(DoubleVector[] resultingStatistics, DoubleVector[] currentLiteralStatistics, int[] currentIndices) {
            
		int numCurrentOutputs=resultingStatistics.length;
		double threshold=thresholdOption.getValue();
		

        //get new outputs
		LinkedList<Integer> newOutputsList= new LinkedList<Integer>();
		for(int i=0; i<numCurrentOutputs;i++){

            double EntRes=Utils.computeEntropy(resultingStatistics[i].getValue(0),resultingStatistics[i].getValue(1));
			double EntCur=Utils.computeEntropy(currentLiteralStatistics[i].getValue(0),currentLiteralStatistics[i].getValue(1));

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

    
