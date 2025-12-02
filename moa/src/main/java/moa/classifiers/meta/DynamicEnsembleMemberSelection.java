/*
 *   DynamicEnsembleMemberSelection.java
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import moa.classifiers.trees.HoeffdingTree;
import moa.core.*;
import moa.evaluation.BasicClassificationPerformanceEvaluator;
import moa.options.ClassOption;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import moa.AbstractMOAObject;
import moa.classifiers.trees.ARFHoeffdingTree;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import moa.AbstractMOAObject;
import moa.core.Utils;

/**
 * Implementation of Dynamic Ensemble Member Selection (DEMS).
 * <p>
 * Only ARF and SRP are included as ensemble options, since other ones are not considered SOTA.
 *
 * <p>See details in:<br> Yibin Sun, Bernhard Pfahringer, Heitor Murilo Gomes, Albert Bifet.
 * Dynamic Ensemble Member Selection for Data Stream Classification.
 * In ACM International Conference on Information and Knowledge Management (CIKM) 2025.
 * https://dl.acm.org/doi/pdf/10.1145/3746252.3761072</p>
 */


public class DynamicEnsembleMemberSelection extends AbstractClassifier implements MultiClassClassifier,
        CapabilitiesHandler {


    private static final long serialVersionUID = 1L;

    public MultiChoiceOption ensembleClassOption = new MultiChoiceOption("ensembleClassOption", 'e',
            "The ensemble class to use.",
            new String[]{"StreamingRandomPatches", "AdaptiveRandomForest"},
            new String[]{"StreamingRandomPatches", "AdaptiveRandomForest"}, 0);

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train on instances.", Classifier.class, "trees.HoeffdingTree -g 50 -c 0.01");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models.", 100, 1, Integer.MAX_VALUE);


    // Number of jobs for ARF (0 or 1 = single-thread, -1 = as many as possible)
    public IntOption numberOfJobsOption = new IntOption("numberOfJobs", 'j',
            "Total number of concurrent jobs used for processing (-1 = as much as possible, 0 = do not use multithreading)",
            1, -1, Integer.MAX_VALUE);

    // SUBSPACE CONFIGURATION (used by both SRP and ARF)
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

    // Yibin New
    public IntOption kValueOption = new IntOption("kValues", 'k', "K values", 5, 1, this.ensembleSizeOption.getValue());
    public FlagOption disableSelfOptimisingOption = new FlagOption("disableSelfOptimizing", 'f', "Self Optimising Option");

    // This options will only be used when the ensemble is ARD
    public ClassOption treeLearnerOption = new ClassOption("treeLearner", '1',
            "Random Forest Tree.", ARFHoeffdingTree.class,
            "ARFHoeffdingTree -e 2000000 -g 50 -c 0.01");

    public static final int TRAIN_RANDOM_SUBSPACES = 0;
    public static final int TRAIN_RESAMPLING = 1;
    public static final int TRAIN_RANDOM_PATCHES = 2;

    protected static final int FEATURES_M = 0;
    protected static final int FEATURES_SQRT = 1;
    protected static final int FEATURES_SQRT_INV = 2;
    protected static final int FEATURES_PERCENT = 3;

    // SRP ensemble
    protected StreamingRandomPatchesClassifier[] ensemble;

    // ARF ensemble
    protected ARFBaseLearner[] arfEnsemble;

    protected long instancesSeen;
    protected ArrayList<ArrayList<Integer>> subspaces;

    // Yibin New for SRP
    protected int[] performances;
    protected List<SortingInformationForDEMS> infos;
    protected int bestK;

    // ARF-specific state
    protected int subspaceSize;
    protected BasicClassificationPerformanceEvaluator arfEvaluator;
    protected ExecutorService executor;
    //    protected int[] performances;
    protected List<SortingInformationForDEMS> informations;
    protected int kBest;


    @Override
    public void resetLearningImpl() {
        this.instancesSeen = 0;
        int ensembleType = this.ensembleClassOption.getChosenIndex();
        if (ensembleType == 0) { // StreamingRandomPatches
            this.ensemble = null;
            this.subspaces = null;
            this.performances = null;
            if (this.infos == null) this.infos = new ArrayList<>();
            this.bestK = 0;
        } else { // AdaptiveRandomForest
            this.arfEnsemble = null;
            this.subspaceSize = 0;
            this.arfEvaluator = new BasicClassificationPerformanceEvaluator();
            this.performances = null;
            if (this.infos == null) this.infos = new ArrayList<>();

            // initialise executor for ARF
            int numberOfJobs;
            if (this.numberOfJobsOption != null && this.numberOfJobsOption.getValue() == -1)
                numberOfJobs = Runtime.getRuntime().availableProcessors();
            else if (this.numberOfJobsOption != null)
                numberOfJobs = this.numberOfJobsOption.getValue();
            else
                numberOfJobs = 0;

            if (numberOfJobs != 0 && numberOfJobs != 1)
                this.executor = Executors.newFixedThreadPool(numberOfJobs);
            else
                this.executor = null;
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        int ensembleType = this.ensembleClassOption.getChosenIndex();

        if (ensembleType == 0) { // StreamingRandomPatches path
            if (this.ensemble == null)
                initSRPEnsemble(instance);

            if (!this.disableSelfOptimisingOption.isSet()) {
                if (this.performances == null)
                    this.performances = new int[this.ensemble.length];
                DoubleVector combinedVotes = new DoubleVector();
                for (int i = this.infos.size() - 1; i >= 0; i--) {
                    SortingInformationForDEMS s = this.infos.get(i);
                    DoubleVector vote = new DoubleVector(this.ensemble[s.getClassifierIndex()].getVotesForInstance(instance));

                    if (vote.sumOfValues() > 0) {
                        vote.normalize();
                        double acc = s.getTreeAcc();
                        if (!this.disableWeightedVote.isSet() && acc > 0.0) {
                            for (int v = 0; v < vote.numValues(); ++v) {
                                vote.setValue(v, vote.getValue(v) * acc);
                            }
                        }
                    }
                    combinedVotes.addValues(vote);

                    if (combinedVotes.maxIndex() == instance.classValue())
                        this.performances[this.ensembleSizeOption.getValue() - i - 1]++;
                }

                this.bestK = Utils.maxIndex(this.performances) + 1;
            }

            for (int i = 0; i < this.ensemble.length; i++) {
                double[] rawVote = this.ensemble[i].getVotesForInstance(instance);
                DoubleVector vote = new DoubleVector(rawVote);
                InstanceExample example = new InstanceExample(instance);

                this.ensemble[i].evaluator.addResult(example, vote.getArrayRef());
                // Train using random subspaces without resampling, i.e. all instances are used for training.
                if (this.trainingMethodOption.getChosenIndex() == TRAIN_RANDOM_SUBSPACES) {
                    this.ensemble[i].trainOnInstance(instance, 1, this.instancesSeen, this.classifierRandom);
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
        } else { // AdaptiveRandomForest
            if (this.arfEnsemble == null)
                initARFEnsemble(instance);

            this.informations = new ArrayList<>();

            for (int i = 0; i < this.arfEnsemble.length; i++) {
                this.informations.add(new SortingInformationForDEMS(this.arfEnsemble[i].evaluator.getTotalWeightObserved() == 0 ? 0 : this.arfEnsemble[i].evaluator.getFractionCorrectlyClassified(), this.arfEnsemble[i].classifier.getTreeRoot() == null ? new double[]{0} : this.arfEnsemble[i].classifier.getTreeRoot().filterInstanceToLeaf(instance, null, -1).node == null ? new double[]{0} : this.arfEnsemble[i].classifier.getTreeRoot().filterInstanceToLeaf(instance, null, -1).node.getObservedClassDistribution(), i));
            }

            if (!this.disableSelfOptimisingOption.isSet() && this.performances == null)
                this.performances = new int[this.ensembleSizeOption.getValue()];

            if (!this.disableSelfOptimisingOption.isSet()) {
                DoubleVector combinedVotes = new DoubleVector();
                for (int i = this.informations.size() - 1; i >= 0; i--) {
                    ARFBaseLearner arfBaseLearner = this.arfEnsemble[this.informations.get(i).getClassifierIndex()];
                    DoubleVector vote = new DoubleVector(arfBaseLearner.getVotesForInstance(instance));

                    if (vote.sumOfValues() > 0) {
                        vote.normalize();
                        double acc = arfBaseLearner.evaluator.getPerformanceMeasurements()[1].getValue();
                        if (!this.disableWeightedVote.isSet() && acc > 0.0) {
                            for (int v = 0; v < vote.numValues(); ++v) {
                                vote.setValue(v, vote.getValue(v) * acc);
                            }
                        }
                    }

                    combinedVotes.addValues(vote);

                    if (combinedVotes.maxIndex() == instance.classValue())
                        this.performances[this.ensembleSizeOption.getValue() - i - 1]++;
                }

                this.kBest = Utils.maxIndex(this.performances) + 1;
            }

            Collection<TrainingRunnable> trainers = new ArrayList<TrainingRunnable>();
            for (int i = 0; i < this.arfEnsemble.length; i++) {
                DoubleVector vote = new DoubleVector(this.arfEnsemble[i].getVotesForInstance(instance));
                InstanceExample example = new InstanceExample(instance);
                this.arfEnsemble[i].evaluator.addResult(example, vote.getArrayRef());
                int k = MiscUtils.poisson(this.lambdaOption.getValue(), this.classifierRandom);
                if (k > 0) {
                    if (this.executor != null) {
                        TrainingRunnable trainer = new TrainingRunnable(this.arfEnsemble[i],
                                instance, k, this.instancesSeen);
                        trainers.add(trainer);
                    } else {
                        this.arfEnsemble[i].trainOnInstance(instance, k, this.instancesSeen);
                    }
                }
            }
            if (this.executor != null) {
                try {
                    this.executor.invokeAll(trainers);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Could not call invokeAll() on training threads.");
                }
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        int ensembleType = this.ensembleClassOption.getChosenIndex();

        if (ensembleType == 0) { // StreamingRandomPatches
            Instance testInstance = instance.copy();
            testInstance.setMissing(instance.classAttribute());
            testInstance.setClassValue(0.0);
            if (this.ensemble == null)
                initEnsemble(testInstance);
            DoubleVector combinedVote = new DoubleVector();

            // Yibin New
            this.infos = new ArrayList<>();
            for (int i = 0; i < this.ensemble.length; i++) {
                if (this.ensemble[i].classifier instanceof HoeffdingTree) {
                    HoeffdingTree ht = (HoeffdingTree) this.ensemble[i].classifier;
                    this.infos.add(new SortingInformationForDEMS(
                            this.ensemble[i].evaluator.getTotalWeightObserved() == 0 ? 0 : this.ensemble[i].evaluator.getFractionCorrectlyClassified(),
                            ht.getTreeRoot() == null ? new double[]{0} :
                                    ht.getTreeRoot().filterInstanceToLeaf(testInstance, null, -1).node == null ?
                                            new double[]{0} :
                                            ht.getTreeRoot().filterInstanceToLeaf(testInstance, null, -1).node.getObservedClassDistribution(),
                            i));
                }
            }
            this.infos = this.infos.stream().sorted(Comparator.comparing(SortingInformationForDEMS::getMargin_TreeAcc)).collect(Collectors.toList());

            if (this.disableSelfOptimisingOption.isSet())
                this.bestK = this.kValueOption.getValue();

            for (int i = 0; i < this.bestK; i++) {
                SortingInformationForDEMS s = this.infos.get(this.ensemble.length - 1 - i);
                DoubleVector vote = new DoubleVector(this.ensemble[s.getClassifierIndex()].getVotesForInstance(testInstance));
                if (vote.sumOfValues() > 0.0) {
                    vote.normalize();
                    double acc = s.getTreeAcc();
                    if (!this.disableWeightedVote.isSet() && acc > 0.0) {
                        for (int v = 0; v < vote.numValues(); ++v) {
                            vote.setValue(v, vote.getValue(v) * acc);
                        }
                    }
                    combinedVote.addValues(vote);
                }
            }
            return combinedVote.getArrayRef();
        } else { // AdaptiveRandomForest
            Instance testInstance = instance.copy();
            if (this.arfEnsemble == null)
                initEnsemble(testInstance);
            DoubleVector combinedVote = new DoubleVector();

            if (this.disableSelfOptimisingOption.isSet() && this.performances == null)
                this.performances = new int[this.ensembleSizeOption.getValue()];
            this.informations = new ArrayList<>();

            for (int i = 0; i < this.arfEnsemble.length; i++) {
                this.informations.add(new SortingInformationForDEMS(this.arfEnsemble[i].evaluator.getTotalWeightObserved() == 0 ? 0 : this.arfEnsemble[i].evaluator.getFractionCorrectlyClassified(), this.arfEnsemble[i].classifier.getTreeRoot() == null ? new double[]{0} : this.arfEnsemble[i].classifier.getTreeRoot().filterInstanceToLeaf(instance, null, -1).node == null ? new double[]{0} : this.arfEnsemble[i].classifier.getTreeRoot().filterInstanceToLeaf(instance, null, -1).node.getObservedClassDistribution(), i));
            }
            this.informations = this.informations.stream().sorted(Comparator.comparing(SortingInformationForDEMS::getMargin_TreeAcc)).collect(Collectors.toList());

            if (this.disableSelfOptimisingOption.isSet())
                this.kBest = this.kValueOption.getValue();

//
            if (!this.informations.isEmpty()) {
                for (int i = 0; i < this.kBest; i++) {
                    int treeIndex = this.informations.get(this.informations.size() - 1 - i).getClassifierIndex();
//                trees += treeIndex + ", ";
                    ARFBaseLearner arfBaseLearner = this.arfEnsemble[treeIndex];
                    DoubleVector vote = new DoubleVector(arfBaseLearner.getVotesForInstance(testInstance));
                    if (vote.sumOfValues() > 0.0) {
                        vote.normalize();
                        double acc = arfBaseLearner.evaluator.getPerformanceMeasurements()[1].getValue();
                        if (!this.disableWeightedVote.isSet() && acc > 0.0) {
                            for (int v = 0; v < vote.numValues(); ++v) {
                                vote.setValue(v, vote.getValue(v) * acc);
                            }
                        }
                        combinedVote.addValues(vote);
                    }
                }
            }
            return combinedVote.getArrayRef();
        }
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
        int ensembleType = this.ensembleClassOption.getChosenIndex();
        if (ensembleType == 0) {
            initSRPEnsemble(instance);
        } else {
            initARFEnsemble(instance);
        }
    }

    protected void initSRPEnsemble(Instance instance) {
        // Init the ensemble.
        int ensembleSize = this.ensembleSizeOption.getValue();
        this.ensemble = new StreamingRandomPatchesClassifier[ensembleSize];

        BasicClassificationPerformanceEvaluator classificationEvaluator = new BasicClassificationPerformanceEvaluator();

        // #1 Select the size of k, it depends on 2 parameters (subspaceSizeOption and subspaceModeOption).
        int k = this.subspaceSizeOption.getValue();
        if (this.trainingMethodOption.getChosenIndex() != DynamicEnsembleMemberSelection.TRAIN_RESAMPLING) {
            // PS: This applies only to subspaces and random patches option.
            int n = instance.numAttributes() - 1; // Ignore the class label by subtracting 1

            switch (this.subspaceModeOption.getChosenIndex()) {
                case DynamicEnsembleMemberSelection.FEATURES_SQRT:
                    k = (int) Math.round(Math.sqrt(n)) + 1;
                    break;
                case DynamicEnsembleMemberSelection.FEATURES_SQRT_INV:
                    k = n - (int) Math.round(Math.sqrt(n) + 1);
                    break;
                case DynamicEnsembleMemberSelection.FEATURES_PERCENT:
                    double percent = k < 0 ? (100 + k) / 100.0 : k / 100.0;
                    k = (int) Math.round(n * percent);

                    if (Math.round(n * percent) < 2)
                        k = (int) Math.round(n * percent) + 1;
                    break;
            }
            // k is negative, use size(features) + -k
            if (k < 0)
                k = n + k;

            // #2 generate the subspaces
            if (this.trainingMethodOption.getChosenIndex() == DynamicEnsembleMemberSelection.TRAIN_RANDOM_SUBSPACES ||
                    this.trainingMethodOption.getChosenIndex() == DynamicEnsembleMemberSelection.TRAIN_RANDOM_PATCHES) {
                if (k != 0 && k < n) {
                    if (n <= 20 || k < 2) {
                        if (k == 1 && instance.numAttributes() > 2)
                            k = 2;
                        this.subspaces = DynamicEnsembleMemberSelection.allKCombinations(k, n);
                        for (int i = 0; this.subspaces.size() < this.ensemble.length; ++i) {
                            i = i == this.subspaces.size() ? 0 : i;
                            ArrayList<Integer> copiedSubspace = new ArrayList<>(this.subspaces.get(i));
                            this.subspaces.add(copiedSubspace);
                        }
                    } else {
                        this.subspaces = DynamicEnsembleMemberSelection.localRandomKCombinations(k, n,
                                this.ensembleSizeOption.getValue(), this.classifierRandom);
                    }
                } else {
                    this.trainingMethodOption.setChosenIndex(DynamicEnsembleMemberSelection.TRAIN_RESAMPLING);
                }
            }
        }

        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < ensembleSize; ++i) {
            switch (this.trainingMethodOption.getChosenIndex()) {
                case DynamicEnsembleMemberSelection.TRAIN_RESAMPLING:
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
                case DynamicEnsembleMemberSelection.TRAIN_RANDOM_SUBSPACES:
                case DynamicEnsembleMemberSelection.TRAIN_RANDOM_PATCHES:
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

    protected void initARFEnsemble(Instance instance) {
        int ensembleSize = this.ensembleSizeOption.getValue();
        this.arfEnsemble = new ARFBaseLearner[ensembleSize];

        BasicClassificationPerformanceEvaluator classificationEvaluator = new BasicClassificationPerformanceEvaluator();

        this.subspaceSize = this.subspaceSizeOption.getValue();

        int n = instance.numAttributes() - 1; // Ignore class label ( -1 )

        switch (this.subspaceModeOption.getChosenIndex()) {
            case DynamicEnsembleMemberSelection.FEATURES_SQRT:
                this.subspaceSize = (int) Math.round(Math.sqrt(n)) + 1;
                break;
            case DynamicEnsembleMemberSelection.FEATURES_SQRT_INV:
                this.subspaceSize = n - (int) Math.round(Math.sqrt(n) + 1);
                break;
            case DynamicEnsembleMemberSelection.FEATURES_PERCENT:
                double percent = this.subspaceSize < 0 ? (100 + this.subspaceSize) / 100.0 : this.subspaceSize / 100.0;
                this.subspaceSize = (int) Math.round(n * percent);
                break;
        }

        if (this.subspaceSize < 0)
            this.subspaceSize = n + this.subspaceSize;
        if (this.subspaceSize <= 0)
            this.subspaceSize = 1;
        if (this.subspaceSize > n)
            this.subspaceSize = n;

        ARFHoeffdingTree treeLearner = (ARFHoeffdingTree) getPreparedClassOption(this.treeLearnerOption);
        // Instantiate ARFHoeffdingTree directly and prepare it so its internal options/config are initialised
//        ARFHoeffdingTree treeLearner = new ARFHoeffdingTree();
//        treeLearner.prepareForUse();
        treeLearner.subspaceSizeOption.setValue(this.subspaceSize);

        for (int i = 0; i < ensembleSize; ++i) {
            ARFHoeffdingTree baseLearner = (ARFHoeffdingTree) treeLearner.copy();
            baseLearner.setRandomSeed(this.classifierRandom.nextInt());
            baseLearner.resetLearning();

            this.arfEnsemble[i] = new ARFBaseLearner(
                    i,
                    baseLearner,
                    (BasicClassificationPerformanceEvaluator) classificationEvaluator.copy(),
                    this.instancesSeen,
                    !this.disableBackgroundLearnerOption.isSet(),
                    !this.disableDriftDetectionOption.isSet(),
                    this.driftDetectionMethodOption,
                    this.warningDetectionMethodOption,
                    false);
        }
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == DynamicEnsembleMemberSelection.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    @Override
    public Classifier[] getSublearners() {
        int ensembleType = this.ensembleClassOption.getChosenIndex();

        // StreamingRandomPatches sublearners
        if (ensembleType == 0) {
            if (this.ensemble == null) {
                // Ensemble not yet initialised
                return new Classifier[0];
            }
            Classifier[] baseModels = new Classifier[this.ensemble.length];
            for (int i = 0; i < baseModels.length; ++i) {
                baseModels[i] = this.ensemble[i].classifier;
            }
            return baseModels;
        }

        // AdaptiveRandomForest sublearners
        if (this.arfEnsemble == null) {
            // ARF ensemble not yet initialised
            return new Classifier[0];
        }
        Classifier[] forest = new Classifier[this.arfEnsemble.length];
        for (int i = 0; i < forest.length; ++i) {
            forest[i] = this.arfEnsemble[i].classifier;
        }
        return forest;
    }

    private static ArrayList<ArrayList<Integer>> localRandomKCombinations(int k, int length,
                                                                          int nCombinations, Random random) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
        for (int i = 0; i < nCombinations; ++i) {
            ArrayList<Integer> combination = new ArrayList<>();
            // Add all possible items
            for (int j = 0; j < length; ++j)
                combination.add(j);
            // Randomly remove each item by index using the current size
            // Out of "length" items, maintain only "k" items.
            for (int j = 0; j < (length - k); ++j)
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
            allKCombinationsInner(i + 1, k - 1, combination, originalSize, combinations);
            combination.remove(combination.size() - 1);
        }
    }

    private static ArrayList<ArrayList<Integer>> allKCombinations(int k, int length) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();
        ArrayList<Integer> combination = new ArrayList<>();
        allKCombinationsInner(0, k, combination, length, combinations);
        return combinations;
    }


    /**
     * Inner class that represents a single tree member of the forest.
     * It contains some analysis information, such as the numberOfDriftsDetected,
     */
    protected final class ARFBaseLearner extends AbstractMOAObject {
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

            if (this.useDriftDetector) {
                this.driftOption = driftOption;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftOption)).copy();
            }

            // Init Drift Detector for Warning detection.
            if (this.useBkgLearner) {
                this.warningOption = warningOption;
                this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
            }
        }

        public ARFBaseLearner(int indexOriginal, ARFHoeffdingTree instantiatedClassifier, BasicClassificationPerformanceEvaluator evaluatorInstantiated,
                              long instancesSeen, boolean useBkgLearner, boolean useDriftDetector, ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner) {
            init(indexOriginal, instantiatedClassifier, evaluatorInstantiated, instancesSeen, useBkgLearner, useDriftDetector, driftOption, warningOption, isBackgroundLearner);
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
            Instance weightedInstance = instance.copy();
            weightedInstance.setWeight(instance.weight() * weight);

            this.classifier.trainOnInstance(weightedInstance);

            if (this.bkgLearner != null)
                this.bkgLearner.classifier.trainOnInstance(instance);

            // Should it use a drift detector? Also, is it a backgroundLearner? If so, then do not "incept" another one.
            if (this.useDriftDetector && !this.isBackgroundLearner) {
                boolean correctlyClassifies = this.classifier.correctlyClassifies(instance);
                // Check for warning only if useBkgLearner is active
                if (this.useBkgLearner) {
                    // Update the warning detection method
                    this.warningDetectionMethod.input(correctlyClassifies ? 0 : 1);
                    // Check if there was a change
                    if (this.warningDetectionMethod.getChange()) {
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
                if (this.driftDetectionMethod.getChange()) {
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

        @Override
        public void getDescription(StringBuilder sb, int indent) {
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
        public Integer call() {
            run();
            return 0;
        }
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

            if (!this.disableDriftDetector) {
                this.driftOption = driftOption;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(driftOption)).copy();
            }

            // Init Drift Detector for Warning detection.
            if (!this.disableBkgLearner) {
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
            for (int i = 0; i < featuresIndexes.size(); ++i) {
                attSub.add(instance.attribute(featuresIndexes.get(i)));
                this.featureIndexes[i] = featuresIndexes.get(i);
            }
            this.subset = new Instances("Subsets Candidate Instances", attSub, 100);
            this.subset.setClassIndex(this.subset.numAttributes() - 1);
            prepareRandomSubspaceInstance(instance, 1);
        }

        public void prepareRandomSubspaceInstance(Instance instance, double weight) {
            // If there is any instance lingering in the subset, remove it.
            while (this.subset.numInstances() > 0)
                this.subset.delete(0);

            double[] values = new double[this.subset.numAttributes()];
            for (int j = 0; j < this.subset.numAttributes(); ++j)
                values[j] = instance.value(this.featureIndexes[j]);

            // Set the class value for each value array.
            values[values.length - 1] = instance.classValue();
            DenseInstance subInstance = new DenseInstance(1.0, values);
            subInstance.setWeight(weight);
            subInstance.setDataset(this.subset);
            this.subset.add(subInstance);
        }

        private ArrayList<Integer> applySubsetResetStrategy(Instance instance, Random random) {
            if (this.subset != null) {
                ArrayList<Integer> fIndexes = new ArrayList<Integer>();
                for (int j = 0; j < instance.numAttributes(); ++j)
                    fIndexes.add(j);
                // Remove the class label... (it will be added latter)
                fIndexes.remove(instance.classIndex());

                for (int j = 0; j < instance.numAttributes() - this.featureIndexes.length; ++j)
                    fIndexes.remove(random.nextInt(fIndexes.size()));
                // Adding the class label...
                fIndexes.add(instance.classIndex());
                return fIndexes;
            }
            return null;
        }

        public void reset(Instance instance, long instancesSeen, Random random) {

            if (!this.disableBkgLearner && this.bkgLearner != null) {
                this.classifier = this.bkgLearner.classifier;
                this.driftDetectionMethod = this.bkgLearner.driftDetectionMethod;
                this.warningDetectionMethod = this.bkgLearner.warningDetectionMethod;
                this.evaluator = this.bkgLearner.evaluator;
                this.evaluator.reset();
                this.createdOn = this.bkgLearner.createdOn;
                this.subset = this.bkgLearner.subset;
                this.featureIndexes = this.bkgLearner.featureIndexes;
            } else {
                this.classifier.resetLearning();
                this.evaluator.reset();
                this.createdOn = instancesSeen;
                this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftOption)).copy();

                if (this.subset != null) {
                    ArrayList<Integer> fIndexes = this.applySubsetResetStrategy(instance, random);
                    for (int i = 0; i < fIndexes.size(); ++i)
                        this.featureIndexes[i] = fIndexes.get(i);
                    ArrayList<Attribute> attSub = new ArrayList<Attribute>();
                    // Add attributes of the selected subset
                    for (int i = 0; i < this.featureIndexes.length; ++i)
                        attSub.add(instance.attribute(this.featureIndexes[i]));

                    this.subset = new Instances("Subsets Candidate Instances", attSub, 100);
                    this.subset.setClassIndex(this.subset.numAttributes() - 1);
                    prepareRandomSubspaceInstance(instance, 1);
                }
            }
        }

        public void trainOnInstance(Instance instance, double weight, long instancesSeen, Random random) {
            boolean correctlyClassifies;
            // The subset object will be null if we are training with all features
            if (this.subset != null) {
                // Selecting just the subset of features that we are going to use
                prepareRandomSubspaceInstance(instance, weight);

                // After prepareRandomSubspaceInstance, index 0 of subset holds the instance with this learner subspaces
                this.classifier.trainOnInstance(this.subset.get(0));
                correctlyClassifies = this.classifier.correctlyClassifies(this.subset.get(0));
                if (this.bkgLearner != null)
                    this.bkgLearner.trainOnInstance(instance, weight, instancesSeen, random);
            } else {
                Instance weightedInstance = instance.copy();
                weightedInstance.setWeight(instance.weight() * weight);
                this.classifier.trainOnInstance(weightedInstance);
                correctlyClassifies = this.classifier.correctlyClassifies(instance);
                if (this.bkgLearner != null)
                    this.bkgLearner.trainOnInstance(instance, weight, instancesSeen, random);
            }

            if (!this.disableDriftDetector && !this.isBackgroundLearner) {

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
            if (this.subset == null) {
                this.bkgLearner = new StreamingRandomPatchesClassifier(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                        this.disableBkgLearner, this.disableDriftDetector, this.driftOption, this.warningOption, true);
            } else {
                ArrayList<Integer> fIndexes = this.applySubsetResetStrategy(instance, random);

                this.bkgLearner = new StreamingRandomPatchesClassifier(indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                        this.disableBkgLearner, this.disableDriftDetector, this.driftOption, this.warningOption,
                        fIndexes, instance, true);
            }
            this.warningDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.warningOption)).copy();
        }

        /**
         * @param instance
         * @return votes for the given instance
         */
        public double[] getVotesForInstance(Instance instance) {
            if (this.subset != null) {
                prepareRandomSubspaceInstance(instance, 1);
                // subset.get(0) returns the instance transformed to the correct subspace (i.e. current model subspace).
                DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(this.subset.get(0)));

                return vote.getArrayRef();
            }
            DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(instance));
            return vote.getArrayRef();
        }
    }

    public class SortingInformationForDEMS extends AbstractMOAObject {

        private double classifierAcc;

        private double[] votes;

        private double nodeWeight;

        public double getNodeWeight() {
            return nodeWeight;
        }

        public void setNodeWeight(double nodeWeight) {
            this.nodeWeight = nodeWeight;
        }

        public double getNodeDepth() {
            return nodeDepth;
        }

        public void setNodeDepth(double nodeDepth) {
            this.nodeDepth = nodeDepth;
        }

        public double getTreeSize() {
            return treeSize;
        }

        public void setTreeSize(double treeSize) {
            this.treeSize = treeSize;
        }

        private double nodeDepth;
        private double treeSize;

        private double sortingValue;

        private int classifierIndex;

        public double getClassifierAcc() {
            return classifierAcc;
        }

        public void setClassifierAcc(double classifierAcc) {
            this.classifierAcc = classifierAcc;
        }

        public SortingInformationForDEMS() {
            setNodeWeight(1);
            setNodeDepth(1);
            setTreeAcc(1);
            setVotes(new double[1]);
            setTreeSize(1);
            setClassifierAcc(1);
        }

        public SortingInformationForDEMS(double classifierAcc, double[] votes) {
            this.classifierAcc = classifierAcc;
            this.votes = votes;
        }


        public SortingInformationForDEMS(double classifierAcc, double[] votes, int classifierIndex) {
            this.classifierAcc = classifierAcc;
            this.votes = votes;
            this.classifierIndex = classifierIndex;
        }

        public double getTreeAcc() {
            return classifierAcc;
        }

        public void setTreeAcc(double treeAcc) {
            this.classifierAcc = treeAcc;
        }

        public double[] getVotes() {
            return votes;
        }

        public void setVotes(double[] votes) {
            this.votes = votes;
        }

        public double getConfidence() {
            return Utils.sum(this.votes) == 0 ? 0 : this.votes[Utils.maxIndex(this.votes)] / Utils.sum(this.votes);
        }

        public int getClassifierIndex() {
            return classifierIndex;
        }

        public void setClassifierIndex(int classifierIndex) {
            this.classifierIndex = classifierIndex;
        }

        public double getMargin() {
            if (this.votes == null || Utils.sum(this.votes) == 0) return 0;
            double max = Double.MIN_VALUE;
            double secondMax = Double.MIN_VALUE;
            if (this.votes.length == 0) {
                return 0;
            } else if (this.votes.length == 1) {
                max = this.votes[0];
                secondMax = 0;
            } else if (this.votes.length == 2) {
                max = this.votes[Utils.maxIndex(this.votes)];
                secondMax = Utils.sum(this.votes) - max;
            } else {
                for (double v : this.votes) {
                    if (v > max) {
                        secondMax = max;
                        max = v;
                    } else if (v <= max && v > secondMax) {
                        secondMax = v;
                    }
                }
            }

            return (max - secondMax) / Utils.sum(this.votes);
        }


        public double getConfidence_TreeAcc() {
            return getClassifierAcc() * getConfidence();
        }

        public double getMargin_TreeAcc() {
            return getClassifierAcc() * getMargin();
        }

        public void setSortingValue(double value) {
            this.sortingValue = value;
        }

        public double getSortingValue() {
            return this.sortingValue;
        }

        @Override
        public void getDescription(StringBuilder sb, int indent) {

        }
    }

}
