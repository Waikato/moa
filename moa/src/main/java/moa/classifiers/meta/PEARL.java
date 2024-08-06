/*
 *    PEARL.java
 *
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
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.trees.ARFHoeffdingTree;
import moa.core.*;
import moa.evaluation.BasicClassificationPerformanceEvaluator;
import moa.options.ClassOption;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Probabilistic Exact Adaptive Random Forest with Lossy Counting (PEARL)
 *
 * <p> Probabilistic Exact Adaptive Random Forest with Lossy Counting (PEARL),
 * utilises an exact technique and a probabilistic technique to replace drifted
 * trees in an ensemble with relevant trees from a repository of trees for
 * learning from a data stream with recurrent concept drifts.</p>
 *
 * <p>See details in:<br> Ocean Wu, Yun Sing Koh, Gillian Dobbie, Thomas Lacombe
 * Probabilistic exact adaptive random forest for recurrent concepts in data streams.
 * International Journal of Data Science and Analytics, 2022.
 * <a href="https://doi.org/10.1007/s41060-021-00273-1">DOI</a>. </p>
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
 * <li>-r : The number of trees in tree pool (default 100000)</li>
 * <li>-c : The number of candidate trees. (default 120)</li>
 * <li>-k : The kappa parameter for tree swapping. (default 0.0)</li>
 * <li>-e : The edit distance parameter for tree swapping (default 100)</li>
 * <li>-f : The size of LRU state queue (default 10000000)</li>
 * <li>-z : The window size for tracking candidate trees' performance (default 50)</li>
 * <li>-g : Enable lossy state graph</li>
 * <li>-d : Lossy window size (default 100000)</li>
 * <li>-v : Candidate tree reuse rate (default 1.0 [0.0 - 1.0])</li>
 * <li>-y : The reuse window size (default 100000)</li>
 * </ul>
 *
 * BEST values for parameters are not available in the
 * @version $Revision: 1 $
 */
public class PEARL extends AbstractClassifier implements MultiClassClassifier,
                                                                        CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "PEARL framework for evolving data streams from Wu et al.";
    }

    private static final long serialVersionUID = 1L;
//  ##### START of ARF parameters (unchanged for fair comparission with ARF)
    public ClassOption treeLearnerOption = new ClassOption("treeLearner", 'l',
            "Random Forest Tree.", ARFHoeffdingTree.class,
            "ARFHoeffdingTree -e 2000000 -g 50 -c 0.01");

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

    public IntOption numberOfJobsOption = new IntOption("numberOfJobs", 'j',
        "Total number of concurrent jobs used for processing (-1 = as much as possible, 0 = do not use multithreading)", 1, -1, Integer.MAX_VALUE);

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
        "Change detector for drifts and its parameters", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-3");

    public ClassOption warningDetectionMethodOption = new ClassOption("warningDetectionMethod", 'p',
        "Change detector for warnings (start training bkg learner)", ChangeDetector.class, "ADWINChangeDetector -a 1.0E-2");

    public FlagOption disableWeightedVote = new FlagOption("disableWeightedVote", 'w',
            "Should use weighted voting?");

    public FlagOption disableDriftDetectionOption = new FlagOption("disableDriftDetection", 'u',
        "Should use drift detection? If disabled then bkg learner is also disabled");

    public FlagOption disableBackgroundLearnerOption = new FlagOption("disableBackgroundLearner", 'q',
        "Should use bkg learner? If disabled then reset tree immediately.");
