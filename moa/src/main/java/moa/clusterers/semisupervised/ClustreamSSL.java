package moa.clusterers.semisupervised;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.semisupervised.attributeSimilarity.*;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.clusterers.clustream.ClustreamKernel;
import moa.core.Measurement;
import moa.core.Utils;

import java.util.*;

/**
 * A modified version of CluStream to work with semi-supervised learning
 */
public class ClustreamSSL extends AbstractClusterer {

    private static final long serialVersionUID = 1L;

    public IntOption timeWindowOption = new IntOption("horizon",
            'h', "Rang of the window.", 1000);

    public IntOption maxNumKernelsOption = new IntOption(
            "maxNumKernels", 'm',
            "Maximum number of micro kernels to use.", 100);

    public IntOption kernelRadiFactorOption = new IntOption(
            "kernelRadiFactor", 't',
            "Multiplier for the kernel radius", 2);

    public IntOption kOption = new IntOption(
            "k", 'k',
            "k of macro k-means (number of clusters)", 5);

    public FloatOption decayFactorOption = new FloatOption("labelDecay", 'l',
            "Controls the decaying of old labels", 0.0, 0.0, 1.0);

    public MultiChoiceOption attributeSimilarityOption = new MultiChoiceOption("attributeSimilarity", 'a',
            "Chooses the method to compute similarity-based distance between categorical attributes",
            new String[] {
                    "Nothing",
                    "Euclidean",
                    "OF",
                    "Lin",
                    "Goodall3",
                    "IOF"
            },
            new String[] {
                    "Ignore categorical attribute",
                    "Euclidean Distance",
                    "Occurrence Frequency",
                    "Lin",
                    "Goodall3",
                    "Inverse Occurrence Frequency"
            }, 0);

    private int timeWindow;
    private long timestamp = -1;
    private ClustreamKernel[] kernels;
    private boolean initialized;
    private List<ClustreamKernel> buffer; // Buffer for initialization with kNN
    private int bufferSize;
    private double t;
    private int m;
    private double lambda;


    public ClustreamSSL() { }

    @Override
    public void resetLearningImpl() {
        this.kernels = new ClustreamKernel[maxNumKernelsOption.getValue()];
        this.timeWindow = timeWindowOption.getValue();
        this.initialized = false;
        this.buffer = new LinkedList<>();
        this.bufferSize = maxNumKernelsOption.getValue();
        this.lambda = decayFactorOption.getValue();
        t = kernelRadiFactorOption.getValue();
        m = maxNumKernelsOption.getValue();
        attributeObserver = chooseSimilarityComputation(attributeSimilarityOption.getChosenIndex());
    }

