/*
 *  OnlineAdaC2.java
 *  
 *  @author Alessio Bernardo (alessio dot bernardo at polimi dot dot it)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
        
 */
package moa.classifiers.meta.imbalanced;

import com.yahoo.labs.samoa.instances.Instance;


import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.core.Utils;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import java.util.ArrayList;
import java.util.Random;

import moa.classifiers.core.driftdetection.ADWIN;


/**
 *  OnlineAdaC2 is the adaptation of the ensemble learner to data streams
 *
 * <p>AdaC2 is a boosting algorithm that takes the different misclassification costs
    into consideration when calculating the classier weights, and updates the sample
    weight. AdaC2 increases more the weights on the misclassified positive samples,
    compared to misclassified negative samples.:math:`C_P` and :math:`C_N` are the
    positive and negative costs.</p>

    <p>This online ensemble learner method is improved by the addition of an ADWIN change
    detector. ADWIN stands for Adaptive Windowing. It works by keeping updated
    statistics of a variable sized window, so it can detect changes and
    perform cuts in its window to better adapt the learning algorithms.</p>
 *
 * <p>See details in:<br> B. Wang and J. Pineau, "Online Bagging and Boosting for Imbalanced Data Streams,"
       in IEEE Transactions on Knowledge and Data Engineering, vol. 28, no. 12, pp.
       3353-3366, 1 Dec. 2016. doi: 10.1109/TKDE.2016.2609424</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Each classiﬁer to train of the ensemble is an instance of the base estimator.</li>
 * <li>-s : The size of the ensemble, in other words, how many classifiers to train.</li>
 * <li>-p : The cost of misclassifying a positive sample.</li>
 * <li>-n : The cost of misclassifying a negative sample.</li>
 * <li>-d : Should use ADWIN as drift detector? If enabled it is used by the method 
 * 	to track the performance of the classifiers and adapt when a drift is detected.</li>
 * <li>-r : Seed for the random state.</li>
 * </ul>
 *
 * @author Alessio Bernardo (alessio dot bernardo at polimi dot dot it)
 * @version $Revision: 1 $
 */