//  ##### END of ARF parameters (unchanged for fair comparission with ARF)

    public IntOption treeRepoSizeOption = new IntOption("treeRepoSize", 'r',
            "The number of trees in tree pool.", 100000, 60, Integer.MAX_VALUE);

    public IntOption candidatePoolSizeOption = new IntOption("candidatePoolSize", 'c',
            "The number of candidate trees.", 120, 60, Integer.MAX_VALUE);

    public FloatOption cdKappaOption = new FloatOption("cdKappa", 'k',
            "The kappa parameter for tree swapping.", 0.0, 0.0, Float.MAX_VALUE);

    public IntOption editDistanceOption = new IntOption("editDistance", 'e',
            "The edit distance parameter for tree swapping", 100, 60, 120);

    public IntOption lruQueueSize = new IntOption("lruQueueSize", 'f',
            "The size of LRU state queue", 10000000, 100, Integer.MAX_VALUE);

    public IntOption performanceEvalWindowSize = new IntOption("performanceEvalWindowSize", 'z',
            "The window size for tracking candidate trees' performance",50, 1, Integer.MAX_VALUE);

    public FlagOption enableStateGraph = new FlagOption("enableStateGraph", 'g',
            "Is lossy state graph enabled");

    public IntOption lossyWindowSizeSizeOption = new IntOption("lossyWindowSize", 'd',
            "Lossy window size", 100000, 100, Integer.MAX_VALUE);

    // according to the paper though v does not have a significan effect on predictive capability of the model
    // using the default used in the Agrawal experiments https://github.com/ingako/PEARL/blob/master/run/run-agrawal-3.sh
    public FloatOption candidateTreeReuseRate = new FloatOption("candidateTreeReuseRate", 'v',
            "Candidate tree reuse rate.", 0.4, 0.0, 1.0);

    public IntOption reuseWindowSizeOption = new IntOption("reuseWindowSize", 'y',
            "The reuse window size",100000, 1, Integer.MAX_VALUE);

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

    // PEARL data structures
    protected ArrayList<ARFBaseLearner> treePool = new ArrayList<>();
    protected ArrayList<ARFBaseLearner> candidateTrees = new ArrayList();
    protected ArrayList actualLabels = new ArrayList<>();
    protected static LRUState stateQueue;
    protected SortedSet<Integer> curState = new TreeSet<>();
    protected LossyStateGraph stateGraph;
    protected StateGraphSwitch graphSwitch;

