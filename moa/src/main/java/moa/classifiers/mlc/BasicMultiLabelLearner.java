/*
 *    BasicMultiLabelLearner.java
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
package moa.classifiers.mlc;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.predictions.MultiLabelClassificationPrediction;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.AbstractMultiLabelClassifier;

import moa.classifiers.rules.AMRulesRegressor;
import moa.core.FastVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.learners.Classifier;
import moa.learners.InstanceLearner;
import moa.options.ClassOption;
import moa.streams.InstanceStream;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;


import java.util.Arrays;;

/**
 * Binary relevance Multilabel Classifier
 *
 */


public class BasicMultiLabelLearner extends AbstractMultiLabelClassifier {
	
//	public IntOption randomSeedOption = new IntOption("randomSeedOption",
//			'r', "randomSeedOption", 
//			1,Integer.MIN_VALUE, Integer.MAX_VALUE);

	public BasicMultiLabelLearner() {
		super.randomSeedOption=randomSeedOption;
		init();
	}

	protected void init() {
		baseLearnerOption = new ClassOption("baseLearner", 'l',
				"Classifier to train.", Classifier.class, AMRulesRegressor.class.getName());
	}

	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption; //rules.AMRules"); 

	protected Classifier[] ensemble;

	protected boolean hasStarted = false;

	@Override
	public void resetLearningImpl() {
		this.hasStarted = false;
		if(ensemble!=null){
			for (int i=0; i<ensemble.length; i++){
				ensemble[i].resetLearning();
			}
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance instance) {
		if (this.hasStarted == false){		
			this.ensemble = new Classifier[instance.numOutputAttributes()];
			InstanceLearner baseLearner = (InstanceLearner) getPreparedClassOption(this.baseLearnerOption);
			if(baseLearner.isRandomizable())
				baseLearner.setRandomSeed(this.randomSeed);
			baseLearner.resetLearning();
			for (int i = 0; i < this.ensemble.length; i++) {
				this.ensemble[i] = (Classifier) baseLearner.copy();
			}
			this.hasStarted = true;
		}
		for (int i = 0; i < this.ensemble.length; i++) {
			Instance weightedInst = transformInstance(instance,i);
			this.ensemble[i].trainOnInstance(weightedInst); 
		}
	}

	protected InstancesHeader[] header;

	protected Instance transformInstance(Instance inst, int outputIndex) {
		if (header == null) {
			this.header = new InstancesHeader[this.ensemble.length];
		}
		if (header[outputIndex] == null) {
			//Create Header
			FastVector<Attribute> attributes = new FastVector<>();
			for (int attributeIndex = 0; attributeIndex < inst.numInputAttributes(); attributeIndex++) {
				attributes.addElement(inst.inputAttribute(attributeIndex));
			}
			//System.out.println("Number of attributes: "+this.numAttributes+ ","+inst.numAttributes());
			attributes.addElement(inst.outputAttribute(outputIndex));
			this.header[outputIndex] =  new InstancesHeader(getCLICreationString(InstanceStream.class), attributes, 0);
			this.header[outputIndex].setClassIndex(attributes.size()-1);
			this.ensemble[outputIndex].setModelContext(this.header[outputIndex]);
		}
		//Instance instance = new DenseInstance(this.numAttributes+1);
		//instance.setDataset(dataset[classifierIndex]);
		int numAttributes = this.header[outputIndex].numInputAttributes();
		double[] attVals = new double[numAttributes+1]; //JD - +1 for class
		for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
			attVals[attributeIndex] = inst.valueInputAttribute(attributeIndex);
		}
		Instance instance = new DenseInstance(1.0, attVals);
		instance.setDataset(header[outputIndex]);
		instance.setClassValue(inst.valueOutputAttribute(outputIndex));
		// System.out.println(inst.toString());
		// System.out.println(instance.toString());
		// System.out.println("============");
		return instance;
	}


	@Override
	public boolean isRandomizable() {
		return true;
	}


	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		Measurement [] baseLearnerMeasurements=((Classifier) getPreparedClassOption(this.baseLearnerOption)).getModelMeasurements();
		int nMeasurements=baseLearnerMeasurements.length;
		Measurement [] m=new Measurement[nMeasurements];

		if(this.ensemble !=null){	
			int ensembleSize=this.ensemble.length;
			for(int i=0; i<nMeasurements; i++){
				double value=0;
				for (int j=0; j<ensembleSize; ++j){
					value+=ensemble[j].getModelMeasurements()[i].getValue();
				}
				m[i]= new Measurement("Sum " + baseLearnerMeasurements[i].getName(), value);
			}
		}
		else{
			for(int i=0; i<baseLearnerMeasurements.length; i++)
				m[i]=baseLearnerMeasurements[i];
		}
		return m;
	}


	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		if(ensemble.length>0 && ensemble[0] instanceof AbstractClassifier)
		{
			for (int i=0; i<ensemble.length;i++){
				StringUtils.appendIndented(out,indent+1,"\nModel output attribute #" + i);
				((AbstractClassifier)ensemble[i]).getModelDescription(out, indent+1);
			}
		}
	}


	@Override
	public Prediction getPredictionForInstance(Instance instance) {
		Prediction prediction=null;
		
		if (this.hasStarted){ 
			prediction=new MultiLabelClassificationPrediction(ensemble.length);
			for (int i = 0; i < this.ensemble.length; i++) {
				Instance inst = transformInstance(instance,i);
				double[] votes = this.ensemble[i].getPredictionForInstance(transformInstance(instance,i)).asDoubleArray();
				
				if(inst.classAttribute().isNumeric()) {
					prediction.setVote(i, 0, votes[0]);
				}
				else {
					double[] dist = new double[votes.length];
					double sum = 0;
					
					for(int l = 0; l < votes.length; l++) {
						dist[l] = votes[l];
						sum += votes[l];
					}

					for(int l = 0; l < votes.length; l++) {
						dist[l] /= sum;
					}

					prediction.setVotes(i, dist);
				}
			}
		}

		return prediction;
	}


}


