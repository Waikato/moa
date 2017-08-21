/*
 *    OddsRatioScore.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package moa.classifiers.rules.core.anomalydetection;

import moa.classifiers.rules.core.Utils;
import moa.classifiers.rules.core.anomalydetection.probabilityfunctions.CantellisInequality;
import moa.classifiers.rules.core.anomalydetection.probabilityfunctions.ProbabilityFunction;
import moa.core.AutoExpandVector;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;


/**
 * Score for anomaly detection: OddsRatio
 *
 * thresholdOption - The threshold value for detecting anomalies
 * minNumberInstancesOption - The minimum number of instances required to perform anomaly detection
 * probabilityFunctionOption - Probability function selection
 */



public class OddsRatioScore extends AbstractAnomalyDetector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final double MINSTD=0.01;
	private static final double MINPROB=0.01;
	private static final double MAXPROB=1-MINPROB;

	public IntOption minNumberInstancesOption = new IntOption(
			"minNumberInstances",
			'n',
			"The minimum number of instances required to perform anomaly detection.",
			30, 0, Integer.MAX_VALUE);

	public FloatOption thresholdOption = new FloatOption(
			"threshold",
			't',
			"The threshold value for detecting anomalies.",
			-0.75, -10, 0);

	public ClassOption probabilityFunctionOption = new ClassOption("probabilityFunction",
			'p', "Probability function", 
			ProbabilityFunction.class,
			CantellisInequality.class.getName());


	private int minInstances;
	private double weightSeen;
	private double threshold;
	AutoExpandVector<double[]> sufficientStatistics;
	private ProbabilityFunction probabilityFunction;
        public double anomalyScore;

        @Override 
        public double getAnomalyScore(){
            return anomalyScore;
        }

	@Override
	public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance) {
		boolean isAnomaly=false;
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
		double anomaly=0;
		if(weightSeen>minInstances){
			int ct=0;
			//check if it is anomaly
			for(int i=0; i<instance.numInputAttributes(); i++){
				double prob=0;
				double [] stats=sufficientStatistics.get(i);
				if(instance.attribute(i).isNumeric()){
					double val=instance.valueInputAttribute(i);
					double sd=Utils.computeSD(stats[1], stats[0], weightSeen);
					if(sd>MINSTD){
						prob=probabilityFunction.getProbability(stats[0]/weightSeen, sd, val);
						if(prob>MAXPROB)
							prob=MAXPROB;
						else if(prob<MINPROB)
							prob=MINPROB;
						anomaly+=Math.log(prob)-Math.log(1-prob);
						ct++;
					}
				}
			}
			if(ct>0)
				anomaly=anomaly/ct;
			isAnomaly=anomaly<threshold;
                        anomalyScore=anomaly;
		}
		//update stats
		if(!isAnomaly){
			if(sufficientStatistics==null)
				sufficientStatistics= new AutoExpandVector<double[]>();
				weightSeen+=instance.weight();
				for(int i=0; i<instance.numInputAttributes(); i++){
					double [] stats=sufficientStatistics.get(i);
					if(instance.attribute(i).isNumeric()){
						double val=instance.valueInputAttribute(i);
						if(stats!=null){
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
		}			
		/*else
		{
			//System.out.println("Anomaly = " + anomaly + "#instances: " + weightSeen);
			printAnomaly(instance, anomaly);
		}*/
		return isAnomaly;
	}

	protected void printAnomaly(Instance inst, double anomaly) {
		StringBuffer sb= new StringBuffer();
		for(int i=0; i<inst.numInputAttributes(); i++){
			if(inst.attribute(i).isNumeric()){
				double [] stats;
				//Attribute name
				sb.append("Attribute " + i +" (" + inst.attribute(i).name()+ ") - ");
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