//    @Override
//    public int getClassifierPoolSize() {
//        return treePool.size();
//    }

    public int getPredictedClass(Instance inst) {
        return Utils.maxIndex(getVotesForInstance(inst));
    }

    @Override
    public void resetLearningImpl() {
        // Reset attributes
        this.ensemble = null;
        this.subspaceSize = 0;
        this.instancesSeen = 0;
        this.evaluator = new BasicClassificationPerformanceEvaluator();

        // Multi-threading
        int numberOfJobs;
        if (this.numberOfJobsOption.getValue() == -1)
            numberOfJobs = Runtime.getRuntime().availableProcessors();
        else
            numberOfJobs = this.numberOfJobsOption.getValue();
        // SINGLE_THREAD and requesting for only 1 thread are equivalent.
        // this.executor will be null and not used...
        if(numberOfJobs != PEARL.SINGLE_THREAD && numberOfJobs != 1)
            this.executor = Executors.newFixedThreadPool(numberOfJobs);
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        if(this.ensemble == null)
            initEnsemble(instance);

        ArrayList<Integer> warningTreePosList = new ArrayList<>();
        ArrayList<Integer> driftedTreePosList = new ArrayList<>();

        if (this.actualLabels.size() >= this.performanceEvalWindowSize.getValue()) {
            this.actualLabels.remove(0);
        }
        this.actualLabels.add((int) instance.classValue());

        Collection<TrainingRunnable> trainers = new ArrayList<TrainingRunnable>();
        for (int i = 0 ; i < this.ensemble.length ; i++) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(instance));
            InstanceExample example = new InstanceExample(instance);
            this.ensemble[i].evaluator.addResult(example, vote.getArrayRef());
            int k = MiscUtils.poisson(this.lambdaOption.getValue(), this.classifierRandom);
            if (k > 0) {
                if (this.executor != null) {
                    TrainingRunnable trainer = new TrainingRunnable(this.ensemble[i],
                        instance, k, this.instancesSeen);
                    trainers.add(trainer);
                } else { // SINGLE_THREAD is in-place...
                    DriftInfo driftInfo = this.ensemble[i].trainOnInstance(instance, k, this.instancesSeen);
                    boolean warningDetectedOnly = false;
                    if (driftInfo.warningDetected) {
                        warningDetectedOnly = true;
                    }
                    if (driftInfo.driftDetected) {
                        warningDetectedOnly = false;
                        driftedTreePosList.add(i);
                    }
                    if (warningDetectedOnly) {
                        warningTreePosList.add(i);
                    }
                }

            }
        }

        for (ARFBaseLearner tree : this.candidateTrees) {
            // candidateTrees performs predictions to keep track of performance
            if (tree.predictedLabelsWindow.size() >=  performanceEvalWindowSize.getValue()) {
                tree.predictedLabelsWindow.remove(0);
            }
            tree.predictedLabelsWindow.add(getPredictedClass(instance));
        }

        if (warningTreePosList.size() > 0) {
            selectCandidateTrees(warningTreePosList);
        }

        if (driftedTreePosList.size() > 0) {
            adaptState(instance, driftedTreePosList);
        }

        if (this.executor != null) {
            try {
                this.executor.invokeAll(trainers);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Could not call invokeAll() on training threads.");
            }
        }
    }

    private void selectCandidateTrees(ArrayList<Integer> warningTreePosList) {
        if (this.enableStateGraph.isSet()) {
            // try trigger lossy counting
            if (this.stateGraph.update(warningTreePosList.size())) {
                // TODO log
            }
        }

        // add selected neighbors as candidate trees if graph is stable
        if (this.stateGraph.getIsStable()) {
            treeTransition(warningTreePosList);
        }

        // trigger pattern matching if graph has become unstable
        if (!this.stateGraph.getIsStable()) {
            patternMatchCandidateTrees(warningTreePosList);

        } else {
            // TODO log
        }
    }

    void patternMatchCandidateTrees(ArrayList<Integer> warningTreePosList) {

        Set<Integer> ids_to_exclude = new HashSet<>();

        for (int tree_pos : warningTreePosList) {
            ARFBaseLearner curTree = this.ensemble[tree_pos];

            if (curTree.treePoolId == -1) {
                System.out.println("Error: tree_pool_id is not updated");
                System.exit(1);
            }

            ids_to_exclude.add(curTree.treePoolId);
        }

        Set<Integer> closestState = this.stateQueue.getClosestState(curState, ids_to_exclude);

        if (closestState.size() == 0) {
            return;
        }

        for (int i : closestState) {
            if ( (i < this.curState.size()) && (i < this.treePool.size()) && !this.curState.contains(i) && !this.treePool.get(i).isCandidate) {

                if (this.candidateTrees.size() >= this.candidatePoolSizeOption.getValue()) {
                    this.candidateTrees.get(0).isCandidate = false;
                    this.candidateTrees.remove(0);
                }

                this.treePool.get(i).isCandidate = true;
                this.candidateTrees.add(treePool.get(i));
            }
        }
    }

    private void treeTransition(ArrayList<Integer> warningTreePosList) {
        ARFBaseLearner cur_tree;
        for (int warning_tree_pos : warningTreePosList) {
            cur_tree = ensemble[warning_tree_pos];

            int warning_tree_id = cur_tree.treePoolId;
            int next_id = stateGraph.get_next_tree_id(warning_tree_id);

            if (next_id == -1) {
                stateGraph.set_is_stable(false);
            } else {
                if (!treePool.get(next_id).isCandidate) {
                    // TODO
                    if (candidateTrees.size() >= candidatePoolSizeOption.getValue()) {
                        candidateTrees.get(0).isCandidate = false;
                        candidateTrees.remove(0);
                    }
                    treePool.get(next_id).isCandidate = true;
                    candidateTrees.add(treePool.get(next_id));
                }
            }
        }
    }

    private void adaptState(Instance instance, ArrayList<Integer> driftedTreePosList) {
        int class_count = instance.numClasses();

        // sort candidate trees by kappa
        for (ARFBaseLearner candidateTree: this.candidateTrees) {
            candidateTree.updateKappa(this.actualLabels, class_count);
        }
        Collections.sort(this.candidateTrees,
                (tree1, tree2) -> Double.compare(tree1.kappa, tree2.kappa));
        // TODO validate sorting order

        for (int i = 0; i < driftedTreePosList.size(); i++) {
            // TODO
            if (this.treePool.size() >= this.treeRepoSizeOption.getValue()) {
                System.out.println("tree_pool full: " + this.treePool.size());
                System.exit(1);
            }

            int drifted_pos = driftedTreePosList.get(i);
            ARFBaseLearner drifted_tree = this.ensemble[drifted_pos];
            ARFBaseLearner swap_tree = null;

            drifted_tree.updateKappa(actualLabels, class_count);

            boolean add_to_repo = false;

            if (candidateTrees.size() > 0) {
                ARFBaseLearner bestCandidate = candidateTrees.get(candidateTrees.size() - 1);
                if (drifted_tree.isEvalReady
                    && bestCandidate.isEvalReady
                    && bestCandidate.kappa - drifted_tree.kappa >= cdKappaOption.getValue()) {

                    bestCandidate.isCandidate = false;
                    swap_tree = bestCandidate;
                    candidateTrees.remove(candidateTrees.size() - 1);

                    if (this.enableStateGraph.isSet()) {
                        graphSwitch.update_reuse_count(1);
                    }
                }
            }

            if (swap_tree == null) {
                add_to_repo = true;

                if (this.enableStateGraph.isSet()) {
                    graphSwitch.update_reuse_count(0);
                }

                ARFBaseLearner bkgLearner = drifted_tree.bkgLearner;

                if (bkgLearner == null) {
                    swap_tree = drifted_tree.makeTree(treePool.size());

                } else {
                    bkgLearner.updateKappa(actualLabels, class_count);

                    if (!bkgLearner.isEvalReady || !drifted_tree.isEvalReady
                            || bkgLearner.kappa - drifted_tree.kappa >= 0.0) {
                        // TODO 0.0: bg_kappa_threshold
                        swap_tree = bkgLearner;

                    } else {
                        // bg tree is a false positive
                        add_to_repo = false;

                    }
                }

                if (add_to_repo) {
                    swap_tree.reset(false);

                    // assign a new tree_pool_id for background tree
                    // and allocate a slot for background tree in tree_pool
                    swap_tree.treePoolId = treePool.size();
                    treePool.add(swap_tree);

                }
            }

            if (swap_tree != null) {
                // update current state pattern
                curState.remove(drifted_tree.treePoolId);
                curState.add(swap_tree.treePoolId);

                // replace drifted_tree with swap tree
                swap_tree.isBackgroundLearner = false;
                ensemble[drifted_pos] = swap_tree;

                if (this.enableStateGraph.isSet()) {
                    stateGraph.add_edge(drifted_tree.treePoolId, swap_tree.treePoolId);
                }

            }

            drifted_tree.reset(false);
        }

        this.stateQueue.enqueue(new TreeSet<>(curState));

        if (this.enableStateGraph.isSet()) {
            this.graphSwitch.update_switch();
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
            case PEARL.FEATURES_SQRT:
                this.subspaceSize = (int) Math.round(Math.sqrt(n)) + 1;
                break;
            case PEARL.FEATURES_SQRT_INV:
                this.subspaceSize = n - (int) Math.round(Math.sqrt(n) + 1);
                break;
            case PEARL.FEATURES_PERCENT:
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

        this.stateQueue = new LRUState(lruQueueSize.getValue(), this.editDistanceOption.getValue());

        this.stateGraph = new LossyStateGraph(this.treeRepoSizeOption.getValue(),
                                              this.lossyWindowSizeSizeOption.getValue());
        this.graphSwitch = new StateGraphSwitch(this.stateGraph,
                                                this.reuseWindowSizeOption.getValue(),
                                                this.candidateTreeReuseRate.getValue());

        ARFHoeffdingTree treeLearner = (ARFHoeffdingTree) getPreparedClassOption(this.treeLearnerOption);
        treeLearner.resetLearning();

        for (int i = 0; i < ensembleSize; ++i) {
            // treeLearner.setRandomSeed(this.classifierRandom.nextInt());
            // treeLearner.resetClassifierRandom();

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

            this.treePool.add(this.ensemble[i]);
            this.curState.add(i);
            this.ensemble[i].treePoolId = i;
        }

        this.stateQueue.enqueue(new TreeSet<>(curState));
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == PEARL.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    /**
     * Inner class that represents a single tree member of the forest.
     * It contains some analysis information, such as the numberOfDriftsDetected,
     */
    protected final class ARFBaseLearner  extends AbstractMOAObject {
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

        // PEARL specific
        protected int treePoolId;
        protected double kappa;
        protected boolean isCandidate;
        protected boolean isEvalReady;
        protected ArrayList<Integer> predictedLabelsWindow;

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

            this.treePoolId = -1;
            this.kappa = Integer.MIN_VALUE;
            this.isCandidate = false;
            this.isEvalReady = false;
            this.predictedLabelsWindow = new ArrayList<>();
        }

        public ARFBaseLearner(int indexOriginal, ARFHoeffdingTree instantiatedClassifier, BasicClassificationPerformanceEvaluator evaluatorInstantiated,
                    long instancesSeen, boolean useBkgLearner, boolean useDriftDetector, ClassOption driftOption, ClassOption warningOption, boolean isBackgroundLearner) {
            init(indexOriginal, instantiatedClassifier, evaluatorInstantiated, instancesSeen, useBkgLearner, useDriftDetector, driftOption, warningOption, isBackgroundLearner);
        }

        public void reset(boolean keepDriftDetectors) {
            if (!keepDriftDetectors) {
                this.driftDetectionMethod.resetLearning();
                this.warningDetectionMethod.resetLearning();
            }
            reset();
        }

        public void reset() {
            this.bkgLearner = null;
            this.evaluator.reset();

            this.isCandidate = false;
            this.predictedLabelsWindow.clear();
            this.kappa = Integer.MIN_VALUE;
            this.isEvalReady = false;
        }

        public ARFBaseLearner makeTree(int treeId) {
            ARFHoeffdingTree bkgClassifier = (ARFHoeffdingTree) this.classifier.copy();
            bkgClassifier.resetLearning();
            // bkgClassifier.setRandomSeed(this.classifier.classifierRandom.nextInt());
            // bkgClassifier.resetClassifierRandom();
            BasicClassificationPerformanceEvaluator bkgEvaluator = (BasicClassificationPerformanceEvaluator) this.evaluator.copy();
            bkgEvaluator.reset();
            ARFBaseLearner newTree = new ARFBaseLearner(this.indexOriginal, bkgClassifier, bkgEvaluator, instancesSeen,
                    this.useBkgLearner, this.useDriftDetector, this.driftOption, this.warningOption, false);
            newTree.treePoolId = treeId;
            return newTree;
        }

        public DriftInfo trainOnInstance(Instance instance, double weight, long instancesSeen) {
            Instance weightedInstance = (Instance) instance.copy();
            weightedInstance.setWeight(instance.weight() * weight);
            this.classifier.trainOnInstance(weightedInstance);

            // train bg tree and track its performance
            if (this.bkgLearner != null) {
                this.bkgLearner.classifier.trainOnInstance(instance);
                int prediction = getPredictedClass(instance);
                if (this.bkgLearner.predictedLabelsWindow.size() >= performanceEvalWindowSize.getValue()) {
                    this.bkgLearner.predictedLabelsWindow.remove(0);
                }
                this.bkgLearner.predictedLabelsWindow.add(prediction);
            }

            boolean warningDetected = false;
            boolean driftDetected = false;

            // Should it use a drift detector? Also, is it a backgroundLearner? If so, then do not "incept" another one. 
            if (this.useDriftDetector && !this.isBackgroundLearner) {
                // boolean correctlyClassifies = this.classifier.correctlyClassifies(instance);
                int prediction =  Utils.maxIndex(getVotesForInstance(instance));
                boolean correctlyClassifies = prediction == (int) instance.classValue();

                if (this.predictedLabelsWindow.size() >= performanceEvalWindowSize.getValue()) {
                    this.predictedLabelsWindow.remove(0);
                }
                this.predictedLabelsWindow.add(prediction);

                // Check for warning only if useBkgLearner is active
                if (this.useBkgLearner) {
                    // Update the warning detection method
                    this.warningDetectionMethod.input(correctlyClassifies ? 0 : 1);
                    // Check if there was a change
                    if (this.warningDetectionMethod.getChange()) {
                        warningDetected = true;
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
                        // this.bkgLearner.classifier.setRandomSeed(this.classifier.classifierRandom.nextInt());
                        // this.bkgLearner.classifier.resetClassifierRandom();

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
                    warningDetected = false;
                    driftDetected = true;
                    this.lastDriftOn = instancesSeen;
                    this.numberOfDriftsDetected++;
                    // this.reset();
                }
            }

            return new DriftInfo(warningDetected, driftDetected, getPredictedClass(instance));
        }

        public void updateKappa(ArrayList<Integer> actualLabels, int classCount) {
            if (predictedLabelsWindow.size() < performanceEvalWindowSize.getValue()
                    || actualLabels.size() < performanceEvalWindowSize.getValue()) {
                this.isEvalReady = false;
                return;
            }

            this.isEvalReady = true;

            int[][] confusionMatrix = new int[classCount][classCount];
            int correct = 0;

            for (int i = 0; i < performanceEvalWindowSize.getValue(); i++) {
                confusionMatrix[actualLabels.get(i)][predictedLabelsWindow.get(i)]++;
                if (actualLabels.get(i) == predictedLabelsWindow.get(i)) {
                    correct++;
                }
            }

            double accuracy = (double) correct / performanceEvalWindowSize.getValue();
            this.kappa = computeKappa(confusionMatrix, accuracy, performanceEvalWindowSize.getValue(), classCount);
        }

        private double computeKappa(int[][] confusionMatrix, double accuracy, int sample_count, int classCount) {
            // computes the Cohen's kappa coefficient
            double p0 = accuracy;
            double pc = 0.0;
            int row_count = classCount;
            int col_count = classCount;

            for (int i = 0; i < row_count; i++) {
                double row_sum = 0;
                for (int j = 0; j < col_count; j++) {
                    row_sum += confusionMatrix[i][j];
                }

                double col_sum = 0;
                for (int j = 0; j < row_count; j++) {
                    col_sum += confusionMatrix[j][i];
                }

                pc += (row_sum / sample_count) * (col_sum / sample_count);
            }

            if (pc == 1) {
                return 1;
            }

            return (p0 - pc) / (1.0 - pc);
        }

        public double[] getVotesForInstance(Instance instance) {
            DoubleVector vote = new DoubleVector(this.classifier.getVotesForInstance(instance));
            return vote.getArrayRef();
        }

        @Override
        public void getDescription(StringBuilder sb, int indent) {

        }

        // @Override
        // public void getDescription(StringBuilder sb, int indent) {
        // }
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

    protected class LRUState extends AbstractMOAObject {

        @Override
        public void getDescription(StringBuilder sb, int indent) {

        }

        class State extends AbstractMOAObject{
            SortedSet<Integer> pattern;
            public int freq;

            State(SortedSet<Integer> pattern, int freq) {
                this.pattern = pattern;
                this.freq = freq;
            }

            @Override
            public void getDescription(StringBuilder sb, int indent) {

            }
        }

        int capacity;
        int editDistanceThreshold;
        LinkedHashMap<String, State> map;

        protected LRUState(int capacity, int editDistanceThreshold) {
            this.capacity = capacity;
            this.editDistanceThreshold = editDistanceThreshold;
            this.map = new LinkedHashMap<String, State>(capacity, 0.75f, true){
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > capacity;
                }
            };
        }

        Set<Integer> getClosestState(Set<Integer> targetPattern, Set<Integer> idsToExclude) {
            int minEditDistance = Integer.MAX_VALUE;
            int maxFreq = 0;
            Set<Integer> closestPattern = new HashSet<>();

            // find the smallest edit distance
            for (Map.Entry<String, State> entry : map.entrySet()) {
                State curState = entry.getValue();
                Set<Integer> curPattern = curState.pattern;

                int curFreq = curState.freq;
                int curEditDistance = 0;

                boolean updateFlag = true;
                for (int id : idsToExclude) {
                    if (curPattern.contains(id)) {
                        // tree with drift must be unset
                        updateFlag = false;
                        break;
                    }
                }

                if (updateFlag) {
                    for (int id : targetPattern) {
                        if (!curPattern.contains(id)) {
                            curEditDistance += 2;
                        }

                        if (curEditDistance > editDistanceThreshold
                                || curEditDistance > minEditDistance) {
                            updateFlag = false;
                            break;
                        }
                    }
                }

                if (!updateFlag) {
                    continue;
                }

                if (minEditDistance == curEditDistance && curFreq < maxFreq) {
                    continue;
                }

                minEditDistance = curEditDistance;
                maxFreq = curFreq;
                closestPattern = curPattern;
            }

            return closestPattern;
        }

        public void enqueue(SortedSet<Integer> pattern) {
            String key = patternToKey(pattern);

            if (map.containsKey(key)) {
                State state = map.get(key);
                state.freq++;

            } else {
                map.put(key, new State(pattern, 1));
            }
        }

        String patternToKey(SortedSet<Integer> pattern) {
            StringBuilder sb = new StringBuilder();
            for (int i : pattern) {
                sb.append(i);
                sb.append(",");
            }

            return sb.toString();
        }

        public String toString() {
            String str = "";

            for (Map.Entry<String, State> entry : map.entrySet()) {
                State s = entry.getValue();
                Set<Integer> cur_pattern = s.pattern;
                String freq = "" + s.freq;

                String delim = "";
                for (int i : cur_pattern) {
                    str += delim;
                    str += i;
                    delim = ",";
                }
                str += ":" + freq + "->";
            }

            return str;
        }
    }


    class LossyStateGraph extends AbstractMOAObject{

        @Override
        public void getDescription(StringBuilder sb, int indent) {

        }

        class Node extends AbstractMOAObject{
            int indegree;
            int total_weight;
            Map<Integer, Integer> neighbors; // <tree_id, freq>
            public Node() {
                this.indegree = 0;
                this.total_weight = 0;
                this.neighbors = new HashMap<>();
            }

            @Override
            public void getDescription(StringBuilder sb, int indent) {

            }
        };

        Node[] graph;
        int capacity;
        int window_size;
        Random mrand;

        int drifted_tree_counter = 0;
        boolean is_stable = false;

        LossyStateGraph(int capacity, int window_size) {
            this.capacity = capacity;
            this.window_size = window_size;
            this.mrand = new Random();

            is_stable = false;
            graph = new Node[capacity];
        }

        int get_next_tree_id(int src) {
            if (graph[src] == null|| graph[src].total_weight == 0){
                return -1;
            }

            int r = mrand.nextInt(graph[src].total_weight + 1);
            int sum = 0;

            // weighted selection
            for (Map.Entry<Integer, Integer> nei : graph[src].neighbors.entrySet()){
                int treeId = nei.getKey();
                int freq = nei.getValue();
                sum += freq;
                if (r < sum) {
                    nei.setValue(nei.getValue() + 1);
                    graph[src].total_weight++;
                    return treeId;
                }
            }

            return -1;
        }

        boolean update(int warning_tree_count) {
            drifted_tree_counter += warning_tree_count;

            if (drifted_tree_counter < window_size) {
                return false;
            }

            drifted_tree_counter -= window_size;

            // lossy count
            for (int i = 0; i < graph.length; i++) {
                if (graph[i] != null) {
                    continue;
                }

                ArrayList<Integer> keys_to_remove = new ArrayList<>();

                for (Map.Entry<Integer, Integer> nei : graph[i].neighbors.entrySet()){
                    int treeId = nei.getKey();
                    int freq = nei.getValue();

                    // decrement freq by 1
                    graph[i].total_weight--;
                    nei.setValue(nei.getValue() - 1); // decrement freq

                    if (freq == 0) {
                        // remove edge
                        graph[treeId].indegree--;
                        try_remove_node(treeId);

                        keys_to_remove.add(treeId);
                    }
                }

                for (int key : keys_to_remove){
                    graph[i].neighbors.remove(key);
                }

                try_remove_node(i);
            }

            return true;
        }

        void try_remove_node(int key) {
            if (graph[key].indegree == 0 && graph[key].neighbors.size() == 0){
                graph[key] = null;
            }
        }

        void add_node(int key) {
            if (key >= capacity) {
                // System.out.println("id exceeded graph capacity");
                return;
            }

            graph[key] = new Node();
        }

        void add_edge(int src, int dest) {
            if (graph[src] == null) {
                add_node(src);
            }

            if (graph[dest] == null) {
                add_node(dest);
            }

            graph[src].total_weight++;

            if (!graph[src].neighbors.containsKey(dest)) {
                graph[src].neighbors.put(dest, 0);
                graph[dest].indegree++;
            }

            graph[src].neighbors.put(dest, graph[src].neighbors.get(dest) + 1);
        }

        void set_is_stable(boolean is_stable_) {
            is_stable = is_stable_;
        }

        boolean getIsStable() {
            return is_stable;
        }

        // String to_string() {
        //     stringstream ss;
        //     for (int i = 0; i < graph.size(); i++) {
        //         ss << i;
        //         if (!graph[i]) {
        //             ss << " {}" << endl;
        //             continue;
        //         }

        //         ss << " w:" << std::to_string (graph[i]->total_weight) <<" {";
        //         for (auto & nei :graph[i]->neighbors){
        //             ss << std::to_string (nei.first) << ":" << std::to_string (nei.second) << " ";
        //         }
        //         ss << "}" << endl;
        //     }

        //     return ss.str();
        // }
    }

    class StateGraphSwitch extends AbstractMOAObject{

        int window_size = 0;
        int reused_tree_count = 0;
        int total_tree_count = 0;
        double reuse_rate = 1.0;

        LossyStateGraph state_graph;
        ArrayDeque <Integer> window;

        public StateGraphSwitch(LossyStateGraph state_graph,
                                int window_size,
                                double reuse_rate) {
            this.state_graph = state_graph;
            this.window_size = window_size;
            this.reuse_rate = reuse_rate;
            this.window = new ArrayDeque<>();
        }

        void update_reuse_count(int num_reused_trees) {
            reused_tree_count += num_reused_trees;
            total_tree_count++;

            if (window_size <= 0) {
                return;
            }

            if (window.size() >= window_size) {
                reused_tree_count -= window.poll();
            }

            window.offer(num_reused_trees);
        }

        void update_switch() {
            double cur_reuse_rate = 0;
            if (window_size <= 0) {
                cur_reuse_rate = (double) reused_tree_count / total_tree_count;
            } else {
                cur_reuse_rate = (double) reused_tree_count / window_size;
            }

            // cout << "reused_tree_count: " << to_string(reused_tree_count)  << endl;
            // cout << "total_tree_count: " << to_string(total_tree_count)  << endl;
            // cout << "cur_reuse_rate: " << to_string(cur_reuse_rate)  << endl;

            if (cur_reuse_rate >= reuse_rate) {
                state_graph.set_is_stable(true);
            } else {
                state_graph.set_is_stable(false);
            }
        }

        @Override
        public void getDescription(StringBuilder sb, int indent) {

        }
    }

    public class DriftInfo {
        public boolean warningDetected;
        public boolean driftDetected;
        public int predictedClass;
        public DriftInfo(boolean warningDetected, boolean driftDetected, int predictedClass) {
            this.warningDetected = warningDetected;
            this.driftDetected = driftDetected;
            this.predictedClass = predictedClass;
        }
    }
}
