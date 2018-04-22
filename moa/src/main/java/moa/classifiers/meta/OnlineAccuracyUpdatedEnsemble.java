/*
 *    OnlineAccuracyUpdatedEnsemble.java
 *    Copyright (C) 2013 Poznan University of Technology, Poznan, Poland
 *    @author Dariusz Brzezinski (dariusz.brzezinski@cs.put.poznan.pl)
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
package moa.classifiers.meta;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.trees.HoeffdingTree;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * The online version of the Accuracy Updated Ensemble as proposed by
 * Brzezinski and Stefanowski in "Combining block-based and online methods 
 * in learning ensembles from concept drifting data streams", Information Sciences, 2014.
 */
public class OnlineAccuracyUpdatedEnsemble extends AbstractClassifier implements MultiClassClassifier {

	private static final long serialVersionUID = 1L;

	/**
	 * Type of classifier to use as a component classifier.
	 */
	public ClassOption learnerOption = new ClassOption("learner", 'l', "Classifier to train.", Classifier.class, 
			"trees.HoeffdingTree -e 2000000 -g 100 -c 0.01");

	/**
	 * Number of component classifiers.
	 */
	public IntOption memberCountOption = new IntOption("memberCount", 'n',
			"The maximum number of classifiers in an ensemble.", 10, 1, Integer.MAX_VALUE);

	/**
	 * Chunk size.
	 */
	public FloatOption windowSizeOption = new FloatOption("windowSize", 'w',
			"The window size used for classifier creation and evaluation.", 500, 1, Integer.MAX_VALUE);

	/**
	 * Determines the maximum size of model (evaluated after every chunk).
	 */
	public IntOption maxByteSizeOption = new IntOption("maxByteSize", 'm', "Maximum memory consumed by ensemble.",
			33554432, 0, Integer.MAX_VALUE);
	
	/**
	 * Determines whether additional information should be sent to the output.
	 */
	public FlagOption verboseOption = new FlagOption("verbose", 'v', "When checked the algorithm outputs additional information about component classifier weights.");
	
	/**
	 * Determines whether additional information should be sent to the output.
	 */
	public FlagOption linearOption = new FlagOption("linearFunction", 'f', "When checked the algorithm uses a linear weighting function.");

	
	/**
	 * The weights of stored classifiers. 
	 * weights[x][0] = weight
	 * weights[x][1] = classifier number in learners
	 */
	protected double[][] weights;
	
	/**
	 * Class distributions.
	 */
	protected long[] classDistributions;
	
	/**
	 * Ensemble classifiers.
	 */
	protected ClassifierWithMemory[] ensemble;
	
	/**
	 * Number of processed examples.
	 */
	protected int processedInstances;
	
	/**
	 * Candidate classifier.
	 */
	protected ClassifierWithMemory candidate;
	
	/**
	 * Current window of instance class values.
	 */
	protected int[] currentWindow;
	
	/**
	 * The mean square residual in a given moment, based on a window of latest examples.
	 */
	protected double mse_r = 0;
	
	/**
	 * Window size.
	 */
	protected int windowSize = 0;
	
	@Override
	public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		this.windowSize = (int)this.windowSizeOption.getValue();
		this.candidate = new ClassifierWithMemory(((Classifier) getPreparedClassOption(this.learnerOption)).copy(), this.windowSize);
		this.candidate.classifier.resetLearning();

