/*
 *    CSMOTE.java
 * 
 *    @author Alessio Bernardo (alessio dot bernardo at polimi dot com)
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
 */

package moa.classifiers.meta.imbalanced;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import weka.core.Attribute;

import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;


/**
 * CSMOTE
 *
 * <p>
 * This strategy save all the minority samples in a window managed by ADWIN. 
 * In the meantime, a model is trained with the data in input. 
 * When the minority sample ratio is less than a certain threshold, an online SMOTE version is applied.
 * A random minority sample is chosen from the window and a new synthetic sample is generated 
 * until the minority sample ratio is greater or equal than the threshold.
 * The model is then trained with the new samples generated.
 * </p>
 * 
 * <p>See details in:<br> Alessio Bernardo, Heitor Murilo Gomes, Jacob Montiel, Bernhard Pfharinger, 
 * Albert Bifet, Emanuele Della Valle. C-SMOTE: Continuous Synthetic Minority Oversampling for Evolving Data Streams. 
 * In BigData, IEEE, 2020.</p>
 *
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classifier to train. Default is ARF</li>
 * <li>-k : Number of neighbors for SMOTE. Default is 5</li>
 * <li>-t : Threshold for the minority samples. Default is 0.5</li>
 * <li>-m : Minimum number of samples in the minority class for applying SMOTE. Default is 100</li>
 * <li>-d : Should use ADWIN as drift detector? If enabled it is used by the method 
 * 	to track the performance of the classifiers and adapt when a drift is detected.</li>
 * </ul>
 *
 * @author Alessio Bernardo (alessio dot bernardo at polimi dot com) 
 * @version $Revision: 1 $
 */
