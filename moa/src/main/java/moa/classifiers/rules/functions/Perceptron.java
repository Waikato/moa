/*
 *    Perceptron.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
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
package moa.classifiers.rules.functions;

import java.util.LinkedList;
import java.util.Random;

import moa.classifiers.AbstractClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

public class Perceptron extends AbstractClassifier implements AMRulesRegressorFunction{

	private final double SD_THRESHOLD = 0.0000001; //THRESHOLD for normalizing attribute and target values

	private static final long serialVersionUID = 1L;

	public FlagOption constantLearningRatioDecayOption = new FlagOption(
			"learningRatio_Decay_set_constant", 'd',
			"Learning Ratio Decay in Perceptron set to be constant. (The next parameter).");

	public FloatOption learningRatioOption = new FloatOption(
			"learningRatio", 'l', 
			"Constante Learning Ratio to use for training the Perceptrons in the leaves.", 0.025);

	public FloatOption learningRateDecayOption = new FloatOption(
			"learningRateDecay", 'm', 
			" Learning Rate decay to use for training the Perceptron.", 0.001);

	public FloatOption fadingFactorOption = new FloatOption(
			"fadingFactor", 'e', 
			"Fading factor for the Perceptron accumulated error", 0.99, 0, 1);
	
	public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random behaviour of the classifier.", 1);
	
	private double nError;
	protected double fadingFactor;

	protected double learningRatio;

	protected double learningRateDecay;

	// The Perception weights 
	protected double[] weightAttribute; 

	// Statistics used for error calculations
	public DoubleVector perceptronattributeStatistics = new DoubleVector();
	public DoubleVector squaredperceptronattributeStatistics = new DoubleVector();

	// The number of instances contributing to this model
	protected double perceptronInstancesSeen;
	protected double perceptronYSeen;

	protected double accumulatedError;

	// If the model  (weights) should be reset or not
	protected boolean initialisePerceptron; 

	protected double perceptronsumY;

	protected double squaredperceptronsumY;

	protected int [] numericAttributesIndex;

	public Perceptron()
	{
		super();
		super.randomSeedOption=randomSeedOption;
		this.initialisePerceptron = true;
	}

	/*
	 * Perceptron
	 */
	public Perceptron(Perceptron p) {
		super();
		this.constantLearningRatioDecayOption = p.constantLearningRatioDecayOption;
		this.learningRatioOption = p.learningRatioOption;
		this.learningRateDecayOption=p.learningRateDecayOption;
		this.fadingFactorOption = p.fadingFactorOption;
		this.nError = p.nError;
		this.fadingFactor = p.fadingFactor;
		this.learningRatio = p.learningRatio;
		this.learningRateDecay = p.learningRateDecay;
		if (p.weightAttribute!=null)
			this.weightAttribute = p.weightAttribute.clone();

		this.perceptronattributeStatistics = new DoubleVector(p.perceptronattributeStatistics);
		this.squaredperceptronattributeStatistics = new DoubleVector(p.squaredperceptronattributeStatistics);
		this.perceptronInstancesSeen = p.perceptronInstancesSeen;

		this.initialisePerceptron = p.initialisePerceptron;
		this.perceptronsumY = p.perceptronsumY;
		this.squaredperceptronsumY = p.squaredperceptronsumY;
		this.perceptronYSeen=p.perceptronYSeen;
		this.numericAttributesIndex=p.numericAttributesIndex.clone();
		this.randomSeed=p.randomSeed;
	}

	public void setWeights(double[] w)
	{
		this.weightAttribute = w;	    
	}

	public double[] getWeights()
	{
		return this.weightAttribute;	    
	}



	public double getInstancesSeen() {
		return perceptronInstancesSeen;
	}

	public void setInstancesSeen(int pInstancesSeen) {
		this.perceptronInstancesSeen = pInstancesSeen;
	}


	/**
	 * A method to reset the model
	 */
	public void resetLearningImpl() {
		this.initialisePerceptron = true;
		this.reset(); 
	}

	public void reset(){
		this.classifierRandom.setSeed(this.randomSeed);
		this.nError=0.0;
		this.accumulatedError = 0.0;
		this.perceptronInstancesSeen = 0;	
		this.perceptronattributeStatistics = new DoubleVector();
		this.squaredperceptronattributeStatistics = new DoubleVector();
		this.perceptronsumY = 0.0;
		this.squaredperceptronsumY = 0.0;
		this.perceptronYSeen=0;
	}

	public void resetError(){
		this.nError=0.0;
		this.accumulatedError = 0.0;
	}

	/**
	 * Update the model using the provided instance
	 */
	public void trainOnInstanceImpl(Instance inst) {
		accumulatedError= Math.abs(this.prediction(inst)-inst.classValue())*inst.weight() + fadingFactor*accumulatedError;
		nError=inst.weight()+fadingFactor*nError;
		// Initialise Perceptron if necessary   
		if (this.initialisePerceptron == true) {
			//Initialize numericAttributesIndex
			LinkedList<Integer> numericIndices= new LinkedList<Integer>();
			for (int i = 0; i < inst.numAttributes(); i++)
				if(inst.attribute(i).isNumeric() && i!=inst.classIndex())
					numericIndices.add(i);
			numericAttributesIndex=new int[numericIndices.size()];
			int j=0;
			for(Integer index : numericIndices)
				numericAttributesIndex[j++]=index;

			this.fadingFactor=this.fadingFactorOption.getValue();
			this.initialisePerceptron = false; // not in resetLearningImpl() because it needs Instance!
			this.weightAttribute = new double[numericAttributesIndex.length+1];
			for (int i = 0; i < numericAttributesIndex.length+1; i++) {
				//if (inst.attribute(i).isNumeric())
				weightAttribute[i] = 2 * this.classifierRandom.nextDouble() - 1;
			}
			// Update Learning Rate
			learningRatio = learningRatioOption.getValue();
			this.learningRateDecay = learningRateDecayOption.getValue();

		}

		// Update attribute statistics
		this.perceptronInstancesSeen+=inst.weight();
		this.perceptronYSeen+=inst.weight();


		for(int j = 0; j < numericAttributesIndex.length; j++)
		{
			int instAttIndex = modelAttIndexToInstanceAttIndex(numericAttributesIndex[j], inst);
			double value=inst.value(instAttIndex);
			perceptronattributeStatistics.addToValue(j, value*inst.weight());	
			squaredperceptronattributeStatistics.addToValue(j, value*value*inst.weight());
		}
		double value=inst.classValue();
		this.perceptronsumY += value*inst.weight();
		this.squaredperceptronsumY += value * value*inst.weight();

		if(constantLearningRatioDecayOption.isSet()==false){
			learningRatio = learningRatioOption.getValue() / (1+ perceptronInstancesSeen*learningRateDecay); 
		}

		//double prediction = this.updateWeights(inst,learningRatio);
		//accumulatedError= Math.abs(prediction-inst.classValue()) + fadingFactor*accumulatedError;


		this.updateWeights(inst,learningRatio);

	}

	/**
	 * Output the prediction made by this perceptron on the given instance
	 */
	private double prediction(Instance inst)
	{
		if(this.initialisePerceptron){
			return 0;
		}else{
			double[] normalizedInstance = normalizedInstance(inst); 
			double normalizedPrediction = prediction(normalizedInstance);
			return denormalizedPrediction(normalizedPrediction);
		}
	}

	public double normalizedPrediction(Instance inst)
	{
		double[] normalizedInstance = normalizedInstance(inst); 
		double normalizedPrediction = prediction(normalizedInstance);
		return normalizedPrediction;
	}

	private double denormalizedPrediction(double normalizedPrediction) {
		if (this.initialisePerceptron==false){
			double meanY = perceptronsumY / perceptronYSeen;
			double sdY = computeSD(squaredperceptronsumY, perceptronsumY, perceptronYSeen);
			if (sdY > SD_THRESHOLD)	
				return normalizedPrediction * sdY + meanY;
			else
				return normalizedPrediction + meanY;
		}
		else
			return normalizedPrediction; //Perceptron may have been "reseted". Use old weights to predict

	}

	public double prediction(double[] instanceValues)
	{
		double prediction = 0.0;
		if(this.initialisePerceptron == false)
		{
			for (int j = 0; j < instanceValues.length - 1; j++) {
				prediction += this.weightAttribute[j] * instanceValues[j];
			} 
			prediction += this.weightAttribute[instanceValues.length - 1];
		}	
		return prediction;
	}

	public double[] normalizedInstance(Instance inst){
		// Normalize Instance
		double[] normalizedInstance = new double[numericAttributesIndex.length+1];
		for(int j = 0; j < numericAttributesIndex.length; j++) {
			int instAttIndex = modelAttIndexToInstanceAttIndex(numericAttributesIndex[j], inst);
			double mean = perceptronattributeStatistics.getValue(j) / perceptronYSeen;
			double sd = computeSD(squaredperceptronattributeStatistics.getValue(j), perceptronattributeStatistics.getValue(j), perceptronYSeen);
			if (sd > SD_THRESHOLD) 
				normalizedInstance[j] = (inst.value(instAttIndex) - mean)/ sd; 
			else
				normalizedInstance[j] = inst.value(instAttIndex) - mean;
		}
		return normalizedInstance;
	}

	public  double computeSD(double squaredVal, double val, double size) {
		if (size > 1) {
			return  Math.sqrt((squaredVal - ((val * val) / size)) / (size - 1.0));
		}
		return 0.0;
	}

	public void updateWeights(Instance inst, double learningRatio ){
		// Normalize Instance
		double[] normalizedInstance = normalizedInstance(inst); 
		// Compute the Normalized Prediction of Perceptron
		double normalizedPredict= prediction(normalizedInstance);
		double normalizedY = normalizeActualClassValue(inst);
		double sumWeights = 0.0;		
		double delta = normalizedY - normalizedPredict;

		for (int j = 0; j < numericAttributesIndex.length; j++) {
			//int instAttIndex = modelAttIndexToInstanceAttIndex(numericAttributesIndex[j], inst);
			//if(inst.attribute(instAttIndex).isNumeric()) {
				this.weightAttribute[j] += learningRatio * delta * normalizedInstance[j]*inst.weight();
				sumWeights += Math.abs(this.weightAttribute[j]);
			//}
		}
		this.weightAttribute[numericAttributesIndex.length] += learningRatio * delta*inst.weight();
		sumWeights += Math.abs(this.weightAttribute[numericAttributesIndex.length]);
		if (sumWeights > numericAttributesIndex.length) { // Lasso regression
			for (int j = 0; j < numericAttributesIndex.length; j++) {
				//int instAttIndex = modelAttIndexToInstanceAttIndex(numericAttributesIndex[j], inst);
				//if(inst.attribute(instAttIndex).isNumeric()) {
					this.weightAttribute[j] = this.weightAttribute[j] / sumWeights;
				//}
			}
			this.weightAttribute[numericAttributesIndex.length]  = this.weightAttribute[numericAttributesIndex.length] / sumWeights;
		}

		//return denormalizedPrediction(normalizedPredict);
	}

	public void normalizeWeights(){
		double sumWeights = 0.0;		

		for (int j = 0; j < this.weightAttribute.length ; j++) {
			sumWeights += Math.abs(this.weightAttribute[j]);
		}
		for (int j = 0; j < this.weightAttribute.length; j++) {
			this.weightAttribute[j] = this.weightAttribute[j] / sumWeights;
		}	
	}

	private double normalizeActualClassValue(Instance inst) {
		double meanY = perceptronsumY / perceptronYSeen;
		double sdY = computeSD(squaredperceptronsumY, perceptronsumY, perceptronYSeen);

		double normalizedY = 0.0;
		if (sdY > SD_THRESHOLD){
			normalizedY = (inst.classValue() - meanY) / sdY;
		}else{
			normalizedY = inst.classValue() - meanY;
		}
		return normalizedY;
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		if (!this.initialisePerceptron) //has started training
			return new double[]{this.prediction(inst)};
		else
			return new double[]{0};
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		if(this.weightAttribute!=null){
			for(int i=0; i< this.weightAttribute.length-1; ++i)
			{
				if(this.weightAttribute[i]>0 && i>0)
					out.append(" +" + Math.round(this.weightAttribute[i]*1000)/1000.0 + " X" + i );
				else if(this.weightAttribute[i]<0 || i==0)
					out.append(" " + Math.round(this.weightAttribute[i]*1000)/1000.0 + " X" + i );
			}
			if(this.weightAttribute[this.weightAttribute.length-1]>=0 )
				out.append(" +" + Math.round(this.weightAttribute[this.weightAttribute.length-1]*1000)/1000.0);
			else 
				out.append(" " + Math.round(this.weightAttribute[this.weightAttribute.length-1]*1000)/1000.0);
		}	
	}

	public void setLearningRatio(double learningRatio) {
		this.learningRatio=learningRatio;

	}

	public double getCurrentError()
	{
		if (nError>0)
			return accumulatedError/nError;
		else 
			return Double.MAX_VALUE;
	}

}


