/*
 *    Perceptron.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author E. Almeida, A. Carvalho, J. Gama
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
package moa.classifiers.rules;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;


/**
 * A Perceptron classifier modified to conform to the specifications of Ikonomovska et al.
 */
public class Perceptron extends AbstractClassifier{
	

	private static final long serialVersionUID = 1L;

	public FlagOption constantLearningRatioDecayOption = new FlagOption(
			"learningRatio_Decay_set_constant", 'd',
			"Learning Ratio Decay in Perceptron set to be constant. (The next parameter).");
	
	public FloatOption learningRatioOption = new FloatOption(
			"learningRatio", 'l', 
			"Constante Learning Ratio to use for training the Perceptrons in the leaves.", 0.01);
	
	protected double initLearnRate = 0.1;
	protected double learningRatio = 0.1;
	
	protected double learnRateDecay = 0.001; // 0.001
	
		// The Perception weights 
	    protected double[] weightAttribute; 
	    
	    // Statistics used for error calculations
	    public DoubleVector perceptronattributeStatistics = new DoubleVector();
	    public DoubleVector squaredperceptronattributeStatistics = new DoubleVector();
	    
	    // The number of instances contributing to this model
	    protected int perceptronInstancesSeen;

		protected double accumulatedError;
	    
		// If the model  (weights) should be reset or not
	    protected boolean initialisePerceptron; //AC
	    
	    protected double perceptronsumY;
	    
	    protected double squaredperceptronsumY;
	        
	    public Perceptron(Perceptron copy)
	    {
	    		this.weightAttribute = copy.getWeights(); 
	    }
	    
	    public Perceptron()
	    {
	    	    this.initialisePerceptron = true;
	    }
	    
	    public void setWeights(double[] w)
	    {
	    	this.weightAttribute = w;	    
	    }
	    
	    public double[] getWeights()
	    {
	    	return this.weightAttribute;	    
	    }
	    
	    public double getAccumulatedError() {
			return accumulatedError;
		}

		public void setAccumulatedError(double accumulatedError) {
			this.accumulatedError = accumulatedError;
		}
		
	    public int getInstancesSeen() {
			return perceptronInstancesSeen;
		}

		public void setInstancesSeen(int pInstancesSeen) {
			this.perceptronInstancesSeen = pInstancesSeen;
		}
	    
	    
	    /**
	     * A method to reset the model
	     */
   public void resetLearningImpl() {
	this.initialisePerceptron = true; //AC
   }

   public void reset(){
	   this.accumulatedError = 0.0;
	   this.perceptronInstancesSeen = 0;	
	   this.perceptronattributeStatistics = new DoubleVector();
	   this.squaredperceptronattributeStatistics = new DoubleVector();
	   this.perceptronsumY = 0.0;
	   this.squaredperceptronsumY = 0.0;
   }
   
   /**
    * Update the model using the provided instance
    */
   public void trainOnInstanceImpl(Instance inst) {
	
	// Initialise Perceptron if necessary   
	if (this.initialisePerceptron == true) {
		this.initialisePerceptron = false; // not in resetLearningImpl() because it needs Instance!
		this.weightAttribute = new double[inst.numAttributes()];
		for (int j = 0; j < inst.numAttributes(); j++) {
			this.classifierRandom.setSeed(100);
		    weightAttribute[j] = 2 * this.classifierRandom.nextDouble() - 1;
		}
		// Update Learning Rate
		
		learningRatio = 0.0;
		if(constantLearningRatioDecayOption.isSet()){
			learningRatio = learningRatioOption.getValue();
		} else{
			learningRatio = initLearnRate / (1+ perceptronInstancesSeen*learnRateDecay);
		}
		this.reset(); // AC
	}
	
	// Update attribute statistics
	this.perceptronInstancesSeen++;
	for(int j = 0; j < inst.numAttributes() -1; j++)
	{
		perceptronattributeStatistics.addToValue(j, inst.value(j));	
		squaredperceptronattributeStatistics.addToValue(j, inst.value(j)*inst.value(j));
	}
	this.perceptronsumY += inst.classValue();
	this.squaredperceptronsumY += inst.classValue() * inst.classValue();
	
	// AC Decay
	if(constantLearningRatioDecayOption.isSet()==false){
		learningRatio = initLearnRate / (1+ perceptronInstancesSeen*learnRateDecay);
	}
	
	double prediction = this.updateWeights(inst,learningRatio);
	this.accumulatedError += Math.abs(prediction-inst.classValue());
	
   }
   
