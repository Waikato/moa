package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
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

    /** Decides whether to normalize the data or not */
    public FlagOption normalizeOption = new FlagOption("normalize", 'n', "Normalize the data");

    public FlagOption excludeLabelOption = new FlagOption("excludeLabel", 'e',
            "Excludes the label when computing the distance");

    public FlagOption useNotNormalizedOption = new FlagOption("useNorNormData", 'f',
            "Use the data to train even if it's not normalized");

    /** Does clustering and holds the micro-clusters for classification */
    private Clusterer clusterer;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    /** Normalize the data before training or not */
    private boolean doNormalization;

    /** Number of nearest clusters used to issue prediction */
    private int k;

    /** A really small value for misc need */
    private final double SMALL_VALUE = 1E-7;

    private double[] ls;
    private double[] ss;
    private double[] m2;
    private double[] s2;
    private int N;

    ////////////////////////
    // just some measures //
    ////////////////////////
    private int nullCTimes = 0;
    private int notNullCTimes = 0;
    private int[] predictionCount;
    private Map<String, Long> times = new HashMap<>();
    private long start = 0, end = 0;
    private int notNormalized = 0;
    private boolean useNotNormalized;
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
        this.clusterer.setExcludeLabel(this.excludeLabelOption.isSet());
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        this.k = kNearestClusterOption.getValue();
        this.doNormalization = normalizeOption.isSet();
        this.useNotNormalized = useNotNormalizedOption.isSet();
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
        Instance X = (doNormalization ? normalize(inst) : inst);

        start = System.nanoTime();
        // get the votes
        double[] votes = new double[0];
        if (k == 1) {
            Cluster C = this.findClosestCluster(X, false);
            if (C != null) votes = C.getLabelVotes();
        } else {
            votes = getVotesFromKClusters(this.findKNearestClusters(X, this.k));
        }
        end = System.nanoTime();
        times.put("Prediction", end - start);

        // collect some measurement
        if (this.predictionCount == null) this.predictionCount = new int[X.dataset().numClasses()];
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
    
    private void accumulateSum(Instance inst) {
        this.N++;
        double[] values = inst.toDoubleArray();
        if (ls == null) ls = new double[inst.numAttributes()];
        if (ss == null) ss = new double[inst.numAttributes()];
        if (m2 == null) m2 = inst.toDoubleArray();
        if (s2 == null) s2 = new double[inst.numAttributes()];
        for (int i = 0; i < values.length; i++) {
            ls[i] += values[i];
            ss[i] += (values[i] * values[i]);
            if (this.N == 1) {
                m2[i] = values[i];
                s2[i] = 0;
            } else {
                double tmp = m2[i];
                m2[i] += (values[i] - m2[i]) / N;
                s2[i] += (values[i] - tmp) * (values[i] - m2[i]);
            }
        }
    }

    private double[] getMean() {
        if (ls == null || N == 0) return new double[0];
        double[] mean = new double[ls.length];
        for (int i = 0; i < ls.length; i++) {
            mean[i] = ls[i] / N;
        }
        return mean;
    }

    private double[] getMean2() {
        return (this.N > 0 ? m2 : new double[0]);
    }


    private double[] getVariance() {
        if (ss == null || ls == null || N == 0) return new double[0];
        double[] var = new double[ss.length];
        for (int i = 0; i < ss.length; i++) {
            var[i] = (ss[i] - (ls[i] * ls[i]) / N) / (N == 1 ? 1 : (N - 1));
            if (var[i] < 0) var[i] = Double.MIN_VALUE; // set to a very small value
        }
        return var;
    }

    private double[] getVariance2() {
        if (this.N < 1) return new double[0];
        if (this.N == 1) return new double[s2.length];
        double[] var = new double[s2.length];
        for (int i = 0; i < s2.length; i++) {
            var[i] = s2[i] / (N - 1);
        }
        return var;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        Objects.requireNonNull(this.clusterer, "Clusterer must not be null!");

        // accumulate the mean and variance of each dimension
        // accumulateSum(inst);
        
        // quick check: if the data is normalize [0.0, 1.0]
        if (!isNormalized(inst)) {
            notNormalized++;
            if (!useNotNormalized) return;
        }

        // normalize the data
        Instance X = (doNormalization ? normalize(inst) : inst);

        start = System.nanoTime();
        if (this.usePseudoLabel) this.trainOnInstanceWithPseudoLabel(X);
        else this.trainOnInstanceNoPseudoLabel(X);
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
                Cluster C = this.findClosestCluster(instPseudoLabel, false);
                pseudoLabel = (C != null ? C.getMajorityLabel() : 0.0);
                instPseudoLabel.setWeight(SMALL_VALUE);
            } else {
                Cluster[] kC = this.findKNearestClusters(instPseudoLabel, this.k);
                pseudoLabel = Utils.maxIndex(getVotesFromKClusters(kC));
                inst.setWeight(SMALL_VALUE);
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
    private Cluster findClosestCluster(Instance instance, boolean includeClass) {
            return clusterer.getNearestCluster(instance, includeClass);
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

    private boolean isNormalized(Instance X) {
        double[] values = X.toDoubleArray();
        for (int i = 0; i < values.length; i++) {
            if (i != X.classIndex() && X.attribute(i).isNumeric() && Math.abs(values[i]) > 1) return false;
        }
        return true;
    }

    private Instance normalize(Instance X) {
        if (ls == null || ss == null) return X;
        double[] values = X.toDoubleArray();
        double[] result = new double[values.length];
        double[] mean = getMean2();
        double[] variance = getVariance2();
        for (int i = 0; i < values.length; i++) {
            if (i == X.classIndex()) continue;
            result[i] = (values[i] - mean[i]) / Math.sqrt(variance[i]);
        }
        return new DenseInstance(X.weight(), result, X.getHeader());
    }
}
