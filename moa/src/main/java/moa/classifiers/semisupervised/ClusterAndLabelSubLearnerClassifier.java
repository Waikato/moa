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

    /** How to select the final prediction (selective or majority voting) */
    public MultiChoiceOption predictionOption = new MultiChoiceOption("prediction", 't',
            "Chooses the way we issue the final prediction",
            new String[] {
                    "majorityVoting",
                    "selective",
                    "force"
            },
            new String[] {
                    "Majority voting combined from all three learners",
                    "Selective prediction chosen from one learner only",
                    "Force the learner to always pick the correct prediction (for experiments only)"
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

    ////////////////////////
    // just some measures //
    ////////////////////////
    private int agreementLocalGlobal;
    private int agreementLearnerCluster;
    private int useGlobal;
    private int useLearner;
    private int useLabelFeature;
    private int useLocal;
    private int[][] confusionMatrix; // row 0 = label feature, row 1 = local learner, row 2 = global learner
    private boolean isFromLocal, isFromGlobal, isFromCluster;
    private int[] correctTime;
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
        this.predictionChoice = predictionOption.getChosenIndex();
        this.confusionMatrix = new int[3][3];
        this.correctTime = new int[8];
        super.prepareForUseImpl(monitor, repository);
    }

    private boolean isAllZero(double[] votes) {
        for (double vote : votes) if (vote != 0) return false;
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        double[] votes;
        if (predictionChoice == 0) {
            //return getMajorityVotes(inst); // get simple majority votes from all 3 learners
            votes = getMajorityVotes(inst);
        } else if (predictionChoice == 1) {
            // return getSelectiveVotes(inst); // get selective votes
            votes = getSelectiveVotes(inst);
        } else {
            // force the learner to choose the correct prediction to establish the baseline
            return getForcedVotes(inst);
        }

        // collect data for confusion matrix
        double realLabel = inst.maskedClassValue();
        double predictedLabel = Utils.maxIndex(votes);
        if (realLabel == predictedLabel) {
            if (isFromCluster) { // cluster is correct while local learner & global learner are incorrect
                confusionMatrix[1][0]++;
                confusionMatrix[2][0]++;
            } else if (isFromLocal) { // local learner is correct while cluster & global learner are incorrect
                confusionMatrix[0][1]++;
                confusionMatrix[2][1]++;
            } else if (isFromGlobal) { // global learner is correct while cluster & local learner are incorrect
                confusionMatrix[0][2]++;
                confusionMatrix[1][2]++;
            }
        } else { // no one is correct lmao
            if (isFromCluster) confusionMatrix[0][0]++;
            if (isFromLocal) confusionMatrix[1][1]++;
            if (isFromGlobal) confusionMatrix[2][2]++;
        }

        // reset the flag
        isFromCluster = isFromGlobal = isFromLocal = false;

        return votes;
    }

    private double[] getForcedVotes(Instance inst) {
        // vote from the cluster
        Cluster C = this.clusterer.getNearestCluster(inst, false);
        double[] clusterVotes = C != null ? C.getLabelFeature().getVotes() : null;
        double clusterLabel = clusterVotes != null ? Utils.maxIndex(clusterVotes) : -1;

        // vote from the cluster's local learner (if C is not null)
        double[] localLearnerVotes;
        double localLearnerLabel;
        if (C != null) {
            localLearnerVotes = C.getLearner() != null ? C.getLearner().getVotesForInstance(inst) : null;
            localLearnerLabel = localLearnerVotes != null ? Utils.maxIndex(localLearnerVotes) : -1;
        } else {
            localLearnerVotes = new double[0];
            localLearnerLabel = -1;
        }

        // vote from the global learner
        double[] globalVotes = globalLearner.getVotesForInstance(inst);
        double globalLabel = Utils.maxIndex(globalVotes);

        // real label
        double trueLabel = (!inst.classIsMasked() && !inst.classIsMissing()) ? inst.classValue() : inst.maskedClassValue();

        // check if there is one factor that gets the true label; if there is, force the correct choice, and note which one is that

        // if all is correct: CL + LL + GL
        if (trueLabel == clusterLabel && trueLabel == localLearnerLabel && trueLabel == globalLabel) {
            correctTime[7]++;
            return clusterVotes;
        }
        // if CL + LL correct
        if (trueLabel == clusterLabel && trueLabel == localLearnerLabel) {
            correctTime[4]++;
            return clusterVotes;
        }
        // if CL + GL are correct
        if (trueLabel == clusterLabel && trueLabel == globalLabel) {
            correctTime[5]++;
            return clusterVotes;
        }
        // if LL + GL are correct
        if (trueLabel == localLearnerLabel && trueLabel == globalLabel) {
            correctTime[6]++;
            return globalVotes;
        }
        // if only CL is correct
        if (trueLabel == clusterLabel) {
            correctTime[1]++;
            return clusterVotes;
        }
        // if only LL is correct
        if (trueLabel == localLearnerLabel) {
            correctTime[2]++;
            return localLearnerVotes;
        }
        // if only GL is correct
        if (trueLabel == globalLabel) {
            correctTime[3]++;
            return globalVotes;
        }
        // if no one is correct it sucks lmao
        correctTime[0]++;
        return new double[0];
    }

    private double[] getSelectiveVotes(Instance inst) {
        // get local and global votes
        Cluster C = this.clusterer.getNearestCluster(inst, false);
        double[] localVotes = getLocalVotes(inst, C);
        double[] globalVotes = getGlobalVotes(inst);

        // TODO if inclusion probab too low --> use global votes
        // if localVotes has only 0, return the global votes
        if (isAllZero(localVotes)) {
            useGlobal++;
            isFromGlobal = true;
            return globalVotes;
        }
        // if local and global votes agree on the best label, return the local votes
        if (Utils.maxIndex(localVotes) == Utils.maxIndex(globalVotes)) {
            agreementLocalGlobal++;
            return localVotes;
        }
        // if the cluster issues the local votes has too few points/ratio of L over U is too small --> return global
        if (hasTooFewData(C)) {
            useGlobal++;
            isFromGlobal = true;
            return globalVotes;
        }
        // if all else, return the local votes
        useLocal++;
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
        if (C.getLearner() == null) {
            useLabelFeature++;
            isFromCluster = true;
            return labelVotes;
        }

        double[] learnerVotes = C.getLearner().getVotesForInstance(inst);

        if (!isAllZero(labelVotes) & !isAllZero(learnerVotes)) {
            if (Utils.maxIndex(labelVotes) == Utils.maxIndex(learnerVotes)) {
                useLabelFeature++;
                agreementLearnerCluster++;
                isFromCluster = true;
                return labelVotes;
            }
            else {
                useLearner++;
                isFromLocal = true;
                return learnerVotes;
            }
        } else if (isAllZero(labelVotes)) {
            useLearner++;
            isFromLocal = true;
            return learnerVotes; // if labelVotes has only 0, return learnerVotes
        }
        useLabelFeature++;
        isFromCluster = true;
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
        List<Measurement> measures = new ArrayList<>();
        measures.add(new Measurement("useGlobal", useGlobal));
        measures.add(new Measurement("useLocal", useLocal));
        measures.add(new Measurement("useLearner", useLearner));
        measures.add(new Measurement("useLabelFeature", useLabelFeature));
        measures.add(new Measurement("agreementLocalGlobal", agreementLocalGlobal));
        measures.add(new Measurement("agreementLearnerCluster", agreementLearnerCluster));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                measures.add(new Measurement("confusionMatrix-" + i + "-" + j , confusionMatrix[i][j]));
            }
        }
        for (int i = 0; i < correctTime.length; i++) {
            measures.add(new Measurement("correctTimes-" + i, correctTime[i]));
        }
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
