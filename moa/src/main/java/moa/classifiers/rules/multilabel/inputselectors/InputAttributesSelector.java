package moa.classifiers.rules.multilabel.inputselectors;

import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.options.OptionHandler;

public interface InputAttributesSelector extends OptionHandler{
	//must be descending sorted
	int[] getNextInputIndices(AttributeExpansionSuggestion[] sortedSplitSuggestions);
}
