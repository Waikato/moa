/*
 *    RebalanceStream.java
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
import weka.core.Attribute;
import weka.core.Instances;
import moa.core.Measurement;
import moa.core.TimingUtils;
import moa.options.ClassOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;



/**
 * RebalanceStream
 *
 * <p>The RebalanceStream algorithm trains a model called learner as the standard algorithm implementation. 
 * Moreover, it saves every new data in input in a batch. If ADWIN detects a warning level, 
 * it starts saving the new data in input also in another batch called resetBatch. 
 * Then, when ADWIN detects a change, so there is a concept drift, it trains in parallel 3 models called learnerBal, learnerReset, learnerResetBal. 
 * The learnerBal model uses the batch data rebalanced by SMOTE, 
 * the learnerReset  model uses the resetBatch data and the learnerResetBal model uses the resetBatch data rebalanced by SMOTE.
 * Then the best model based on k-statistic is chosen to continue the experiment with a new sample.
 </p>
 *
 * <p>See details in:<br> Alessio Bernardo, Albert Bifet, Emanuele Della Valle. 
 * Incremental Rebalancing Learning \\on Evolving Data Streams. In ICDM Workshop, 2020.</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classiﬁer to train. Default is TemporallyAugmentedClassifier</li>
 * <li>-c : Minimum number of samples in the batch for applying SMOTE. Default is -1 (no limit)</li>
 * <li>-g : Maximum number of samples in the batch for applying SMOTE. Default is -1 (no limit)</li>
 * <li>-h : Minimum number of samples in the ResetBatch for applying SMOTE. Default is -1 (no limit)</li>
 * <li>-g : Maximum number of samples in the ResetBatch for applying SMOTE. Default is -1 (no limit)</li>
 * </ul>
 *
 * @author Alessio Bernardo (alessio dot bernardo at polimi dot com)
 * @version $Revision: 1 $
 */
