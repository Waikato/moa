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
import moa.clusterers.semisupervised.ClustreamSSL;
import moa.clusterers.semisupervised.LabeledClustreamKernel;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A simple semi-supervised classifier that serves as a baseline.
 * The idea is to group the incoming data into micro-clusters, each of which
 * is assigned a label. The micro-clusters will then be used for classification of unlabelled data.
 */
public class BaseSemiSupervisedClassifier extends AbstractClassifier
        implements SemiSupervisedLearner {

    private static final long serialVersionUID = 1L;

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

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

    private boolean usePseudoLabel;

    private int k;

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
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        this.k = kNearestClusterOption.getValue();
        super.prepareForUseImpl(monitor, repository);
    }

    private Clustering getClusteringResult() {
//        return (this.clusterer.getClusteringResult() != null ?
//                this.clusterer.getClusteringResult() : this.clusterer.getMicroClusteringResult());

        // get the online clustering result?
        if (clusterer.implementsMicroClusterer()) return clusterer.getMicroClusteringResult();
        return clusterer.getClusteringResult();
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");

        // TODO get votes from k nearest cluster
        Cluster C = this.findClosestCluster(inst);
        if (C == null) {
            nullCTimes++;
            return new double[0];
        }

        // LabeledClustreamKernel[] kC = this.findKNearestClusters(inst, this.k);

        double[] votes = C.getLabelVotes();
        if (this.predictionCount == null) this.predictionCount = new int[inst.dataset().numClasses()];
        int predictedClass = Utils.maxIndex(votes);
        predictionCount[predictedClass]++;
        //C.incrementTimesGivingPrediction(1);
        //C.setHasGivenPrediction();
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
        if (this.usePseudoLabel) this.trainOnInstanceWithPseudoLabel(inst);
        else this.trainOnInstanceNoPseudoLabel(inst);
    }

    private void trainOnInstanceNoPseudoLabel(Instance inst) {
        // train stuffs
        this.clusterer.trainOnInstance(inst);

        // If X has no label, do nothing
        // if (inst.classIsMissing()) return;

        // Else, update the cluster's label
        // Cluster C = this.clusterer.getUpdatedCluster();
        // if (C == null) return;
        // if (!(C instanceof LabeledClustreamKernel)) return;
        // LabeledClustreamKernel labeledC = (LabeledClustreamKernel) C;
        // C.incrementLabelCount(inst.classValue(), 1); // update the count (+ 1)
    }

    private void trainOnInstanceWithPseudoLabel(Instance inst) {
        // if the class is masked (simulated as missing) or is missing (for real) --> pseudo-label
        if (inst.classIsMasked() || inst.classIsMissing()) {
            Instance instPseudoLabel = inst.copy();
            Cluster C = this.findClosestCluster(instPseudoLabel);
            double pseudoLabel = C != null ? C.getMajorityLabel() : 0.0;
            instPseudoLabel.setClassValue(pseudoLabel);
            this.clusterer.trainOnInstance(instPseudoLabel);
        } else {
            this.clusterer.trainOnInstance(inst); // else, just train it normally
        }

        // If X has no label, do nothing (we don't use the pseudo-label to update the count)
        // if (inst.classIsMissing()) return;

        // Else, update the cluster's label
        // Cluster C = this.clusterer.getUpdatedCluster();
        // if (C == null) return;
        // if (!(C instanceof CFCluster)) return;
        // CFCluster labeledC = (LabeledClustreamKernel) C;
        // C.incrementLabelCount(inst.classValue(), 1); // update the count (+ 1)
    }

    class DistanceKernelComparator implements Comparator<LabeledClustreamKernel> {

        private Instance instance;

        public DistanceKernelComparator(Instance instance) {
            this.instance = instance;
        }

        @Override
        public int compare(LabeledClustreamKernel C1, LabeledClustreamKernel C2) {
            double distanceC1 = ClustreamSSL.distance(C1.getCenter(), instance.toDoubleArray());
            double distanceC2 = ClustreamSSL.distance(C2.getCenter(), instance.toDoubleArray());
            return Double.compare(distanceC1, distanceC2);
        }
    }

    private LabeledClustreamKernel[] findKNearestClusters(Instance instance, int k) {
        Set<LabeledClustreamKernel> sortedClusters = new TreeSet<>(new DistanceKernelComparator(instance));
        Clustering clustering = this.getClusteringResult();
        if (clustering == null) return new LabeledClustreamKernel[0];
        for (Cluster cluster : clustering.getClustering()) {
            if (!(cluster instanceof LabeledClustreamKernel)) continue;
            LabeledClustreamKernel lCluster = (LabeledClustreamKernel) cluster;
            sortedClusters.add(lCluster);
        }
        LabeledClustreamKernel[] topK = new LabeledClustreamKernel[k];
        Iterator<LabeledClustreamKernel> it = sortedClusters.iterator();
        int i = 0;
        while (it.hasNext() && i < k) {
            topK[i++] = it.next();
        }
        return topK;
    }

    /**
     * Finds the nearest cluster to the given point.
     * @param instance an instance point
     * @return the nearest cluster if any found, <code>null</code> otherwise
     */
    private Cluster findClosestCluster(Instance instance) {
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
    private Cluster findClosestClusterByEuclidean(Instance instance) {
        Cluster C = null;
        double minDistance = Double.MAX_VALUE;
        Clustering clustering = this.getClusteringResult();
        for (Cluster cluster : clustering.getClustering()) {
            // if (!(cluster instanceof LabeledClustreamKernel)) continue;
            // CFCluster labeledC = (LabeledClustreamKernel) cluster;
            double distance = ClustreamSSL.distance(cluster.getCenter(), instance.toDoubleArray());
            if (distance < minDistance) {
                minDistance = distance;
                C = cluster;
            }
        }

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

        System.out.println("\n#clusters: " + clustering.getClustering().size());
        for (Cluster cluster : clustering.getClustering()) {
            //if (cluster.getLabelCount().size() > 1) {
                System.out.println("\t#entries: " + cluster.getLabelCount().size() + "-----------------");
                cluster.getLabelCount().entrySet().forEach(e -> {
                    System.out.println("\t\t" + e.getKey() + " : " + e.getValue());
                });
            //}
        }
        // print the total count in each micro-cluster & number of times it has given prediction
//        int j = 0;
//        int countEffectiveCluster = 0;


//        for (Cluster cluster : clustering.getClustering()) {
//            // if (!(cluster instanceof LabeledClustreamKernel)) return new Measurement[0];
//            // LabeledClustreamKernel lc = (LabeledClustreamKernel) cluster;
//
//            // count the number of labels stored in one cluster & the prediction times of that cluster
//            int total = 0;
//            for (Map.Entry<Double, Integer> entry : cluster.getLabelCount().entrySet()) total += entry.getValue();
//            if (total > maxCount) maxCount = total;
//            measurements.add(new Measurement("count cluster " + j, total));
//            // measurements.add(new Measurement("prediction times " + j, cluster.getTimesGivingPrediction()));
//            // if (cluster.hasGivenPrediction()) countEffectiveCluster++;
//
//            // reset some measures
////            lc.setTimesGivingPrediction(0);
////            lc.unsetHasGivenPrediction();
//
//            j++;
//        }


        // side information
        measurements.add(new Measurement("max count", maxCount));
        // measurements.add(new Measurement("useful clusters", countEffectiveCluster));
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
