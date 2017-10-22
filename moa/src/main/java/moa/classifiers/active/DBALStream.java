/*
 *    DBALStream.java
 *    Copyright (C) 2016 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 *    
 *    Based on algorithm and implementation by
 *    @author Dino Ienco (dino.ienco@irstea.fr)
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
package moa.classifiers.active;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import java.util.ArrayList;

import moa.core.Utils;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;


/**
 * <p>This class contains an active learning method for evolving data streams 
 * based on a combination of density and prediction uncertainty (DBALStream),
 * which was first presented in [IZP]. The approach decides to label an 
 * instance or not, considering whether it lies in an high density partition of
 * the data space. This allows focusing labeling efforts in the instance space 
 * where more data is concentrated; hence, the benefits of learning a more 
 * accurate classifier are expected to be higher. Instance density is 
 * approximated in an online manner by a sliding window mechanism, a standard 
 * technique for data streams.</p>
 * 
 * <p>[IZP] Dino Ienco, Indre Zliobaite, Bernhard Pfahringer: High 
 * density-focused uncertainty sampling for active learning over evolving
 * stream data. PMLR (36) 2014:133-148.</p>
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @author Dino Ienco (dino.ienco@irstea.fr)
 * @version $Revision: 1 $
 */
public class DBALStream extends AbstractClassifier implements ALClassifier {
	