public class RebalanceStream extends AbstractClassifier implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "RebalanceStream algorithm for rebalancing a stream and training a model with it";
    }
    
    private static final long serialVersionUID = 1L;
    
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "meta.TemporallyAugmentedClassifier");   
    
    public IntOption minInstanceLimitBatchOption = new IntOption("minInstanceLimitBatch", 'c',
            "Minimum number of instances in the batch in order to rebalance it  (-1 = no limit).",
            -1, -1, Integer.MAX_VALUE);
    
    public IntOption maxInstanceLimitBatchOption = new IntOption("maxInstanceLimitBatch", 'g',
            "Maximum number of instances in the batch in order to rebalance it  (-1 = no limit).",
            -1, -1, Integer.MAX_VALUE);
    
    public IntOption minInstanceLimitResetBatchOption = new IntOption("minInstanceLimitResetBatch", 'h',
            "Minimum number of instances in the Resetbatch in order to rebalance it  (-1 = no limit).",
            -1, -1, Integer.MAX_VALUE);
    
    public IntOption maxInstanceLimitResetBatchOption = new IntOption("maxInstanceLimitResetBatch", 'm',
            "Maximum number of instances in the Resetbatch in order to rebalance it  (-1 = no limit).",
            -1, -1, Integer.MAX_VALUE);
    
    protected Classifier learner;
    protected Classifier learnerResetBal;
    protected Classifier learnerReset;
    protected Classifier learnerBal;
    protected ADWIN adwin = new ADWIN();
    
    protected int nAttributes;
    protected int minInstanceLimitBatch;
    protected int maxInstanceLimitBatch;
    protected int minInstanceLimitResetBatch;
    protected int maxInstanceLimitResetBatch;        
    
	protected int[][] confusionMatrixLearner = new int[2][2];
    protected double accLearner = 0;
    protected double kStatLearner = 0;
	
    protected int[][] confusionMatrixResetBal = new int[2][2];
    protected double accResetBal = 0;
    protected double kStatResetBal = 0;
	
    protected int[][] confusionMatrixReset = new int[2][2];
    protected double accReset = 0;
    protected double kStatReset = 0;
	
    protected int[][] confusionMatrixBal = new int[2][2];
    protected double accBal = 0;
    protected double kStatBal = 0;		   
    
    protected ArrayList<Instance> batch = new ArrayList<Instance>();
    protected ArrayList<Instance> batchMinority = new ArrayList<Instance>();
    protected ArrayList<Instance> batchMajority = new ArrayList<Instance>();
    
    protected ArrayList<Instance> resetBatch = new ArrayList<Instance>();
    protected ArrayList<Instance> resetBatchMinority = new ArrayList<Instance>();
    protected ArrayList<Instance> resetBatchMajority = new ArrayList<Instance>();

    boolean warning = false;    
    SamoaToWekaInstanceConverter samoaToWeka = new SamoaToWekaInstanceConverter();
    WekaToSamoaInstanceConverter wekaToSamoa = new WekaToSamoaInstanceConverter();        	

	protected double modelInUse = 0.0;
	
	protected int nMinorityTotal;
    protected int nMajorityTotal;
    protected int nGeneratedMinorityTotal;
    protected int nGeneratedMajorityTotal;
    protected HashMap<Instance,Integer> instanceGenerated = new HashMap<Instance,Integer>();
    
    protected ArrayList<Integer> alreadyUsed = new ArrayList<Integer>();        
    protected int effectiveNearestNeighbors;
    protected Instances minorityInstances;
	protected Map vdmMap = new HashMap();
	protected int[] indexValues;
	
    
    @Override
    public void resetLearningImpl() {     	    	
        this.learner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        this.learnerResetBal = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        this.learnerReset = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        this.learnerBal = (Classifier) getPreparedClassOption(this.baseLearnerOption);
     
        this.adwin.resetChange();
        this.nAttributes = -1;
        
        this.learner.resetLearning();
      	clean(this.confusionMatrixLearner,this.accLearner,this.kStatLearner); 

        this.modelInUse = 0.0;
        
        this.nMinorityTotal = 0;
      	this.nMajorityTotal = 0;
      	this.nGeneratedMinorityTotal = 0;
        this.nGeneratedMajorityTotal = 0;        
        
        this.minInstanceLimitBatch = this.minInstanceLimitBatchOption.getValue();
        this.maxInstanceLimitBatch = this.maxInstanceLimitBatchOption.getValue();
        this.minInstanceLimitResetBatch = this.minInstanceLimitResetBatchOption.getValue();
        this.maxInstanceLimitResetBatch = this.maxInstanceLimitResetBatchOption.getValue();
        
        if (this.minInstanceLimitBatch != -1 && this.maxInstanceLimitBatch != -1) {
        	if (this.minInstanceLimitBatch > this.maxInstanceLimitBatch) {
            	System.out.println("The minimum number of instances in the batch cannot be greater than the maximum number allowed");
            	return;
            }
        }
        
        if (this.minInstanceLimitResetBatch != -1 && this.maxInstanceLimitResetBatch != -1) {
        	if (this.minInstanceLimitResetBatch > this.maxInstanceLimitResetBatch) {
            	System.out.println("The minimum number of instances in the resetBatch cannot be greater than the maximum number allowed");
            	return;
            }
        }  
        
        this.effectiveNearestNeighbors = -1;
        this.alreadyUsed.clear();
      	this.minorityInstances = null;      
      	this.vdmMap.clear();
      	this.instanceGenerated.clear();
      	this.indexValues = null;
      	this.classifierRandom = new Random(this.randomSeed);
        
        resetAfterDrift();	 
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {   	
    	if (this.nAttributes == -1) {
    		this.nAttributes = instance.numAttributes();
    		this.indexValues = new int[this.nAttributes];
    		for (int i = 0; i < this.nAttributes; i ++) {
    			this.indexValues[i] = i;
    		}
    	}
    	this.learner.trainOnInstance(instance);
    	fillBatches(instance);
    	this.adwin.setInput(instance.classValue()); 
    	try {
			checkADWINWidth(instance);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	    	
    }    
    
    //save the instance in batch and in resetBatch
    private void fillBatches(Instance instance) {      	
    	this.batch.add(instance);
    	if (instance.classValue() == 1.0) {
    		this.batchMajority.add(instance); 
    		this.nMajorityTotal ++;
        } else {
        	this.batchMinority.add(instance);   
        	this.nMinorityTotal ++;
        }
    	
        if (this.warning == true) {
        	this.resetBatch.add(instance);
        	if (instance.classValue() == 1.0) {
        		this.resetBatchMajority.add(instance);         		
            } else {
            	this.resetBatchMinority.add(instance);               	
            }
        }
    }
    
    private void checkADWINWidth(Instance instance) throws Exception{
    	//check if there is a warning
    	if (this.adwin.getWarning() && this.warning == false) {     	    		
			//if there is, starts saving the samples also in the resetBatch
			this.resetBatch.add(instance);
			if (instance.classValue() == 1.0) {
        		this.resetBatchMajority.add(instance);         		
            } else {
            	this.resetBatchMinority.add(instance);               	
            }
	        this.warning = true;	          
	    } 
    	
    	if (this.adwin.getChange()) {    		
    		boolean minBatchSizeCheck = checkMinConstraints(this.batch.size(),this.minInstanceLimitBatch);
    		boolean minResetBatchSizeCheck = checkMinConstraints(this.resetBatch.size(),this.minInstanceLimitResetBatch);      		    		 		    		
    		//if both batches has reached the minimum size
    		//it starts training the models in parallel
    	    if (minBatchSizeCheck && minResetBatchSizeCheck) {    	    	   	    	
    	    	trainsParallelModels();
    	    }    	    
    		//set the new batch
    		int newWidth = this.adwin.getWidth();
    		int windowSize = this.batch.size();
    		int diff = windowSize - newWidth;    		       	
    		for (int i = 0; i < diff; i ++) {    			
    			Instance instanceRemoved = this.batch.remove(0);
    			if (instanceRemoved.classValue() == 1.0) {
    				this.batchMajority.remove(instanceRemoved);
    				this.nMajorityTotal --;
    				if (this.instanceGenerated.get(instanceRemoved) != null) {
        				this.nGeneratedMajorityTotal -= this.instanceGenerated.get(instanceRemoved);
        				this.instanceGenerated.remove(instanceRemoved);
            		}
    			} else {
    				this.batchMinority.remove(instanceRemoved);
    				this.nMinorityTotal --;
    				if (this.instanceGenerated.get(instanceRemoved) != null) {
        				this.nGeneratedMinorityTotal -= this.instanceGenerated.get(instanceRemoved);
        				this.instanceGenerated.remove(instanceRemoved);
            		}
    			}
    		}    		    		
    	} else {
    		boolean maxBatchSizeCheck = checkMaxConstraints(this.batch.size(),this.maxInstanceLimitBatch);
    		boolean maxResetBatchCheck = checkMaxConstraints(this.resetBatch.size(),this.maxInstanceLimitResetBatch); 	        	
    		//if one of the 2 batches has reached the maximun size
    		//even if there isn't a change, it starts training the models in parallel
        	if (maxBatchSizeCheck || maxResetBatchCheck) {   		        		
    			trainsParallelModels();    			
        	}     		    		    	  		
    	}
    }
    
    private boolean checkMaxConstraints(int size, int maxSize) {
    	if (maxSize != -1 && size >= maxSize) {
    		return true;
    	}     	    	
    	else {
    		return false;
    	}
    }
    
    private boolean checkMinConstraints(int size, int minSize) {
    	if ( (minSize == -1 && size > 0) || (minSize != -1 && size >= minSize) ) {
    		return true;
    	}     	
    	else {
    		return false;
    	}
    }
   
    private void trainsParallelModels() throws Exception {    	
    	//start timer
    	long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
    	
    	//calculate accuracy and kStatistic of learner
    	this.accLearner = calculateAccuracy(this.confusionMatrixLearner);
    	this.kStatLearner = calculateKStatistic(this.confusionMatrixLearner, this.accLearner);   
    	
    	//learnerBal
    	double ratio = calculateRatio(this.nMajorityTotal,this.nMinorityTotal,this.nGeneratedMajorityTotal,this.nGeneratedMinorityTotal); 					
		//if the ratio is less than threshold, 
		//I apply the online SMOTE version 
		//until the ratio will be equal to the threshold
		boolean rebalance = false;		
		if (this.nMinorityTotal > 1 && this.nMajorityTotal > 1) {
			this.learnerBal.prepareForUse();
			while (0.5 > ratio) {
				Instance newInstance;
				rebalance = true;			
		    	//class 0 is the real minority
		    	if ((this.nMinorityTotal + this.nGeneratedMinorityTotal) < (this.nMajorityTotal + this.nGeneratedMajorityTotal)) {
		    		newInstance = generateNewInstance(this.batchMinority,true);
					this.nGeneratedMinorityTotal ++;
				}
				//class 1 is the real minority
				else {
					newInstance = generateNewInstance(this.batchMajority,true);
					this.nGeneratedMajorityTotal ++;			
				}			
				//prequential evaluation
				fillConfusionMatrix(newInstance,this.confusionMatrixBal,this.learnerBal);        				        				        				        				
				//model training
				this.learnerBal.trainOnInstance(newInstance);				
				ratio = calculateRatio(this.nMajorityTotal,this.nMinorityTotal,this.nGeneratedMajorityTotal,this.nGeneratedMinorityTotal);	
			} 
		}

		if (rebalance) {
			//calculate accuracy and KStatistic of learnerBal
			this.accBal = calculateAccuracy(this.confusionMatrixBal);
	    	this.kStatBal = calculateKStatistic(this.confusionMatrixBal, this.accBal);
		} else {
			this.kStatBal = -1;
		}		
    	
		this.alreadyUsed.clear();
		this.effectiveNearestNeighbors = -1;
		this.minorityInstances.clear();
		this.vdmMap.clear();
    	    	
        //resetBatchBal from resetBatch
        Instances resetBatchBal = createRandomInstances();  
        resetBatchBal = fillNewBatch(this.resetBatch);                        
        resetBatchBal.setClassIndex(resetBatchBal.numAttributes() - 1);
        
        //learnerReset on resetBatchBal without smote
        this.learnerReset.prepareForUse();			
		for (int r = 0; r < resetBatchBal.numInstances(); r ++) {
			Instance trainInstBal = this.wekaToSamoa.samoaInstance(resetBatchBal.instance(r));
			//prequential evaluation
			fillConfusionMatrix(trainInstBal,this.confusionMatrixReset,this.learnerReset);
			//model training
			this.learnerReset.trainOnInstance(trainInstBal);
		}
		
		//calculate accuracy and kStatistic of learnerReset
		this.accReset = calculateAccuracy(this.confusionMatrixReset);
    	this.kStatReset = calculateKStatistic(this.confusionMatrixReset, this.accReset); 						                 
        
        //learnerResetBal on resetBatchBal
    	int minGenerated = 0;
		int maxGenerated = 0;
    	ratio = calculateRatio(this.resetBatchMajority.size(),this.resetBatchMinority.size(),maxGenerated,minGenerated); 					
		//if the ratio is less than threshold, 
		//I apply the online SMOTE version 
		//until the ratio will be equal to the threshold
		rebalance = false;	
		if (this.resetBatchMinority.size() > 1 && this.resetBatchMajority.size() > 1) {
			this.learnerResetBal.prepareForUse();
			while (0.5 > ratio) {			
				Instance newInstance;
		    	//class 0 is the real minority
		    	if ((this.resetBatchMinority.size()+ minGenerated) < (this.resetBatchMajority.size() + maxGenerated)) {	    		
	    			newInstance = generateNewInstance(this.resetBatchMinority,false);
		    		minGenerated ++;		    			    		    	
				}
				//class 1 is the real minority
				else {				
					newInstance = generateNewInstance(this.resetBatchMajority,false);
					maxGenerated ++;														
				}			
				//prequential evaluation				
				fillConfusionMatrix(newInstance,this.confusionMatrixResetBal,this.learnerResetBal);        				        				        				        				
				//model training
				this.learnerResetBal.trainOnInstance(newInstance);				
				ratio = calculateRatio(this.resetBatchMajority.size(),this.resetBatchMinority.size(),maxGenerated,minGenerated);	
			} 
		}

		if (rebalance) {
			//calculate accuracy and kStatistic of learnerResetBal
			this.accResetBal = calculateAccuracy(this.confusionMatrixResetBal);
        	this.kStatResetBal = calculateKStatistic(this.confusionMatrixResetBal, this.accResetBal); 
		} else {
			this.kStatResetBal = -1;
		}		
    	
		this.alreadyUsed.clear();
		this.effectiveNearestNeighbors = -1;
		this.minorityInstances.clear();
		this.vdmMap.clear();
        
    	//find the best model based on kStatistic
    	int maxPos = findMaxKStatistic();  
    	//save the best model in the learner
    	swipeModelInUse(maxPos);  
    	//reset the model created
    	resetAfterDrift();    	
    }
    
    private Instance generateNewInstance(ArrayList<Instance> minoritySamples, boolean newInstanceBatch) {     	     	   
    	//check if I need to check the neighbors and fill the Value Distance Metric matrices
    	if (this.effectiveNearestNeighbors == -1) {
    		setParameters(minoritySamples);
    	}
    	         
        Instance[] nnArray = new Instance[1];
        try {
        	nnArray = new Instance[this.effectiveNearestNeighbors];
		} catch (Exception e) {
			// TODO: handle exception			
		}
        
        
        //find randomly an instance
        int pos = 0;
        try {
        	pos = this.classifierRandom.nextInt(this.minorityInstances.numInstances());
		} catch (Exception e) {			
		}
        
        while (this.alreadyUsed.contains(pos)) {
        	pos = this.classifierRandom.nextInt(this.minorityInstances.numInstances());
        }
        this.alreadyUsed.add(pos);
        if (this.alreadyUsed.size() == minoritySamples.size()) {
        	this.alreadyUsed.clear();
        }
        
    	Instance instanceI = this.wekaToSamoa.samoaInstance(this.minorityInstances.instance(pos));
    	
    	//find k nearest neighbors for the chosen instance
    	List distanceToInstance = new LinkedList();
    	for (int j = 0; j < this.minorityInstances.numInstances(); j++) {
    		Instance instanceJ = this.wekaToSamoa.samoaInstance(this.minorityInstances.instance(j));
    		if (pos != j) {
    			double distance = 0;
    			Enumeration attrEnum = this.minorityInstances.enumerateAttributes();
    			while(attrEnum.hasMoreElements()) {
    				Attribute attr = (Attribute) attrEnum.nextElement();
    				if (!attr.equals(this.minorityInstances.classAttribute())) {
    					double iVal = this.samoaToWeka.wekaInstance(instanceI).value(attr);
    					double jVal = this.samoaToWeka.wekaInstance(instanceJ).value(attr);
    					if (attr.isNumeric()) {
    						distance += Math.pow(iVal - jVal, 2);
    					} else {
    						distance += ((double[][]) this.vdmMap.get(attr))[(int) iVal][(int) jVal];
    					}
    				}
    			}
    			distance = Math.pow(distance, .5);
    			distanceToInstance.add(new Object[] {distance, instanceJ});
    		}
    	}

    	// sort the neighbors according to distance
    	Collections.sort(distanceToInstance, new Comparator() {
    		public int compare(Object o1, Object o2) {
    			double distance1 = (Double) ((Object[]) o1)[0];
    			double distance2 = (Double) ((Object[]) o2)[0];
    			return Double.compare(distance1, distance2);
    		}
    	});

    	// populate the actual nearest neighbor instance array
    	Iterator entryIterator = distanceToInstance.iterator();
    	int j = 0;
    	while(entryIterator.hasNext() && j < this.effectiveNearestNeighbors) {
    		nnArray[j] = (Instance) ((Object[])entryIterator.next())[1];
    		j++;
    	}

    	// create synthetic sample    	
		double[] values = new double[this.minorityInstances.numAttributes()];
		int nn = this.classifierRandom.nextInt(this.effectiveNearestNeighbors);
		Enumeration attrEnum = this.minorityInstances.enumerateAttributes();
		while(attrEnum.hasMoreElements()) {
			Attribute attr = (Attribute) attrEnum.nextElement();
			if (!attr.equals(this.minorityInstances.classAttribute())) {
				if (attr.isNumeric()) {
					double dif = this.samoaToWeka.wekaInstance(nnArray[nn]).value(attr) - this.samoaToWeka.wekaInstance(instanceI).value(attr);
					double gap = this.classifierRandom.nextDouble();
					values[attr.index()] = (double) (this.samoaToWeka.wekaInstance(instanceI).value(attr) + gap * dif);
				} else if (attr.isDate()) {
					double dif = this.samoaToWeka.wekaInstance(nnArray[nn]).value(attr) - this.samoaToWeka.wekaInstance(instanceI).value(attr);
					double gap = this.classifierRandom.nextDouble();
					values[attr.index()] = (long) (this.samoaToWeka.wekaInstance(instanceI).value(attr) + gap * dif);
				} else {
					int[] valueCounts = new int[attr.numValues()];
					int iVal = (int) this.samoaToWeka.wekaInstance(instanceI).value(attr);
					valueCounts[iVal]++;
					for (int nnEx = 0; nnEx < this.effectiveNearestNeighbors; nnEx++) {
						int val = (int) this.samoaToWeka.wekaInstance(nnArray[nnEx]).value(attr);
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
		values[this.minorityInstances.classIndex()] = instanceI.classValue();						
		
		Instance synthetic = instanceI.copy();
		synthetic.addSparseValues(this.indexValues, values, this.nAttributes);
		
		if (newInstanceBatch == true) {
			if (this.instanceGenerated.get(instanceI) != null) {
	    		this.instanceGenerated.replace(instanceI, this.instanceGenerated.get(instanceI)+1);
	    	} else {
	    		this.instanceGenerated.put(instanceI, 1);
	    	}
		}
		
		
		return synthetic;	
    }
    
    private void setParameters(ArrayList<Instance> minoritySamples) {
    	if (5 >= minoritySamples.size()) {
    		this.effectiveNearestNeighbors = minoritySamples.size() -1;
    	} else {
    		this.effectiveNearestNeighbors = 5;
    	}
       	        	    	
    	this.minorityInstances = fillNewBatch(minoritySamples);
    	this.minorityInstances.setClassIndex(this.minorityInstances.numAttributes() - 1);
    	    	    	
    	//compute Value Distance Metric matrices for nominal features                   
    	Enumeration attrEnum = this.minorityInstances.enumerateAttributes();        
        while(attrEnum.hasMoreElements()) {        	
        	Attribute attr = (Attribute) attrEnum.nextElement();
        	if (!attr.equals(this.minorityInstances.classAttribute())) {
        		if (attr.isNominal() || attr.isString()) {
        			double[][] vdm = new double[attr.numValues()][attr.numValues()];
        			this.vdmMap.put(attr, vdm);
        			int[] featureValueCounts = new int[attr.numValues()];
        			int[][] featureValueCountsByClass = new int[this.minorityInstances.classAttribute().numValues()][attr.numValues()];
        			Enumeration instanceEnum = this.minorityInstances.enumerateInstances();
        			while(instanceEnum.hasMoreElements()) {        				
        				Instance instance = (Instance) instanceEnum.nextElement();
        				int value = (int) this.samoaToWeka.wekaInstance(instance).value(attr);
        				int classValue = (int) instance.classValue();
        				featureValueCounts[value]++;
        				featureValueCountsByClass[classValue][value]++;
        			}
        			for (int valueIndex1 = 0; valueIndex1 < attr.numValues(); valueIndex1++) {
        				for (int valueIndex2 = 0; valueIndex2 < attr.numValues(); valueIndex2++) {
        					double sum = 0;
        					for (int classValueIndex = 0; classValueIndex < this.minorityInstances.numClasses(); classValueIndex++) {
        						double c1i = (double) featureValueCountsByClass[classValueIndex][valueIndex1];
        						double c2i = (double) featureValueCountsByClass[classValueIndex][valueIndex2];
        						double c1 = (double) featureValueCounts[valueIndex1];
        						double c2 = (double) featureValueCounts[valueIndex2];
        						double term1 = c1i / c1;
        						double term2 = c2i / c2;
        						sum += Math.abs(term1 - term2);
        					}
        					vdm[valueIndex1][valueIndex2] = sum;
        				}
        			}
        		}
        	}
        }
    }
    
    private double calculateRatio(int nMajority, int nMinority, int nMajorityGenerated, int nMinorityGenerated) {
    	double ratio = 0.0;
    	//class 0 is the real minority
		if ((nMinority + nMinorityGenerated) <= (nMajority + nMajorityGenerated)) {
			ratio = ( (double) nMinority + (double) nMinorityGenerated ) / ( (double) nMinority + (double) nMinorityGenerated + (double) nMajority + (double) nMajorityGenerated );			
		}
		//class 1 is the real minority
		else {
			ratio = ( (double) nMajority + (double) nMajorityGenerated ) / ( (double) nMinority + (double) nMinorityGenerated + (double) nMajority + (double) nMajorityGenerated );			
		}
    	 
    	return ratio;
    }
    
    //create the dateset of instances with 2 class 
    //and the number of attributes chosen in the stream configuration
    private Instances createRandomInstances() {
		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		ArrayList<String> label = new ArrayList<String>();
		
		for (int i = 1; i <= this.nAttributes-1; i ++) {
			atts.add(new Attribute("att" + i));
    	
			if (i < 3) {
				label.add(Integer.toString(i-1));
			}
		}
		atts.add(new Attribute("label",label));
   
		Instances data = new Instances("BatchBalance",atts,0);
		return data;
	}
    
    //clean the matrix, accuracy and kStatistic selected in input
    private void clean(int[][] confusionMatrix, double accuracy, double kStatistic) {
    	for (int r = 0; r < 2; r++) {
			for (int c = 0; c < 2; c++) {				
				confusionMatrix[r][c] = 0;
			}	
		}
				
    	accuracy = 0;
    	kStatistic = 0;	
    } 
    
    //reset the structures after a drift occours
    private void resetAfterDrift() {
    	this.learnerBal.resetLearning();
		this.learnerReset.resetLearning();
		this.learnerResetBal.resetLearning();   
		clean(this.confusionMatrixResetBal, this.accResetBal, this.kStatResetBal);
        clean(this.confusionMatrixReset, this.accReset, this.kStatReset);            	
    	clean(this.confusionMatrixBal,this.accBal,this.kStatBal); 
    	this.resetBatch.clear();
    	this.resetBatchMinority.clear();
    	this.resetBatchMajority.clear();
        this.warning = false;  	
    }
    
    //calculate accuracy of the confusion matrix in input
    private double calculateAccuracy(int[][] confusionMatrix) {
    	int numberSamplesCorrect = confusionMatrix[0][0] + confusionMatrix[1][1];
		int numberSamples = confusionMatrix[0][0] + confusionMatrix[1][1] + confusionMatrix[0][1] + confusionMatrix[1][0];
        double accuracy = (double) numberSamplesCorrect/ (double) numberSamples;
        
        return accuracy;
    }
    
    //calculate kStatistic of the confusion matrix in input
    private double calculateKStatistic(int[][] confusionMatrix, double accuracy) {
    	int numberSamples = confusionMatrix[0][0] + confusionMatrix[1][1] + confusionMatrix[0][1] + confusionMatrix[1][0];
    	double p0 = (((double)confusionMatrix[0][0] + (double)confusionMatrix[0][1]) / (double) numberSamples) * (((double)confusionMatrix[0][0] + (double)confusionMatrix[1][0]) / (double) numberSamples);
        double p1 = (((double)confusionMatrix[1][0] + (double)confusionMatrix[1][1]) / (double) numberSamples) * (((double)confusionMatrix[0][1] + (double)confusionMatrix[1][1]) / (double) numberSamples);
        double pc = p0 + p1;
        double kStatistic = (double)(accuracy - pc) / (double)(1 - pc);
         
        return kStatistic;
    }
    
    //create a dataset of instances ready to iterate
    private Instances fillNewBatch(ArrayList<Instance> batch) {
    	Instances newBatch = createRandomInstances();
    	for (int l = 0; l < batch.size(); l ++) {
        	newBatch.add(this.samoaToWeka.wekaInstance(batch.get(l)));                    	
        }
    	
    	return newBatch;
    }
    
    //find the model with the highest kStatistic
    private int findMaxKStatistic() {
    	ArrayList<Double> compare = new ArrayList<Double>();
    	compare.add(0,this.kStatLearner);
    	compare.add(1,this.kStatBal);
    	compare.add(2,this.kStatReset);
    	compare.add(3,this.kStatResetBal);
    	
    	double max = compare.get(0);
    	int maxPos = 0;
    	for (int pos = 0; pos < compare.size(); pos ++) {
    		if (compare.get(pos) > max) {
    			max = compare.get(pos);
    			maxPos = pos;
    		}
    	}
    	
    	return maxPos;
    }	
    
    //check which is the best model trained
    private void swipeModelInUse(int maxPos) {
    	if (maxPos == 0) { //learner is the best
    		this.modelInUse = (double) maxPos;        	
    	}
    	else if (maxPos == 1) { //learnerBal is the best
    		this.modelInUse = (double) maxPos;
    		copyInLearner(this.learnerBal,this.confusionMatrixBal);  
    	}
    	else if (maxPos == 2) { //learnerReset is the best
    		this.modelInUse = (double) maxPos;
    		copyInLearner(this.learnerReset,this.confusionMatrixReset);	
    	}
    	else { //learnerResetBal is the best
    		this.modelInUse = (double) maxPos;
    		copyInLearner(this.learnerResetBal,this.confusionMatrixResetBal);              	
    	}         
    }
    
    //copy the best learner and its confusion matrix into the learner and its confusion matrix
    private void copyInLearner(Classifier learnerSelected ,int[][] confusionMatrixSelected) {
    	this.learner = (Classifier) learnerSelected.copy();		        
        for(int r = 0; r < 2; r ++){
    	    for(int c = 0; c < 2 ; c ++){
    	    	this.confusionMatrixLearner[r][c] = confusionMatrixSelected[r][c];
    	    }
    	} 
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
    	double[] prediction = this.learner.getVotesForInstance(instance);
    	fillConfusionMatrix(instance, this.confusionMatrixLearner, this.learner);
        return prediction;
    }
    
    private void fillConfusionMatrix(Instance instance, int[][] confusionMatrix, Classifier learner) {    	
   	 /*					learner.correctlyClassifies(trainInst)
					pred 0.0	pred 1.0
		trainInst.classValue()
		correct 0.0
		correct 1.0
		*/	   	
		if (learner.correctlyClassifies(instance) && instance.classValue() == 0.0){
			confusionMatrix[0][0] ++;					                    	
		}
		else if (learner.correctlyClassifies(instance) && instance.classValue() == 1.0){
			confusionMatrix[1][1] ++;			                   	
		}
		else if (!learner.correctlyClassifies(instance) && instance.classValue() == 0.0){
			confusionMatrix[0][1] ++;					                    
		}
		else if (!learner.correctlyClassifies(instance) && instance.classValue() == 1.0){
			confusionMatrix[1][0] ++;			                    	
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
    	return "RebalanceStream strategy trains in parallel four models and each one of them uses a different batch of data";
    }
}
