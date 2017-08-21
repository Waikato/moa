/*
 *    AnomalinessRatioScore.java
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
import moa.classifiers.rules.core.anomalydetection.probabilityfunctions.ProbabilityFunction;
import moa.core.AutoExpandVector;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

/**
 * Score for anomaly detection
 *
 * percentageAnomalousAttributesOption - Percentage of anomalous attributes.
 * univariateAnomalyprobabilityThresholdOption - Univariate anomaly threshold value
 * minNumberInstancesOption - The minimum number of instances required to perform anomaly detection
 * probabilityFunctionOption - Probability function selection
 */



public class AnomalinessRatioScore extends AbstractAnomalyDetector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final double MINSTD=0.01;

	public FloatOption percentageAnomalousAttributesOption = new FloatOption(
			"percentageAnomalousAttributes",
			'm',
			"Percentage of anomalous attributes.",
			0.5, 0.0, 1.0);
	public FloatOption univariateAnomalyprobabilityThresholdOption = new FloatOption(
			"univariateAnomalyprobabilityThreshold",
			'u',
			"Univariate anomaly threshold value.",
			0.90, 0.0, 1.0);

	public IntOption minNumberInstancesOption = new IntOption(
			"minNumberInstances",
			'n',
			"The minimum number of instances required to perform anomaly detection.",
			30, 0, Integer.MAX_VALUE);

	public ClassOption probabilityFunctionOption = new ClassOption("probabilityFunction",
			'p', "Probability function", 
			ProbabilityFunction.class,
			"GaussInequality");


	private int minInstances;
	private double weightSeen;
	private double univariateThreshold;
	private double percentageAnomalous;
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
			univariateThreshold=univariateAnomalyprobabilityThresholdOption.getValue();
			percentageAnomalous=percentageAnomalousAttributesOption.getValue();	
			probabilityFunction=(ProbabilityFunction)getPreparedClassOption(probabilityFunctionOption);
			
			//free memory
			minNumberInstancesOption=null;
			univariateAnomalyprobabilityThresholdOption=null;
			percentageAnomalousAttributesOption=null;
			probabilityFunctionOption=null;
		}
		double anomaly=0;
		if(weightSeen>minInstances){
			int anomalousTotal=0, total=0;
			//check if it is anomaly
			for(int i=0; i<instance.numInputAttributes(); i++){
				double prob=0;
				double [] stats=sufficientStatistics.get(i);
				if(instance.attribute(i).isNumeric()){
					double val=instance.valueInputAttribute(i);
					double sd=Utils.computeSD(stats[1], stats[0], weightSeen);
					if(sd>MINSTD){
						prob=probabilityFunction.getProbability(stats[0]/weightSeen, Utils.computeSD(stats[1], stats[0], weightSeen), val);
						if((1-prob)>univariateThreshold)
							anomalousTotal++;
						total++;
					}
				}
			}
			if(total>0)
				anomaly=anomaly/total;
			isAnomaly=(anomalousTotal/((double)total)>percentageAnomalous);
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
		return isAnomaly;
	}


	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
