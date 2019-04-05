package moa.classifiers.semisupervised;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.clusterers.semisupervised.ClustreamSSL;
import moa.clusterers.semisupervised.LabeledClustreamKernel;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.util.*;

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

    /** Count the number of times a class is predicted */
    private int[] predictionCount;

    /** Maximum number of memorized labels */
    private int maxCount = 0;

    private int nullCTimes = 0;

    private int notNullCTimes = 0;

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

    private Clustering getClusteringResult() {
        return (this.clusterer.getClusteringResult() != null ?
                this.clusterer.getClusteringResult() : this.clusterer.getMicroClusteringResult());
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");
        LabeledClustreamKernel C = this.findClosestCluster(inst);
        if (C == null) {
            nullCTimes++;
            return new double[0];
        }

        double[] votes = C.getLabelVotes();
        if (this.predictionCount == null) this.predictionCount = new int[inst.dataset().numClasses()];
        int predictedClass = Utils.maxIndex(votes);
        predictionCount[predictedClass]++;
        C.incrementTimesGivingPrediction(1);
        C.setHasGivenPrediction();
        this.notNullCTimes++;

        return votes;
    }

    @Override
    public void resetLearningImpl() {
        // just clean up everything
        this.clusterer.resetLearning();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");
        this.clusterer.trainOnInstance(inst);

        // If X has no label, do nothing
        if (inst.classIsMissing()) return;

        // Else, update the cluster's label
        Cluster C = this.clusterer.getUpdatedCluster();
        if (C == null) return;
        if (!(C instanceof LabeledClustreamKernel)) return;
        LabeledClustreamKernel labeledC = (LabeledClustreamKernel) C;
        labeledC.incrementLabelCount(inst.classValue(), 1); // update the count (+ 1)
    }

    /**
     * Finds the nearest cluster to the given point.
     * @param instance an instance point
     * @return the nearest cluster if any found, <code>null</code> otherwise
     */
    private LabeledClustreamKernel findClosestCluster(Instance instance) {
        return findClosestClusterByEuclidean(instance);
        // return findClosestClusterByInclusionProbab(instance);
    }

    /**
     * Finds the nearest cluster using Euclidean distance
     * between that instance and a cluster's center.
     * This may not work so well in case of non-convex clusters.
     * @param instance the given instance
     * @return the cluster whose center is nearest to the instance,
     * <code>null</code> otherwise
     */
    private LabeledClustreamKernel findClosestClusterByEuclidean(Instance instance) {
        LabeledClustreamKernel C = null;
        double minDistance = Double.MAX_VALUE;
        Clustering clustering = this.getClusteringResult();
        for (Cluster cluster : clustering.getClustering()) {
            if (!(cluster instanceof LabeledClustreamKernel)) continue;
            LabeledClustreamKernel labeledC = (LabeledClustreamKernel) cluster;
            double distance = ClustreamSSL.distance(labeledC.getCenter(), instance.toDoubleArray());
//            System.out.println("Distance " + distance);
            if (distance < minDistance) {
                minDistance = distance;
                C = labeledC;
            }
        }
//        System.out.println("Closest distance " + minDistance + "\n");
//        if (minDistance > 5.0) System.out.println("Quite big distance...\n");

        return C;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Clustering clustering = this.getClusteringResult();
        if (clustering == null) return new Measurement[0];
        List<Measurement> measurements = new ArrayList<>();

        // print the count of predictions
        if (this.predictionCount != null) {
            for (int i = 0; i < this.predictionCount.length; i++) {
                measurements.add(new Measurement("prediction count of class " + i, this.predictionCount[i]));
            }
        }

        // print the total count in each micro-cluster & number of times it has given prediction
        int j = 0;
        int countEffectiveCluster = 0;
        for (Cluster cluster : clustering.getClustering()) {
            if (!(cluster instanceof LabeledClustreamKernel)) return new Measurement[0];
            LabeledClustreamKernel lc = (LabeledClustreamKernel) cluster;

            // count the number of labels stored in one cluster & the prediction times of that cluster
            int total = 0;
            for (Map.Entry<Double, Integer> entry : lc.getLabelCount().entrySet()) total += entry.getValue();
            if (total > maxCount) maxCount = total;
            measurements.add(new Measurement("count cluster " + j, total));
            measurements.add(new Measurement("prediction times " + j, lc.getTimesGivingPrediction()));
            if (lc.hasGivenPrediction()) countEffectiveCluster++;

            // reset some measures
//            lc.setTimesGivingPrediction(0);
//            lc.unsetHasGivenPrediction();

            j++;
        }


        // side information
        measurements.add(new Measurement("max count", maxCount));
        measurements.add(new Measurement("useful clusters", countEffectiveCluster));
        measurements.add(new Measurement("null times", this.nullCTimes));
        measurements.add(new Measurement("not null times", this.notNullCTimes));

        Measurement[] result = new Measurement[measurements.size()];
        return measurements.toArray(result);
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
