/*
 *    ADACC.java
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
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;

import java.util.Arrays;
import java.util.Collections;
import moa.core.Utils;

/**
 * Anticipative and Dynamic Adaptation to Concept Changes. 
 * Ensemble method for data streams that adapts to concept changes 
 * and deals with concept recurrence.
 * 
 * Reference: JABER, G., CORNUEJOLS, A., and TARROUX, P. A New On-Line Learning Method 
 * for Coping with Recurring Concepts: The ADACC System. In : Neural Information 
 * Processing. Springer Berlin Heidelberg, 2013. p. 595-604.
 * 
 * @author Ghazal Jaber (ghazal.jaber@gmail.com)
 *
 */

public class ADACC extends DACC implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;
    
    @Override
    public String getPurposeString() {
        return "Anticipative and Dynamic Adaptation to Concept Changes for data streams.";
    }
    /**
     * Evaluation window for the stability index computation  
     */
    public IntOption tauSizeOption = new IntOption("tau", 't',
            "The size of the evaluation window for the meta-learning.", 100, 1, 10000);
    /**
     * Threshold for the stability index
     */
    public FloatOption stabIndexSizeOption = new FloatOption("StabThr", 'z',
            "The threshold for stability", 0.8, 0, 1);    
    /**
     * Threshold for concept equivalence
     */
    public FloatOption equivIndexSizeOption = new FloatOption("CeThr", 'q',
            "The threshold for concept equivalence", 0.7, 0, 1);
    /**
     * Size of the evaluation window to compute the stability index  
     */
    protected int tau_size = 0; 
    /**
     * Last chunk of data of size (tau_size) to compute the stability index   
     */
    protected Instances recentChunk;
    /**
     * Threshold values for the stability index and concept equivalence
     */
	protected double theta_stab, theta_diff; 
    /**
	 * Current stability index
	 */
	protected double index; 
    /**
     * Maximum number of snapshots (copies of classifiers kept in case of recurrence)
     */
    protected final static int MAXPERMANENT = 100; 
    /**
     * Number of added snapshots
     */
    protected int addedPermanent = 0; 
    
	@Override
	protected void initVariables(){
		    	
        this.tau_size = this.tauSizeOption.getValue();
        this.theta_stab = this.stabIndexSizeOption.getValue();
        this.theta_diff = this.equivIndexSizeOption.getValue();
        this.recentChunk = null;

        int ensembleSize = (int)this.memberCountOption.getValue() + MAXPERMANENT;
        this.ensemble = new Classifier[ensembleSize];
    	this.ensembleAges = new double[ensembleSize];
        this.ensembleWindows = new int[ensembleSize][(int)this.evaluationSizeOption.getValue()];
        
	}
	
    
    @Override
    public void trainOnInstanceImpl(Instance inst) {
	
    	if (recentChunk == null)
            recentChunk = new Instances(this.getModelContext());
    	
    	if (recentChunk.size() < this.tau_size)
    		recentChunk.add(inst);
    	else
    		recentChunk.set(this.nbInstances % this.tau_size,inst);
	    	  
    	trainAndClassify(inst);
    	
        if ((this.nbInstances % this.tau_size)==0)
        	takeSnapshot();
    }
    
    /**
     * If the environment is stable enough, take a snapshot
     * (a copy) of the best adaptive classifier and keep it 
     * for future use, in case of concept recurrence 
     */
    private void takeSnapshot(){

    	this.index = computeStabilityIndex();
    	 
    	if (this.index >= this.theta_stab)
    		if (addedPermanent == 0){
    			this.ensemble[this.ensemble.length-MAXPERMANENT+addedPermanent] = getBestAdaptiveClassifier().copy();
    			addedPermanent++;
    		}
    		else{
    			
    			Classifier candidate = getBestAdaptiveClassifier().copy();
    			
    			boolean duplicate = false;
    			for (int j=0;j<Math.min(MAXPERMANENT,addedPermanent);j++){
        			Classifier lastSnapshot=this.ensemble[this.ensemble.length-MAXPERMANENT+j];
        			
        			int[][] votes=new int[2][tau_size];
        			for (int k=0;k<tau_size;k++){
        				votes[0][k]=Utils.maxIndex(candidate.getVotesForInstance(recentChunk.get(k)));	
        				votes[1][k]=Utils.maxIndex(lastSnapshot.getVotesForInstance(recentChunk.get(k)));	
        			}
        			double kappa=computeKappa(votes[0],votes[1]);
        			
        			if (kappa>=this.theta_diff){
        				duplicate = true; break;
        			} 
    			}
    			if (!duplicate){
        			this.ensemble[this.ensemble.length-MAXPERMANENT+(addedPermanent%MAXPERMANENT)]=candidate;
    				addedPermanent++;
    			}	
    		}	
    	}

                
    /**
     * Returns the kappa statistics, 
     * a statistical measure of agreement in the predictions
     * of 2 classifiers. Used as a measure of diversity of predictive 
     * models: the higher the kappa value, the smaller the diversity
     * @param y1 the predictions of classifier A
     * @param y2 the predictions of classifier B
     * @return the kappa measure
     */
    private double computeKappa(int[] y1,int[] y2){
    	
    	int m=y1.length;
    	
    	double theta1=0;
    	double counts[][]=new double[2][this.modelContext.numClasses()];
    	
    	for (int i=0;i<m;i++){
	    
    		if (y1[i]==y2[i])
	    		theta1=theta1+1;
	    	
	    	counts[0][y1[i]]=counts[0][y1[i]]+1;
	    	counts[1][y2[i]]=counts[1][y2[i]]+1;
	    		
    	}
	    
    	theta1=theta1/m;

    	double theta2=0;
    	
    	for(int i=0;i<this.modelContext.numClasses();i++)
    		theta2+=counts[0][i]/m*counts[1][i]/m;
    	
    	if (theta1==theta2 && theta2==1)
    		return 1;
    	
    	return (theta1-theta2)/(1-theta2);
    }

   /**
    * Returns the stability index of the adaptive ensemble 
    * of classifiers. The ensemble is considered stable here 
    * if its diversity level and error rates are low. 
    * @return the stability measure value
    */
    private double computeStabilityIndex(){
    	
    	int m = (int)Math.floor((this.ensemble.length-MAXPERMANENT)/2);
    	int[][] votes=new int[m][tau_size];
    	double errors=0;
    	int count=0;
   
    	
    	Pair[] arr = getHalf(true);
    	
    	for (int i=0;i<m;i++){
    		for (int j=0;j<tau_size;j++){
    			votes[i][j]=Utils.maxIndex(this.ensemble[arr[i].index].getVotesForInstance(recentChunk.get(j)));
    			errors+=(votes[i][j]==(int) this.recentChunk.get(j).classValue())?0:1;
    			count++;
    		}
    	}
    	errors = errors/count;

    	double res=0;  count=0;
    	for (int i=0;i<m;i++)
        	for (int j=i+1;j<m;j++)
        		if (i!=j){
        			res+=computeKappa(votes[i],votes[j]);
        			count++;	
        		}

    	return res/count-errors;
    }
    
    /**
     * Returns the adaptive classifier with the highest weight
     * @return the best adaptive classifier
     */
    private Classifier getBestAdaptiveClassifier(){
    	
		//take a copy of the ensemble weights (excluding snapshots)
		Pair[] newEnsembleWeights = new Pair[ensembleWeights.length-MAXPERMANENT];
		for (int i = 0 ; i < newEnsembleWeights.length; i++)	
			newEnsembleWeights[i]=ensembleWeights[i];
		
	    //sort the weight values	
		Arrays.sort(newEnsembleWeights,Collections.reverseOrder());
	    			
		return this.ensemble[newEnsembleWeights[0].index].copy();
    }

  
    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Measurement[] measurements  = new Measurement[10];
 
        measurements[0] = new Measurement("size ",
                    this.ensemble.length-MAXPERMANENT);
        measurements[1] = new Measurement("maturity ",
                   this.maturityOption.getValue());
        measurements[2] = new Measurement("evalsize ",
                   this.evaluationSizeOption.getValue());
        measurements[3] = new Measurement("cmb ",
                   this.combinationOption.getChosenIndex());
        measurements[4] = new Measurement("tau",
        		this.tau_size);
        measurements[5] = new Measurement("MaxSnapshotsSize",
        		MAXPERMANENT);
        measurements[6] = new Measurement("SnapshotsSize",
        		this.addedPermanent);
        measurements[7] = new Measurement("stabilityIndex",
        		this.index);
        measurements[8]=new Measurement("stabilityThreshold", 
        		this.theta_stab);
        measurements[9]=new Measurement("differenceThreshold", 
        		this.theta_diff);
  
        return measurements;
    }
    
    
    @Override
    protected int getNbActiveClassifiers(){
    	return ensemble.length-MAXPERMANENT+Math.min(addedPermanent,MAXPERMANENT);
    }
    
    @Override
    protected int getNbAdaptiveClassifiers(){
    	return this.ensemble.length-MAXPERMANENT;
    }
}

