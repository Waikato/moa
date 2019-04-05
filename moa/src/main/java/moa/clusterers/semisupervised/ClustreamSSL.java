package moa.clusterers.semisupervised;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.clusterers.AbstractClusterer;
import moa.core.Measurement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An extension of the Clustream algorithm. Apart from the statistics of the micro-clusters,
 * it also updates the label count in each cluster. To be used with <code>LabeledClustreamKernel</code>.
 */
public class ClustreamSSL extends AbstractClusterer {

    private static final long serialVersionUID = 1L;

    public IntOption timeWindowOption = new IntOption("horizon",
            'h', "Range of the window.", 1000);

    public IntOption maxNumKernelsOption = new IntOption(
            "maxNumKernels", 'k',
            "Maximum number of micro kernels to use.", 100);

    public IntOption kernelRadiFactorOption = new IntOption(
            "kernelRadiFactor", 't',
            "Multiplier for the kernel radius", 2);

    private int timeWindow;
    private long timestamp = -1;
    private LabeledClustreamKernel[] kernels;
    private boolean initialized;
    private List<LabeledClustreamKernel> buffer; // Buffer for initialization with kNN
    private int bufferSize;
    private double t;
    private int m;

    private LabeledClustreamKernel updatedCluster;

    @Override
    public void resetLearningImpl() {
        this.kernels = new LabeledClustreamKernel[maxNumKernelsOption.getValue()];
        this.timeWindow = timeWindowOption.getValue();
        this.initialized = false;
        this.buffer = new LinkedList<>();
        this.bufferSize = maxNumKernelsOption.getValue();
        t = kernelRadiFactorOption.getValue();
        m = maxNumKernelsOption.getValue();
    }

    @Override
    public LabeledClustreamKernel getUpdatedCluster() { return this.updatedCluster; }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        int dim = instance.numValues();
        timestamp++;

        // 0. Initialize
        if (!initialized) {
            if (buffer.size() < bufferSize) {
                buffer.add(new LabeledClustreamKernel(instance, dim, timestamp, t, m));
                return;
            }

            int k = kernels.length;
            assert (k <= bufferSize);

            LabeledClustreamKernel[] centers = new LabeledClustreamKernel[k];
            for ( int i = 0; i < k; i++ ) centers[i] = buffer.get(i); // TODO: make random!
            // kernels = centers; // TODO soft copy?

            Clustering kmeans_clustering = kMeans(k, centers, buffer);
            for ( int i = 0; i < kmeans_clustering.size(); i++) {
                // TODO if doing this, we lost the label count (only one for each label, but still...)
                kernels[i] = new LabeledClustreamKernel(
                        new DenseInstance(1.0, centers[i].getCenter()),
                        dim, timestamp, t, m);
                // kernels[i] = centers[i]; // TODO if doing this, we omit the result of k-means clustering
            }

            buffer.clear();
            initialized = true;
            return;
        }

        // 1. Determine closest kernel
        LabeledClustreamKernel closestKernel = null;
        double minDistance = Double.MAX_VALUE;
        for (LabeledClustreamKernel kernel : kernels) {
            double distance = distance(instance.toDoubleArray(), kernel.getCenter());
            if (distance < minDistance) {
                closestKernel = kernel;
                minDistance = distance;
            }
        }

        // 2. Check whether instance fits into closestKernel
        double radius = 0.0;
        if (closestKernel != null) {
            if (closestKernel.getWeight() == 1) {
                // Special case: estimate radius by determining the distance to the next closest cluster
                radius = Double.MAX_VALUE;
                double[] center = closestKernel.getCenter();
                for (LabeledClustreamKernel kernel : kernels) {
                    if (kernel == closestKernel) continue;
                    double distance = distance(kernel.getCenter(), center);
                    radius = Math.min(distance, radius);
                }
            } else {
                radius = closestKernel.getRadius();
            }
        }

        if (closestKernel != null) {
            if (minDistance < radius) {
                // Date fits, put into kernel and be happy
                closestKernel.insert(instance, timestamp);
                updatedCluster = closestKernel;
                return;
            }
        }

        // 3. Date does not fit, we need to free some space to insert a new kernel
        long threshold = timestamp - timeWindow; // Kernels before this can be forgotten