	private static final long serialVersionUID = 1L;
	
	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 
			'l', "Classifier to train.", Classifier.class, 
			"drift.SingleClassifierDrift");
	
	public IntOption windowSizeOption = new IntOption("windowSize", 'w', 
			"Window size.", 100, 0, Integer.MAX_VALUE);
	
	public FloatOption budgetOption = new FloatOption("budget", 'b', 
			"Budget to use.", 0.1, 0, 1);
	
	public FloatOption thresholdOption = new FloatOption("threshold", 't',
			"Initial threshold value.", 1.0, 0, Double.MAX_VALUE);
	
	public FloatOption stepOption = new FloatOption("step", 's', 
			"Floating budget step.", 0.01, 0, 1);
	
	public FloatOption numInstancesInitOption = new FloatOption(
			"numInstancesInit", 'n', 
			"Number of instances at beginning without active learning.",
            0.0, 0.0, Integer.MAX_VALUE);
	
	private Classifier classifier;
	private int windowSize;
	private double threshold;
	private ArrayList<Instance> windowInstances;
	private ArrayList<Double> windowMinDist;
	private int costLabeling;
	private int nInstances;
	private int nClasses;
	private int lastLabelAcq;
	

	/* EUCLIDEAN DISTANCE */
	private double distance(Instance a, Instance b){
		if (a == null || b == null)
			return Double.MAX_VALUE;
		double dist = 0;
		for (int i=0; i < a.numAttributes() ; ++i){
			if (i != a.classIndex()){
				double valA = a.value(i);
				double valB = b.value(i);
					double diff = (valA - valB);
					dist += diff*diff;
			}
		}
		return Math.sqrt(dist);		
	}

	private static double getSecondMaxPosterior(double[] incomingPrediction) {
		double outSecPosterior = 0;
		if (incomingPrediction.length > 1) {
			DoubleVector vote = new DoubleVector(incomingPrediction);
			if (vote.sumOfValues() > 0.0) {
				vote.normalize();
			}
			incomingPrediction = vote.getArrayRef();
			incomingPrediction[Utils.maxIndex(incomingPrediction)] = 0;
			
			outSecPosterior = 
					(incomingPrediction[Utils.maxIndex(incomingPrediction)]);
		} else {
			outSecPosterior = 0;
		}
		return outSecPosterior;
	}

	private static double getMaxPosterior(double[] incomingPrediction) {
		double outPosterior = 0;
		if (incomingPrediction.length > 1) {
			DoubleVector vote = new DoubleVector(incomingPrediction);
			if (vote.sumOfValues() > 0.0) {
				vote.normalize();
			}
			incomingPrediction = vote.getArrayRef();
			outPosterior = 
					(incomingPrediction[Utils.maxIndex(incomingPrediction)]);
		} else {
			outPosterior = 0;
		}
		return outPosterior;
	}

	public int add_with_constraints(Instance inst){
		double loca_minDist = Double.MAX_VALUE;
		int numberTimes1NN = 0;
		
		for (int i=0; i < this.windowInstances.size(); ++i){			
			double dist = distance(this.windowInstances.get(i) , inst);
			if (dist < loca_minDist){
				loca_minDist = dist;
			}
			if (dist < windowMinDist.get(i)){
				numberTimes1NN++;
				this.windowMinDist.set(i,dist);
			}
		}
		if (this.windowInstances.size() == this.windowSize){
			this.windowInstances.remove(0);
			this.windowMinDist.remove(0);		
		}
		this.windowInstances.add(inst);
		this.windowMinDist.add(loca_minDist);
		return numberTimes1NN;
	}
	
	@Override
	public void prepareForUse() {
		super.prepareForUse();
		this.classifier.prepareForUse();
	}
	
	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public int getLastLabelAcqReport() {
		int help = this.lastLabelAcq;
		this.lastLabelAcq = 0;
		return help;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return this.classifier.getVotesForInstance(inst);
	}

	@Override
	public void resetLearningImpl() {
		this.nInstances = 0;
		this.costLabeling = 0;
		this.lastLabelAcq = 0;
		
		this.windowSize = this.windowSizeOption.getValue();
		this.threshold = this.thresholdOption.getValue();
		
		this.windowInstances = new ArrayList<Instance>();
		this.windowMinDist = new ArrayList<Double>();
		
		this.classifier = (Classifier) 
				getPreparedClassOption(this.baseLearnerOption);
	}
	
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		this.nInstances++;
		
		if (this.nInstances <= this.numInstancesInitOption.getValue()) {
			// train on all initial instances without active learning
            this.classifier.trainOnInstance(inst);
            this.costLabeling++;
            return;
        }
		
		double[] real_class = new double[this.nClasses];
		double[] predicted_class = new double[this.nClasses];
		
		java.util.Random r = new java.util.Random();
			
        double[] distribClassification = this.classifier.getVotesForInstance(inst);
        real_class[(int)inst.classValue()]++;
        predicted_class[Utils.maxIndex(distribClassification)]++;
        
		int nCount1NN = this.add_with_constraints(inst);
		
		double map = getMaxPosterior( distribClassification );
		double beforeMap = getSecondMaxPosterior(distribClassification);
		
        //I did this step because some times I get very low values E-300 
		//from the getMaxPosterior and getSecondMaxPosterior function
        map = (Double.isNaN(map)||map < 10E-5|| Double.isInfinite(map)) 
        		? 0 : map;
		beforeMap = (Double.isNaN(beforeMap) || beforeMap < 10E-5 || 
				     Double.isInfinite(beforeMap))
				? 0 : beforeMap;	
		
        double margin = map - beforeMap;
		double costNow = 
				((double) this.costLabeling - 
				 this.numInstancesInitOption.getValue()) /
				(this.nInstances - 
				 this.numInstancesInitOption.getValue());
        
		/*active learning step*/
		if (costNow < this.budgetOption.getValue() && nCount1NN != 0){
			margin = margin / (r.nextGaussian() + 1.0);
			
			if (margin < this.threshold) {
				this.classifier.trainOnInstance(inst);
				this.costLabeling++;
				this.lastLabelAcq += 1;
				this.threshold *= (1 - this.stepOption.getValue());
			} else {
				this.threshold *= (1 + this.stepOption.getValue());
			}
		}
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return this.classifier.getModelMeasurements();
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		if(this.classifier instanceof AbstractClassifier)
		{
			((AbstractClassifier) this.classifier)
				.getModelDescription(out, indent);
		}
	}
	
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		this.classifier.setModelContext(ih);
		
		this.nClasses = ih.numClasses();
		
		resetLearning();
	}
}