package moa.classifiers.rules.multilabel.inputselectors;

import com.github.javacliparser.FloatOption;

import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class SelectAllInputs extends AbstractOptionHandler implements
		InputAttributesSelector {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FloatOption percentageThresholdOption = new FloatOption("percentageThreshold",
			'p', "Percentage of allowed merit increase relative to the best input attribute.",
			0.5, 0.0, 1.0);

	@Override
	public int[] getNextInputIndices(
			AttributeExpansionSuggestion[] sortedSplitSuggestions) {
		int [] nextInput=null;
		if(sortedSplitSuggestions.length>0){
			int [] temp=new int[sortedSplitSuggestions.length];
			double threshold=sortedSplitSuggestions[sortedSplitSuggestions.length-1].merit*(1-percentageThresholdOption.getValue());
			temp[sortedSplitSuggestions.length-1]=sortedSplitSuggestions[sortedSplitSuggestions.length-1].predicate.getAttributeIndex();
			int ct;
			for (ct=sortedSplitSuggestions.length-2; ct>0 && sortedSplitSuggestions[ct].merit>threshold; ct--){
				temp[ct]=sortedSplitSuggestions[ct].getPredicate().getAttributeIndex();		
			}
			nextInput=new int[sortedSplitSuggestions.length-ct-1];
			for(int i=0; i<nextInput.length; i++){
				nextInput[i]=temp[i];
			}
			
		}
		return nextInput;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		// TODO Auto-generated method stub
		
	}


}