   /**
    * Output the prediction made by this perceptron on the given instance
    */
   public double prediction(Instance inst)
   {
	  // return prediction(inst.toDoubleArray());
	   double[] normalizedInstance = normalizedInstance(inst); 
	   double normalizedPrediction = prediction(normalizedInstance);
	   return denormalizedPrediction(normalizedPrediction);
   }
   
   public double normalizedPrediction(Instance inst)
   {
	   double[] normalizedInstance = normalizedInstance(inst); 
	   double normalizedPrediction = prediction(normalizedInstance);
	   //return denormalizedPrediction(normalizedPrediction);
	   return normalizedPrediction;
   }
   
   private double denormalizedPrediction(double normalizedPrediction) {
	    double meanY = perceptronsumY / perceptronInstancesSeen;
		double sdY = computeSD(squaredperceptronsumY, perceptronsumY, perceptronInstancesSeen);
		
		return normalizedPrediction * sdY + meanY;
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
	//return (double)Math.round(prediction * 1000) / 1000;
	return prediction;
   }

   public double[] normalizedInstance(Instance inst){
	   // Normalize Instance
		double[] normalizedInstance = new double[inst.numAttributes()];
		for(int j = 0; j < inst.numAttributes() -1; j++) {
			double mean = perceptronattributeStatistics.getValue(j) / perceptronInstancesSeen;
			double sd = computeSD(squaredperceptronattributeStatistics.getValue(j), perceptronattributeStatistics.getValue(j), perceptronInstancesSeen);
			if (sd > 1)
				normalizedInstance[j] = (inst.value(j) - mean)/ sd;
			else
				normalizedInstance[j] = inst.value(j) - mean;
		}
		return normalizedInstance;
   }
   
   public  double computeSD(double squaredVal, double val, int size) {
		if (size > 1) {
			return  Math.sqrt((squaredVal - ((val * val) / size)) / (size - 1.0));
		}
		return 0.0;
	}
   
   public double updateWeights(Instance inst, double learningRatio ){
	   // Normalize Instance
		double[] normalizedInstance = normalizedInstance(inst); 
		// Compute the Normalized Prediction of Perceptron
		double normalizedPredict= prediction(normalizedInstance);
		double normalizedY = normalizeActualClassValue(inst);
		double sumWeights = 0.0;		
		double delta = normalizedY - normalizedPredict;
		
		for (int j = 0; j < inst.numAttributes() - 1; j++) {
			if(inst.attribute(j).isNumeric()) {
				this.weightAttribute[j] += learningRatio * delta * normalizedInstance[j];
				sumWeights += Math.abs(this.weightAttribute[j]);
			}
		}
		this.weightAttribute[inst.numAttributes() - 1] += learningRatio * delta;
		sumWeights += Math.abs(this.weightAttribute[inst.numAttributes() - 1]);
		if (sumWeights > inst.numAttributes()*10) { // AC inst.numAttributes()*10
			for (int j = 0; j < inst.numAttributes() - 1; j++) {
				if(inst.attribute(j).isNumeric()) {
					this.weightAttribute[j] = this.weightAttribute[j] / sumWeights;
				}
			}
			this.weightAttribute[inst.numAttributes() - 1]  = this.weightAttribute[inst.numAttributes() - 1] / sumWeights;
		}
			
		return this.denormalizedPrediction(normalizedPredict);
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
		double meanY = perceptronsumY / perceptronInstancesSeen;
		double sdY = computeSD(squaredperceptronsumY, perceptronsumY, perceptronInstancesSeen);
		
		double normalizedY = 0.0;
		if (sdY > 0.000000001) { // AC 0.0000001
			normalizedY = (inst.classValue() - meanY) / sdY;
		}else{
			normalizedY = inst.classValue() - meanY; // AC 11-02-2014
		}
	   return normalizedY;
	}

@Override
public boolean isRandomizable() {
	return true;
}

@Override
public double[] getVotesForInstance(Instance inst) {
	return new double[]{this.prediction(inst)};
}

@Override
protected Measurement[] getModelMeasurementsImpl() {
	return null;
	}

@Override
public void getModelDescription(StringBuilder out, int indent) {
	}

}


