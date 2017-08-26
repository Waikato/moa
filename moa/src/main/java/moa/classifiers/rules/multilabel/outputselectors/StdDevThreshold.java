package moa.classifiers.rules.multilabel.outputselectors;

import java.util.LinkedList;

import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;

public class StdDevThreshold extends AbstractOptionHandler implements
OutputAttributesSelector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FloatOption thresholdOption = new FloatOption("Threshold",
			'p', "Maximum allowed standar deviation ratio (stdev(new)/stdev(old)).",
			1.0, 0.5, 2.0);


	public int[] getNextOutputIndices(DoubleVector[] resultingStatistics, DoubleVector[] currentLiteralStatistics, int[] currentIndices) {
		int numCurrentOutputs=resultingStatistics.length;
		double threshold=thresholdOption.getValue();
		//get new outputs
		LinkedList<Integer> newOutputsList= new LinkedList<Integer>();
		for(int i=0; i<numCurrentOutputs;i++){
			double stdRes=Math.sqrt(Utils.computeVariance(resultingStatistics[i].getValue(0),resultingStatistics[i].getValue(3),resultingStatistics[i].getValue(4)));
			double stdCur=Math.sqrt(Utils.computeVariance(currentLiteralStatistics[i].getValue(0),currentLiteralStatistics[i].getValue(3),currentLiteralStatistics[i].getValue(4)));
	
			if(stdRes/stdCur<=threshold)
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