    /**
     * Chooses the calculator of similarity of categorical attributes
     * @param method index of method
     * @return the calculator
     */
    private AttributeSimilarityCalculator chooseSimilarityComputation(int method) {
        switch(method) {
            case 0:
                return new IgnoreSimilarityCalculator();
            case 1:
                return new EuclideanDistanceSimilarityCalculator();
            case 2:
                return  new OccurrenceFrequencySimilarityCalculator();
            case 3:
                return new LinSimilarityCalculator();
            case 4:
                return new GoodAll3SimilarityCalculator();
            case 5:
                return new InverseOccurrenceFrequencySimilarityCalculator();
        }
        return new IgnoreSimilarityCalculator();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        int dim = instance.numValues();
        timestamp++;

        // find the attributes that must be excluded from the distance computation
        List<Integer> excludes = getExcludedAttributes(instance);

        // 0. Initialize
        if (!initialized) {
            if (buffer.size() < bufferSize) {
                buffer.add(new ClustreamKernel(instance, dim, timestamp, t, m));
                return;
            } else {
                for (int i = 0; i < buffer.size(); i++) {
                    // do this to keep the instance header & masked values
                    Instance x = instance.copy();
                    double[] data = buffer.get(i).getCenter();
                    for (int j = 0; j < data.length; j++) x.setValue(j, data[j]);
                    kernels[i] = new ClustreamKernel(x, dim, timestamp, t, m);
                    kernels[i].setDecayFactor(lambda);
                }
                buffer.clear();
                initialized = true;
                return;
            }
        }

        // 1. Determine closest kernel using Euclidean distance
        ClustreamKernel closestKernel = null;
        double minDistance = Double.MAX_VALUE;
        for (ClustreamKernel kernel : kernels) {
            double distance = Clusterer.distance(instance, kernel.getCenterPoint(this.header),
                    excludes, attributeObserver);
            if (distance < minDistance) {
                closestKernel = kernel;
                minDistance = distance;
            }
        }

        // 2. Check whether instance fits into closestKernel
        double radius = 0.0;
        if (closestKernel != null) {
            if (closestKernel.getN() == 1) {
                // Special case: estimate radius by determining the distance to the next closest cluster
                radius = Double.MAX_VALUE;
                for (ClustreamKernel kernel : kernels) {
                    if (kernel == closestKernel) continue;
                    double distance = Clusterer.distance(
                            kernel.getCenterPoint(this.header), closestKernel.getCenterPoint(this.header),
                            excludes, attributeObserver);
                    radius = Math.min(distance, radius);
                }
            } else { radius = closestKernel.getRadius(); }

            if (minDistance < radius) {
                // Date fits, put into kernel and be happy
                closestKernel.insert(instance, timestamp);
                updatedCluster = closestKernel;
                return;
            }
        }

        // 3. Date does not fit, we need to free some space to insert a new kernel
        long threshold = timestamp - timeWindow; // Kernels before this can be forgotten

        // 3.1 Try to forget old kernels
        for ( int i = 0; i < kernels.length; i++ ) {
            if ( kernels[i].getRelevanceStamp() < threshold ) {
                kernels[i] = new ClustreamKernel( instance, dim, timestamp, t, m );
                updatedCluster = closestKernel;
                kernels[i].setDecayFactor(lambda);
                return;
            }
        }

        // 3.2 Merge closest two kernels
        int closestA = 0;
        int closestB = 0;
        minDistance = Double.MAX_VALUE;
        for ( int i = 0; i < kernels.length; i++ ) {
            for ( int j = i + 1; j < kernels.length; j++ ) {
                double dist = Clusterer.distance(
                        kernels[i].getCenterPoint(this.header), kernels[j].getCenterPoint(this.header),
                        excludes, attributeObserver);
                if ( dist < minDistance ) {
                    minDistance = dist;
                    closestA = i;
                    closestB = j;
                }
            }
        }
        assert (closestA != closestB);


        kernels[closestA].add(kernels[closestB], timestamp);
        kernels[closestB] = new ClustreamKernel(instance, dim, timestamp, t, m);
        updatedCluster = kernels[closestB];
        kernels[closestB].setDecayFactor(lambda);
    }

    @Override
    public Clustering getMicroClusteringResult() {
        if (!initialized) return new Clustering(new Cluster[0]);

        // weight each cluster based on the lambda factor
        ClustreamKernel[] result = new ClustreamKernel[kernels.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new ClustreamKernel(kernels[i], t, m);
            result[i].setDecayFactor(lambda);
            result[i].setWeight(kernels[i].getWeight());
        }

        return new Clustering(result);
    }

    @Override
    public Clustering getClusteringResult() {
        if (!initialized) {
            return new Clustering(new Cluster[0]);
        }
        return kMeans_rand(kOption.getValue(), getMicroClusteringResult());
    }

    public Clustering getClusteringResult(Clustering gtClustering) {
        return kMeans_gta(kOption.getValue(), getMicroClusteringResult(), gtClustering);
    }

    public String getName() {
        return "CluStreamWithKMeans " + timeWindow;
    }

    /**
     * k-means of (micro)clusters, with ground-truth-aided initialization.
     * (to produce best results)
     *
     * @param k k centroids for k-means
     * @param clustering the clustering result
     * @return (macro)clustering - CFClusters
     */
    public Clustering kMeans_gta(int k, Clustering clustering, Clustering gtClustering) {

        ArrayList<CFCluster> microclusters = new ArrayList<CFCluster>();
        for (int i = 0; i < clustering.size(); i++) {
            if (clustering.get(i) instanceof CFCluster) {
                microclusters.add((CFCluster)clustering.get(i));
            } else {
                System.out.println("Unsupported Cluster Type:" + clustering.get(i).getClass() + ". Cluster needs to extend moa.cluster.CFCluster");
            }
        }

        int n = microclusters.size();
        assert (k <= n);

        /* k-means */
        Random random = new Random(0);
        Cluster[] centers = new Cluster[k];
        int K = gtClustering.size();

        for (int i = 0; i < k; i++) {
            if (i < K) {	// GT-aided
                centers[i] = new SphereCluster(gtClustering.get(i).getCenter(), 0);
            } else {		// Randomized
                int rid = random.nextInt(n);
                centers[i] = new SphereCluster(microclusters.get(rid).getCenter(), 0);
            }
        }

        return cleanUpKMeans(kMeans(k, centers, microclusters), microclusters);
    }