public class CSMOTE extends AbstractClassifier implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "OnlineSMOTE strategy that saves the data in a sliding window and when the minority class ratio is less than a threshold it generates some synthetic new samples using SMOTE";
    }
    
    private static final long serialVersionUID = 1L;
    
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "meta.AdaptiveRandomForest");         
    
    public IntOption neighborsOption = new IntOption("neighbors", 'k',
            "Number of neighbors for SMOTE.",
            5, 1, Integer.MAX_VALUE); 
    
    public FloatOption thresholdOption = new FloatOption("threshold", 't',
            "Minority class samples threshold.",
            0.5, 0.1, 0.5); 
    
    public IntOption minSizeAllowedOption = new IntOption("minSizeAllowed", 'm',
            "Minimum number of samples in the minority class for appling SMOTE.",
            100, 10, Integer.MAX_VALUE); 
    
    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'd',
            "Should use ADWIN as drift detector?");
    
    protected Classifier learner;                  
        
    protected int neighbors; 
    protected double threshold;
    protected int minSizeAllowed;
    protected boolean driftDetection;
    
    protected ADWIN adwin; 
    protected ADWIN adwinDriftDetector; 
    protected ArrayList<Instance> W = new ArrayList<Instance>();
    protected Instances min = null;
    protected Instances maj = null;
    
    protected int nMinorityTotal;
    protected int nMajorityTotal;
    protected int nGeneratedMinorityTotal;
    protected int nGeneratedMajorityTotal;    
    protected HashMap<Instance,Integer> instanceGenerated = new HashMap<Instance,Integer>();
    protected ArrayList<Integer> alreadyUsed = new ArrayList<Integer>();            
    
    protected SamoaToWekaInstanceConverter samoaToWeka = new SamoaToWekaInstanceConverter();
    protected WekaToSamoaInstanceConverter wekaToSamoa = new WekaToSamoaInstanceConverter();    
	protected int[] indexValues;
    
    @Override
    public void resetLearningImpl() {     	    	
        this.learner = (Classifier) getPreparedClassOption(this.baseLearnerOption);                               
        this.neighbors = this.neighborsOption.getValue();
        this.threshold = this.thresholdOption.getValue();
        this.minSizeAllowed = this.minSizeAllowedOption.getValue();
        this.driftDetection = !this.disableDriftDetectionOption.isSet();
        this.learner.resetLearning();    
      	this.nMinorityTotal = 0;
      	this.nMajorityTotal = 0;
      	this.nGeneratedMinorityTotal = 0;
        this.nGeneratedMajorityTotal = 0;      	      	      	
      	this.alreadyUsed.clear();      	
      	this.instanceGenerated.clear();
      	this.indexValues = null;
      	this.adwin = new ADWIN();
      	this.adwinDriftDetector = new ADWIN();
      	this.min = null;
      	this.maj = null;
      	this.W.clear();  
      	this.classifierRandom = new Random(this.randomSeed);
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
    	double[] prediction = this.learner.getVotesForInstance(instance);    	
        return prediction;
    }
    
    @Override
    public void trainOnInstanceImpl(Instance instance) { 
    	this.learner.trainOnInstance(instance);
    	fillBatches(instance);    	
    	//update adwin change detector
    	this.adwin.setInput(instance.classValue()); 		
		checkADWINWidth();		
		
		//check if the number of minority class samples are greater than -m
		boolean allowSMOTE = false;
		if (this.min != null && this.maj != null) {
			if (this.min.numInstances() <= this.maj.numInstances()) {
				if (this.min.numInstances() > this.minSizeAllowed) {
					allowSMOTE = true;
				}
			}
			else {
				if (this.maj.numInstances() > this.minSizeAllowed) {
					allowSMOTE = true;
				}
			}
		}				 
		
		//Apply SMOTE only if the number of minority class samples are greater than -m
		if (allowSMOTE) {				
			//Apply the online SMOTE version until the ratio will be equal to the threshold			
			while (this.threshold > calculateRatio()) {								
				Instance newInstance = onlineSMOTE();
				if (newInstance != null) {
					this.learner.trainOnInstance(newInstance);
				}																
			} 
			this.alreadyUsed.clear();			
		}
				 
		if (this.driftDetection) {			
			double pred = Utils.maxIndex(this.learner.getVotesForInstance(instance));
			double errorEstimation = this.adwinDriftDetector.getEstimation();
			double inputValue = pred == instance.classValue() ? 1.0 : 0.0;
			boolean resInput = this.adwinDriftDetector.setInput(inputValue);
			if (resInput) {
				if (this.adwinDriftDetector.getEstimation() > errorEstimation) {					
					this.learner.resetLearning();
	        		this.adwinDriftDetector = new ADWIN();
				}
			}			   
		}
    }
    
    //save the instance in window
    private void fillBatches(Instance instance) {      	    	
    	this.W.add(instance);

    	if (instance.classValue() == 1.0) {
    		if (this.maj == null) {
        		this.maj = instance.dataset();
        		this.maj.setClassIndex(this.maj.numAttributes() - 1);
        	}    		
    		this.nMajorityTotal ++;    		
    		this.maj.add(instance);    		    		
        } else {
        	if (this.min == null) {
        		this.min = instance.dataset();
        		this.min.setClassIndex(this.min.numAttributes() - 1);    		
        	}   
        	this.nMinorityTotal ++;        	
        	this.min.add(instance);        	
        }
    }
    
    //adapt the window in case of change
    private void checkADWINWidth(){    	
    	if (this.adwin.getChange()) {     		    	
    		int newWidth = this.adwin.getWidth();
    		int windowSize = this.W.size();
    		int diff = windowSize - newWidth;
    		   		        	                	
    		for (int i = 0; i < diff; i ++) {   
    			//remove the old instance    			
    			Instance instanceRemoved = this.W.remove(0);
    			//remove it also from the min or maj window
    			if (instanceRemoved.classValue() == 1.0) {    				
    				//this.majority.remove(instanceRemoved);    				    			
    				this.maj.delete(0);      				
    				//adapt the counter
    				this.nMajorityTotal --;
    				//check if the instance removed was used to generate synthetic instances
    				//and update the counter
    				if (this.instanceGenerated.get(instanceRemoved) != null) {
        				this.nGeneratedMajorityTotal -= this.instanceGenerated.get(instanceRemoved);
        				this.instanceGenerated.remove(instanceRemoved);
            		}
    			} else {
    				//this.minority.remove(instanceRemoved);
    				this.min.delete(0);
    				this.nMinorityTotal --;
    				if (this.instanceGenerated.get(instanceRemoved) != null) {
        				this.nGeneratedMinorityTotal -= this.instanceGenerated.get(instanceRemoved);
        				this.instanceGenerated.remove(instanceRemoved);
            		}
    			}    			
    		} 
    	}
    }        
    
    private double calculateRatio() {
    	double ratio = 0.0;
    	//class 0 is the real minority
		if ((this.nMinorityTotal + this.nGeneratedMinorityTotal) <= (this.nMajorityTotal + this.nGeneratedMajorityTotal)) {
			ratio = ( (double) this.nMinorityTotal + (double) this.nGeneratedMinorityTotal ) / ( (double) this.nMinorityTotal + (double) this.nGeneratedMinorityTotal + (double) this.nGeneratedMajorityTotal + (double) this.nMajorityTotal );			
		}
		//class 1 is the real minority
		else {
			ratio = ( (double) this.nMajorityTotal + (double) this.nGeneratedMajorityTotal ) / ( (double) this.nMinorityTotal + (double) this.nGeneratedMinorityTotal + (double) this.nGeneratedMajorityTotal + (double) this.nMajorityTotal );			
		}    				
    	return ratio;
    }
	
    //introduce a new instance
    private Instance onlineSMOTE() {
    	Instance newInstance;    	
    	//class 0 is the real minority
    	if ((this.nMinorityTotal + this.nGeneratedMinorityTotal) < (this.nMajorityTotal + this.nGeneratedMajorityTotal)) {  
    		//this.output.add("Min = 0");
    		newInstance = generateNewInstance(this.min);
    		if (newInstance != null) {
    			this.nGeneratedMinorityTotal ++;
    		}				
		}
		//class 1 is the real minority
		else {					
			newInstance = generateNewInstance(this.maj);
			//this.output.add("Min = 1");
			if (newInstance != null) {
				this.nGeneratedMajorityTotal ++;
			}						
		}
    	
    	return newInstance;
    }
    
    private Instance generateNewInstance(Instances minoritySamples) {       	    	    		    	
    	//find randomly an instance    	
        int pos = this.classifierRandom.nextInt(minoritySamples.numInstances());
        while (this.alreadyUsed.contains(pos)) {
        	pos = this.classifierRandom.nextInt(minoritySamples.numInstances());
        }
        this.alreadyUsed.add(pos);
        if (this.alreadyUsed.size() == minoritySamples.numInstances()) {
        	this.alreadyUsed.clear();
        }
    	Instance instanceI = minoritySamples.instance(pos);    	
    	
    	NearestNeighbourSearch search;
    	search = new LinearNNSearch(minoritySamples);     	 
		try {
			Instances neighbours = search.kNearestNeighbours(instanceI,Math.min(this.neighbors,minoritySamples.numInstances()-1));			
			// create synthetic sample    	
			double[] values = new double[minoritySamples.numAttributes()];
			int nn = this.classifierRandom.nextInt(neighbours.numInstances());
			Enumeration attrEnum = this.samoaToWeka.wekaInstance(minoritySamples.instance(0)).enumerateAttributes();
			while(attrEnum.hasMoreElements()) {
				Attribute attr = (Attribute) attrEnum.nextElement();				
				if (!attr.equals(this.samoaToWeka.wekaInstance(minoritySamples.instance(0)).classAttribute())) {
					if (attr.isNumeric()) {
						double dif = this.samoaToWeka.wekaInstance(neighbours.instance(nn)).value(attr) - this.samoaToWeka.wekaInstance(instanceI).value(attr);
						double gap = this.classifierRandom.nextDouble();
						values[attr.index()] = (double) (this.samoaToWeka.wekaInstance(instanceI).value(attr) + gap * dif);
					} else if (attr.isDate()) {
						double dif = this.samoaToWeka.wekaInstance(neighbours.instance(nn)).value(attr) - this.samoaToWeka.wekaInstance(instanceI).value(attr);
						double gap = this.classifierRandom.nextDouble();
						values[attr.index()] = (long) (this.samoaToWeka.wekaInstance(instanceI).value(attr) + gap * dif);
					} else {
						int[] valueCounts = new int[attr.numValues()];
						int iVal = (int) this.samoaToWeka.wekaInstance(instanceI).value(attr);
						valueCounts[iVal]++;
						for (int nnEx = 0; nnEx < neighbours.numInstances(); nnEx++) {
							int val = (int) this.samoaToWeka.wekaInstance(neighbours.instance(nnEx)).value(attr);
							valueCounts[val]++;
						}
						int maxIndex = 0;
						int max = Integer.MIN_VALUE;
						for (int index = 0; index < attr.numValues(); index++) {
							if (valueCounts[index] > max) {
								max = valueCounts[index];
								maxIndex = index;
							}
						}
						values[attr.index()] = maxIndex;
					}
				} 
			}								
			values[minoritySamples.classIndex()] = instanceI.classValue();
			
			if (this.indexValues == null) {    		
	    		this.indexValues = new int[instanceI.numAttributes()];
	    		for (int i = 0; i < instanceI.numAttributes(); i ++) {
	    			this.indexValues[i] = i;
	    		}
	    	}    	
			//new synthetic instance
			Instance synthetic = instanceI.copy();
			synthetic.addSparseValues(this.indexValues, values, instanceI.numAttributes());
			//update the counter of generated instances
			if (this.instanceGenerated.get(instanceI) != null) {
	    		this.instanceGenerated.replace(instanceI, this.instanceGenerated.get(instanceI)+1);
	    	} else {
	    		this.instanceGenerated.put(instanceI, 1);
	    	}
			
			return synthetic;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}		
    }

    @Override
    public boolean isRandomizable() {
    	if (this.learner != null) {
    		return this.learner.isRandomizable();	
    	}
    	else {
    		return false;
    	}
    }

    @Override
    public void getModelDescription(StringBuilder arg0, int arg1) {
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }
    
    public String toString() {
        return "SMOTE online stategy using " + this.learner + " and ADWIN as sliding window";
    }       
    
}
