/*
 *    MeritThreshold.java
 *    Copyright (C) 2017 University of Porto, Portugal
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
package moa.classifiers.rules.multilabel.inputselectors;

import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;


/**
 *  Input selection algorithm based on Merit threshold
 */

public class MeritThreshold extends AbstractOptionHandler implements
InputAttributesSelector {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FloatOption percentageThresholdOption = new FloatOption("percentageThreshold",
			'p', "Allowed merit decrease in percentage of best input attribute.",
			0.1, 0.0, 1.0);

	@Override
	public int[] getNextInputIndices(
			AttributeExpansionSuggestion[] sortedSplitSuggestions) {
		int [] nextInput=null;
		if(sortedSplitSuggestions.length>0){
			int [] temp=new int[sortedSplitSuggestions.length]; //use list instead?
			double threshold=sortedSplitSuggestions[sortedSplitSuggestions.length-1].merit*(percentageThresholdOption.getValue());
			temp[0]=sortedSplitSuggestions[sortedSplitSuggestions.length-1].predicate.getAttributeIndex();
			int c=1;
			for (int i=sortedSplitSuggestions.length-2; i>=0 && sortedSplitSuggestions[i].merit>=threshold; i--){
				temp[c]=sortedSplitSuggestions[i].getPredicate().getAttributeIndex();		
				c++;
			}
			if(c==1 && sortedSplitSuggestions.length>1 ){ //if only one is selected, add a second attribute for computing hoeffding bound
				c=2;
				temp[1]=sortedSplitSuggestions[sortedSplitSuggestions.length-2].predicate.getAttributeIndex();
			}
			nextInput=new int[c];
			for(int i=0; i<c; i++){
				nextInput[i]=temp[i];
			}
			
		}
		//System.out.println("Indices: " + java.util.Arrays.toString(nextInput));
		return nextInput;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
	}


}
