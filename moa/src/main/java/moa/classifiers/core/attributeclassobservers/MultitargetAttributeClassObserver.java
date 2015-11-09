package moa.classifiers.core.attributeclassobservers;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;

public interface MultitargetAttributeClassObserver extends AttributeClassObserver {

	public void observeAttributeClassVector(double attVal, DoubleVector classVector, double weight);
	
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
            SplitCriterion criterion, double examlpesSeen, DoubleVector preSplitSums, DoubleVector preSplitSquares, int attIndex,
            int numTargets, boolean binaryOnly);

}
