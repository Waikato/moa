package moa.classifiers.semisupervised;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.core.Measurement;
import moa.clusterers.Clusterer;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.clusterers.AbstractClusterer;
import moa.tasks.TaskMonitor;

import java.util.Objects;

/**
 * A simple semi-supervised classifier that serves as a baseline.
 * The idea is to group the incoming data into micro-clusters, each of which
 * is assigned a label. The micro-clusters will then be used for classification of unlabelled data.
 */
public class BaseSemiSupervisedClassifier extends AbstractClassifier
        implements SemiSupervisedLearner {

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** Lets user choose a clusterer, defaulted to Clustream */
    public ClassOption clustererOption = new ClassOption("clusterer", 'c',
            "A clusterer to perform clustering",
            AbstractClusterer.class, "clustream.Clustream -M");

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "A basic semi-supervised learner";
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.clusterer = (AbstractClusterer) getPreparedClassOption(this.clustererOption);
        this.clusterer.prepareForUse();

        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");

        Clustering clustering = this.clusterer.getClusteringResult();
        if (clustering == null) return new double[0];

        Cluster C = null;
        double maxZIndex = 0;
        for (Cluster cluster : clustering.getClustering()) {
            // force it to be a micro cluster to have the necessary statistics
            if (!(cluster instanceof CFCluster)) break;

            // get the mean and standard deviation to compute z-index
            CFCluster uCluster = (CFCluster) cluster;
            double[] mean = uCluster.getCenter();
            int dim = mean.length;
            double meanZIndex = 0;
            for (int i = 0; i < dim; i++) {
                double std = (uCluster.SS[i] - uCluster.LS[i] / uCluster.getN()) / (uCluster.getN() - 1);
                double zIndex = Math.abs((inst.value(i) - mean[i]) / std);
                meanZIndex += zIndex / (float) dim;
            }
            if (meanZIndex > maxZIndex) {
                maxZIndex = meanZIndex;
                C = uCluster;
            }
        }

        if (C == null) return new double[0];

        System.out.println(C.getInfo());

        // Get the majority votes from labeled instances in C

        // Use it as the prediction

        // Probability: number of that label in C / number of all labeled data in C

        return new double[0];
    }

    @Override
    public void resetLearningImpl() {
        /*
        What to do when resetting learning?
        - clear out all the clusters
         */
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");

        // first, use the clusterer to group instances into micro-cluster
        this.clusterer.trainOnInstance(inst);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
