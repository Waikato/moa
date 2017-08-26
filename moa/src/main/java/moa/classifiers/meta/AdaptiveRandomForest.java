/*
 *    AdaptiveRandomForest.java
 * 
 *    @author Heitor Murilo Gomes (heitor_murilo_gomes at yahoo dot com dot br)
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
package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import moa.classifiers.trees.ARFHoeffdingTree;
import moa.evaluation.BasicClassificationPerformanceEvaluator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import moa.classifiers.core.driftdetection.ChangeDetector;


/**
 * Adaptive Random Forest
 *
 * <p>Adaptive Random Forest (ARF). The 3 most important aspects of this 
 * ensemble classifier are: (1) inducing diversity through resampling;
 * (2) inducing diversity through randomly selecting subsets of features for 
 * node splits (See moa.classifiers.trees.ARFHoeffdingTree.java); (3) drift 
 * detectors per base tree, which cause selective resets in response to drifts. 
 * It also allows training background trees, which start training if a warning
 * is detected and replace the active tree if the warning escalates to a drift. </p>
 *
 * <p>See details in:<br> Heitor Murilo Gomes, Albert Bifet, Jesse Read, 
 * Jean Paul Barddal, Fabricio Enembreck, Bernhard Pfharinger, Geoff Holmes, 
 * Talel Abdessalem. Adaptive random forests for evolving data stream classification. 
 * In Machine Learning, DOI: 10.1007/s10994-017-5642-8, Springer, 2017.</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classiﬁer to train. Must be set to ARFHoeffdingTree</li>
 * <li>-s : The number of trees in the ensemble</li>
 * <li>-o : How the number of features is interpreted (4 options): 
 * "Specified m (integer value)", "sqrt(M)+1", "M-(sqrt(M)+1)"</li>
 * <li>-m : Number of features allowed considered for each split. Negative 
 * values corresponds to M - m</li>
 * <li>-a : The lambda value for bagging (lambda=6 corresponds to levBag)</li>
 * <li>-j : Number of threads to be used for training</li>
 * <li>-x : Change detector for drifts and its parameters</li>
 * <li>-p : Change detector for warnings (start training bkg learner)</li>
 * <li>-w : Should use weighted voting?</li>
 * <li>-u : Should use drift detection? If disabled then bkg learner is also disabled</li>
 * <li>-q : Should use bkg learner? If disabled then reset tree immediately</li>
 * </ul>
 *
 * @author Heitor Murilo Gomes (heitor_murilo_gomes at yahoo dot com dot br)
 * @version $Revision: 1 $
 */