    /**
     * k-means of (micro)clusters, with randomized initialization.
     *
     * @param k k centroids
     * @param clustering clustering result
     * @return (macro)clustering - CFClusters
     */
    public Clustering kMeans_rand(int k, Clustering clustering) {

        ArrayList<CFCluster> microclusters = new ArrayList<CFCluster>();
        for (int i = 0; i < clustering.size(); i++) {
            if (clustering.get(i) instanceof CFCluster) {
                microclusters.add((CFCluster)clustering.get(i));
            } else {
                System.out.println("Unsupported Cluster Type:" + clustering.get(i).getClass()
                        + ". Cluster needs to extend moa.cluster.CFCluster");
            }
        }

        int n = microclusters.size();
        assert (k <= n);

        /* k-means */
        Random random = new Random(0);
        Cluster[] centers = new Cluster[k];

        for (int i = 0; i < k; i++) {
            int rid = random.nextInt(n);
            centers[i] = new SphereCluster(microclusters.get(rid).getCenter(), 0);
        }

        return cleanUpKMeans(kMeans(k, centers, microclusters), microclusters);
    }

    /**
     * (The Actual Algorithm) k-means of (micro)clusters, with specified initialization points.
     *
     * @param k
     * @param centers - initial centers
     * @param data
     * @return (macro)clustering - SphereClusters
     */
    protected Clustering kMeans(int k, Cluster[] centers, List<? extends Cluster> data) {
        assert (centers.length == k);
        assert (k > 0);

        int dimensions = centers[0].getCenter().length;

        ArrayList<ArrayList<Cluster>> clustering = new ArrayList<ArrayList<Cluster>>();
        for (int i = 0; i < k; i++) {
            clustering.add(new ArrayList<Cluster>());
        }

        while (true) {
            // Assign points to clusters
            for (Cluster point : data) {
                //double minDistance = Clusterer.distance(point.getCenter(), centers[0].getCenter(), null);
                double minDistance = Clusterer.distance(
                        point.getCenterPoint(header), centers[0].getCenterPoint(header),
                        null, attributeObserver
                );
                int closestCluster = 0;
                for (int i = 1; i < k; i++) {
                    //double distance = Clusterer.distance(point.getCenter(), centers[i].getCenter(), null);
                    double distance = Clusterer.distance(
                            point.getCenterPoint(header), centers[i].getCenterPoint(header),
                            null, attributeObserver
                    );
                    if (distance < minDistance) {
                        closestCluster = i;
                        minDistance = distance;
                    }
                }

                clustering.get(closestCluster).add(point);
            }

            // Calculate new centers and clear clustering lists
            SphereCluster[] newCenters = new SphereCluster[centers.length];
            for (int i = 0; i < k; i++) {
                newCenters[i] = calculateCenter(clustering.get(i), dimensions);
                clustering.get(i).clear();
            }

            // Convergence check
            boolean converged = true;
            for (int i = 0; i < k; i++) {
                if (!Arrays.equals(centers[i].getCenter(), newCenters[i].getCenter())) {
                    converged = false;
                    break;
                }
            }

            if (converged) {
                break;
            } else {
                centers = newCenters;
            }
        }

        return new Clustering(centers);
    }