		super.prepareForUseImpl(monitor, repository);
	}

	@Override
	public void resetLearningImpl() {
		this.currentWindow = null;
		this.windowSize = (int)this.windowSizeOption.getValue();
		this.classDistributions = null;
		this.processedInstances = 0;
		this.ensemble = new ClassifierWithMemory[0];

		this.candidate = new ClassifierWithMemory(((Classifier) getPreparedClassOption(this.learnerOption)).copy(), this.windowSize);
		this.candidate.classifier.resetLearning();
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		this.initVariables();
    	
    	if(this.processedInstances < this.windowSize)
        {
    		this.classDistributions[(int) inst.classValue()]++;			
        }
    	else
        {  		
    		this.classDistributions[this.currentWindow[processedInstances % this.windowSize]]--;
    		this.classDistributions[(int) inst.classValue()]++;
        }
    	
    	this.currentWindow[processedInstances % this.windowSize] = (int)inst.classValue();		 	
    	this.processedInstances++;
    	this.computeMseR();
    	
        if (this.processedInstances % this.windowSize == 0) {
            this.createNewClassifier(inst);
        } else {
        	this.candidate.classifier.trainOnInstance(inst);
        	
        	for (int i = 0; i < this.ensemble.length; i++) {
        		this.weights[i][0] = this.computeWeight(i, inst);
            }
        }
        	
    	for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i].classifier.trainOnInstance(inst);
        }
	}

	/**
	 * Determines whether the classifier is randomizable.
	 */
	public boolean isRandomizable() {
		return false;
	}

	/**
	 * Predicts a class for an example.
	 */
	public double[] getVotesForInstance(Instance inst) {
		DoubleVector combinedVote = new DoubleVector();

		if (this.trainingWeightSeenByModel > 0.0) {
			for (int i = 0; i < this.ensemble.length; i++) {
				if (this.weights[i][0] > 0.0) {
					DoubleVector vote = new DoubleVector(this.ensemble[(int) this.weights[i][1]].classifier.getVotesForInstance(inst));

					if (vote.sumOfValues() > 0.0) {
						vote.normalize();
						// scale weight and prevent overflow
						vote.scaleValues(this.weights[i][0] / (1.0 * this.ensemble.length + 1.0));
						combinedVote.addValues(vote);
					}
				}
			}
		}
		
		//combinedVote.normalize();
		return combinedVote.getArrayRef();
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

	@Override
	public Classifier[] getSubClassifiers() {
		Classifier[] subClassifiers = new Classifier[this.ensemble.length];
		
		for (int i = 0; i < this.ensemble.length; i++) {
			subClassifiers[i] = this.ensemble[i].classifier;
		}
		
		return subClassifiers;
	}

	/**
     * Processes a chunk.
     *
     * @param inst New example
     */
    protected void createNewClassifier(Instance inst) {
        // Compute weights
    	double candidateClassifierWeight = 1.0 / (this.mse_r + Double.MIN_VALUE);
    	
    	if(linearOption.isSet())
        {
    		candidateClassifierWeight = java.lang.Math.max(this.mse_r, Double.MIN_VALUE);
        }
    	
    	for (int i = 0; i < this.ensemble.length; i++) {
			this.weights[i][0] = this.computeWeight(i, inst);
		}	
    	
    	// Configure candidate classifier
    	// The candidate classifier has been trained on the last window of examples
    	this.candidate.birthday = this.processedInstances;
    	
    	if (this.ensemble.length < this.memberCountOption.getValue()) {
			// Add candidate classifier		
			this.addToStored(this.candidate, candidateClassifierWeight);
		} else {
			// Substitute poorest classifier
			int poorestClassifier = this.getPoorestClassifierIndex();

			if (this.weights[poorestClassifier][0] < candidateClassifierWeight) {
				this.weights[poorestClassifier][0] = candidateClassifierWeight;
				this.candidate.classifier = this.candidate.classifier;
				this.ensemble[(int) this.weights[poorestClassifier][1]] = this.candidate;
			}
		}

    	this.candidate = new ClassifierWithMemory(((Classifier) getPreparedClassOption(this.learnerOption)).copy(), this.windowSize);
		this.candidate.classifier.resetLearning();
		
		this.enforceMemoryLimit();
    }
	
	/**
	 * Checks if the memory limit is exceeded and if so prunes the classifiers in the ensemble.
	 */
	protected void enforceMemoryLimit() {
		double memoryLimit = this.maxByteSizeOption.getValue() / (double) (this.ensemble.length + 1);

		for (int i = 0; i < this.ensemble.length; i++) {
			((HoeffdingTree) this.ensemble[(int) this.weights[i][1]].classifier).maxByteSizeOption.setValue((int) Math
					.round(memoryLimit));
			((HoeffdingTree) this.ensemble[(int) this.weights[i][1]].classifier).enforceTrackerLimit();
		}
	}

	/**
	 * Computes the MSEr threshold.
	 * 
	 * @return The MSEr threshold.
	 */
	protected void computeMseR() {
		double p_c;
		this.mse_r = 0;

		for (int i = 0; i < this.classDistributions.length; i++) {
			p_c = (double) this.classDistributions[i] / (double) this.windowSize;
			this.mse_r += p_c * ((1 - p_c) * (1 - p_c));
		}
	}
	
	/**
	 * Computes the weight of a learner before training a given example.
	 * @param i the identifier (in terms of array learners) 
	 * of the classifier for which the weight is supposed to be computed
	 * @param example the newest example
	 * @return the computed weight.
	 */	
    protected double computeWeight(int i, Instance example) {
    	
    	int d = this.windowSize;
    	int t = this.processedInstances - this.ensemble[i].birthday;

        double e_it = 0;
        double mse_it = 0;
        double voteSum = 0;
        
        try{
	        double[] votes = this.ensemble[i].classifier.getVotesForInstance(example);
	        
	        for (double element : votes) {
	            voteSum += element;
	        }
	        
	        if (voteSum > 0) {
		        double f_it = 1 - (votes[(int) example.classValue()] / voteSum);
		        e_it = f_it * f_it;
	        } else {
	        	e_it = 1;
	        }
		} catch (Exception e) {
			e_it = 1;
		}
        
        if(t > d)
        {
        	mse_it = this.ensemble[i].mse_it + e_it/(double)d - this.ensemble[i].squareErrors[t % d]/(double)d;
        }
        else
        {
        	mse_it = this.ensemble[i].mse_it*(t-1)/t + e_it/(double)t;
        }

        this.ensemble[i].squareErrors[t % d] = e_it;
        this.ensemble[i].mse_it = mse_it;
        
        if(linearOption.isSet())
        {
        	return java.lang.Math.max(mse_r - mse_it, Double.MIN_VALUE);
        }
        else
        {
        	return 1.0 / (this.mse_r + mse_it + Double.MIN_VALUE);
        }
    }
	
	/**
	 * Adds ensemble weights to the measurements.
	 */
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		if(this.verboseOption.isSet())
		{
			Measurement[] measurements = new Measurement[(int) this.memberCountOption.getValue()];
	
			for (int m = 0; m < this.memberCountOption.getValue(); m++) {
				measurements[m] = new Measurement("Member weight " + (m + 1), -1);
			}
	
			if (this.weights != null) {
				for (int i = 0; i < this.weights.length; i++) {
					measurements[i] = new Measurement("Member weight " + (i + 1), this.weights[i][0]);
				}
			}
	
			return measurements;
		}
		else
		{
			return null;
		}
	}

	/**
	 * Adds a classifier to the storage.
	 * 
	 * @param newClassifier
	 *            The classifier to add.
	 * @param newClassifiersWeight
	 *            The new classifiers weight.
	 */
	protected void addToStored(ClassifierWithMemory newClassifier, double newClassifiersWeight) {
		ClassifierWithMemory[] newStored = new ClassifierWithMemory[this.ensemble.length + 1];
		double[][] newStoredWeights = new double[newStored.length][2];

		for (int i = 0; i < newStored.length; i++) {
			if (i < this.ensemble.length) {
				newStored[i] = this.ensemble[i];
				newStoredWeights[i][0] = this.weights[i][0];
				newStoredWeights[i][1] = this.weights[i][1];
			} else {
				newStored[i] = newClassifier;
				newStoredWeights[i][0] = newClassifiersWeight;
				newStoredWeights[i][1] = i;
			}
		}
		this.ensemble = newStored;
		this.weights = newStoredWeights;
	}
	
	/**
	 * Finds the index of the classifier with the smallest weight.
	 * @return
	 */
	private int getPoorestClassifierIndex() {
		int minIndex = 0;
		
		for (int i = 1; i < this.weights.length; i++) {
			if(this.weights[i][0] < this.weights[minIndex][0]){
				minIndex = i;
			}
		}
		
		return minIndex;
	}
	
	/**
	 * Initiates the current chunk and class distribution variables.
	 */
	private void initVariables() {
		if (this.currentWindow == null) {
			this.currentWindow = new int[this.windowSize];
		}

		if (this.classDistributions == null) {
			this.classDistributions = new long[this.getModelContext().classAttribute().numValues()];
		}
	}
	
	protected class ClassifierWithMemory
	{
		private Classifier classifier;
		private int birthday;
		private double[] squareErrors;
		private double mse_it;
			
		protected ClassifierWithMemory(Classifier classifier, int windowSize)
		{
			this.classifier = classifier;
			this.squareErrors = new double[windowSize];
			this.mse_it = 0;
		}
	}
}
