/*
 *    AdaptiveRandomForestRegressor.java
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

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.AbstractMOAObject;
import moa.classifiers.Regressor;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.trees.ARFFIMTDD;
import moa.core.DoubleVector;
import moa.core.InstanceExample;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.options.ClassOption;

/**
 * Implementation of AdaptiveRandomForestRegressor, an extension of AdaptiveRandomForest for classification.
 *
 * <p>See details in:<br> Heitor Murilo Gomes, Jean Paul Barddal, Luis Eduardo Boiko Ferreira, Albert Bifet.
 * Adaptive random forests for data stream regression.
 * In European Symposium on Artificial Neural Networks, Computational Intelligence and Machine Learning (ESANN), 2018.
 * https://www.elen.ucl.ac.be/Proceedings/esann/esannpdf/es2018-183.pdf</p>
 */
public class AdaptiveRandomForestRegressor extends AbstractClassifier implements Regressor {

    @Override
    public String getPurposeString() {
        return "Adaptive Random Forest Regressor algorithm for evolving data streams from Gomes et al.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption treeLearnerOption = new ClassOption("treeLearner", 'l',
            "Random Forest Tree.", ARFFIMTDD.class,
            "ARFFIMTDD -s VarianceReductionSplitCriterion -g 50 -c 0.01");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of trees.", 100, 1, Integer.MAX_VALUE);

    public MultiChoiceOption mFeaturesModeOption = new MultiChoiceOption("mFeaturesMode", 'o',
            "Defines how m, defined by mFeaturesPerTreeSize, is interpreted. M represents the total number of features.",
            new String[]{"Specified m (integer value)", "sqrt(M)+1", "M-(sqrt(M)+1)",
                    "Percentage (M * (m / 100))"},
            new String[]{"SpecifiedM", "SqrtM1", "MSqrtM1", "Percentage"}, 3);