    /**
     * Rearrange the k-means result into a set of CFClusters, cleaning up the redundancies.
     *
     * @param kMeansResult
     * @param microclusters
     * @return
     */
    protected Clustering cleanUpKMeans(Clustering kMeansResult, ArrayList<CFCluster> microclusters) {
        /* Convert k-means result to CFClusters */
        int k = kMeansResult.size();
        CFCluster[] converted = new CFCluster[k];

        for (CFCluster mc : microclusters) {
            // Find closest kMeans cluster
            double minDistance = Double.MAX_VALUE;
            int closestCluster = 0;
            for (int i = 0; i < k; i++) {
                //double distance = Clusterer.distance(kMeansResult.get(i).getCenter(), mc.getCenter(), null);
                double distance = Clusterer.distance(
                        kMeansResult.get(i).getCenterPoint(header), mc.getCenterPoint(header),
                        null, attributeObserver
                );
                if (distance < minDistance) {
                    closestCluster = i;
                    minDistance = distance;
                }
            }

            // Add to cluster
            if ( converted[closestCluster] == null ) {
                converted[closestCluster] = (CFCluster)mc.copy();
            } else {
                converted[closestCluster].add(mc);
            }
        }

        // Clean up
        int count = 0;
        for (int i = 0; i < converted.length; i++) {
            if (converted[i] != null)
                count++;
        }

        CFCluster[] cleaned = new CFCluster[count];
        count = 0;
        for (int i = 0; i < converted.length; i++) {
            if (converted[i] != null)
                cleaned[count++] = converted[i];
        }

        return new Clustering(cleaned);
    }



    /**
     * k-means helper: Calculate a wrapping cluster of assigned points[microclusters].
     *
     * @param assigned
     * @param dimensions
     * @return SphereCluster (with center and radius)
     */
    private SphereCluster calculateCenter(ArrayList<Cluster> assigned, int dimensions) {
        double[] result = new double[dimensions];
        for (int i = 0; i < result.length; i++) {
            result[i] = 0.0;
        }

        if (assigned.size() == 0) {
            return new SphereCluster(result, 0.0);
        }

        for (Cluster point : assigned) {
            double[] center = point.getCenter();
            for (int i = 0; i < result.length; i++) {
                result[i] += center[i];
            }
        }

        // Normalize
        for (int i = 0; i < result.length; i++) {
            result[i] /= assigned.size();
        }

        // Calculate radius: biggest wrapping distance from center
        double radius = 0.0;
        Instance resultPoint = new DenseInstance(1.0, result, header);
        for (Cluster point : assigned) {
            //double dist = Clusterer.distance(result, point.getCenter(), null);
            double dist = Clusterer.distance(
                    resultPoint, point.getCenterPoint(header), null, attributeObserver
            );
            if (dist > radius) {
                radius = dist;
            }
        }
        SphereCluster sc = new SphereCluster(result, radius);
        sc.setWeight(assigned.size());
        return sc;
    }


    /** Miscellaneous **/

    @Override
    public boolean implementsMicroClusterer() {
        return true;
    }

    public boolean isRandomizable() {
        return false;
    }

    public double[] getVotesForInstance(Instance inst) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Cluster getNearestCluster(Instance X, boolean includeClass) {
        // use Euclidean distance for now
        double minDist = Double.MAX_VALUE;
        double distance;
        Cluster result = null;

        // exclude the class when finding the nearest cluster or not
        List<Integer> excluded = new ArrayList<>();
        if (!includeClass) excluded.add(X.classIndex());

        for (ClustreamKernel kernel : kernels) {
            if (kernel == null) continue;
            //distance = Clusterer.distance(kernel.getCenter(), X.toDoubleArray(), excluded);
            distance = Clusterer.distance(kernel.getCenterPoint(header), X, excluded, attributeObserver);
            if (distance < minDist) {
                minDist = distance;
                result = kernel;
            }
        }
        return result;
    }

    @Override
    public double getConfidenceLevel(Instance X, Cluster C) {
        if (!(C instanceof ClustreamKernel)) return 0; // force the type to be ClustreamKernel

        // confidence level = sigmoid(R - D)
        List<Integer> excludes = getExcludedAttributes(X);
        //double distance = Clusterer.distance(X.toDoubleArray(), C.getCenter(), excludes);
        double distance = Clusterer.distance(X, C.getCenterPoint(header), excludes, attributeObserver);
        double radius = 0.0;
        ClustreamKernel mc = (ClustreamKernel) C;
        if (mc.getN() == 1) {
            double[] center = C.getCenter();
            radius = Double.MAX_VALUE;
            // find the nearest cluster from C's center
            for (ClustreamKernel kernel : this.kernels) {
                if (kernel == C) continue;
                double d = Clusterer.distance(kernel.getCenter(), center, excludes);
                if (d < radius) {
                    radius = d;
                }
            }
        } else {
            radius = ((ClustreamKernel) C).getRadius();
        }
        //double confidence = 1 / (1 + distance / radius); // spread out more evenly but it shouldn't reach 1.0
        return Utils.sigmoid(radius - distance);
    }
}
