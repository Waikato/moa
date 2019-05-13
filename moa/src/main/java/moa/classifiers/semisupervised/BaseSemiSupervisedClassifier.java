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
import moa.clusterers.clustream.WithKmeans;
import moa.clusterers.denstream.WithDBSCAN;
import moa.clusterers.dstream.Dstream;
import moa.clusterers.semisupervised.ClustreamSSL;
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
 * is assigned a label. The micro-clusters will then be used for classification of unlabelled data.
 */
public class BaseSemiSupervisedClassifier extends AbstractClassifier
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

    /** Decides whether to normalize the data or not */
    public FlagOption normalizeOption = new FlagOption("normalize", 'n', "Normalize the data");

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    /** Normalize the data before training or not */
    private boolean doNormalization;

    /** Number of nearest clusters used to issue prediction */
    private int k;

    ////////////////////////
    // just some measures //
    ////////////////////////
    private int nullCTimes = 0;
    private int notNullCTimes = 0;
    private int[] predictionCount;
    private Map<String, Long> times = new HashMap<>();
    private long start = 0, end = 0;
    ////////////////////////
    // just some measures //
    ////////////////////////

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
        this.doNormalization = normalizeOption.isSet();
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

        // normalize the instance
        if (doNormalization) inst.normalize();

        start = System.nanoTime();
        // get the votes
        double[] votes = new double[0];
        if (k == 1) {
            Cluster C = this.findClosestCluster(inst);
            if (C != null) {
                votes = C.getLabelVotes();
                this.notNullCTimes++;
            } else {
                nullCTimes++;
            }
        } else {
            Cluster[] kC = this.findKNearestClusters(inst, this.k);
            votes = getVotesFromKClusters(kC);
        }
        end = System.nanoTime();
        times.put("Prediction", end - start);

        // collect some measurement
        if (this.predictionCount == null) this.predictionCount = new int[inst.dataset().numClasses()];
        int predictedClass = Utils.maxIndex(votes);
        predictionCount[predictedClass]++;

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

        // normalize the data
        if (doNormalization) inst.normalize();

        start = System.nanoTime();
        if (this.usePseudoLabel) this.trainOnInstanceWithPseudoLabel(inst);
        else this.trainOnInstanceNoPseudoLabel(inst);
        end = System.nanoTime();
        times.put("Training", end - start);
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
                Cluster C = this.findClosestCluster(instPseudoLabel);
                pseudoLabel = (C != null ? C.getMajorityLabel() : 0.0);
            } else {
                Cluster[] kC = this.findKNearestClusters(instPseudoLabel, this.k);
                double[] votes = getVotesFromKClusters(kC);
                pseudoLabel = Utils.maxIndex(votes);
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
        if (clustering == null || clustering.size() == 0) {
            this.nullCTimes++;
            return new Cluster[0];
        }
        this.notNullCTimes++;
        sortedClusters.addAll(clustering.getClustering());
        Cluster[] topK = new Cluster[k];
        Iterator<Cluster> it = sortedClusters.iterator();
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
        return clusterer.getNearestCluster(instance);
        //return findClosestClusterByEuclidean(instance);
        //return findClosestClusterByInclusionProbab(instance);
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
            double distance = Clusterer.distance(cluster.getCenter(), instance.toDoubleArray());
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

        // examines the measures from the clusterer
        if (clusterer instanceof WithDBSCAN) {
            measurements.add(new Measurement("#micro-cluster", clustering.size()));
            measurements.add(new Measurement("#outlier-cluster",
                    ((WithDBSCAN) clusterer).getOutlierClusteringResult().size()));
            measurements.add(new Measurement("tp", ((WithDBSCAN) clusterer).getMinimalTimeSpan()));
            measurements.add(new Measurement("#pruned pmc", ((WithDBSCAN) clusterer).prunedPMC));
            measurements.add(new Measurement("#pruned omc", ((WithDBSCAN) clusterer).prunedOMC));
            measurements.add(new Measurement("#omc to pmc", ((WithDBSCAN) clusterer).grownClusters));
        } else if (clusterer instanceof WithKmeans) {
            measurements.add(new Measurement("m_added", ((WithKmeans) clusterer).numAdded));
            measurements.add(new Measurement("m_deleted", ((WithKmeans) clusterer).numDeleted));
            measurements.add(new Measurement("m_updated", ((WithKmeans) clusterer).numUpdated));
            ((WithKmeans) clusterer).numAdded = 0;
            ((WithKmeans) clusterer).numDeleted = 0;
            ((WithKmeans) clusterer).numUpdated = 0;

            // time measurement
            for (Map.Entry<String, Long> entry : ((WithKmeans) clusterer).time_measures.entrySet()) {
                measurements.add(new Measurement(entry.getKey(), entry.getValue()));
            }
            for (Map.Entry<String, Long> entry : times.entrySet()) {
                measurements.add(new Measurement(entry.getKey(), entry.getValue()));
            }
        } else if (clusterer instanceof Dstream) {
            measurements.add(new Measurement("num_clusters", clustering.size()));
            measurements.add(new Measurement("#pruning", ((Dstream) clusterer).countPruning));
            measurements.add(new Measurement("times_pruning", ((Dstream) clusterer).timePruning));
            ((Dstream) clusterer).countPruning = 0;
            ((Dstream) clusterer).timePruning = 0;
        } else if (clusterer instanceof ClustreamSSL) {
            
        }

        // side information
        // measurements.add(new Measurement("null times", this.nullCTimes));
        // measurements.add(new Measurement("not null times", this.notNullCTimes));

        Measurement[] result = new Measurement[measurements.size()];
        return measurements.toArray(result);
    }

    private double sigmoid(double d) {
        return 1 / (1 + Math.exp(-d));
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
