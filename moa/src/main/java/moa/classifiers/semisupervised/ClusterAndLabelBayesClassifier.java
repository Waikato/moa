package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.SphereCluster;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.clusterers.clustream.ClustreamKernel;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * This version of cluster-and-label learner runs naive bayes in the cluster to issue the votes
 */
public class ClusterAndLabelBayesClassifier extends AbstractClassifier implements SemiSupervisedLearner {

    @Override
    public String getPurposeString() {
        return "Cluster-and-label classifier that does Naive Bayes in each cluster to issue predictions";
    }

    private static final long serialVersionUID = 1L;

    /** Lets user choose a clusterer, defaulted to Clustream */
    public ClassOption clustererOption = new ClassOption("clusterer", 'c',
            "A clusterer to perform clustering",
            AbstractClusterer.class, "clustream.Clustream -M");

    /** Lets user decide if they want to use pseudo-labels */
    public FlagOption usePseudoLabelOption = new FlagOption("pseudoLabel", 'p',
            "Using pseudo-label while training");

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    ////////////////////////
    // just some measures //
    ////////////////////////

    ////////////////////////
    // just some measures //
    ////////////////////////

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.clusterer = (AbstractClusterer) getPreparedClassOption(this.clustererOption);
        this.clusterer.prepareForUse();
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        // get the nearest cluster C to X
        Cluster C = this.clusterer.getNearestCluster(inst, false);
        if (C == null) return new double[0];
        // do Naive bayes inside the cluster C
        double[] votes;
        // if enough points, do naive bayes; else, get the label with the highest weight
        if (((CFCluster) C).getN() >= 5) votes = doNaiveBayes(inst, C);
        else votes = C.getLabelVotes();

        // just print stuff out
//        for (int i = 0; i < votes.length; i++) { System.out.print(i + "|" + votes[i] + "  "); }
//        System.out.println();

        return votes;
    }

    private double getBestLabel(Cluster C, Instance inst) {
        double[] votes = getVotesForInstance(inst);
        return Utils.maxIndex(votes);
    }

    private double[] doNaiveBayes(Instance inst, Cluster C) {
        if (C == null || C.getLabelFeature() == null) return new double[0];
        DoubleVector labelFeature = C.getLabelFeature().getData();
        double[] votes = new double[labelFeature.numValues()];
        double observedClassSum = labelFeature.sumOfValues();

        // compute the normalization term in advance
        // Z = P(x) = sum of (class_k * product of (x_i | class_k))
        double normalization = 0;
        for (int classIndex = 0; classIndex < votes.length; classIndex++) { // for each class y_k
            double p_yk = labelFeature.getValue(classIndex) / observedClassSum;
            double p_xy = 1;
            for (int attIndex = 0; attIndex < inst.numAttributes(); attIndex++) { // for every attribute i of x
                if (attIndex == inst.classIndex()) continue;
                int instAttIndex = modelAttIndexToInstanceAttIndex(attIndex, inst);
                AttributeClassObserver obs = C.getAttributeObservers().get(attIndex);
                if ((obs != null) && !inst.isMissing(instAttIndex)) {
                    p_xy *= obs.probabilityOfAttributeValueGivenClass(inst.value(instAttIndex), classIndex);
                }
            }
            normalization += p_yk * p_xy;
        }

        if (normalization == 0) normalization = 1; // i.e. do no normalization at all

        for (int classIndex = 0; classIndex < votes.length; classIndex++) {
            votes[classIndex] = labelFeature.getValue(classIndex) / observedClassSum;
            for (int attIndex = 0; attIndex < inst.numAttributes() - 1; attIndex++) {
                int instAttIndex = modelAttIndexToInstanceAttIndex(attIndex, inst);
                AttributeClassObserver obs = C.getAttributeObservers().get(attIndex);
                if ((obs != null) && !inst.isMissing(instAttIndex)) {
                    votes[classIndex] *= obs.probabilityOfAttributeValueGivenClass(inst.value(instAttIndex), classIndex);
                }
            }
            votes[classIndex] /= normalization;
        }

        // TODO: need logic to prevent underflow?
        return votes;
    }

    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.usePseudoLabel) this.trainOnInstanceWithPseudoLabel(inst);
        else this.trainOnInstanceNoPseudoLabel(inst);
    }

    private void trainOnInstanceNoPseudoLabel(Instance inst) {
        // train stuffs
        this.clusterer.trainOnInstance(inst);
    }

    private void trainOnInstanceWithPseudoLabel(Instance inst) {
        // if the class is masked (simulated as missing) or is missing (for real) --> pseudo-label
        if (inst.classIsMasked() || inst.classIsMissing()) {
            Instance instPseudoLabel = inst.copy();
            double pseudoLabel;
            Cluster C = this.clusterer.getNearestCluster(instPseudoLabel, false);
            pseudoLabel = (C != null ?  getBestLabel(C, inst): 0.0);
            instPseudoLabel.setClassValue(pseudoLabel);
            this.clusterer.trainOnInstance(instPseudoLabel);
        } else {
            this.clusterer.trainOnInstance(inst); // else, just train it normally
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
