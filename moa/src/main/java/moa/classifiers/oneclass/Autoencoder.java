/*
 *    Autoencoder.java
 *    Copyright (C) 2018
 *    @author Richard Hugh Moulton
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

package moa.classifiers.oneclass;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.math3.linear.*;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.OneClassClassifier;
import moa.core.Measurement;

/**
 * Implements an autoencoder: a neural network that attempts to reconstruct the input.
 * The autoencoder's structure is an input layer with one neuron for each non-class attribute,
 * a hidden layer of two neurons and an output layer with one neuron for each non-class attribute.
 * 
 * Partially modeled on Albert Bifet's Perceptron.java, also found in the MOA project.
 * 
 * Backpropagation implementation based on Matt Mazur's excellent tutorial on this topic:
 * https://mattmazur.com/2015/03/17/a-step-by-step-backpropagation-example/
 * 
 * @author Richard Hugh Moulton
 *
 */
public class Autoencoder extends AbstractClassifier implements Classifier, OneClassClassifier
{
	private static final long serialVersionUID = 1L;

	public String getPurposeString()
	{
		return "An autoencoder is a neural network that attempts to reconstruct the input vector.";
	}
	
	public IntOption hiddenLayerOption = new IntOption("hiddenLayer", 'h', "The number of neurons in the hidden layer."
			+ " Should be less than the dimensionality of the data stream to ensure that the identity function is not learned.",
			2, 1, 100);
	
	public FloatOption learningRateOption = new FloatOption("learningRate", 'l', 
			"The rate to adapt the autoencoder's weights after each instance.", 0.5);
	
	public FloatOption thresholdOption = new FloatOption("threshold", 't',
			"Determines the threshold for recognizing outliers. Higher values means fewer outliers.", 0.6, 0.001, 0.999);
	
	/**
	 * If <b>true</b>, denotes that the autoencoder needs to be initialized.
	 */
	private boolean reset;
	
	/**
	 * The dimensionality of the data stream.
	 */
	private int numAttributes;
	
	/**
	 * The number of neurons in the hidden layer.
	 */
	private int hiddenLayerSize;
	
	/**
	 * The weights between the input layer and the hidden layer.
	 */
	private RealMatrix weightsOne;
	
	/**
	 * the weights between the hidden layer and the output layer.
	 */
	private RealMatrix weightsTwo;
	
	/**
	 * The bias for the calculations between the input layer and the hidden layer.
	 */
	private double biasOne;
	
	/**
	 * the bias for the calculations between the hidden layer and the output layer.
	 */
	private double biasTwo;
	
	/**
	 * The learning rate; controls the size of the adjustments of the weights and bias unit.
	 */
	private double learningRate;
	
	/**
	 * Controls the cut off point for declaring outliers. 
	 */
	private double threshold;
	
	/**
	 * Marks the autoencoder as needing to be reinitialized.
	 */
	@Override
	public void resetLearningImpl()
	{
		this.reset = true;
	}
	
	/**
	 * Initializes the autoencoder network.
	 */
	private void initializeNetwork()
	{
		this.hiddenLayerSize = this.hiddenLayerOption.getValue();
		this.learningRate = this.learningRateOption.getValue();
		this.threshold = this.thresholdOption.getValue();
		double[][] randomWeightsOne = new double[this.hiddenLayerSize][this.numAttributes];
		double[][] randomWeightsTwo = new double[this.numAttributes][this.hiddenLayerSize];
		
		for(int i = 0 ; i < this.numAttributes ; i++)
		{
			for(int j = 0 ; j < this.hiddenLayerSize ; j++)
			{
				randomWeightsOne[j][i] = this.classifierRandom.nextDouble();
				randomWeightsTwo[i][j] = this.classifierRandom.nextDouble();
			}
		}
		
		this.weightsOne = new Array2DRowRealMatrix(randomWeightsOne);
		this.weightsTwo = new Array2DRowRealMatrix(randomWeightsTwo);
		this.biasOne = this.classifierRandom.nextDouble();
		this.biasTwo = this.classifierRandom.nextDouble();
		
		this.reset = false;
	}
	
