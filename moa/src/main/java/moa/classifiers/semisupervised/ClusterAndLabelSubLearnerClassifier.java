package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Heterogeneous Dynamic Selection variance of Cluster-and-Label:
 * - Heterogeneous: use a variety of learner (a clusterer, one local learner, one global learner)
 * - Dynamic Selection: the predictions for each instance are decided by a heuristic
 */
public class ClusterAndLabelSubLearnerClassifier extends AbstractClassifier implements SemiSupervisedLearner {
    @Override
    public String getPurposeString() {
        return "Cluster-and-label classifier and each cluster itself has a learner";
    }

    private static final long serialVersionUID = 1L;

    /** Lets user choose a clusterer, defaulted to Clustream */
    public ClassOption clustererOption = new ClassOption("clusterer", 'c',
            "A clusterer to perform clustering",
            AbstractClusterer.class, "clustream.Clustream -M");

    /** Chooses the base learner in each cluster */
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "A learner that trains from the instances in a cluster",
            AbstractClassifier.class, "bayes.NaiveBayes");

    /** Chooses the global learner */
    public ClassOption globalLearnerOption = new ClassOption("globalLearner", 'g',
            "A learner that observes the stream globally",
            AbstractClassifier.class, "trees.HoeffdingTree");

    /** Lets user decide if they want to use pseudo-labels */
    public FlagOption usePseudoLabelOption = new FlagOption("pseudoLabel", 'p',
            "Using pseudo-label while training");

    /** How to select the final prediction (selective or majority voting) */
    public MultiChoiceOption predictionOption = new MultiChoiceOption("prediction", 't',
            "Chooses the way we issue the final prediction",
            new String[] {
                    "majorityVoting",
                    "selective"
            },
            new String[] {
                    "Majority voting combined from all three learners",
                    "Selective prediction chosen from one learner only"
            }, 1);

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** Base learner in each cluster */
    private Classifier baseLearner;

    /** Global learner that observes the stream */
    private Classifier globalLearner;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    /** Which prediction to do */
    private int predictionChoice;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.clusterer = (AbstractClusterer) getPreparedClassOption(this.clustererOption);
        this.clusterer.prepareForUse();
        this.baseLearner = (AbstractClassifier) getPreparedClassOption(this.baseLearnerOption);
        this.baseLearner.prepareForUse();
        this.globalLearner = (AbstractClassifier) getPreparedClassOption(this.globalLearnerOption);
        this.globalLearner.prepareForUse();
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        this.predictionChoice = predictionOption.getChosenIndex();
        super.prepareForUseImpl(monitor, repository);
    }

    private boolean isAllZero(double[] votes) {
        for (double vote : votes) if (vote != 0) return false;
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (predictionChoice == 0) return getMajorityVotes(inst); // get simple majority votes from all 3 learners
        else return getSelectiveVotes(inst); // get selective votes
    }

    private double[] getSelectiveVotes(Instance inst) {
        // get local and global votes
        Cluster C = this.clusterer.getNearestCluster(inst, false);
        double[] localVotes = getLocalVotes(inst, C);
        double[] globalVotes = getGlobalVotes(inst);

        // TODO if inclusion probab too low --> use global votes
        // if localVotes has only 0, return the global votes
        if (isAllZero(localVotes)) return globalVotes;
        // if local and global votes agree on the best label, return the local votes
        if (Utils.maxIndex(localVotes) == Utils.maxIndex(globalVotes)) return localVotes;
        // if the cluster issues the local votes has too few points/ratio of L over U is too small --> return global
        if (hasTooFewData(C)) return globalVotes;
        // if all else, return the local votes
        return localVotes;
    }

    private double[] getMajorityVotes(Instance inst) {
        // get votes from the nearest cluster, local & global learner
        Cluster C = clusterer.getNearestCluster(inst, false);
        double[] clusterVotes = (C != null ? C.getLabelVotes() :  new double[0]);
        double[] localVotes = (C != null && C.getLearner() != null ? C.getLearner().getVotesForInstance(inst) : new double[0]);
        double[] globalVotes = globalLearner.getVotesForInstance(inst);

        // final prediction = majority voting
        DoubleVector prediction = new DoubleVector();
        // TODO the vote from the label feature of the cluster is not always so reliable...
        prediction.addToValue(Utils.maxIndex(clusterVotes), 1);
        prediction.addToValue(Utils.maxIndex(localVotes), 1);
        prediction.addToValue(Utils.maxIndex(globalVotes), 1);
        return prediction.getArrayRef();
    }

    private boolean hasTooFewData(Cluster C) {
        return ( ((CFCluster) C).getN() < 10
                || ((float)C.getNumLabeledPoints() / (float)C.getNumUnlabeledPoints() < 0.3));
    }

    private double[] getLocalVotes(Instance inst, Cluster C) {
        if (C == null) return new double[0];
        double[] labelVotes = C.getLabelVotes();
        if (C.getLearner() == null) return labelVotes;
        double[] learnerVotes = C.getLearner().getVotesForInstance(inst);
        if (!isAllZero(labelVotes) & !isAllZero(learnerVotes)) {
            if (Utils.maxIndex(labelVotes) == Utils.maxIndex(learnerVotes)) return labelVotes;
            else return learnerVotes;
        } else if (isAllZero(labelVotes)) return learnerVotes; // if labelVotes has only 0, return learnerVotes
        return labelVotes; // or otherwise
    }

    private double[] getGlobalVotes(Instance inst) {
        return this.globalLearner.getVotesForInstance(inst);
    }

    @Override
    public void resetLearningImpl() {
        this.globalLearner.resetLearning();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        // update the clusters
        if (this.usePseudoLabel) this.trainOnInstanceWithPseudoLabel(inst);
        else this.trainOnInstanceNoPseudoLabel(inst);

        // train the global learner on this instance
        this.globalLearner.trainOnInstance(inst);
    }

    private void trainOnInstanceNoPseudoLabel(Instance inst) {
        this.clusterer.trainOnInstance(inst);
        trainLearnerInCluster(inst, this.clusterer.getUpdatedCluster());
    }

    private void trainOnInstanceWithPseudoLabel(Instance inst) {
        // if the class is masked (simulated as missing) or is missing (for real) --> pseudo-label
        if (inst.classIsMasked() || inst.classIsMissing()) {
            Instance instPseudoLabel = inst.copy();
            // get the pseudo-label
            double pseudoLabel = Utils.maxIndex(this.getVotesForInstance(inst));
            instPseudoLabel.setClassValue(pseudoLabel);
            this.clusterer.trainOnInstance(instPseudoLabel);
            trainLearnerInCluster(instPseudoLabel, this.clusterer.getUpdatedCluster());
        } else {
            this.clusterer.trainOnInstance(inst); // else, just train it normally
            trainLearnerInCluster(inst, this.clusterer.getUpdatedCluster());
        }
    }

    private void trainLearnerInCluster(Instance inst, Cluster C) {
        if (C != null) {
            if (C.getLearner() == null) C.setLearner(this.baseLearner.copy());
            C.getLearner().trainOnInstance(inst);
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) { }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
