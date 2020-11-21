/*
 *    StreamingRandomPatches.java
 *
 *    @author Heitor Murilo Gomes (heitor dot gomes at waikato dot ac dot nz)
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
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.core.*;
import moa.evaluation.BasicClassificationPerformanceEvaluator;
import moa.options.ClassOption;

import java.util.ArrayList;
import java.util.Random;

/**
 * Streaming Random Patches
 *
 * <p>Streaming Random Patches (SRP). This ensemble method uses a hoeffding tree by default,
 * but it can be used with any other base model (differently from random forest variations).
 * This algorithm can be used to simulate bagging or random subspaces, see parameter -t.
 * The default algorithm uses both bagging and random subspaces, namely Random Patches.</p>
 *
 * <p>See details in:<br> Heitor Murilo Gomes, Jesse Read, Albert Bifet.
 * Streaming Random Patches for Evolving Data Stream Classification.
 * IEEE International Conference on Data Mining (ICDM), 2019.</p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : Classiﬁer to train. Default to a Hoeffding Tree, but it is not restricted to decision trees.</li>
 * <li>-s : The number of learners in the ensemble.</li>
 * <li>-o : How the number of features is interpreted (4 options):
 * "Specified m (integer value)", "sqrt(M)+1", "M-(sqrt(M)+1)".</li>
 * <li>-m : Number of features allowed considered for each split. Negative values corresponds to M - m.</li>
 * <li>-t : The training method to use: Random Patches, Random Subspaces or Bagging.</li>
 * <li>-a : The lambda value for the poisson distribution (used to emulate bagging).</li>
 * <li>-x : Change detector for drifts and its parameters.</li>
 * <li>-p : Change detector for warnings.</li>
 * <li>-w : Should use weighted voting?</li>
 * <li>-u : Should use drift detection? If disabled, then the bkg learner is also disabled.</li>
 * <li>-q : Should use bkg learner? If disabled, then trees are reset immediately.</li>
 * </ul>
 *
 * @author Heitor Murilo Gomes (heitor dot gomes at waikato dot ac dot nz)
 * @version $Revision: 1 $
 */
