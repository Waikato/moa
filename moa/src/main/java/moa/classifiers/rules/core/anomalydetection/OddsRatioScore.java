package moa.classifiers.rules.core.anomalydetection;

import moa.classifiers.rules.core.Utils;
import moa.classifiers.rules.core.anomalydetection.probabilityfunctions.ProbabilityFunction;
import moa.classifiers.rules.core.attributeclassobservers.FIMTDDNumericAttributeClassLimitObserver;
import moa.core.AutoExpandVector;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

public class OddsRatioScore extends AbstractAnomalyDetector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IntOption minNumberInstancesOption = new IntOption(
			"minNumberInstances",
			'n',
			"The minimum number of instances required to perform anomaly detection.",
			30, 0, Integer.MAX_VALUE);

	public ClassOption probabilityFunctionOption = new ClassOption("probabilityFunction",
			'p', "Probability function", 
			ProbabilityFunction.class,
			"CantellisInequality");


	private int minInstances;
	private double weightSeen;
	private double univariateThreshold;
	private double multivariateThreshold;
	AutoExpandVector<double[]> sufficientStatistics;
	private ProbabilityFunction probabilityFunction;

	@Override
	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
		if(probabilityFunction==null){
			weightSeen=0.0;
			//free memory
			minNumberInstancesOption=null;
			probabilityFunctionOption=null;
		}

		boolean doTest=false;
		if(sufficientStatistics==null)
			sufficientStatistics= new AutoExpandVector<double[]>();

			if(weightSeen>minInstances)
				doTest=true;

			double anomaly=0;
			//check if it is anomaly
			for(int i=0; i<instance.numInputAttributes(); i++){
				double prob=0;
				double [] stats=sufficientStatistics.get(i);
				if(instance.attribute(i).isNumeric()){
					double val=instance.valueInputAttribute(i);
					if(stats!=null){
						if(doTest){
							prob=probabilityFunction.getProbability(stats[0]/weightSeen, Utils.computeSD(stats[1], stats[0], weightSeen), val);
							anomaly+=Math.log(prob/(1-prob));
						}
						//update statistics for numeric attributes
						stats[0]+=val;
						stats[1]+=(val*val);
					}
					else{
						stats=new double[]{val,val*val};
						sufficientStatistics.set(i,stats);
					}
				}
			}
			weightSeen+=instance.weight();
			if(doTest)
				return anomaly<0;
			else
				return false;
	}





	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
