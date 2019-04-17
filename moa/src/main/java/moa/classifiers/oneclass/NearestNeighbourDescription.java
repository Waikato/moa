/*
 *    NearestNeighbourDescription.java
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
import java.util.List;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.OneClassClassifier;
import moa.core.FixedLengthList;
import moa.core.Measurement;

/**
 * Implements David Tax's Nearest Neighbour Description method described in
 * Section 3.4.2 of D. M. J. Tax, “One-Class Classification: Concept-learning 
 * in the absence of counter-examples,” Delft University of Technology, 2001
 * with adaptations for the data stream environment.
 * 
 * @author Richard Hugh Moulton
 *
 */
public class NearestNeighbourDescription extends AbstractClassifier implements Classifier, OneClassClassifier
{
	private static final long serialVersionUID = 1L;

	public String getPurposeString()
	{
		return "NN-d is a nearest neighbour method for one-class classification.";
	}
	
	public IntOption neighbourhoodSizeOption = new IntOption("neighbourhoodSize", 'n',
			"The number of instances to store in the neighbourhood.", 200, 1, Integer.MAX_VALUE);
	
	//public IntOption kOption = new IntOption("k", 'k',
	//		"Up to the kth nearest neighbour of the argument instance will be considered. //NOT IMPLEMENTED//", 1);
	
	//public IntOption mOption = new IntOption("m", 'm',
	//		"Up to the mth nearest neighbour of the argument's nearest neighbour(s) will be considered. //NOT IMPLEMENTED//", 1);
	
	public FloatOption thresholdOption = new FloatOption("tau", 't',
			"The threshold value to determine whether an instance is a member of the target class or an outlier", 1.0);
	
	private int nbhdSize;
	//private int instances, k, m; //NOT IMPLEMENTED//
	private double tau;
	private FixedLengthList<Instance> neighbourhood;
		
	public NearestNeighbourDescription()
	{
		
	}
	
	/**
	 * Constructor for a Nearest Neighbour Description classifier based on an argument
	 * training set of instances.
	 * 
	 * @param trainingSet the instances on which to train the classifier
	 */
	public NearestNeighbourDescription(List<Instance> trainingSet)
	{
		this.resetLearningImpl();
		
		neighbourhood.addAll(trainingSet);
	}

	/**
	 * Resets the implementation's parameters and data structures.
	 */
	@Override
	public void resetLearningImpl()
	{
		this.nbhdSize = this.neighbourhoodSizeOption.getValue();
		//this.k = this.kOption.getValue(); //NOT IMPLEMENTED//
		//this.m = this.mOption.getValue(); //NOT IMPLEMENTED//
		
		this.tau = this.thresholdOption.getValue();
		
		this.neighbourhood = new FixedLengthList<Instance>(nbhdSize);
	}
	
	/**
	 * The classifier adds the argument instance to its neighbourhood. If the neighbourhood
	 * is full, then the FixedLengthList removes the oldest instance in it to accommodate 
	 * the new instance.
	 */
	@Override
	public void trainOnInstanceImpl(Instance inst)
	{
		if(this.getAnomalyScore(inst) < 0.5)
			this.neighbourhood.add(inst);
	}
	
	/**
	 * Calculates the distance between the argument instance and its nearest neighbour as well as the distance between that
	 * nearest neighbour and its own nearest neighbour. The ratio of these distances is compared to the threshold value, tau,
	 * and converted into a vote score.
	 * 
	 * @param inst the instance to get votes for.
	 * 
	 * @return the votes for the instance's label [normal, outlier]
	 */
	@Override
	public double[] getVotesForInstance(Instance inst)
	{
		double[] votes = {0.5, 0.5};

		if(this.neighbourhood.size() > 2)
		{
			votes[1] = Math.pow(2.0, -1.0 * this.getAnomalyScore(inst) / this.tau);
			votes[0] = 1.0 - votes[1];
		}
				
		return votes;
	}
	
	/**
	 * Returns the anomaly score for an argument instance based on the distance from it to its nearest neighbour compared
	 * to the distance from its nearest neighbour to the neighbour's nearest neighbour.
	 * 
	 * @param inst the argument instance
	 * 
	 * @return d(inst, instNN) / d(instNN, instNNNN)
	 */
	public double getAnomalyScore(Instance inst)
	{
		if(this.neighbourhood.size() < 2)
			return 1.0;
		
		Instance nearestNeighbour = getNearestNeighbour(inst, this.neighbourhood, false);
		Instance nnNearestNeighbour = getNearestNeighbour(nearestNeighbour, this.neighbourhood, true);
		
		double indicatorArgument = distance(inst, nearestNeighbour) / distance(nearestNeighbour, nnNearestNeighbour);
				
		return indicatorArgument;
	}

	/**
	 * Searches the neighbourhood in order to find the argument instance's nearest neighbour.
	 * 
	 * @param inst the instance whose nearest neighbour is sought
	 * @param neighbourhood2 the neighbourhood to search for the nearest neighbour
	 * @param inNbhd if inst is in neighbourhood2: <b>true</b>, else: <b>false</b>
	 * 
	 * @return the instance that is inst's nearest neighbour in neighbourhood2
	 */
	private Instance getNearestNeighbour(Instance inst, List<Instance> neighbourhood2, boolean inNbhd)
	{
		double dist = Double.MAX_VALUE;
		Instance nearestNeighbour = null;
		
		for(Instance candidateNN : neighbourhood2)
		{
			// If inst is in neighbourhood2 and an identical instance is found, then it is no longer required to
			// look for inst and the inNbhd flag can be set to FALSE.
			if(inNbhd && (distance(inst, candidateNN) == 0))
			{
				inNbhd = false;
			}
			else
			{
				if(distance(inst, candidateNN) < dist)
				{
					nearestNeighbour = candidateNN.copy();
					dist = distance(inst, candidateNN);
				}
			}
		}
		
		return nearestNeighbour;
	}

	/**
	 * Calculates the Euclidean distance between two instances.
	 * 
	 * @param inst1 the first instance
	 * @param inst2 the second instance
	 * 
	 * @return the Euclidean distance between the two instances
	 */
	private double distance(Instance inst1, Instance inst2)
	{
		double dist = 0.0;
		
		for(int i = 0 ; i < inst1.numAttributes() ; i++)
		{
			dist += Math.pow((inst1.value(i) - inst2.value(i)), 2.0);
		}
		
		return Math.sqrt(dist);
	}

	/**
	 * Nearest Neighbour Description is not randomizable.
	 */
	@Override
	public boolean isRandomizable()
	{
		return false;
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
	 * Initializes the Nearest Neighbour Distance (NN-d) classifier with the argument training points.
	 * 
	 * @param trainingPoints the Collection of instances on which to initialize the NN-d classifier.
	 */
	@Override
	public void initialize(Collection<Instance> trainingPoints)
	{
		Iterator<Instance> trgPtsIterator = trainingPoints.iterator();
		
		if(trgPtsIterator.hasNext())
			this.trainOnInstance(trgPtsIterator.next());
	}
}
