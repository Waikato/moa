/*
 *  OnlineUnderOverBagging.java
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
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import java.util.ArrayList;
import java.util.Random;

import moa.classifiers.core.driftdetection.ADWIN;



/**
 *  Online UnderOverBagging is the online version of the ensemble method.
 *
 * <p>In case of imbalanced classes UnderOverBagging uses the strategy of under-sampling
     the majority class and oversampling the minority class. In addition the sampling
     rate can be also varied over the bagging iterations, which further boosts the
     diversity of the base learners. <p>

   <p>The derivation of the online UnderOverBagging algorithm is made through the observation
     that a Binomial distribution with sampling rate :math:`\frac{C}{N}` corresponds to a
     poisson distribution with :math:`\lambda=C`.</p>

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
 * <li>-i : The sampling rate of the positive instances.</li>
 * <li>-d : Should use ADWIN as drift detector? If enabled it is used by the method 
 * 	to track the performance of the classifiers and adapt when a drift is detected.</li>
 * <li>-r : Seed for the random state.</li>
 * </ul>
 *
 * @author Alessio Bernardo (alessio dot bernardo at polimi dot dot it)
 * @version $Revision: 1 $
 */
public class OnlineUnderOverBagging extends AbstractClassifier implements MultiClassClassifier,
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
    
    public IntOption samplingRateOption = new IntOption("samplingRate", 'i',
            "The sampling rate of the positive instances.", 2, 1, 10);
    
    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'd',
            "Should use ADWIN as drift detector?");
    
    protected Classifier baseLearner;
    protected int nEstimators;    
    protected int samplingRate; 
    protected boolean driftDetection;        
    protected ArrayList<Classifier> ensemble;
    protected ArrayList<ADWIN> adwinEnsemble;     
    
    @Override
    public void resetLearningImpl() {
        // Reset attributes
    	this.baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
    	this.baseLearner.resetLearning();
        this.nEstimators = this.ensembleSizeOption.getValue();        
        this.samplingRate = this.samplingRateOption.getValue();
        this.driftDetection = !this.disableDriftDetectionOption.isSet();
        this.ensemble = new ArrayList<Classifier>();
        if (this.driftDetection) {
            this.adwinEnsemble = new ArrayList<ADWIN>();
        }
        for (int i = 0; i < this.nEstimators; i++) {
        	this.ensemble.add(this.baseLearner.copy());         	        
        	if (this.driftDetection) {
        		this.adwinEnsemble.add(new ADWIN());
        	}        	
		} 
        this.classifierRandom = new Random(this.randomSeed);
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {        
        if(this.ensemble.isEmpty()) {
        	resetLearningImpl();
        }  
        adjustEnsembleSize(instance.numClasses());
                
        boolean changeDetected = false;        
        double lambda = 0.0;
        
        for (int i = 0 ; i < this.ensemble.size(); i++) {
        	double a = (double)(i + 1) / (double)this.nEstimators;
        	if (instance.classValue() == 1.0) {
        		lambda = a * this.samplingRate;
        	}
        	else {
        		lambda = a;
        	}        	        				
			double k = MiscUtils.poisson(lambda, this.classifierRandom);
			if (k > 0) {
				for (int b = 0; b < k; b++) {
					this.ensemble.get(i).trainOnInstance(instance);					
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
			}
    	}
    }
    
    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == OnlineUnderOverBagging.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
