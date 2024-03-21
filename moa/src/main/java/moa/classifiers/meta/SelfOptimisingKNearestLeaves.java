package moa.classifiers.meta;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.AbstractMOAObject;
import moa.classifiers.Regressor;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.trees.SelfOptimisingBaseTree;
import moa.core.*;
import moa.evaluation.BasicRegressionPerformanceEvaluator;
import moa.options.ClassOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Implementation of Self-Optimising K Nearest Leaves.
 *
 * <p> See details in: <br> Yibin Sun, Bernhard Pfahringer, Heitor Murilo Gomes, Albert Bifet. </br>
 * SOKNL: a novel way of integrating K nearest neighbours with adaptive random forest regression for data streams.
 * In European Conference on Machine Learning and Principle and Practice of Knowledge Discovery in Databases (ECML-PKDD), 2022.
 * https://doi.org/10.1007/s10618-022-00858-9 </p>
 */

public class SelfOptimisingKNearestLeaves extends AbstractClassifier implements Regressor {
    @Override
    public String getPurposeString() {
        return "Adaptive Random Forest Regressor algorithm for evolving data streams from Gomes et al.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption treeLearnerOption = new ClassOption("treeLearner", 'l',
            "baseLearnerForSOKNL.", SelfOptimisingBaseTree.class,
            "SelfOptimisingBaseTree -s VarianceReductionSplitCriterion -g 50 -c 0.01");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of trees.", 100, 1, Integer.MAX_VALUE);

    public FlagOption DisableSelfOptimisingOption = new FlagOption("DisableSelfOptimising",'f',"Disable the self optimising procedure.");

    public IntOption kOption =  new IntOption("kNearestLeaves",'k',"Specify k value when not self-optimising",10,1,this.ensembleSizeOption.getMaxValue());

    public IntOption randomSeedOption = new IntOption("randomSeed", 'r', "The random seed", 1);

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

    protected SelfOptimisingKNearestLeavesBaseLearner[] ensemble;
    protected long instancesSeen;
    protected int subspaceSize;
    protected BasicRegressionPerformanceEvaluator evaluator;

    protected BasicRegressionPerformanceEvaluator[] selfOptimisingEvaluators;

    protected double[] previousPrediction;

    @Override
    public void resetLearningImpl() {
        // Reset attributes
        this.ensemble = null;
        this.subspaceSize = 0;
        this.instancesSeen = 0;
        this.evaluator = new BasicRegressionPerformanceEvaluator();

        this.classifierRandom = new Random(randomSeedOption.getValue());

        this.previousPrediction = new double[this.ensembleSizeOption.getValue()];

        this.selfOptimisingEvaluators = new BasicRegressionPerformanceEvaluator[this.ensembleSizeOption.getValue()];
        for (int i = 0; i < this.selfOptimisingEvaluators.length; i++) {
            this.selfOptimisingEvaluators[i] = new BasicRegressionPerformanceEvaluator();
            this.selfOptimisingEvaluators[i].reset();
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        if (this.ensemble == null)
            initEnsemble(instance);

        for (int i = 0; i < this.ensemble.length; i++) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(instance));
            InstanceExample example = new InstanceExample(instance);
            this.ensemble[i].evaluator.addResult(example, vote.getArrayRef());
            int k = MiscUtils.poisson(this.lambdaOption.getValue(), this.classifierRandom);
            if (k > 0) {
                this.ensemble[i].trainOnInstance(instance, k, this.instancesSeen);
            }
        }

