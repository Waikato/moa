package moa.classifiers.active;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import java.util.ArrayList;

import weka.core.Utils;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.active.budget.BudgetManager;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;


public class DBALStream extends AbstractClassifier implements ALClassifier {
	
	private static final long serialVersionUID = 1L;
	
	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 
			'l', "Classifier to train.", Classifier.class, 
			"drift.SingleClassifierDrift");
	
	public ClassOption budgetManagerOption = new ClassOption("budgetManager",
            'b', "BudgetManager that should be used.", BudgetManager.class, 
            "ThresholdBM");
	
	// TODO: What should the default window size be?
	public IntOption windowSizeOption = new IntOption("windowSize", 'w', 
			"Window size.", 100, 0, Integer.MAX_VALUE);
	
	public FloatOption thresholdOption = new FloatOption("threshold", 't',
			"Initial threshold value.", 0.9, 0, 1);
	
	public FloatOption stepOption = new FloatOption("step", 's', 
			"Step option.", 0.01, 0, 1);
	
	private Classifier classifier;
	private BudgetManager budgetManager;
	private int windowSize;
	private double threshold;
	private ArrayList<Instance> windowInstances;
	private ArrayList<Double> windowMinDist;
	private int costLabeling;
	private int nInstances;
	private int nClasses;
	

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
		return this.budgetManager.getLastLabelAcqReport();
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return this.classifier.getVotesForInstance(inst);
	}

	@Override
	public void resetLearningImpl() {
		this.nInstances = 0;
		this.costLabeling = 0;
		
		this.windowSize = this.windowSizeOption.getValue();
		this.threshold = this.thresholdOption.getValue();
		
		this.windowInstances = new ArrayList<Instance>();
		this.windowMinDist = new ArrayList<Double>();
		
		this.classifier = (Classifier) 
				getPreparedClassOption(this.baseLearnerOption);
		this.budgetManager = (BudgetManager) 
				getPreparedClassOption(this.budgetManagerOption);
	}
	
	@Override
	public void trainOnInstanceImpl(Instance inst) {
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
        map = (Double.isNaN(map)||map < 10E-5|| Double.isInfinite(map)) ? 0 : map;
		beforeMap = (Double.isNaN(beforeMap) || beforeMap < 10E-5 || 
				     Double.isInfinite(beforeMap))
				? 0 : beforeMap;	
		
        double margin = map - beforeMap;
        this.nInstances++;
		double costNow = ((double) this.costLabeling) / this.nInstances;
       
		/*active learning step*/
		// TODO: use budget manager
		double budget = 0.5;
		if (costNow < budget && nCount1NN != 0){
			margin = margin / (r.nextGaussian() + 1.0);
			
			if (margin < this.threshold) {
				this.classifier.trainOnInstance(inst);
				this.costLabeling++;
				this.budgetManager.isAbove(1);
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