    public IntOption mFeaturesPerTreeSizeOption = new IntOption("mFeaturesPerTreeSize", 'm',
            "Number of features allowed considered for each split. Negative values corresponds to M - m", 60, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public FloatOption lambdaOption = new FloatOption("lambda", 'a',
            "The lambda parameter for bagging.", 6.0, 1.0, Float.MAX_VALUE);

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
            "Change detector for drifts and its parameters", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-3");

    public ClassOption warningDetectionMethodOption = new ClassOption("warningDetectionMethod", 'p',
            "Change detector for warnings (start training bkg learner)", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-2");

    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'u',
            "Should use drift detection? If disabled then bkg learner is also disabled");

    public FlagOption disableBackgroundLearnerOption = new FlagOption("disableBackgroundLearner", 'q',
            "Should use bkg learner? If disabled then reset tree immediately.");

    protected static final int FEATURES_M = 0;
    protected static final int FEATURES_SQRT = 1;
    protected static final int FEATURES_SQRT_INV = 2;
    protected static final int FEATURES_PERCENT = 3;

    protected ARFFIMTDDBaseLearner[] ensemble;
    protected long instancesSeen;
    protected int subspaceSize;
    protected BasicRegressionPerformanceEvaluator evaluator;

    @Override
    public void resetLearningImpl() {
        // Reset attributes
        this.ensemble = null;
        this.subspaceSize = 0;
        this.instancesSeen = 0;
        this.evaluator = new BasicRegressionPerformanceEvaluator();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        if(this.ensemble == null)
            initEnsemble(instance);

        for (int i = 0 ; i < this.ensemble.length ; i++) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(instance));
            InstanceExample example = new InstanceExample(instance);
            this.ensemble[i].evaluator.addResult(example, vote.getArrayRef());
            int k = MiscUtils.poisson(this.lambdaOption.getValue(), this.classifierRandom);
            if (k > 0) {
                this.ensemble[i].trainOnInstance(instance, k, this.instancesSeen);
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        Instance testInstance = instance.copy();
        if(this.ensemble == null)
            initEnsemble(testInstance);

        double sumPredictions = 0;
        DoubleVector ages = new DoubleVector();
        DoubleVector performance = new DoubleVector();

        for(int i = 0 ; i < this.ensemble.length ; ++i) {
            double currentPrediction = this.ensemble[i].getVotesForInstance(testInstance)[0];

            ages.addToValue(i, this.instancesSeen - this.ensemble[i].createdOn);
            performance.addToValue(i, this.ensemble[i].evaluator.getSquareError());
            sumPredictions += currentPrediction;
        }
        double predicted = sumPredictions / this.ensemble.length;
//        if(predicted >= 1500) {
//            System.out.println(this.instancesSeen + " = " + predictions);
//            System.out.println(this.instancesSeen + " = " + ages);
//            System.out.println(this.instancesSeen + " = " + performance);
//            System.out.println();
//        }

        return new double[] {predicted};
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    protected void initEnsemble(Instance instance) {
        // Init the ensemble.
        int ensembleSize = this.ensembleSizeOption.getValue();
        this.ensemble = new ARFFIMTDDBaseLearner[ensembleSize];

        // TODO: this should be an option with default = BasicClassificationPerformanceEvaluator
        BasicRegressionPerformanceEvaluator regressionEvaluator = new BasicRegressionPerformanceEvaluator();

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

        ARFFIMTDD treeLearner = (ARFFIMTDD) getPreparedClassOption(this.treeLearnerOption);
        treeLearner.resetLearning();

        for(int i = 0 ; i < ensembleSize ; ++i) {
            treeLearner.subspaceSizeOption.setValue(this.subspaceSize);
            this.ensemble[i] = new ARFFIMTDDBaseLearner(
                    i,
                    (ARFFIMTDD) treeLearner.copy(),
                    (BasicRegressionPerformanceEvaluator) regressionEvaluator.copy(),
                    this.instancesSeen,
                    ! this.disableBackgroundLearnerOption.isSet(),
                    ! this.disableDriftDetectionOption.isSet(),
                    driftDetectionMethodOption,
                    warningDetectionMethodOption,
                    false);
        }
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }


    protected final class ARFFIMTDDBaseLearner extends AbstractMOAObject {
        public int indexOriginal;
        public long createdOn;
        public long lastDriftOn;
        public long lastWarningOn;
        public ARFFIMTDD classifier;
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
        protected ARFFIMTDDBaseLearner bkgLearner;
        // Statistics
        public BasicRegressionPerformanceEvaluator evaluator;
        protected int numberOfDriftsDetected;
        protected int numberOfWarningsDetected;

        private void init(int indexOriginal, ARFFIMTDD instantiatedClassifier, BasicRegressionPerformanceEvaluator evaluatorInstantiated,
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

        public ARFFIMTDDBaseLearner(int indexOriginal, ARFFIMTDD instantiatedClassifier, BasicRegressionPerformanceEvaluator evaluatorInstantiated,
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
//                boolean correctlyClassifies = this.classifier.correctlyClassifies(instance);
                double prediction = this.classifier.getVotesForInstance(instance)[0];
                // Check for warning only if useBkgLearner is active
                if(this.useBkgLearner) {
                    // Update the warning detection method
                    this.warningDetectionMethod.input(prediction);
                    // Check if there was a change
                    if(this.warningDetectionMethod.getChange()) {
                        this.lastWarningOn = instancesSeen;
                        this.numberOfWarningsDetected++;
                        // Create a new bkgTree classifier
                        ARFFIMTDD bkgClassifier = (ARFFIMTDD) this.classifier.copy();
                        bkgClassifier.resetLearning();

                        // Resets the evaluator
                        BasicRegressionPerformanceEvaluator bkgEvaluator = (BasicRegressionPerformanceEvaluator) this.evaluator.copy();
                        bkgEvaluator.reset();

                        // Create a new bkgLearner object
                        this.bkgLearner = new ARFFIMTDDBaseLearner(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                                this.useBkgLearner, this.useDriftDetector, this.driftOption, this.warningOption, true);

                        // Update the warning detection object for the current object
                        // (this effectively resets changes made to the object while it was still a bkg learner).
                        this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
                    }
                }

                /*********** drift detection ***********/

                // Update the DRIFT detection method
                this.driftDetectionMethod.input(prediction);
                // Check if there was a change
                if(this.driftDetectionMethod.getChange()) {
                    this.lastDriftOn = instancesSeen;
                    this.numberOfDriftsDetected++;
                    this.reset();
                }
            }
        }

        public double[] getVotesForInstance(Instance instance) {
//            DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(instance));
//            return vote.getArrayRef();
            return this.classifier.getVotesForInstance(instance);
        }

        @Override
        public void getDescription(StringBuilder sb, int indent) {
        }
    }
}