        InstanceExample example = new InstanceExample(instance);
        for (int i = 0; i < this.previousPrediction.length; i++) {
            this.selfOptimisingEvaluators[i].addResult(example, new double[]{this.previousPrediction[i]});
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        Instance testInstance = instance.copy();
        if (this.ensemble == null)
            initEnsemble(testInstance);

        ArrayList<SelfOptimisingBaseTree.LeafNode> candidates = new ArrayList<>();

        for (SelfOptimisingKNearestLeavesBaseLearner a : this.ensemble)
            if (a.classifier != null)
                candidates.add((SelfOptimisingBaseTree.LeafNode) a.classifier.getLeafForInstance(instance, a.classifier.getTreeRoot()));

        double[] distances = new double[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i) != null) {
                double[] centroid = new double[instance.numAttributes() - 1];

                for (int j = 0; j < centroid.length; j++)
                    if (candidates.get(i).sumsForAllAttrs != null)
                        centroid[j] = candidates.get(i).sumsForAllAttrs[j] / candidates.get(i).learntInstances;

                distances[i] += getDistanceFromCentroid(instance, centroid) / candidates.get(i).learntInstances;
            }
        }

        // Activate Self-Optimising K-Nearest Leaves
        if (!this.DisableSelfOptimisingOption.isSet()) {
            InstanceExample example = new InstanceExample(instance);
            int n = selfOptimisingEvaluators.length;
            double[] performances = new double[n];

            this.previousPrediction = new double[n];

            for (int i = 0; i < n; i++) {
                double[] temporaryPrediction = {getKNLPrediction(instance, i + 1, candidates, distances)};
                this.selfOptimisingEvaluators[i].addResult(example, temporaryPrediction);
                performances[i] = this.selfOptimisingEvaluators[i].getSquareError();
                this.previousPrediction[i] = temporaryPrediction[0];
            }
            int k = indexOfSmallestValue(performances);
            return new double[]{this.previousPrediction[k]};
        }

        return new double[]{getKNLPrediction(instance,this.kOption.getValue(),candidates,distances)};
    }

    private double getKNLPrediction(Instance instance, int k, ArrayList<SelfOptimisingBaseTree.LeafNode> candidates, double[] distances) {
        double prediction = 0;
        if (candidates.size() > 0) {
            int[] indices = indicesOfKSmallestValues(distances, Math.min(k, candidates.size()));
            for (Integer i : indices)
                if (candidates.get(i) != null && candidates.get(i).sumsForAllAttrs != null)
                    prediction += candidates.get(i).sumsForAllAttrs[instance.numAttributes() - 1] / (candidates.get(i).learntInstances * k);
        }
        return prediction;
    }

    protected void initEnsemble(Instance instance) {
        // Init the ensemble.
        int ensembleSize = this.ensembleSizeOption.getValue();
        this.ensemble = new SelfOptimisingKNearestLeavesBaseLearner[ensembleSize];

        // TODO: this should be an option with default = BasicClassificationPerformanceEvaluator
        BasicRegressionPerformanceEvaluator regressionEvaluator = new BasicRegressionPerformanceEvaluator();

        this.subspaceSize = this.mFeaturesPerTreeSizeOption.getValue();

        // The size of m depends on:
        // 1) mFeaturesPerTreeSizeOption
        // 2) mFeaturesModeOption
        int n = instance.numAttributes() - 1; // Ignore class label ( -1 )

        switch (this.mFeaturesModeOption.getChosenIndex()) {
            case AdaptiveRandomForest.FEATURES_SQRT:
                this.subspaceSize = (int) Math.round(Math.sqrt(n)) + 1;
                break;
            case AdaptiveRandomForest.FEATURES_SQRT_INV:
                this.subspaceSize = n - (int) Math.round(Math.sqrt(n) + 1);
                break;
            case AdaptiveRandomForest.FEATURES_PERCENT:
                // If subspaceSize is negative, then first find out the actual percent, i.e., 100% - m.
                double percent = this.subspaceSize < 0 ? (100 + this.subspaceSize) / 100.0 : this.subspaceSize / 100.0;
                this.subspaceSize = (int) Math.round(n * percent);
                break;
        }
        // Notice that if the selected mFeaturesModeOption was
        //  AdaptiveRandomForest.FEATURES_M then nothing is performed in the
        //  previous switch-case, still it is necessary to check (and adjusted)
        //  for when a negative value was used.

        // m is negative, use size(features) + -m
        if (this.subspaceSize < 0)
            this.subspaceSize = n + this.subspaceSize;
        // Other sanity checks to avoid runtime errors.
        //  m <= 0 (m can be negative if this.subspace was negative and
        //  abs(m) > n), then use m = 1
        if (this.subspaceSize <= 0)
            this.subspaceSize = 1;
        // m > n, then it should use n
        if (this.subspaceSize > n)
            this.subspaceSize = n;

        SelfOptimisingBaseTree treeLearner = (SelfOptimisingBaseTree) getPreparedClassOption(this.treeLearnerOption);
        treeLearner.resetLearning();

        for (int i = 0; i < ensembleSize; ++i) {
            treeLearner.subspaceSizeOption.setValue(this.subspaceSize);
            this.ensemble[i] = new SelfOptimisingKNearestLeavesBaseLearner(
                    i,
                    (SelfOptimisingBaseTree) treeLearner.copy(),
                    (BasicRegressionPerformanceEvaluator) regressionEvaluator.copy(),
                    this.instancesSeen,
                    !this.disableBackgroundLearnerOption.isSet(),
                    !this.disableDriftDetectionOption.isSet(),
                    driftDetectionMethodOption,
                    warningDetectionMethodOption,
                    false,
                    this.classifierRandom);
        }
    }

    @Override
    public Prediction getPredictionForInstance(Instance inst){
        Prediction prediction = new MultiLabelPrediction(1);
        prediction.setVotes(0,getVotesForInstance(inst));
        return prediction;
    }

    private double getDistanceFromCentroid(Instance inst, double[] centroid) {
        double sumOfSquare = 0;
        if (inst.numAttributes() - 1 != centroid.length) return 0;
        else {
            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                sumOfSquare += Math.pow((inst.value(i) - centroid[i]), 2);
            }
        }
        return Math.sqrt(sumOfSquare);
    }

    private int indexOfSmallestValue(double[] values) {
        int smallest = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[smallest]) smallest = i;
        }
        return smallest;
    }

    private int[] indicesOfKSmallestValues(double[] values, int k) {
        int[] smallest = new int[k];
        ArrayList<IndicesSorting> sortings = new ArrayList<>();
        for (int i = 0; i < values.length; i++)
            sortings.add(new IndicesSorting(i, values[i]));
        sortings = (ArrayList<IndicesSorting>) sortings.stream().sorted(Comparator.comparing(IndicesSorting::getValues)).collect(Collectors.toList());
        for (int i = 0; i < k; i++) {
            smallest[i] = sortings.get(i).getIndex();
        }

        return smallest;
    }

    private static class IndicesSorting{
        private int index;
        private double values;

        public IndicesSorting(int index, double values) {
            this.index = index;
            this.values = values;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public double getValues() {
            return values;
        }

        public void setValues(double values) {
            this.values = values;
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    protected final class SelfOptimisingKNearestLeavesBaseLearner extends AbstractMOAObject {
        public int indexOriginal;
        public long createdOn;
        public long lastDriftOn;
        public long lastWarningOn;
        public SelfOptimisingBaseTree classifier;
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
        protected SelfOptimisingKNearestLeavesBaseLearner bkgLearner;
        // Statistics
        public BasicRegressionPerformanceEvaluator evaluator;
        protected int numberOfDriftsDetected;
        protected int numberOfWarningsDetected;

        private void init(int indexOriginal, SelfOptimisingBaseTree instantiatedClassifier, BasicRegressionPerformanceEvaluator evaluatorInstantiated,
                          long instancesSeen, boolean useBkgLearner, boolean useDriftDetector, ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner, Random random) {
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

            if (this.useDriftDetector) {
                this.driftOption = driftOption;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftOption)).copy();
            }

            // Init Drift Detector for Warning detection.
            if (this.useBkgLearner) {
                this.warningOption = warningOption;
                this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
            }

            this.classifier.classifierRandom = random;
        }

        public SelfOptimisingKNearestLeavesBaseLearner(int indexOriginal, SelfOptimisingBaseTree instantiatedClassifier, BasicRegressionPerformanceEvaluator evaluatorInstantiated,
                                                       long instancesSeen, boolean useBkgLearner, boolean useDriftDetector, ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner, Random random) {
            init(indexOriginal, instantiatedClassifier, evaluatorInstantiated, instancesSeen, useBkgLearner, useDriftDetector, driftOption, warningOption, isBackgroundLearner, random);
        }

        public void reset() {
            if (this.useBkgLearner && this.bkgLearner != null) {
                this.classifier = this.bkgLearner.classifier;

                this.driftDetectionMethod = this.bkgLearner.driftDetectionMethod;
                this.warningDetectionMethod = this.bkgLearner.warningDetectionMethod;

                this.evaluator = this.bkgLearner.evaluator;
                this.createdOn = this.bkgLearner.createdOn;
                this.bkgLearner = null;
            } else {
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

            if (this.bkgLearner != null)
                this.bkgLearner.classifier.trainOnInstance(instance);

            // Should it use a drift detector? Also, is it a backgroundLearner? If so, then do not "incept" another one.
            if (this.useDriftDetector && !this.isBackgroundLearner) {
//                boolean correctlyClassifies = this.classifier.correctlyClassifies(instance);
                double prediction = this.classifier.getVotesForInstance(instance)[0];
                // Check for warning only if useBkgLearner is active
                if (this.useBkgLearner) {
                    // Update the warning detection method
                    this.warningDetectionMethod.input(prediction);
                    // Check if there was a change
                    if (this.warningDetectionMethod.getChange()) {
                        this.lastWarningOn = instancesSeen;
                        this.numberOfWarningsDetected++;
                        // Create a new bkgTree classifier
                        SelfOptimisingBaseTree bkgClassifier = (SelfOptimisingBaseTree) this.classifier.copy();
                        bkgClassifier.resetLearning();

                        // Resets the evaluator
                        BasicRegressionPerformanceEvaluator bkgEvaluator = (BasicRegressionPerformanceEvaluator) this.evaluator.copy();
                        bkgEvaluator.reset();

                        // Create a new bkgLearner object
                        this.bkgLearner = new SelfOptimisingKNearestLeavesBaseLearner(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                                this.useBkgLearner, this.useDriftDetector, this.driftOption, this.warningOption, true, this.classifier.classifierRandom);

                        // Update the warning detection object for the current object
                        // (this effectively resets changes made to the object while it was still a bkg learner).
                        this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
                    }
                }

                /*********** drift detection ***********/

                // Update the DRIFT detection method
                this.driftDetectionMethod.input(prediction);
                // Check if there was a change
                if (this.driftDetectionMethod.getChange()) {
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
