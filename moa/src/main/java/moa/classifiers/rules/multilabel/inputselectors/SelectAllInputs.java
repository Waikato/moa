package moa.classifiers.rules.multilabel.inputselectors;

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


	@Override
	public int[] getNextInputIndices(
			AttributeExpansionSuggestion[] sortedSplitSuggestions) {
		int [] nextInput=null;
		if(sortedSplitSuggestions.length>0){
			nextInput=new int[sortedSplitSuggestions.length];
			for (int i=0; i<sortedSplitSuggestions.length; i++){
				nextInput[i]=sortedSplitSuggestions[i].getPredicate().getAttributeIndex();		
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
		
	}




}
