package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
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
            AbstractClassifier.class, "trees.HoeffdingTree");

    /** Chooses the global learner */
    public ClassOption globalLearnerOption = new ClassOption("globalLearner", 'g',
            "A learner that observes the stream globally",
            AbstractClassifier.class, "trees.HoeffdingTree");

    /** Lets user decide if they want to use pseudo-labels */
    public FlagOption usePseudoLabelOption = new FlagOption("pseudoLabel", 'p',
            "Using pseudo-label while training");

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** Base learner in each cluster */
    private Classifier baseLearner;

    /** Global learner that observes the stream */
    private Classifier globalLearner;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    ////////////////////////
    // just some measures //
    ////////////////////////
    private int disagreement;
    private int useGlobal;
    private int useLearner;
    private int useLabelFeature;
    private int useLocal;
    ////////////////////////
    // just some measures //
    ////////////////////////

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.clusterer = (AbstractClusterer) getPreparedClassOption(this.clustererOption);
        this.clusterer.prepareForUse();
        this.baseLearner = (AbstractClassifier) getPreparedClassOption(this.baseLearnerOption);
        this.baseLearner.prepareForUse();
        this.globalLearner = (AbstractClassifier) getPreparedClassOption(this.globalLearnerOption);
        this.globalLearner.prepareForUse();
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        super.prepareForUseImpl(monitor, repository);
    }

    private boolean isAllZero(double[] votes) {
        for (int i = 0; i < votes.length; i++) {
            if (votes[i] != 0) return false;
        }
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        // get the nearest cluster C to X
//        Cluster C = this.clusterer.getNearestCluster(inst, false);
//        if (C == null || C.getLearner() == null) return new double[0];
//        double[] votesLocal = C.getLearner().getVotesForInstance(inst);
//        return votesLocal;
//        double[] votesGlobal = this.globalLearner.getVotesForInstance(inst);
//        // count the number of times they disagree
//        if (votesLocal.length != votesGlobal.length) disagreement++;
//        else for (int i = 0; i < votesLocal.length; i++) { if (votesLocal[i] != votesGlobal[i]) disagreement++; }
//        // if the local and global votes agree on the final prediction, choose it
//        if (Utils.maxIndex(votesGlobal) == Utils.maxIndex(votesLocal)) return votesGlobal;
//        else {
//            if (isAllZero(votesLocal)) return votesGlobal;
//            return votesLocal;
//        }

        // get local and global votes
        Cluster C = this.clusterer.getNearestCluster(inst, false);
        double[] localVotes = getLocalVotes(inst, C);
        double[] globalVotes = getGlobalVotes(inst);

        // TODO if inclusion probab too low --> use global votes
        // if localVotes has only 0, return the global votes
        if (isAllZero(localVotes)) {
            useGlobal++;
            return globalVotes;
        }

        // if local and global votes agree on the best label, return the local votes
        if (Utils.maxIndex(localVotes) == Utils.maxIndex(globalVotes)) {
            useLocal++;
            return localVotes;
        }

        // if the cluster issues the local votes has too few points/ratio of L over U is too small --> return global
        if (hasTooFewData(C)) {
            useGlobal++;
            return globalVotes;
        }

        // if all else, return the local votes
        useLocal++;
        return localVotes;
    }

    private boolean hasTooFewData(Cluster C) {
        return ( ((CFCluster) C).getN() < 10
                || ((float)C.getNumLabeledPoints() / (float)C.getNumUnlabeledPoints() < 0.3));
    }

    private double[] getLocalVotes(Instance inst, Cluster C) {
        if (C == null) return new double[0]; // TODO why is C null ??? Should not be!!!
        double[] labelVotes = C.getLabelVotes();
        if (C.getLearner() == null) {
            useLabelFeature++;
            return labelVotes;
        }
        double[] learnerVotes = C.getLearner().getVotesForInstance(inst);

        // TODO has to choose between the local learner's votes (learnerVotes)  OR the label feature (labelVotes)
        if (!isAllZero(labelVotes) & !isAllZero(learnerVotes)) {
            // if they agree then no big deal
            if (Utils.maxIndex(labelVotes) == Utils.maxIndex(learnerVotes)) return labelVotes;
            // otherwise...return the learner votes(?)
            else {
                useLearner++;
                return learnerVotes;
            }
        } else if (isAllZero(labelVotes)) {
            useLearner++;
            return learnerVotes; // if labelVotes has only 0, return learnerVotes
        }
        useLabelFeature++;
        return labelVotes; // or otherwise
    }

    private double[] getLocalVotes(Instance inst) {
        Cluster C = this.clusterer.getNearestCluster(inst, false);
        if (C == null || C.getLearner() == null) return new double[0]; // TODO or get global vote?
        return C.getLearner().getVotesForInstance(inst);
    }

    private double[] getGlobalVotes(Instance inst) {
        return this.globalLearner.getVotesForInstance(inst);
    }

    @Override
    public void resetLearningImpl() {

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
        // train stuffs
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
            // TODO train the updated cluster OR the nearest cluster of the point???
            trainLearnerInCluster(instPseudoLabel, this.clusterer.getUpdatedCluster());
        } else {
            this.clusterer.trainOnInstance(inst); // else, just train it normally
            trainLearnerInCluster(inst, this.clusterer.getUpdatedCluster());
        }
    }

    private void trainLearnerInCluster(Instance inst, Cluster C) {
        if (C != null) { // TODO Should not be null!!!
            if (C.getLearner() == null) C.setLearner(this.baseLearner.copy());
            C.getLearner().trainOnInstance(inst);
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measures = new ArrayList<>();
        measures.add(new Measurement("useGlobal", useGlobal));
        measures.add(new Measurement("useLocal", useLocal));
        measures.add(new Measurement("useLearner", useLearner));
        measures.add(new Measurement("useLabelFeature", useLabelFeature));
        Measurement[] result = new Measurement[measures.size()];
        return measures.toArray(result);
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) { }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
