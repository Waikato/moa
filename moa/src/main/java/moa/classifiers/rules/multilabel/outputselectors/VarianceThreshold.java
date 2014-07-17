package moa.classifiers.rules.multilabel.outputselectors;

import java.util.LinkedList;

import com.github.javacliparser.FloatOption;

import moa.classifiers.rules.core.Utils;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class VarianceThreshold extends AbstractOptionHandler implements
		OutputAttributesSelector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FloatOption percentageThresholdOption = new FloatOption("percentageThreshold",
			'p', "Percentage of allowed normalized variance increase relative to the best output score.",
			0.5, 0.0, 1.0);


	public VarianceThreshold() {

	}
	
	public int[] getNextOutputIndices(DoubleVector[] resultingStatistics, DoubleVector[] currentLiteralStatistics, int[] currentIndices) {
		int numCurrentOutputs=resultingStatistics.length;
		double [] normalizedVariances= new double [numCurrentOutputs];
		double minNormVariance=Double.MAX_VALUE;
		
		//compute minimum normalized variance
		for(int i=0; i<numCurrentOutputs;i++){
			normalizedVariances[i]=Math.sqrt(Utils.computeVariance(resultingStatistics[i]))/Math.sqrt(Utils.computeVariance(currentLiteralStatistics[i]));
			if(minNormVariance>normalizedVariances[i])
				minNormVariance=normalizedVariances[i];
		
		}
		double maxAllowedVariance=minNormVariance*(1+percentageThresholdOption.getValue());
		//get new outputs
		LinkedList<Integer> newOutputsList= new LinkedList<Integer>();
		for(int i=0; i<numCurrentOutputs;i++){
			if(normalizedVariances[i]<=maxAllowedVariance)
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