public class StreamingRandomPatches extends AbstractClassifier implements MultiClassClassifier,
        CapabilitiesHandler {

    private static final long serialVersionUID = 1L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train on instances.", Classifier.class, "trees.HoeffdingTree -g 50 -c 0.01");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models.", 100, 1, Integer.MAX_VALUE);

    // SUBSPACE CONFIGURATION
    public MultiChoiceOption subspaceModeOption = new MultiChoiceOption("subspaceMode", 'o',
            "Defines how m, defined by mFeaturesPerTreeSize, is interpreted. M represents the total number of features.",
            new String[]{"Specified m (integer value)", "sqrt(M)+1", "M-(sqrt(M)+1)",
                    "Percentage (M * (m / 100))"},
            new String[]{"SpecifiedM", "SqrtM1", "MSqrtM1", "Percentage"}, 3);

    public IntOption subspaceSizeOption = new IntOption("subspaceSize", 'm',
            "# attributes per subset for each classifier. Negative values = totalAttributes - #attributes", 60, Integer.MIN_VALUE, Integer.MAX_VALUE);

    // TRAINING
    public MultiChoiceOption trainingMethodOption = new MultiChoiceOption("trainingMethod", 't',
            "The training method to use: Random Patches, Random Subspaces or Bagging.",
            new String[]{"Random Subspaces", "Resampling (bagging)", "Random Patches"},
            new String[]{"RandomSubspaces", "Resampling", "RandomPatches"}, 2);

    public FloatOption lambdaOption = new FloatOption("lambda", 'a',
            "The lambda parameter for bagging.", 6.0, 1, Float.MAX_VALUE);

    // DRIFT and WARNING DETECTION
    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
            "Change detector for drifts and its parameters", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-5");

    public ClassOption warningDetectionMethodOption = new ClassOption("warningDetectionMethod", 'p',
            "Change detector for warnings (start training bkg learner)", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-4");

    // VOTING
    public FlagOption disableWeightedVote = new FlagOption("disableWeightedVote", 'w',
            "Should use weighted voting?");

    // DISABLING DRIFT DETECTION and BKG LEARNER (warning is also disabled in this case)
    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'u',
            "Should use drift detection? If disabled, then the bkg learner is also disabled.");

    public FlagOption disableBackgroundLearnerOption = new FlagOption("disableBackgroundLearner", 'q',
            "Should use bkg learner? If disabled, then trees are reset immediately.");

    public static final int TRAIN_RANDOM_SUBSPACES = 0;
    public static final int TRAIN_RESAMPLING = 1;
    public static final int TRAIN_RANDOM_PATCHES = 2;

    protected static final int FEATURES_M = 0;
    protected static final int FEATURES_SQRT = 1;
    protected static final int FEATURES_SQRT_INV = 2;
    protected static final int FEATURES_PERCENT = 3;

    protected StreamingRandomPatchesClassifier[] ensemble;
    protected long instancesSeen;
    protected ArrayList<ArrayList<Integer>> subspaces;

    @Override
    public void resetLearningImpl() {
        this.instancesSeen = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        if(this.ensemble == null)
            initEnsemble(instance);

        for (int i = 0 ; i < this.ensemble.length ; i++) {
            double[] rawVote = this.ensemble[i].getVotesForInstance(instance);
            DoubleVector vote = new DoubleVector(rawVote);
            InstanceExample example = new InstanceExample(instance);

            this.ensemble[i].evaluator.addResult(example, vote.getArrayRef());
            // Train using random subspaces without resampling, i.e. all instances are used for training.
            if(this.trainingMethodOption.getChosenIndex() == TRAIN_RANDOM_SUBSPACES) {
                this.ensemble[i].trainOnInstance(instance,1, this.instancesSeen, this.classifierRandom);
            }
            // Train using random patches or resampling, thus we simulate online bagging with poisson(lambda=...)
            else {
                int k = MiscUtils.poisson(this.lambdaOption.getValue(), this.classifierRandom);
                if (k > 0) {
                    double weight = k;
                    this.ensemble[i].trainOnInstance(instance, weight, this.instancesSeen, this.classifierRandom);
                }
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        Instance testInstance = instance.copy();
        testInstance.setMissing(instance.classAttribute());
        testInstance.setClassValue(0.0);
        if(this.ensemble == null)
            initEnsemble(testInstance);
        DoubleVector combinedVote = new DoubleVector();

        for(int i = 0 ; i < this.ensemble.length ; ++i) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(testInstance));
            if (vote.sumOfValues() > 0.0) {
                vote.normalize();
                double acc = this.ensemble[i].evaluator.getPerformanceMeasurements()[1].getValue();
                if(!this.disableWeightedVote.isSet() && acc > 0.0) {
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
        this.ensemble = new StreamingRandomPatchesClassifier[ensembleSize];

        BasicClassificationPerformanceEvaluator classificationEvaluator = new BasicClassificationPerformanceEvaluator();

        // #1 Select the size of k, it depends on 2 parameters (subspaceSizeOption and subspaceModeOption).
        int k = this.subspaceSizeOption.getValue();
        if(this.trainingMethodOption.getChosenIndex() != StreamingRandomPatches.TRAIN_RESAMPLING) {
            // PS: This applies only to subspaces and random patches option.
            int n = instance.numAttributes()-1; // Ignore the class label by subtracting 1

            switch(this.subspaceModeOption.getChosenIndex()) {
                case StreamingRandomPatches.FEATURES_SQRT:
                    k = (int) Math.round(Math.sqrt(n)) + 1;
                    break;
                case StreamingRandomPatches.FEATURES_SQRT_INV:
                    k = n - (int) Math.round(Math.sqrt(n) + 1);
                    break;
                case StreamingRandomPatches.FEATURES_PERCENT:
                    double percent = k < 0 ? (100 + k)/100.0 : k / 100.0;
                    k = (int) Math.round(n * percent);

                    if(Math.round(n * percent) < 2)
                        k = (int) Math.round(n * percent) + 1;
                    break;
            }
            // k is negative, use size(features) + -k
            if(k < 0)
                k = n + k;

            // #2 generate the subspaces
            if(this.trainingMethodOption.getChosenIndex() == StreamingRandomPatches.TRAIN_RANDOM_SUBSPACES ||
                    this.trainingMethodOption.getChosenIndex() == StreamingRandomPatches.TRAIN_RANDOM_PATCHES) {
                if(k != 0 && k < n) {
                    // For low dimensionality it is better to avoid more than 1 classifier with the same subspaces,
                    // thus we generate all possible combinations of subsets of features and select without replacement.
                    // n is the total number of features and k is the actual size of the subspaces.
                    if(n <= 20 || k < 2) {
                        if(k == 1 && instance.numAttributes() > 2)
                            k = 2;
                        // Generate all possible combinations of size k
                        this.subspaces = StreamingRandomPatches.allKCombinations(k, n);
                        for(int i = 0 ; this.subspaces.size() < this.ensemble.length ; ++i) {
                            i = i == this.subspaces.size() ? 0 : i;
                            ArrayList<Integer> copiedSubspace = new ArrayList<>(this.subspaces.get(i));
                            this.subspaces.add(copiedSubspace);
                        }
                    }
                    // For high dimensionality we can't generate all combinations as it is too expensive (memory).
                    // On top of that, the chance of repeating a subspace is lower, so we can just randomly generate
                    // subspaces without worrying about repetitions.
                    else {
                        this.subspaces = StreamingRandomPatches.localRandomKCombinations(k, n,
                                this.ensembleSizeOption.getValue(), this.classifierRandom);
                    }
                }
                // k == 0 or k > n (subspace size greater than the total number of features), then default to resampling
                else {
                    this.trainingMethodOption.setChosenIndex(StreamingRandomPatches.TRAIN_RESAMPLING);
                }
            }
        }

        // Obtain the base learner. It is not restricted to a specific learner.
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for(int i = 0 ; i < ensembleSize ; ++i) {
            switch(this.trainingMethodOption.getChosenIndex()) {
                case StreamingRandomPatches.TRAIN_RESAMPLING:
                    this.ensemble[i] = new StreamingRandomPatchesClassifier(
                            i,
                            baseLearner.copy(),
                            (BasicClassificationPerformanceEvaluator) classificationEvaluator.copy(),
                            this.instancesSeen,
                            this.disableBackgroundLearnerOption.isSet(),
                            this.disableDriftDetectionOption.isSet(),
                            this.driftDetectionMethodOption,
                            this.warningDetectionMethodOption,
                            false);
                    break;
                case StreamingRandomPatches.TRAIN_RANDOM_SUBSPACES:
                case StreamingRandomPatches.TRAIN_RANDOM_PATCHES:
                    int selectedValue = this.classifierRandom.nextInt(subspaces.size());
                    ArrayList<Integer> subsetOfFeatures = this.subspaces.get(selectedValue);
                    subsetOfFeatures.add(instance.classIndex());
                    this.ensemble[i] = new StreamingRandomPatchesClassifier(
                            i,
                            baseLearner.copy(),
                            (BasicClassificationPerformanceEvaluator) classificationEvaluator.copy(),
                            this.instancesSeen,
                            this.disableBackgroundLearnerOption.isSet(),
                            this.disableDriftDetectionOption.isSet(),
                            this.driftDetectionMethodOption,
                            this.warningDetectionMethodOption,
                            subsetOfFeatures,
                            instance,
                            false);
                    this.subspaces.remove(selectedValue);
                    break;
            }
        }

    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == StreamingRandomPatches.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    @Override
    public Classifier[] getSublearners() {
        /* Extracts the reference to the base learner object from within the ensemble of StreamingRandomPatchesClassifier */
        Classifier[] baseModels = new Classifier[this.ensemble.length];
        for(int i = 0 ; i < baseModels.length ; ++i)
            baseModels[i] = this.ensemble[i].classifier;
        return baseModels;
    }

    private static ArrayList<ArrayList<Integer>> localRandomKCombinations(int k, int length,
                                                                          int nCombinations, Random random) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
        for(int i = 0 ; i < nCombinations ; ++i) {
            ArrayList<Integer> combination = new ArrayList<>();
            // Add all possible items
            for(int j = 0 ; j < length ; ++j)
                combination.add(j);
            // Randomly remove each item by index using the current size
            // Out of "length" items, maintain only "k" items.
            for(int j = 0 ; j < (length - k) ; ++j)
                combination.remove(random.nextInt(combination.size()));

            combinations.add(combination);
        }
        return combinations;
    }

    private static void allKCombinationsInner(int offset, int k, ArrayList<Integer> combination, long originalSize,
                                              ArrayList<ArrayList<Integer>> combinations) {
        if (k == 0) {
            combinations.add(new ArrayList<>(combination));
            return;
        }
        for (int i = offset; i <= originalSize - k; ++i) {
            combination.add(i);
            allKCombinationsInner(i+1, k-1, combination, originalSize, combinations);
            combination.remove(combination.size()-1);
        }
    }

    private static ArrayList<ArrayList<Integer>> allKCombinations(int k, int length) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
        ArrayList<Integer> combination = new ArrayList<>();
        allKCombinationsInner(0, k, combination, length, combinations);
        return combinations;
    }

    // Inner class representing the base learner of SRP.
    protected class StreamingRandomPatchesClassifier {
        public int indexOriginal;
        public long createdOn;
        public Classifier classifier;

        // Stores current model subspace representation of the original instances.
        public Instances subset;
        public int[] featureIndexes;

        // Drift detection
        public boolean disableBkgLearner;
        public boolean disableDriftDetector;

        protected ChangeDetector driftDetectionMethod;
        protected ChangeDetector warningDetectionMethod;
        // The drift and warning object parameters.
        protected ClassOption driftOption;
        protected ClassOption warningOption;

        // Bkg learner
        public StreamingRandomPatchesClassifier bkgLearner;
        public boolean isBackgroundLearner;
        // Statistics
        public BasicClassificationPerformanceEvaluator evaluator;
        public int numberOfDriftsDetected;
        public int numberOfWarningsDetected;

        // induced drifts/warnings
        public int numberOfDriftsInduced;
        public int numberOfWarningsInduced;

        private void init(int indexOriginal, Classifier instantiatedClassifier,
                          BasicClassificationPerformanceEvaluator evaluatorInstantiated,
                          long instancesSeen, boolean disableBkgLearner, boolean disableDriftDetector,
                          ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner) {
            this.indexOriginal = indexOriginal;
            this.createdOn = instancesSeen;

            this.classifier = instantiatedClassifier;
            this.evaluator = evaluatorInstantiated;
            this.disableBkgLearner = disableBkgLearner;
            this.disableDriftDetector = disableDriftDetector;

            if(!this.disableDriftDetector) {
                this.driftOption = driftOption;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(driftOption)).copy();
            }

            // Init Drift Detector for Warning detection.
            if(!this.disableBkgLearner) {
                this.warningOption = warningOption;
                this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(warningOption)).copy();
            }

            this.numberOfDriftsDetected = this.numberOfDriftsInduced = 0;
            this.numberOfWarningsDetected = this.numberOfWarningsInduced = 0;
            this.isBackgroundLearner = isBackgroundLearner;
        }

        // Create to simulate "Bagging" only, i.e., no random subspaces.
        public StreamingRandomPatchesClassifier(int indexOriginal, Classifier instantiatedClassifier,
                                                BasicClassificationPerformanceEvaluator evaluatorInstantiated,
                                                long instancesSeen, boolean disableBkgLearner, boolean disableDriftDetector,
                                                ClassOption driftOption, ClassOption warningOption,
                                                boolean isBackgroundLearner) {
            init(indexOriginal, instantiatedClassifier, evaluatorInstantiated, instancesSeen, disableBkgLearner,
                    disableDriftDetector, driftOption,
                    warningOption,
                    isBackgroundLearner);

            this.featureIndexes = null;
            this.subset = null;
        }

        // Create the subspaces for the current model.
        public StreamingRandomPatchesClassifier(int indexOriginal, Classifier instantiatedClassifier,
                                                BasicClassificationPerformanceEvaluator evaluatorInstantiated,
                                                long instancesSeen, boolean disableBkgLearner, boolean disableDriftDetector,
                                                ClassOption driftOption, ClassOption warningOption,
                                                ArrayList<Integer> featuresIndexes, Instance instance,
                                                boolean isBackgroundLearner) {
            init(indexOriginal, instantiatedClassifier, evaluatorInstantiated, instancesSeen, disableBkgLearner,
                    disableDriftDetector, driftOption, warningOption, isBackgroundLearner);

            // Features + class (last index)
            this.featureIndexes = new int[featuresIndexes.size()];
            ArrayList<Attribute> attSub = new ArrayList<Attribute>();

            // Add attributes of the selected subset
            for(int i = 0 ; i < featuresIndexes.size() ; ++i) {
                attSub.add(instance.attribute(featuresIndexes.get(i)));
                this.featureIndexes[i] = featuresIndexes.get(i);
            }
            this.subset = new Instances("Subsets Candidate Instances", attSub, 100);
            this.subset.setClassIndex(this.subset.numAttributes()-1);
            prepareRandomSubspaceInstance(instance,1);
        }

        public void prepareRandomSubspaceInstance(Instance instance, double weight) {
            // If there is any instance lingering in the subset, remove it.
            while(this.subset.numInstances() > 0)
                this.subset.delete(0);

            double[] values = new double[this.subset.numAttributes()];
            for(int j = 0 ; j < this.subset.numAttributes() ; ++j)
                values[j] = instance.value(this.featureIndexes[j]);

            // Set the class value for each value array.
            values[values.length-1] = instance.classValue();
            DenseInstance subInstance = new DenseInstance(1.0, values);
            subInstance.setWeight(weight);
            subInstance.setDataset(this.subset);
            this.subset.add(subInstance);
        }

        private ArrayList<Integer> applySubsetResetStrategy(Instance instance, Random random) {
            if(this.subset != null) {
                ArrayList<Integer> fIndexes = new ArrayList<Integer>();
                for(int j = 0 ; j < instance.numAttributes() ; ++j)
                    fIndexes.add(j);
                // Remove the class label... (it will be added latter)
                fIndexes.remove(instance.classIndex());

                for(int j = 0 ; j < instance.numAttributes() - this.featureIndexes.length ; ++j)
                    fIndexes.remove(random.nextInt(fIndexes.size()));
                // Adding the class label...
                fIndexes.add(instance.classIndex());
                return fIndexes;
            }
            return null;
        }

        public void reset(Instance instance, long instancesSeen, Random random) {

            if(!this.disableBkgLearner && this.bkgLearner != null) {
                this.classifier = this.bkgLearner.classifier;
                this.driftDetectionMethod = this.bkgLearner.driftDetectionMethod;
                this.warningDetectionMethod = this.bkgLearner.warningDetectionMethod;
                this.evaluator = this.bkgLearner.evaluator;
                this.evaluator.reset();
                this.createdOn = this.bkgLearner.createdOn;
                this.subset = this.bkgLearner.subset;
                this.featureIndexes = this.bkgLearner.featureIndexes;
            }
            else {
                this.classifier.resetLearning();
                this.evaluator.reset();
                this.createdOn = instancesSeen;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftOption)).copy();

                if(this.subset != null) {
                    ArrayList<Integer> fIndexes = this.applySubsetResetStrategy(instance, random);
                    for(int i = 0 ; i < fIndexes.size() ; ++i)
                        this.featureIndexes[i] = fIndexes.get(i);
                    ArrayList<Attribute> attSub = new ArrayList<Attribute>();
                    // Add attributes of the selected subset
                    for(int i = 0 ; i < this.featureIndexes.length ; ++i)
                        attSub.add(instance.attribute(this.featureIndexes[i]));

                    this.subset = new Instances("Subsets Candidate Instances", attSub, 100);
                    this.subset.setClassIndex(this.subset.numAttributes()-1);
                    prepareRandomSubspaceInstance(instance, 1);
                }
            }
        }

        public void trainOnInstance(Instance instance, double weight, long instancesSeen, Random random) {
            boolean correctlyClassifies;
            // The subset object will be null if we are training with all features
            if(this.subset != null) {
                // Selecting just the subset of features that we are going to use
                prepareRandomSubspaceInstance(instance, weight);

                // After prepareRandomSubspaceInstance, index 0 of subset holds the instance with this learner subspaces
                this.classifier.trainOnInstance(this.subset.get(0));
                correctlyClassifies = this.classifier.correctlyClassifies(this.subset.get(0));
                if(this.bkgLearner != null)
                    this.bkgLearner.trainOnInstance(instance, weight, instancesSeen, random);
            }
            else {
                Instance weightedInstance = instance.copy();
                weightedInstance.setWeight(instance.weight() * weight);
                this.classifier.trainOnInstance(weightedInstance);
                correctlyClassifies = this.classifier.correctlyClassifies(instance);
                if(this.bkgLearner != null)
                    this.bkgLearner.trainOnInstance(instance, weight, instancesSeen, random);
            }

            if(!this.disableDriftDetector && !this.isBackgroundLearner) {

                // Check for warning only if useBkgLearner is active
                if (!this.disableBkgLearner) {
                    // Update the warning detection method
                    this.warningDetectionMethod.input(correctlyClassifies ? 0 : 1);
                    // Check if there was a change
                    if (this.warningDetectionMethod.getChange()) {
                        this.numberOfWarningsDetected++;
                        triggerWarning(instance, instancesSeen, random);
                    }
                }

                /*********** drift detection ***********/
                // Update the DRIFT detection method
                this.driftDetectionMethod.input(correctlyClassifies ? 0 : 1);
                // Check if there was a change
                if (this.driftDetectionMethod.getChange()) {
                    this.numberOfDriftsDetected++;
                    // There was a change, this model must be reset
                    this.reset(instance, instancesSeen, random);
                }
            }
        }

        public void triggerWarning(Instance instance, long instancesSeen, Random random) {
            Classifier bkgClassifier = this.classifier.copy();
            bkgClassifier.resetLearning();

            BasicClassificationPerformanceEvaluator bkgEvaluator = (BasicClassificationPerformanceEvaluator) this.evaluator.copy();
            bkgEvaluator.reset();
            if(this.subset == null) {
                this.bkgLearner = new StreamingRandomPatchesClassifier(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                        this.disableBkgLearner, this.disableDriftDetector, this.driftOption, this.warningOption,true);
            }
            else {
                ArrayList<Integer> fIndexes = this.applySubsetResetStrategy(instance, random);

                this.bkgLearner = new StreamingRandomPatchesClassifier(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                        this.disableBkgLearner, this.disableDriftDetector, this.driftOption, this.warningOption,
                        fIndexes, instance,true);
            }
            this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
        }

        /**
         * @param instance
         * @return votes for the given instance
         */
        public double[] getVotesForInstance(Instance instance) {
            if(this.subset != null) {
                prepareRandomSubspaceInstance(instance, 1);
                // subset.get(0) returns the instance transformed to the correct subspace (i.e. current model subspace).
                DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(this.subset.get(0)));

                return vote.getArrayRef();
            }
            DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(instance));
            return vote.getArrayRef();
        }
    }
}