        // 3.1 Try to forget old kernels (i.e. we also forget the label count attached to that kernel)
        for (int i = 0; i < kernels.length; i++) {
            if (kernels[i].getRelevanceStamp() < threshold) {
                kernels[i] = new LabeledClustreamKernel(instance, dim, timestamp, t, m);
                return;
            }
        }

        // 3.2 Merge closest two kernels
        int closestA = 0;
        int closestB = 0;
        minDistance = Double.MAX_VALUE;
        for (int i = 0; i < kernels.length; i++) {
            double[] centerA = kernels[i].getCenter();
            for (int j = i + 1; j < kernels.length; j++) {
                double dist = distance(centerA, kernels[j].getCenter());
                if (dist < minDistance) {
                    minDistance = dist;
                    closestA = i;
                    closestB = j;
                }
            }
        }
        assert (closestA != closestB);

        kernels[closestA].add(kernels[closestB]);
        kernels[closestB] = new LabeledClustreamKernel(instance, dim, timestamp, t, m);
        this.updatedCluster = kernels[closestB];
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        return new double[0];
    }

    @Override
    public Clustering getClusteringResult() {
        return null;
    }

    @Override
    public Clustering getMicroClusteringResult() {
        if (!initialized) {
            return new Clustering(new Cluster[0]);
        }

//        LabeledClustreamKernel[] res = new LabeledClustreamKernel[kernels.length];
//        for ( int i = 0; i < res.length; i++ ) {
//            res[i] = new LabeledClustreamKernel(kernels[i], t, m);
//        }

        return new Clustering(this.kernels);
    }

    /**
     * Computes the Euclidean distance between two instance points.
     * @param pointA point A
     * @param pointB point B
     * @return the Euclidean distance between them
     */
    public static double distance(double[] pointA, double [] pointB) {
        double distance = 0.0;
        for (int i = 0; i < pointA.length; i++) {
            // sometimes, the value of the missing class is NaN & the final distance is NaN (which we don't want)
            if (!Double.isNaN(pointA[i]) && !Double.isNaN(pointB[i])) {
                double d = pointA[i] - pointB[i];
                distance += d * d;
            }
        }
        return Math.sqrt(distance);
    }

    /**
     * Runs kMeans
     * @param k number of clusters
     * @param centers the centers
     * @param data the clusters
     * @return a clustering
     */
    private static Clustering kMeans(int k, Cluster[] centers, List<? extends Cluster> data) {
        assert (centers.length == k);
        assert (k > 0);

        int dimensions = centers[0].getCenter().length;

        ArrayList<ArrayList<Cluster>> clustering = new ArrayList<>();
        for (int i = 0; i < k; i++) clustering.add(new ArrayList<>());

        int repetitions = 100;
        while (repetitions-- >= 0) {
            // Assign points to clusters
            for (Cluster point : data) {
                double minDistance = distance(point.getCenter(), centers[0].getCenter());
                int closestCluster = 0;
                for (int i = 1; i < k; i++) {
                    double distance = distance(point.getCenter(), centers[i].getCenter());
                    if ( distance < minDistance ) {
                        closestCluster = i;
                        minDistance = distance;
                    }
                }
                clustering.get( closestCluster ).add( point );
            }

            // Calculate new centers and clear clustering lists
            SphereCluster[] newCenters = new SphereCluster[centers.length];
            for (int i = 0; i < k; i++) {
                newCenters[i] = calculateCenter(clustering.get(i), dimensions);
                clustering.get(i).clear();
            }
            centers = newCenters;
        }

        return new Clustering(centers);
    }

    private static SphereCluster calculateCenter(ArrayList<Cluster> cluster, int dimensions) {
        double[] res = new double[dimensions];
        for (int i = 0; i < res.length; i++) res[i] = 0.0;
        if (cluster.size() == 0) return new SphereCluster(res, 0.0);
        for (Cluster point : cluster) {
            double [] center = point.getCenter();
            for (int i = 0; i < res.length; i++) res[i] += center[i];
        }

        // Normalize
        for (int i = 0; i < res.length; i++) res[i] /= cluster.size();

        // Calculate radius
        double radius = 0.0;
        for (Cluster point : cluster) {
            double dist = distance(res, point.getCenter());
            if (dist > radius) radius = dist;
        }
        SphereCluster sc = new SphereCluster(res, radius);
        sc.setWeight(cluster.size());
        return sc;
    }
}
