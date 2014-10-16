package moa.classifiers.rules.multilabel.outputselectors;

import moa.core.DoubleVector;
import moa.options.OptionHandler;

public interface OutputAttributesSelector extends OptionHandler{
	int[] getNextOutputIndices(DoubleVector[] resultingLiteralStatistics,DoubleVector[] currentLiteralStatistics, int[] currentIndices);
}