	/**
	 * Uses backpropagation to update the weights in the autoencoder.
	 */
	@Override
	public void trainOnInstanceImpl(Instance inst)
	{
		//Initialize
		if(this.reset)
		{
			this.numAttributes = inst.numAttributes()-1;
			this.initializeNetwork();
		}
		
		this.backpropagation(inst);		
	}
	
	/**
	 * Performs the requisite calculations between the input layer and the hidden layer.
	 * 
	 * @param input the input values
	 * 
	 * @return the activations of the hidden units
	 */
	private RealMatrix firstLayer(RealMatrix input)
	{
		RealMatrix hidden = (this.weightsOne.multiply(input)).scalarAdd(this.biasOne);
		double[] tempValues = new double[this.hiddenLayerSize];
		
		// Logistic function used for hidden layer activation
		for(int i = 0 ; i < this.hiddenLayerSize ; i++)
		{
			tempValues[i] = 1.0 / (1.0 + Math.pow(Math.E, -1.0*hidden.getEntry(i, 0)));
		}
		
		return new Array2DRowRealMatrix(tempValues);
	}
	
	/**
	 * Performs the requisite calculations between the hidden layer and the output layer.
	 * 
	 * @param hidden the activations of the hidden units
	 * 
	 * @return the activations of the output layer
	 */
	private RealMatrix secondLayer(RealMatrix hidden)
	{
		RealMatrix output = (this.weightsTwo.multiply(hidden)).scalarAdd(this.biasTwo);
		double[] tempValues = new double[this.numAttributes];
		
		// Logistic function used for output layer activation
		for(int i = 0 ; i < this.numAttributes ; i++)
		{
			tempValues[i] = 1.0 / (1.0 + Math.pow(Math.E, -1.0*output.getEntry(i, 0)));
		}
		
		return new Array2DRowRealMatrix(tempValues);
		
	}
	
	/**
	 * Performs backpropagation based on a training instance.
	 * 
	 * @param inst the training instance
	 */
	private void backpropagation(Instance inst)
	{
		double [] attributeValues = new double[this.numAttributes];
		
		for(int i = 0 ; i < this.numAttributes ; i++)
		{
			attributeValues[i] = inst.value(i);
		}
		
		RealMatrix input = new Array2DRowRealMatrix(attributeValues);
		RealMatrix hidden = firstLayer(input);
		RealMatrix output = secondLayer(hidden);
		
		RealMatrix delta = new Array2DRowRealMatrix(this.numAttributes,1);

		double adjustBiasTwo = 0.0;
		
		// Backpropagation to adjust the weights in layer two
		for(int i = 0 ; i < this.numAttributes ; i++)
		{
			double inputVal = input.getEntry(i, 0);
			double outputVal = output.getEntry(i, 0);
			delta.setEntry(i, 0, (outputVal-inputVal)*outputVal*(1.0-outputVal));
			//squaredError += 0.5*Math.pow((outputVal-inputVal), 2.0);
			adjustBiasTwo -= this.learningRate*delta.getEntry(i, 0)*this.biasTwo;
		}
		
		RealMatrix adjustmentTwo = (delta.multiply(hidden.transpose())).scalarMultiply(-1.0*this.learningRate);
				
		// Back propagation to adjust the weights in layer one
		RealMatrix hidden2 = hidden.scalarMultiply(-1.0).scalarAdd(1.0);
		RealMatrix delta2 = delta.transpose().multiply(this.weightsTwo);
		double adjustBiasOne = 0.0;
		
		for (int i = 0 ; i < this.hiddenLayerSize ; i++)
		{
			delta2.setEntry(0, i, delta2.getEntry(0, i)*hidden2.getEntry(i, 0)*hidden.getEntry(i, 0));
			adjustBiasOne -= this.learningRate*delta2.getEntry(0, i)*this.biasOne;
		}
		
		RealMatrix adjustmentOne = delta2.transpose().multiply(input.transpose()).scalarMultiply(-1.0*this.learningRate);
		
		this.weightsOne = this.weightsOne.add(adjustmentOne);
		this.biasOne += adjustBiasOne;
		this.weightsTwo = this.weightsTwo.add(adjustmentTwo);
		this.biasTwo += adjustBiasTwo;
	}
	
