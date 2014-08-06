package moa.classifiers.rules.multilabel.outputselectors;

import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class SelectAllOutputs extends AbstractOptionHandler implements OutputAttributesSelector {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int[] getNextOutputIndices(DoubleVector[] resultingStatistics, DoubleVector[] currentLiteralStatistics,
			int[] currentIndices) {
		return currentIndices.clone();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
	}

}
