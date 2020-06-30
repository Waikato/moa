/*
 *    DACC.java
 *
 *    @author Ghazal Jaber (ghazal.jaber@gmail.com)
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

package moa.classifiers.meta;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import moa.options.ClassOption;

/**
 * Dynamic Adaptation to Concept Changes. 
 * Ensemble method for data streams that adapts to concept changes.
 * 
 * Reference: JABER, G., CORNUEJOLS, A., and TARROUX, P. A New On-Line Learning Method 
 * for Coping with Recurring Concepts: The ADACC System. In : Neural Information 
 * Processing. Springer Berlin Heidelberg, 2013. p. 595-604.
 * 
 * @author Ghazal Jaber (ghazal.jaber@gmail.com)
 *
 */

public class DACC extends AbstractClassifier implements MultiClassClassifier {

	private static final long serialVersionUID = 1L;
	
	@Override
    public String getPurposeString() {
        return "Dynamic Adaptation to Concept Changes for data streams.";
    }
        
    /**
     * Base classifier
     */
    public ClassOption learnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "bayes.NaiveBayes");
    /**
     * Ensemble size
     */
    public FloatOption memberCountOption = new FloatOption("ensembleSize", 'n', "The maximum number of classifiers in an ensemble.", 20, 1, Integer.MAX_VALUE);
    /**
     * Maturity age of classifiers
     */
    public FloatOption maturityOption = new FloatOption("maturity", 'a',
            "The maturity age.", 20, 0, 100);
    /**
     * Size of the evaluation window for weights computing
     */
    public FloatOption evaluationSizeOption = new FloatOption("evalSize", 'e',
            "The size of the evaluation window.", 20, 1, 1000);
    /**
     * Combination functions: MAX and WVD (MAX leads to a faster reactivity to the change, WVD is more robust to noise) 
     */
    public MultiChoiceOption combinationOption= new MultiChoiceOption("cmb", 'c', "The combination function.",
            new String[]{"MAX","WVD"} , new String[] {"Maximum","Weighted Vote of the best"},
            0);
    /**
     * Ensemble of classifiers
     */
    protected Classifier[] ensemble;
    /**
     * Weights of classifiers 
     */
    protected Pair[] ensembleWeights;
    /**
     * Age of classifiers (to compare with maturity age)
     */
    protected double[] ensembleAges;    
    /**
     * Evaluation windows (recent classification errors)
     */
    protected int[][] ensembleWindows;
    /**
     * Number of instances from the stream 
     */
    protected int nbInstances = 0;
    

    /**
     * Initializes the method variables
     */
    protected void initVariables(){
    	int ensembleSize = (int)this.memberCountOption.getValue();
        this.ensemble = new Classifier[ensembleSize];
        this.ensembleAges = new double[ensembleSize];
        this.ensembleWindows = new int[ensembleSize][(int)this.evaluationSizeOption.getValue()];    
    }
    
    @Override
    public void resetLearningImpl() {
    
        Classifier learner = (Classifier) getPreparedClassOption(this.learnerOption);
        learner.resetLearning();

        initVariables();
         
        this.ensembleWeights = new Pair[this.ensemble.length];
        
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = learner.copy();
            this.ensembleAges[i] = 0;
            this.ensembleWeights[i] = new Pair(0.0,i);
            this.ensembleWindows[i] = new int[(int)this.evaluationSizeOption.getValue()];
        }
       
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
    	trainAndClassify(inst);
    }

    @Override 
    public double[] getVotesForInstance(Instance inst) {

        DoubleVector combinedVote = new DoubleVector();
        ArrayList<Integer> arr;
  
        int cmb = this.combinationOption.getChosenIndex();
        
        if (cmb == 0)    
        	arr = getMAXIndexes();
        else
        	arr = getWVDIndexes();
        
     
        if (this.trainingWeightSeenByModel > 0.0) {

            for (int i = 0; i < arr.size(); i++) {
                if (this.ensembleWeights[arr.get(i)].val > 0.0) {

                    DoubleVector vote = new DoubleVector(this.ensemble[arr.get(i)].getVotesForInstance(inst));

                    if (vote.sumOfValues() > 0.0) {
                        vote.normalize();
                        vote.scaleValues(this.ensembleWeights[arr.get(i)].val);
                        combinedVote.addValues(vote);
                    }
                }
            }
        }
        return combinedVote.getArrayRef();
    }

    
    /**
     * Receives a training instance from the stream and 
     * updates the adaptive classifiers accordingly
     * @param inst the instance from the stream
     */
    protected void trainAndClassify(Instance inst){
    	
        nbInstances++;
    	
        boolean mature = true;
        boolean unmature = true;
        
    	for (int i = 0; i < getNbActiveClassifiers(); i++) {
        	
    		// check if all adaptive learners are mature
    		if (this.ensembleAges[i] < this.maturityOption.getValue() && i<getNbAdaptiveClassifiers())
            	mature = false;
        	
    		// check if all adaptive learners are not mature 
        	if (this.ensembleAges[i] >= this.maturityOption.getValue() && i<getNbAdaptiveClassifiers())
            	unmature = false;
        	
        	if (this.nbInstances >= this.ensembleWeights[i].index + 1){
        
        		// train adaptive learners
        		if (i < getNbAdaptiveClassifiers())
        			this.ensemble[i].trainOnInstance(inst);
        
        		int val = this.ensemble[i].correctlyClassifies(inst)?1:0;
            	double sum = updateEvaluationWindow(i, val);  
            	this.ensembleWeights[i].val = sum;
        		this.ensembleAges[i] = this.ensembleAges[i]+1;
        		
        	}
        	
        }
    	
    	// if all adaptive learners are not mature --> set weights to one 
    	if (unmature)
        	for (int i = 0; i < getNbAdaptiveClassifiers(); i++)
        		this.ensembleWeights[i].val=1;
        		
    	// if all adaptive learners are mature --> delete one learner
        if (mature){
        	Pair[] learners = getHalf(false);
        	
        	if (learners.length > 0){
        		double rand = classifierRandom.nextInt(learners.length);
        		discardModel(learners[(int)rand].index);		
        	}
        }
	
    }
    
    /**
     * Resets a classifier in the ensemble
     * @param index the index of the classifier in the ensemble
     */
    public void discardModel(int index) {
    	this.ensemble[index].resetLearning();
        this.ensembleWeights[index].val = 0;
        this.ensembleAges[index] = 0;
        this.ensembleWindows[index]=new int[(int)this.evaluationSizeOption.getValue()];
    }
    
    /**
     * Updates the evaluation window of a classifier and returns the
     * updated weight value.
     * @param index the index of the classifier in the ensemble
     * @param val the last evaluation record of the classifier 
     * @return the updated weight value of the classifier 
     */
    protected double updateEvaluationWindow(int index,int val){
    	
    	int[] newEnsembleWindows = new int[this.ensembleWindows[index].length]; 	
    	
    	int wsize = (int)Math.min(this.evaluationSizeOption.getValue(),this.ensembleAges[index]+1);
    	
    	int sum = 0;   
    	for (int i = 0; i < wsize-1 ; i++){
    		newEnsembleWindows[i+1] = this.ensembleWindows[index][i];
    		sum = sum + this.ensembleWindows[index][i];	
    	}
    	
    	newEnsembleWindows[0] = val; 
    	this.ensembleWindows[index] = newEnsembleWindows; 

    	if (this.ensembleAges[index] >= this.maturityOption.getValue())
    		return (sum + val) * 1.0/wsize;
    	else
    		return 0; 
    		
    }
    
    /** 
     * Returns the best (or worst) half of classifiers in the adaptive ensemble.
     * The best classifiers are used to compute the stability index in ADACC. The worst 
     * classifiers are returned in order to select a classifier for deletion.  
     * @param bestHalf boolean value set to true (false) if we want to return 
     * the best (worst) half of adaptive classifiers.
     * @return an array containing the weight values of the corresponding classifiers
     * and their indexes in the ensemble.
     */
    protected Pair[] getHalf(boolean bestHalf){
    	
    	Pair[] newEnsembleWeights = new Pair[getNbAdaptiveClassifiers()];
    	System.arraycopy(ensembleWeights, 0, newEnsembleWeights, 0, newEnsembleWeights.length);
    	
    	if (bestHalf)
    		Arrays.sort(newEnsembleWeights,Collections.reverseOrder());
    	else
    		Arrays.sort(newEnsembleWeights);
    		
    	Pair[] result = new Pair[(int)Math.floor(newEnsembleWeights.length/2)];
    	System.arraycopy(newEnsembleWeights, 0, result, 0, result.length);
    	
    	return result;
    }
    
    
    /**
     * Returns the classifiers that vote for the final prediction
     * when the MAX combination function is selected
     * @return the classifiers with the highest weight value
     */
    protected ArrayList<Integer> getMAXIndexes(){
    	
    	ArrayList<Integer> maxWIndex=new ArrayList<Integer>();
    	Pair[] newEnsembleWeights = new Pair[getNbActiveClassifiers()];  	
    	System.arraycopy(ensembleWeights, 0, newEnsembleWeights, 0, newEnsembleWeights.length);
    	
    	Arrays.sort(newEnsembleWeights);
    	
    	double maxWVal = newEnsembleWeights[newEnsembleWeights.length-1].val;
    		
    	for (int i = newEnsembleWeights.length-1 ; i>=0 ; i--){
    		if (newEnsembleWeights[i].val!=maxWVal)
    			break;
    		else
    			maxWIndex.add(newEnsembleWeights[i].index);
    		
    	}
    	return maxWIndex;
    }

    /**
     * Returns the classifiers that vote for the final prediction
     * when the WVD combination function is selected
     * @return the classifiers whose weights lie in the higher half 
     * of the ensemble's weight interval.
     */
    protected ArrayList<Integer> getWVDIndexes(){
    	
    	ArrayList<Integer> maxWIndex = new ArrayList<Integer>();
    	
    	Pair[] newEnsembleWeights = new Pair[getNbActiveClassifiers()];
    	
    	System.arraycopy(ensembleWeights, 0, newEnsembleWeights, 0, newEnsembleWeights.length);
    	
    	Arrays.sort(newEnsembleWeights);
    	
    	double minWVal = newEnsembleWeights[0].val;
    	double maxWVal = newEnsembleWeights[newEnsembleWeights.length-1].val;
    	double med = (maxWVal-minWVal)*1.0/2;
    
    	for (int i = newEnsembleWeights.length-1 ; i>=0 ; i--)
    		if (newEnsembleWeights[i].val < med)
    			break;		
    		else
    			maxWIndex.add(newEnsembleWeights[i].index);
    	
    	return maxWIndex;
    }
    
    
    /**
     * Returns the number of classifiers used for prediction
     * which includes the adaptive learners and the snapshots in ADACC 
     * @return the number of classifiers used for prediction
     */
    protected int getNbActiveClassifiers(){
    	return this.ensemble.length;
    }
    
	/** 
	 * Returns the number of adaptive classifiers in the ensemble 
	 * which excludes the static snapshots in ADACC  
	 * @return the number of adaptive classifiers
	 */
    protected int getNbAdaptiveClassifiers(){
    	return this.ensemble.length;
    }
    
    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Measurement[] measurements  = new Measurement[4];
        measurements[0] = new Measurement("size ",
                    this.ensemble.length);
        measurements[1] = new Measurement("maturity ",
                   this.maturityOption.getValue());
        measurements[2] = new Measurement("evalsize ",
                   this.evaluationSizeOption.getValue());
        measurements[3] = new Measurement("cmb ",
                   this.combinationOption.getChosenIndex());      
       return measurements;
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return this.ensemble.clone();
    }
    
    /**
     * This helper class is used to sort an array of pairs of integers: val and index. 
     * The array is sorted based on the val field.
     * @author Ghazal Jaber
     *
     */
    protected class Pair implements Comparable<Pair>, Serializable   {
 	   
		private static final long serialVersionUID = 1L;
		double val;
	    int index;

	    public Pair(double d, int i){
	        this.val = d;
	        this.index = i;
	    }

	    @Override
	    public int compareTo(Pair other){
	    	if (this.val - other.val > 0 )
		        	return 1;    
	    	else
	    		if (this.val == other.val)
		        		return 0;    
	    	return -1;
	    }
	    
	    public double getValue(){
	    	return val;	
	    }
	    
	}

}