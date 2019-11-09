package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.util.*;

/**
 * A simple semi-supervised classifier that serves as a baseline.
 * The idea is to group the incoming data into micro-clusters, each of which
 * is assigned a label. The micro-clusters will then be used for classification of unlabeled data.
 */
public class ClusterAndLabelClassifier extends AbstractClassifier
        implements SemiSupervisedLearner {

    private static final long serialVersionUID = 1L;

    /** Lets user choose a clusterer, defaulted to Clustream */
    public ClassOption clustererOption = new ClassOption("clusterer", 'c',
            "A clusterer to perform clustering",
            AbstractClusterer.class, "clustream.Clustream -M");

    /** Lets user decide if they want to use pseudo-labels */
    public FlagOption usePseudoLabelOption = new FlagOption("pseudoLabel", 'p',
            "Using pseudo-label while training");

    /** Decides the labels based on k-nearest cluster, k defaults to 1 */
    public IntOption kNearestClusterOption = new IntOption("kNearestCluster", 'k',
            "Issue predictions based on the majority vote from k-nearest cluster", 1);

    public FlagOption excludeLabelOption = new FlagOption("excludeLabel", 'e',
            "Excludes the label when computing the distance");

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    /** Number of nearest clusters used to issue prediction */
    private int k;

    @Override
    public String getPurposeString() {
        return "A basic semi-supervised learner";
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.clusterer = (AbstractClusterer) getPreparedClassOption(this.clustererOption);
        this.clusterer.prepareForUse();
        this.clusterer.setExcludeLabel(this.excludeLabelOption.isSet());
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        this.k = kNearestClusterOption.getValue();
        super.prepareForUseImpl(monitor, repository);
    }

    private Clustering getClusteringResult() {
        // TODO get the online clustering result?
        if (clusterer.implementsMicroClusterer()) return clusterer.getMicroClusteringResult();
        return clusterer.getClusteringResult();
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");
        // get the votes
        double[] votes = new double[0];
        if (k == 1) {
            Cluster C = this.findClosestCluster(inst, false);
            if (C != null) votes = C.getLabelVotes();
        } else { votes = getVotesFromKClusters(this.findKNearestClusters(inst, this.k)); }
        return votes;
    }

    private double[] getVotesFromKClusters(Cluster[] kC) {
        DoubleVector result = new DoubleVector();
        for (Cluster cluster : kC) {
            if (cluster == null) continue;
            int predictedClass = Utils.maxIndex(cluster.getLabelVotes());
            double oldCount = result.getValue(predictedClass);
            result.setValue(predictedClass, oldCount + cluster.getWeight());
        }
        if (result.numValues() > 0) result.normalize(); // avoid division by 0
        return result.getArrayRef();
    }

    @Override
    public void resetLearningImpl() {
        // just clean up everything
        this.clusterer.resetLearning();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");
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
            if (k == 1) {
                Cluster C = this.findClosestCluster(instPseudoLabel, false);
                pseudoLabel = (C != null ? C.getMajorityLabel() : 0.0);
            } else {
                Cluster[] kC = this.findKNearestClusters(instPseudoLabel, this.k);
                pseudoLabel = Utils.maxIndex(getVotesFromKClusters(kC));
            }
            instPseudoLabel.setClassValue(pseudoLabel);
            this.clusterer.trainOnInstance(instPseudoLabel);
        } else {
            this.clusterer.trainOnInstance(inst); // else, just train it normally
        }
    }

    class DistanceKernelComparator implements Comparator<Cluster> {

        private Instance instance;

        public DistanceKernelComparator(Instance instance) {
            this.instance = instance;
        }

        @Override
        public int compare(Cluster C1, Cluster C2) {
            double distanceC1 = Clusterer.distance(C1.getCenter(), instance.toDoubleArray());
            double distanceC2 = Clusterer.distance(C2.getCenter(), instance.toDoubleArray());
            return Double.compare(distanceC1, distanceC2);
        }
    }

    private Cluster[] findKNearestClusters(Instance instance, int k) {
        Set<Cluster> sortedClusters = new TreeSet<>(new DistanceKernelComparator(instance));
        Clustering clustering = this.getClusteringResult();
        if (clustering == null || clustering.size() == 0) return new Cluster[0];
        sortedClusters.addAll(clustering.getClustering());
        Cluster[] topK = new Cluster[k];
        Iterator<Cluster> it = sortedClusters.iterator();
        int i = 0;
        while (it.hasNext() && i < k) topK[i++] = it.next();
        return topK;
    }

    /**
     * Finds the nearest cluster to the given point.
     * @param instance an instance point
     * @return the nearest cluster if any found, <code>null</code> otherwise
     */
    private Cluster findClosestCluster(Instance instance, boolean includeClass) {
            return clusterer.getNearestCluster(instance, includeClass);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