public class AdaptiveRandomForest extends AbstractClassifier implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "Adaptive Random Forest algorithm for evolving data streams from Gomes et al.";
    }
    
    private static final long serialVersionUID = 1L;

    public ClassOption treeLearnerOption = new ClassOption("treeLearner", 'l',
            "Random Forest Tree.", ARFHoeffdingTree.class,
            "ARFHoeffdingTree -e 2000000 -g 50 -c 0.01");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
        "The number of trees.", 10, 1, Integer.MAX_VALUE);
    
    public MultiChoiceOption mFeaturesModeOption = new MultiChoiceOption("mFeaturesMode", 'o', 
        "Defines how m, defined by mFeaturesPerTreeSize, is interpreted. M represents the total number of features.",
        new String[]{"Specified m (integer value)", "sqrt(M)+1", "M-(sqrt(M)+1)",
            "Percentage (M * (m / 100))"},
        new String[]{"SpecifiedM", "SqrtM1", "MSqrtM1", "Percentage"}, 1);
    
    public IntOption mFeaturesPerTreeSizeOption = new IntOption("mFeaturesPerTreeSize", 'm',
        "Number of features allowed considered for each split. Negative values corresponds to M - m", 2, Integer.MIN_VALUE, Integer.MAX_VALUE);
    
    public FloatOption lambdaOption = new FloatOption("lambda", 'a',
        "The lambda parameter for bagging.", 6.0, 1.0, Float.MAX_VALUE);

    public IntOption numberOfJobsOption = new IntOption("numberOfJobs", 'j',
        "Total number of concurrent jobs used for processing (-1 = as much as possible, 0 = do not use multithreading)", 1, -1, Integer.MAX_VALUE);
    
    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
        "Change detector for drifts and its parameters", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-5");

    public ClassOption warningDetectionMethodOption = new ClassOption("warningDetectionMethod", 'p',
        "Change detector for warnings (start training bkg learner)", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-4");
    
    public FlagOption disableWeightedVote = new FlagOption("disableWeightedVote", 'w', 
            "Should use weighted voting?");
    
    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'u',
        "Should use drift detection? If disabled then bkg learner is also disabled");

    public FlagOption disableBackgroundLearnerOption = new FlagOption("disableBackgroundLearner", 'q', 
        "Should use bkg learner? If disabled then reset tree immediately.");
    
    protected static final int FEATURES_M = 0;
    protected static final int FEATURES_SQRT = 1;
    protected static final int FEATURES_SQRT_INV = 2;
    protected static final int FEATURES_PERCENT = 3;
    
    protected static final int SINGLE_THREAD = 0;
	
    protected ARFBaseLearner[] ensemble;
    protected long instancesSeen;
    protected int subspaceSize;
    protected BasicClassificationPerformanceEvaluator evaluator;

    private ExecutorService executor;
    
    @Override
    public void resetLearningImpl() {
        // Reset attributes
        this.ensemble = null;
        this.subspaceSize = 0;
        this.instancesSeen = 0;
        this.evaluator = new BasicClassificationPerformanceEvaluator();
        
        // Multi-threading
        int numberOfJobs;
        if(this.numberOfJobsOption.getValue() == -1) 
            numberOfJobs = Runtime.getRuntime().availableProcessors();
        else 
            numberOfJobs = this.numberOfJobsOption.getValue();
        // SINGLE_THREAD and requesting for only 1 thread are equivalent. 
        // this.executor will be null and not used...
        if(numberOfJobs != AdaptiveRandomForest.SINGLE_THREAD && numberOfJobs != 1)
            this.executor = Executors.newFixedThreadPool(numberOfJobs);
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        if(this.ensemble == null) 
            initEnsemble(instance);
        
        Collection<TrainingRunnable> trainers = new ArrayList<TrainingRunnable>();
        for (int i = 0 ; i < this.ensemble.length ; i++) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(instance));
            InstanceExample example = new InstanceExample(instance);
            this.ensemble[i].evaluator.addResult(example, vote.getArrayRef());
            int k = MiscUtils.poisson(this.lambdaOption.getValue(), this.classifierRandom);
            if (k > 0) {
                if(this.executor != null) {
                    TrainingRunnable trainer = new TrainingRunnable(this.ensemble[i], 
                        instance, k, this.instancesSeen);
                    trainers.add(trainer);
                }
                else { // SINGLE_THREAD is in-place... 
                    this.ensemble[i].trainOnInstance(instance, k, this.instancesSeen);
                }
            }
        }
        if(this.executor != null) {
            try {
                this.executor.invokeAll(trainers);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Could not call invokeAll() on training threads.");
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        Instance testInstance = instance.copy();
        if(this.ensemble == null) 
            initEnsemble(testInstance);
        DoubleVector combinedVote = new DoubleVector();

        for(int i = 0 ; i < this.ensemble.length ; ++i) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(testInstance));
            if (vote.sumOfValues() > 0.0) {
                vote.normalize();
                double acc = this.ensemble[i].evaluator.getPerformanceMeasurements()[1].getValue();
                if(! this.disableWeightedVote.isSet() && acc > 0.0) {                        
                    for(int v = 0 ; v < vote.numValues() ; ++v) {
                        vote.setValue(v, vote.getValue(v) * acc);
                    }
                }
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

    protected void initEnsemble(Instance instance) {
        // Init the ensemble.
        int ensembleSize = this.ensembleSizeOption.getValue();
        this.ensemble = new ARFBaseLearner[ensembleSize];
        
        // TODO: this should be an option with default = BasicClassificationPerformanceEvaluator
//        BasicClassificationPerformanceEvaluator classificationEvaluator = (BasicClassificationPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
        BasicClassificationPerformanceEvaluator classificationEvaluator = new BasicClassificationPerformanceEvaluator();
        
        this.subspaceSize = this.mFeaturesPerTreeSizeOption.getValue();
  
        // The size of m depends on:
        // 1) mFeaturesPerTreeSizeOption
        // 2) mFeaturesModeOption
        int n = instance.numAttributes()-1; // Ignore class label ( -1 )
        
        switch(this.mFeaturesModeOption.getChosenIndex()) {
            case AdaptiveRandomForest.FEATURES_SQRT:
                this.subspaceSize = (int) Math.round(Math.sqrt(n)) + 1;
                break;
            case AdaptiveRandomForest.FEATURES_SQRT_INV:
                this.subspaceSize = n - (int) Math.round(Math.sqrt(n) + 1);
                break;
            case AdaptiveRandomForest.FEATURES_PERCENT:
                // If subspaceSize is negative, then first find out the actual percent, i.e., 100% - m.
                double percent = this.subspaceSize < 0 ? (100 + this.subspaceSize)/100.0 : this.subspaceSize / 100.0;
                this.subspaceSize = (int) Math.round(n * percent);
                break;
        }
        // Notice that if the selected mFeaturesModeOption was 
        //  AdaptiveRandomForest.FEATURES_M then nothing is performed in the
        //  previous switch-case, still it is necessary to check (and adjusted) 
        //  for when a negative value was used. 
        
        // m is negative, use size(features) + -m
        if(this.subspaceSize < 0)
            this.subspaceSize = n + this.subspaceSize;
        // Other sanity checks to avoid runtime errors. 
        //  m <= 0 (m can be negative if this.subspace was negative and 
        //  abs(m) > n), then use m = 1
        if(this.subspaceSize <= 0)
            this.subspaceSize = 1;
        // m > n, then it should use n
        if(this.subspaceSize > n)
            this.subspaceSize = n;
        
        ARFHoeffdingTree treeLearner = (ARFHoeffdingTree) getPreparedClassOption(this.treeLearnerOption);
        treeLearner.resetLearning();
        
        for(int i = 0 ; i < ensembleSize ; ++i) {
            treeLearner.subspaceSizeOption.setValue(this.subspaceSize);
            this.ensemble[i] = new ARFBaseLearner(
                i, 
                (ARFHoeffdingTree) treeLearner.copy(), 
                (BasicClassificationPerformanceEvaluator) classificationEvaluator.copy(), 
                this.instancesSeen, 
                ! this.disableBackgroundLearnerOption.isSet(),
                ! this.disableDriftDetectionOption.isSet(), 
                driftDetectionMethodOption,
                warningDetectionMethodOption,
                false);
        }
    }
    
    /**
     * Inner class that represents a single tree member of the forest. 
     * It contains some analysis information, such as the numberOfDriftsDetected, 
     */
    protected final class ARFBaseLearner {
        public int indexOriginal;
        public long createdOn;
        public long lastDriftOn;
        public long lastWarningOn;
        public ARFHoeffdingTree classifier;
        public boolean isBackgroundLearner;
        
        // The drift and warning object parameters. 
        protected ClassOption driftOption;
        protected ClassOption warningOption;
        
        // Drift and warning detection
        protected ChangeDetector driftDetectionMethod;
        protected ChangeDetector warningDetectionMethod;
        
        public boolean useBkgLearner;
        public boolean useDriftDetector;
        
        // Bkg learner
        protected ARFBaseLearner bkgLearner;
        // Statistics
        public BasicClassificationPerformanceEvaluator evaluator;
        protected int numberOfDriftsDetected;
        protected int numberOfWarningsDetected;

        private void init(int indexOriginal, ARFHoeffdingTree instantiatedClassifier, BasicClassificationPerformanceEvaluator evaluatorInstantiated, 
            long instancesSeen, boolean useBkgLearner, boolean useDriftDetector, ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner) {
            this.indexOriginal = indexOriginal;
            this.createdOn = instancesSeen;
            this.lastDriftOn = 0;
            this.lastWarningOn = 0;
            
            this.classifier = instantiatedClassifier;
            this.evaluator = evaluatorInstantiated;
            this.useBkgLearner = useBkgLearner;
            this.useDriftDetector = useDriftDetector;
            
            this.numberOfDriftsDetected = 0;
            this.numberOfWarningsDetected = 0;
            this.isBackgroundLearner = isBackgroundLearner;

            if(this.useDriftDetector) {
                this.driftOption = driftOption;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftOption)).copy();
            }

            // Init Drift Detector for Warning detection. 
            if(this.useBkgLearner) {
                this.warningOption = warningOption;
                this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
            }
        }

        public ARFBaseLearner(int indexOriginal, ARFHoeffdingTree instantiatedClassifier, BasicClassificationPerformanceEvaluator evaluatorInstantiated, 
                    long instancesSeen, boolean useBkgLearner, boolean useDriftDetector, ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner) {
            init(indexOriginal, instantiatedClassifier, evaluatorInstantiated, instancesSeen, useBkgLearner, useDriftDetector, driftOption, warningOption, isBackgroundLearner);
        }

        public void reset() {
            if(this.useBkgLearner && this.bkgLearner != null) {
                this.classifier = this.bkgLearner.classifier;
                
                this.driftDetectionMethod = this.bkgLearner.driftDetectionMethod;
                this.warningDetectionMethod = this.bkgLearner.warningDetectionMethod;
                
                this.evaluator = this.bkgLearner.evaluator;
                this.createdOn = this.bkgLearner.createdOn;
                this.bkgLearner = null;
            }
            else {
                this.classifier.resetLearning();
                this.createdOn = instancesSeen;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftOption)).copy();
            }
            this.evaluator.reset();
        }

        public void trainOnInstance(Instance instance, double weight, long instancesSeen) {
            Instance weightedInstance = (Instance) instance.copy();
            weightedInstance.setWeight(instance.weight() * weight);
            this.classifier.trainOnInstance(weightedInstance);
            
            if(this.bkgLearner != null)
                this.bkgLearner.classifier.trainOnInstance(instance);
            
            // Should it use a drift detector? Also, is it a backgroundLearner? If so, then do not "incept" another one. 
            if(this.useDriftDetector && !this.isBackgroundLearner) {
                boolean correctlyClassifies = this.classifier.correctlyClassifies(instance);
                // Check for warning only if useBkgLearner is active
                if(this.useBkgLearner) {
                    // Update the warning detection method
                    this.warningDetectionMethod.input(correctlyClassifies ? 0 : 1);
                    // Check if there was a change
                    if(this.warningDetectionMethod.getChange()) {
                        this.lastWarningOn = instancesSeen;
                        this.numberOfWarningsDetected++;
                        // Create a new bkgTree classifier
                        ARFHoeffdingTree bkgClassifier = (ARFHoeffdingTree) this.classifier.copy();
                        bkgClassifier.resetLearning();
                        
                        // Resets the evaluator
                        BasicClassificationPerformanceEvaluator bkgEvaluator = (BasicClassificationPerformanceEvaluator) this.evaluator.copy();
                        bkgEvaluator.reset();
                        
                        // Create a new bkgLearner object
                        this.bkgLearner = new ARFBaseLearner(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen, 
                            this.useBkgLearner, this.useDriftDetector, this.driftOption, this.warningOption, true);
                        
                        // Update the warning detection object for the current object 
                        // (this effectively resets changes made to the object while it was still a bkg learner). 
                        this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
                    }
                }
                
                /*********** drift detection ***********/
                
                // Update the DRIFT detection method
                this.driftDetectionMethod.input(correctlyClassifies ? 0 : 1);
                // Check if there was a change
                if(this.driftDetectionMethod.getChange()) {
                    this.lastDriftOn = instancesSeen;
                    this.numberOfDriftsDetected++;
                    this.reset();
                }
            }
        }

        public double[] getVotesForInstance(Instance instance) {
            DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(instance));
            return vote.getArrayRef();
        }
    }
    
    /***
     * Inner class to assist with the multi-thread execution. 
     */
    protected class TrainingRunnable implements Runnable, Callable<Integer> {
        final private ARFBaseLearner learner;
        final private Instance instance;
        final private double weight;
        final private long instancesSeen;

        public TrainingRunnable(ARFBaseLearner learner, Instance instance, 
                double weight, long instancesSeen) {
            this.learner = learner;
            this.instance = instance;
            this.weight = weight;
            this.instancesSeen = instancesSeen;
        }

        @Override
        public void run() {
            learner.trainOnInstance(this.instance, this.weight, this.instancesSeen);
        }

        @Override
        public Integer call() throws Exception {
            run();
            return 0;
        }
    }
}