	/**
	 * Calculates the autoencoder's output for a given input instance
	 * 
	 * @param inst the instance given to the autoencoder
	 * @return the outputs of the hidden layer and the output vector that attempts to reconstruct the input
	 *
	private RealMatrix prediction(RealMatrix input)
	{
		RealMatrix output = new double[this.numAttributes+2];
		
		// Initialize arrays and compute total net input for the hidden layer
		for(int i = 0 ; i < this.numAttributes ; i++)
		{
			input[i] = inst.value(i);
			output[i+2] = this.biasTwo;
			output[0] += input[i]*this.weightsOne[i][0];
			output[1] += input[i]*this.weightsOne[i][1];
		}
		
		// Logistic function used for hidden layer activation 
		output[0] = 1.0 / (1.0 + Math.pow(Math.E,-1.0 * (output[0] + this.biasOne)));
		output[1] = 1.0 / (1.0 + Math.pow(Math.E,-1.0 * (output[1] + this.biasOne)));
		
		// Compute total net input for the output layer
		// Use the logistic function for output layer activation.
		for(int i = 2 ; i < this.numAttributes ; i++)
		{
			output[i] += (output[0]*this.weightsTwo[0][i])+(output[1]*this.weightsTwo[1][i]);
			output[i] = 1.0 / (1.0 + Math.pow(Math.E,-1.0 * output[i]));
		}
		
		return output;
	}*/

	/**
	 * Calculates the error between the autoencoder's reconstruction of the input and the argument instances.
	 * This error is converted to vote scores.
	 * 
	 * @param inst the instance to get votes for
	 * 
	 * @return the votes for the instance's label [normal, outlier]
	 */
	@Override
	public double[] getVotesForInstance(Instance inst)
	{
		double[] votes = new double[2];

		if (this.reset == false)
		{
			double error = this.getAnomalyScore(inst);
			
			// Exponential function to convert the error [0, +inf) into a vote [1,0].
			votes[0] = Math.pow(2.0, -1.0 * (error / this.threshold));
			votes[1] = 1.0 - votes[0];
		}

		return votes;
	}

	/**
	 * Returns the squared error between the input value and the reconstructed value as 
	 * the anomaly score for the argument instance.
	 * 
	 * @param inst the instance to score
	 * 
	 * @return the argument instance's anomaly score.
	 */
	public double getAnomalyScore(Instance inst)
	{
		double error = 0.0;
		
		if(!this.reset)
		{
			double [] attributeValues = new double[inst.numAttributes()-1];


			for(int i = 0 ; i < attributeValues.length ; i++)
			{
				attributeValues[i] = inst.value(i);
			}

			RealMatrix input = new Array2DRowRealMatrix(attributeValues);
			RealMatrix output = secondLayer(firstLayer(input));

			for(int i = 0 ; i < this.numAttributes ; i++)
			{
				error += 0.5 * Math.pow(output.getEntry(i, 0) - input.getEntry(i, 0), 2.0);
			}
		}
		
		return error;
	}
	
	
	/**
	 * Autoencoder is randomizable.
	 */
	@Override
	public boolean isRandomizable()
	{
		return true;
	}
	
	@Override
	protected Measurement[] getModelMeasurementsImpl()
	{
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent)
	{
	}

	/**
	 * Initializes the Autoencoder classifier on the argument trainingPoints.
	 * 
	 * @param trainingPoints the Collection of instances on which to initialize the Autoencoder classifier.
	 */
	@Override
	public void initialize(Collection<Instance> trainingPoints)
	{
		Iterator<Instance> trgPtsIterator = trainingPoints.iterator();
		
		if(trgPtsIterator.hasNext() && this.reset)
		{
			Instance inst = (Instance)trgPtsIterator.next();
			this.numAttributes = inst.numAttributes()-1;
			this.initializeNetwork();
		}
		
		while(trgPtsIterator.hasNext())
		{
			this.trainOnInstance((Instance)trgPtsIterator.next());			
		}
	}

}
