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

	public FloatOption thresholdOption = new FloatOption(
			"threshold",
			't',
			"The threshold value for detecting anomalies.",
			-10, -100, 0);

	public ClassOption probabilityFunctionOption = new ClassOption("probabilityFunction",
			'p', "Probability function", 
			ProbabilityFunction.class,
			"GaussInequality");


	private int minInstances;
	private double weightSeen;
	private double threshold;
	AutoExpandVector<double[]> sufficientStatistics;
	private ProbabilityFunction probabilityFunction;

	@Override
	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
		if(probabilityFunction==null){
			weightSeen=0.0;
			//load options
			minInstances=minNumberInstancesOption.getValue();
			threshold=thresholdOption.getValue();
			probabilityFunction=(ProbabilityFunction)getPreparedClassOption(probabilityFunctionOption);
			//free memory
			minNumberInstancesOption=null;
			probabilityFunctionOption=null;
		}

		boolean doTest=weightSeen>minInstances;
		if(sufficientStatistics==null)
			sufficientStatistics= new AutoExpandVector<double[]>();

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
							//	System.out.println("prob = " + prob);
							/*	if(prob==1)
								anomaly+=Math.log(Double.MAX_VALUE);
							else if(prob==0)
								anomaly+=Math.log(Double.MIN_VALUE);
							else
								anomaly+=Math.log(prob/(1-prob));	*/	
							if(prob>0.9999)
								prob=0.9999;
							else if(prob<0.0001)
								prob=0.0001;
							anomaly+=Math.log(prob/(1-prob));
						}
						//update statistics for numeric attributes
						stats[0]+=val;
						stats[1]+=(val*val);
					}
					else{
						stats=new double[]{instance.weight()*val,instance.weight()*val*val};
						sufficientStatistics.set(i,stats);
					}
				}
			}
			weightSeen+=instance.weight();
			//System.out.println("Anomaly = " + anomaly);
			if(doTest){
				if(anomaly<threshold)
					printAnomaly(instance ,anomaly);
				return anomaly<threshold;
			}
			else
				return false;
	}





	protected void printAnomaly(Instance inst, double anomaly) {
		StringBuffer sb= new StringBuffer();
		for(int i=0; i<inst.numInputAttributes(); i++){
			if(inst.attribute(i).isNumeric()){
				double [] stats;
				//Atribute name
				sb.append("Attribute " + i +" (" + inst.attribute(i).name()+ ") - ");
				System.out.println();
				//Val for instance
				double val=inst.valueInputAttribute(i);
				sb.append("Value: ").append(val);
				stats=sufficientStatistics.get(i);	
				double mean=stats[0]/weightSeen;
				double std=Utils.computeSD(stats[1], stats[0], weightSeen);
				double prob=probabilityFunction.getProbability(mean, Utils.computeSD(stats[1], stats[0], weightSeen), val);
				//Mean
				sb.append(" - Prob: ").append(prob);
				//Mean
				sb.append(" - Mean: ").append(mean);
				//SD
				sb.append(" - Std: ").append(std).append("\n");	
			}
		}
		sb.append("Score - ").append(anomaly);
		System.out.println(sb);

	}





	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