public class OnlineAdaC2 extends AbstractClassifier implements MultiClassClassifier,
                                                                        CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "OnlineAdaC2 is the adaptation of the ensemble learner to data streams from B. Wang and J. Pineau";
    }
    
    private static final long serialVersionUID = 1L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "meta.AdaptiveRandomForest");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
        "The size of the ensemble.", 10, 1, Integer.MAX_VALUE);        
    
    public FloatOption costPositiveOption = new FloatOption("costPositive", 'p',
        "The cost of misclassifying a positive sample.", 1, 0.1, 1);
    
    public FloatOption costNegativeOption = new FloatOption("costNegative", 'n',
            "The cost of misclassifying a negative sample.", 0.1, 0.1, 1);
    
    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'd',
            "Should use ADWIN as drift detector?");
    
    protected Classifier baseLearner;
    protected int nEstimators;    
    protected double costPositive;
    protected double costNegative;
    protected boolean driftDetection;        
    protected ArrayList<Classifier> ensemble;
    protected ArrayList<ADWIN> adwinEnsemble;
    protected ArrayList<Double> lambdaTP;
    protected ArrayList<Double> lambdaTN;
    protected ArrayList<Double> lambdaFP;
    protected ArrayList<Double> lambdaFN;
    protected ArrayList<Double> lambdaSum;
    protected ArrayList<Double> wAcc;
    protected ArrayList<Double> wErr;
    
    @Override
    public void resetLearningImpl() {
        // Reset attributes
    	this.baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
    	this.baseLearner.resetLearning();
        this.nEstimators = this.ensembleSizeOption.getValue();        
        this.costPositive = this.costPositiveOption.getValue();
        this.costNegative = this.costNegativeOption.getValue();
        this.driftDetection = !this.disableDriftDetectionOption.isSet();
        this.ensemble = new ArrayList<Classifier>();
        if (this.driftDetection) {
            this.adwinEnsemble = new ArrayList<ADWIN>();
        }
        this.lambdaTP = new ArrayList<Double>();
        this.lambdaTN = new ArrayList<Double>();
        this.lambdaFP = new ArrayList<Double>();
        this.lambdaFN = new ArrayList<Double>();
        this.lambdaSum = new ArrayList<Double>();
        this.wAcc = new ArrayList<Double>();
        this.wErr = new ArrayList<Double>();
        for (int i = 0; i < this.nEstimators; i++) {
        	this.ensemble.add(this.baseLearner.copy());         	        
        	if (this.driftDetection) {
        		this.adwinEnsemble.add(new ADWIN());
        	}
        	this.lambdaTP.add(0.0);
            this.lambdaTN.add(0.0);
            this.lambdaFP.add(0.0);
            this.lambdaFN.add(0.0);
            this.lambdaSum.add(0.0);
            this.wAcc.add(0.0);
            this.wErr.add(0.0);               
		}                   
        this.classifierRandom = new Random(this.randomSeed);
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {        
        if(this.ensemble.isEmpty()) {
        	resetLearningImpl();
        }  
        adjustEnsembleSize(instance.numClasses());
        
        double lambda = 1.0;
        boolean changeDetected = false;        
        
        for (int i = 0 ; i < this.ensemble.size(); i++) {
			this.lambdaSum.set(i, this.lambdaSum.get(i) + lambda);
			double k = MiscUtils.poisson(lambda, this.classifierRandom);			
			if (k > 0) {
				for (int b = 0; b < k; b++) {
					this.ensemble.get(i).trainOnInstance(instance);					
				}
				if (Utils.maxIndex(this.ensemble.get(i).getVotesForInstance(instance)) == 1.0 && instance.classValue() == 1.0) {
					this.lambdaTP.set(i, this.lambdaTP.get(i) + (this.costPositive * lambda));
					this.wAcc.set(i, (this.lambdaTP.get(i) + this.lambdaTN.get(i)) / this.lambdaSum.get(i));
					this.wErr.set(i, (this.lambdaFP.get(i) + this.lambdaFN.get(i)) / this.lambdaSum.get(i));
					lambda = (this.costPositive * lambda) / (2 * this.wAcc.get(i));
				}
				else if (Utils.maxIndex(this.ensemble.get(i).getVotesForInstance(instance)) == 0.0 && instance.classValue() == 0.0) {
					this.lambdaTN.set(i, this.lambdaTN.get(i) + (this.costNegative * lambda));
					this.wAcc.set(i, (this.lambdaTP.get(i) + this.lambdaTN.get(i)) / this.lambdaSum.get(i));
					this.wErr.set(i, (this.lambdaFP.get(i) + this.lambdaFN.get(i)) / this.lambdaSum.get(i));
					lambda = (this.costNegative * lambda) / (2 * this.wAcc.get(i));
				}
				else if (Utils.maxIndex(this.ensemble.get(i).getVotesForInstance(instance)) == 0.0 && instance.classValue() == 1.0) {
					this.lambdaFN.set(i, this.lambdaFN.get(i) + (this.costPositive * lambda));
					this.wAcc.set(i, (this.lambdaTP.get(i) + this.lambdaTN.get(i)) / this.lambdaSum.get(i));
					this.wErr.set(i, (this.lambdaFP.get(i) + this.lambdaFN.get(i)) / this.lambdaSum.get(i));
					lambda = (this.costPositive * lambda) / (2 * this.wErr.get(i));
				}
				else if (Utils.maxIndex(this.ensemble.get(i).getVotesForInstance(instance)) == 1.0 && instance.classValue() == 0.0) {
					this.lambdaFP.set(i, this.lambdaFP.get(i) + (this.costNegative * lambda));
					this.wAcc.set(i, (this.lambdaTP.get(i) + this.lambdaTN.get(i)) / this.lambdaSum.get(i));
					this.wErr.set(i, (this.lambdaFP.get(i) + this.lambdaFN.get(i)) / this.lambdaSum.get(i));
					lambda = (this.costNegative * lambda) / (2 * this.wErr.get(i));
				}
			}
			if (this.driftDetection) {
				double pred = Utils.maxIndex(this.ensemble.get(i).getVotesForInstance(instance));
				double errorEstimation = this.adwinEnsemble.get(i).getEstimation();
				double inputValue = pred == instance.classValue() ? 1.0 : 0.0;
				boolean resInput = this.adwinEnsemble.get(i).setInput(inputValue);
				if (resInput) {
					if (this.adwinEnsemble.get(i).getEstimation() > errorEstimation) {
						changeDetected = true;
					}
				}
			}
		}
        
        if (changeDetected && this.driftDetection) {
        	double maxThreshold = 0.0;
        	int iMax = -1;
        	for (int i = 0; i < this.ensemble.size(); i++) {
				if (maxThreshold < this.adwinEnsemble.get(i).getEstimation()) {
					maxThreshold = this.adwinEnsemble.get(i).getEstimation();
					iMax = i;
				}
			}
        	if (iMax != -1) {
        		this.ensemble.get(iMax).resetLearning();
        		this.adwinEnsemble.set(iMax,new ADWIN());
        	}
        }     
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        Instance testInstance = instance.copy();        
        DoubleVector combinedVote = new DoubleVector();

        for(int i = 0 ; i < this.ensemble.size() ; ++i) {
            DoubleVector vote = new DoubleVector(this.ensemble.get(i).getVotesForInstance(testInstance));
            if (vote.sumOfValues() > 0.0) {                                                                                  
                for(int v = 0 ; v < vote.numValues() ; ++v) {
                    vote.setValue(v, vote.getValue(v) * Math.log(this.wAcc.get(i) / this.wErr.get(i)));
                }                                
            	vote.normalize();
                combinedVote.addValues(vote);                
            }
        }
        return combinedVote.getArrayRef();
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public void getModelDescription(StringBuilder arg0, int arg1) {
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }
    
    protected void adjustEnsembleSize(int nClasses) {
    	if (nClasses > this.nEstimators) {
    		for (int i = this.nEstimators; i < nClasses; i++) {
    			this.ensemble.add(this.baseLearner.copy()); 
    			this.nEstimators ++;
                if (this.driftDetection) {
                    this.adwinEnsemble.add(new ADWIN());
                }
    			this.lambdaTP.add(0.0);
                this.lambdaTN.add(0.0);
                this.lambdaFP.add(0.0);
                this.lambdaFN.add(0.0);
                this.lambdaSum.add(0.0);
                this.wAcc.add(0.0);
                this.wErr.add(0.0);  
			}
    	}
    }    

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == OnlineAdaC2.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
