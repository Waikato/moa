/*
 *    StackedPredictor.java
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
package moa.classifiers.rules.multilabel.functions;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.rules.core.Utils;
import moa.core.Measurement;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;


public class StackedPredictor extends AbstractMultiLabelLearner implements
MultiTargetRegressor, AMRulesFunction {

	/**
	 * Multi-Target Stacked Predictor for regression
	 */
	private static final long serialVersionUID = 1L;

	//THRESHOLD for normalizing attribute and target values
	private final double SD_THRESHOLD = 0.0000001;


	// Parameters and options
	public FlagOption constantLearningRatioDecayOption = new FlagOption(
			"learningRatio_Decay_set_constant", 'd',
			"Learning Ratio Decay in Perceptron set to be constant. (The next parameter).");

	public FloatOption learningRatioOption = new FloatOption(
			"learningRatio", 'l', 
			"Learning Ratio to use for training the 1st layer.", 0.025);

	public FloatOption learningRatio2ndLayerOption = new FloatOption(
			"learningRatio2ndLayer", 'n', 
			"Learning Ratio to use in the second layer.", 0.001);

	public FloatOption learningRateDecayOption = new FloatOption(
			"learningRateDecay", 'm', 
			" Learning Rate decay to use for training the 1st layer.", 0.001);

	public FlagOption skipStackingOption = new FlagOption(
			"skipStackingOption", 's',
			"Predicts the outputs of the first layer (no dependence among output is computed)");

	public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
			"Seed for random behaviour of the classifier.", 1);

	public FlagOption printWeightsOption = new FlagOption(
			"printWeights", 'p',
			"Outputs the 2nd layer weights as measurements.");


	/*
	 * Other class attributes 
	 */

	private boolean hasStarted;
	//Weight seen so far
	private double count;

	//Input attributes statistics
	private double [] inAttrSum;
	private double [] inAttrSquaredSum;

	//Output attributes statistics
	private double [] outAttrSum;
	private double [] outAttrSquaredSum;

	//First Layer Weights 
	private double [][] layer1Weights;

	//Second Layer Weights
	private double [][] layer2Weights;

	//Algorithm auxiliary variables
	double currentLearningRate;

	LinkedList<Integer> numericIndices;


	/*
	 * Algorithm's behavior 
	 */	
	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void resetWithMemory() {
		currentLearningRate=this.learningRatioOption.getValue();

	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		int numOutputs=instance.numOutputAttributes();

		if(!hasStarted){
			hasStarted=true;
			numericIndices= new LinkedList<Integer>();
			//Initialize numericAttributesIndex
			for (int i = 0; i < instance.numInputAttributes(); i++)
				if(instance.inputAttribute(i).isNumeric())
					numericIndices.add(i);

			int numInputs=numericIndices.size();
			inAttrSum=new double[numInputs];
			inAttrSquaredSum=new double[numInputs];

			outAttrSum=new double[numOutputs];
			outAttrSquaredSum=new double[numOutputs];

			layer1Weights=new double[numInputs+1][numOutputs];
			layer2Weights=new double[numOutputs+1][numOutputs];

			/*
			 * Initialize first layer randomly uniform between -1 and 1
			 * Initialize second layer such that the weights between
			 * correspondent outputs are 1 and the remaining are 0
			 */
			for (int j=0; j<numOutputs; j++)
			{
				//Iterator<Integer> it=numericIndices.iterator();
				for (int i=0; i<numInputs+1; i++)
					//while(it.hasNext())
					layer1Weights[i][j] = 2 * this.classifierRandom.nextDouble() - 1;
				layer2Weights[j][j]=1.0;
			}

			/*	for (int i=0; i<numOutputs+1; i++)
				for (int j=0; j<numOutputs; j++)
					layer2Weights[i][j]= 2 * this.classifierRandom.nextDouble() - 1;*/

		}

		/*
		 * Update statistics
		 */
		int numInputs=numericIndices.size();
		//Update input statistics
		double w=instance.weight();
		count+=w;
		Iterator<Integer> it=numericIndices.iterator();
		int ct=0;
		while(it.hasNext()){
			double value=instance.valueInputAttribute(it.next());
			inAttrSum[ct]+=value*w;
			inAttrSquaredSum[ct]+=value*value*w;
			ct++;
		}

		//Update  output statistics
		for(int i=0; i<numOutputs; i++){
			double value=instance.valueOutputAttribute(i);
			outAttrSum[i]+=value*w;
			outAttrSquaredSum[i]+=value*value*w;
		}

		/*
		 * Learn new weights
		 */

		//predict outputs
		double [] normInputs=getNormalizedInput(instance);
		//1st Layer
		double [] firstLayerOutput=predict1stLayer(normInputs);
		//2nd Layer
		double [] secondLayerOutput=null;
		if(!skipStackingOption.isSet()){
			secondLayerOutput=predict2ndLayer(firstLayerOutput);
		}


		if(!constantLearningRatioDecayOption.isSet()){
			currentLearningRate = learningRatioOption.getValue() / (1+ count*this.learningRateDecayOption.getValue()); 
		}

		//update weights
		//1st Layer
		double [] normOutputs=getNormalizedOutput(instance);
		for (int j=0; j<numOutputs; j++){
			double delta=normOutputs[j] - firstLayerOutput[j];
			double sumLayer=0;
			for(int i=0; i<numInputs; i++){
				layer1Weights[i][j]+=currentLearningRate * delta * normInputs[i]*instance.weight();
				sumLayer+=Math.abs(layer1Weights[i][j]);
			}

			layer1Weights[numInputs][j]+=currentLearningRate * delta * instance.weight();
			sumLayer+=Math.abs(layer1Weights[numInputs][j]);
			if(sumLayer>(numInputs)){
				for(int i=0; i<(numInputs+1); i++)
					layer1Weights[i][j]/=sumLayer;
			}
		}
		if(!skipStackingOption.isSet()){
			//update weights
			//2nd Layer
			double learningRate2ndLayer=learningRatio2ndLayerOption.getValue();
			for (int j=0; j<numOutputs; j++){
				double delta=normOutputs[j] - secondLayerOutput[j];
				double sumLayer=0;
				for(int i=0; i<numOutputs; i++){
					layer2Weights[i][j]+=learningRate2ndLayer * delta * firstLayerOutput[i]*instance.weight();
					sumLayer+=Math.abs(layer2Weights[i][j]);
				}

				layer2Weights[numOutputs][j]+=learningRate2ndLayer * delta * instance.weight();
				sumLayer+=Math.abs(layer2Weights[numOutputs][j]);
				if(sumLayer>(numOutputs)){
					for(int i=0; i<(numOutputs+1); i++)
						layer2Weights[i][j]/=sumLayer;
				}
			}

		}

	}


	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance inst) {
		Prediction pred=null;
		if(hasStarted)
		{
			int numOutputs=outAttrSum.length;
			pred=new MultiLabelPrediction(numOutputs);

			double [] normInputs=getNormalizedInput(inst);
			double [] firstLayerOutput=predict1stLayer(normInputs);

			double [] denormalizedOutput=null;
			if(!skipStackingOption.isSet()){
				double [] secondLayerOutput=predict2ndLayer(firstLayerOutput);
				denormalizedOutput=getDenormalizedOutput(secondLayerOutput);
			}
			else
				denormalizedOutput=getDenormalizedOutput(firstLayerOutput);

			for(int i=0; i<numOutputs; i++)
				pred.setVotes(i, new double[]{denormalizedOutput[i]});	
		}	
		return pred;
	}

	@Override
	public void resetLearningImpl() {

		hasStarted=false;
		count=0;
		inAttrSum=null;
		inAttrSquaredSum=null;

		outAttrSum=null;
		outAttrSquaredSum=null;

		layer1Weights=null;
		layer2Weights=null;

		numericIndices=null;

		currentLearningRate=this.learningRatioOption.getValue();

		//TODO: JD Check if random generator is somehow overridden
		this.classifierRandom=new Random();
		this.classifierRandom.setSeed(randomSeedOption.getValue());

	}

	protected double [] getNormalizedInput(MultiLabelInstance instance) {
		int numInputs=numericIndices.size();
		double [] normalizedInput=new double[numInputs];
		Iterator<Integer> it=numericIndices.iterator();
		int i=0;
		while(it.hasNext()){
			double mean=inAttrSum[i]/count;
			double std=Utils.computeSD(inAttrSquaredSum[i], inAttrSum[i], count);
			normalizedInput[i]=instance.valueInputAttribute(it.next())-mean;
			if (std > SD_THRESHOLD)
				normalizedInput[i]/=std;
			i++;
		}
		return normalizedInput;
	}

	protected double [] getNormalizedOutput(MultiLabelInstance instance) {
		int numOutputs=instance.numOutputAttributes();
		double [] normalizedOutput=new double[numOutputs];
		for(int i=0; i<numOutputs; i++){
			double mean=outAttrSum[i]/count;
			double std=Utils.computeSD(outAttrSquaredSum[i], outAttrSum[i], count);
			normalizedOutput[i]=instance.valueOutputAttribute(i)-mean;
			if (std > SD_THRESHOLD)
				normalizedOutput[i]/=std;
		}
		return normalizedOutput;
	}

	protected double [] getDenormalizedOutput(double [] normOutputs) {
		int numOutputs=normOutputs.length;
		double [] denormalizedOutput=new double[numOutputs];

		for(int i=0; i<numOutputs; i++){
			double mean=outAttrSum[i]/count;
			double std=Utils.computeSD(outAttrSquaredSum[i], outAttrSum[i], count);
			if(std > SD_THRESHOLD)
				denormalizedOutput[i]=normOutputs[i]*std+mean;
			else
				denormalizedOutput[i]=normOutputs[i]+mean;
		}
		return denormalizedOutput;
	}

	private double[] predict1stLayer(double [] normInputs) {
		int numInputs=numericIndices.size();
		int numOutputs=this.outAttrSum.length;

		double [] firstLayerOutput=new double[numOutputs];
		for (int j=0; j<numOutputs; j++){
			for(int i=0; i<numInputs; i++)
				firstLayerOutput[j]+=normInputs[i]*layer1Weights[i][j];
			firstLayerOutput[j]+=layer1Weights[numInputs][j]; //bias
		}
		return firstLayerOutput;
	}

	private double[] predict2ndLayer(double[] firstLayerOutput) {
		int numOutputs=firstLayerOutput.length;
		double [] secondLayerOutput=new double[numOutputs];
		for (int j=0; j<numOutputs; j++){
			for(int i=0; i<numOutputs; i++)
				secondLayerOutput[j]+=firstLayerOutput[i]*layer2Weights[i][j];
			secondLayerOutput[j]+=layer2Weights[numOutputs][j]; //bias
		}
		return secondLayerOutput;
	}


	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		Measurement [] measurements=null;
		if(printWeightsOption.isSet()){
			int numWeights=this.layer2Weights.length;
			measurements= new Measurement[numWeights*(numWeights-1)];
			int ct=0;
			for(int j=0; j<numWeights-1; j++){
				for(int i=0; i<numWeights-1; i++){
					measurements[ct++]= new Measurement("W Out" + (i+1) + ": Out"+(j+1), layer2Weights[i][j]);
				}

				measurements[ct++]= new Measurement("W Bias: Out"+(j+1), layer2Weights[numWeights-1][j]);
			}
		}
		return measurements;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectOutputsToLearn(int[] outputAtributtes) {

		//Remove weights for unselected outputs
		int numOutputs=outputAtributtes.length;
		double [] newOutAttrSum=new double[numOutputs];
		double [] newOutAttrSquaredSum=new double[numOutputs];
		int numInputsPlus1=layer1Weights.length;

		double [][] newLayer1Weights=new double[numInputsPlus1][numOutputs];
		double [][] newLayer2Weights=new double[numInputsPlus1][numOutputs];

		int oldNumOutputs=layer2Weights.length-1;
		for (int j=0; j<numOutputs; j++){
			int out=outputAtributtes[j];
			newOutAttrSum[j]=outAttrSum[out];
			newOutAttrSquaredSum[j]=outAttrSquaredSum[out];
			for(int i=0; i<numInputsPlus1; i++)
				newLayer1Weights[i][j]=layer1Weights[i][out];
			for (int i=0; i<numOutputs; i++){
				int out2=outputAtributtes[i];
				newLayer2Weights[i][j]=layer2Weights[out2][out];
			}
			newLayer2Weights[numOutputs][j]=layer2Weights[oldNumOutputs][out];
		}
		outAttrSum=newOutAttrSum;
		outAttrSquaredSum=newOutAttrSquaredSum;
		layer1Weights=newLayer1Weights;
		layer2Weights=newLayer2Weights;
	}
}
