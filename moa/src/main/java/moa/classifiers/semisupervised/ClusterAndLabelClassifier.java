package moa.classifiers.semisupervised;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.clustream.Clustream;
import moa.clusterers.clustream.ClustreamKernel;
import moa.core.*;
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

    public ClassOption clustererOption = new ClassOption("clustream", 'c',
            "Used to configure clustream",
            Clustream.class, "Clustream");

    /** Lets user decide if they want to use pseudo-labels */
    public FlagOption usePseudoLabelOption = new FlagOption("pseudoLabel", 'p',
            "Using pseudo-label while training");

    public FlagOption debugModeOption = new FlagOption("debugMode", 'e',
            "Print information about the clusters on stdout");

    /** Decides the labels based on k-nearest cluster, k defaults to 1 */
    public IntOption kNearestClusterOption = new IntOption("kNearestCluster", 'k',
            "Issue predictions based on the majority vote from k-nearest cluster", 1);

    /** Number of nearest clusters used to issue prediction */
    private int k;

    private Clustream clustream;

    /** To train using pseudo-label or not */
    private boolean usePseudoLabel;

    /** Number of nearest clusters used to issue prediction */
//    private int k;

    // Statistics
    protected long instancesSeen;
    protected long instancesPseudoLabeled;
    protected long instancesCorrectPseudoLabeled;

    @Override
    public String getPurposeString() {
        return "A basic semi-supervised learner";
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.clustream = (Clustream) getPreparedClassOption(this.clustererOption);
        this.clustream.prepareForUse();
        this.usePseudoLabel = usePseudoLabelOption.isSet();
        this.k = kNearestClusterOption.getValue();
        super.prepareForUseImpl(monitor, repository);
    }

    @Override
    public void resetLearningImpl() {
        this.clustream.resetLearning();
        this.instancesSeen = 0;
        this.instancesCorrectPseudoLabeled = 0;
        this.instancesPseudoLabeled = 0;
    }


    @Override
    public void trainOnInstanceImpl(Instance instance) {
        ++this.instancesSeen;
        Objects.requireNonNull(this.clustream, "Cluster must not be null!");
        if(this.clustream.getModelContext() == null)
            this.clustream.setModelContext(this.getModelContext());
        this.clustream.trainOnInstance(instance);
    }

    @Override
    public int trainOnUnlabeledInstance(Instance instance) {
        // Creates a copy of the instance to be pseudoLabeled
        Instance unlabeledInstance = instance.copy();
        // In case the label is available for debugging purposes (i.e. checking the pseudoLabel accuracy),
        // we want to save it, but then immediately remove the label to avoid it being used
        int groundTruthClassLabel = -999;
        if(! unlabeledInstance.classIsMissing()) {
            groundTruthClassLabel = (int) unlabeledInstance.classValue();
            unlabeledInstance.setMissing(unlabeledInstance.classIndex());
        }

        int pseudoLabel = -1;
        if (this.usePseudoLabel) {
            ClustreamKernel closestCluster = getNearestClustreamKernel(this.clustream, unlabeledInstance, false);
            pseudoLabel = (closestCluster != null ? Utils.maxIndex(closestCluster.classObserver) : -1);

            unlabeledInstance.setClassValue(pseudoLabel);
            this.clustream.trainOnInstance(unlabeledInstance);

            if (pseudoLabel == groundTruthClassLabel) {
                ++this.instancesCorrectPseudoLabeled;
            }
            ++this.instancesPseudoLabeled;
        }
        else { // Update the cluster without using the pseudoLabel
            this.clustream.trainOnInstance(unlabeledInstance);
        }
        return pseudoLabel;
    }

    @Override
    public void addInitialWarmupTrainingInstances() {
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        Objects.requireNonNull(this.clustream, "Cluster must not be null!");
        // Creates a copy of the instance to be used in here (avoid changing the instance passed to this method)
        Instance unlabeledInstance = instance.copy();

        if(! unlabeledInstance.classIsMissing())
            unlabeledInstance.setMissing(unlabeledInstance.classIndex());

        Clustering clustering = clustream.getMicroClusteringResult();

        double[] votes = new double[unlabeledInstance.numClasses()];

        if(clustering != null) {
            if (k == 1) {
                ClustreamKernel closestKernel = getNearestClustreamKernel(clustream, unlabeledInstance, false);
                if (closestKernel != null)
                    votes = closestKernel.classObserver;
            }
            else {
                votes = getVotesFromKClusters(this.findKNearestClusters(unlabeledInstance, this.k));
            }
        }
        return votes;
    }

    /**
     * Gets the predictions from K nearest clusters
     * @param kClusters array of k nearest clusters
     * @return the final predictions
     */
    private double[] getVotesFromKClusters(ClustreamKernel[] kClusters) {
        DoubleVector result = new DoubleVector();

        for(ClustreamKernel microCluster : kClusters) {
            if(microCluster == null)
                continue;

            int maxIndex = Utils.maxIndex(microCluster.classObserver);
            result.setValue(maxIndex, 1.0);
        }
        if(result.numValues() > 0) {
            result.normalize();
        }
        return result.getArrayRef();
    }

    /**
     * Finds K nearest cluster from an instance
     * @param instance the instance X
     * @param k K closest clusters
     * @return set of K closest clusters
     */
    private ClustreamKernel[] findKNearestClusters(Instance instance, int k) {
        Set<ClustreamKernel> sortedClusters = new TreeSet<>(new DistanceKernelComparator(instance));
        Clustering clustering = clustream.getMicroClusteringResult();

        if (clustering == null || clustering.size() == 0)
            return new ClustreamKernel[0];

        // There should be a better way of doing this instead of creating a separate array list
        ArrayList<ClustreamKernel> clusteringArray = new ArrayList<>();
        for(int i = 0 ; i < clustering.getClustering().size() ; ++i)
            clusteringArray.add((ClustreamKernel) clustering.getClustering().get(i));

        // Sort the clusters according to their distance to instance
        sortedClusters.addAll(clusteringArray);
        ClustreamKernel[] topK = new ClustreamKernel[k];
        // Keep only the topK clusters, i.e. the closest clusters to instance
        Iterator<ClustreamKernel> it = sortedClusters.iterator();
        int i = 0;
        while (it.hasNext() && i < k)
            topK[i++] = it.next();

        //////////////////////////////////
        if(this.debugModeOption.isSet())
            debugVotingScheme(clustering, instance, topK, true);
        //////////////////////////////////

        return topK;
    }

    class DistanceKernelComparator implements Comparator<ClustreamKernel> {

        private Instance instance;

        public DistanceKernelComparator(Instance instance) {
            this.instance = instance;
        }

        @Override
        public int compare(ClustreamKernel C1, ClustreamKernel C2) {
            double distanceC1 = Clustream.distanceIgnoreNaN(C1.getCenter(), instance.toDoubleArray());
            double distanceC2 = Clustream.distanceIgnoreNaN(C2.getCenter(), instance.toDoubleArray());
            return Double.compare(distanceC1, distanceC2);
        }
    }

    private ClustreamKernel getNearestClustreamKernel(Clustream clustream, Instance instance, boolean includeClass) {
        double minDistance = Double.MAX_VALUE;
        ClustreamKernel closestCluster = null;

        List<Integer> excluded = new ArrayList<>();
        if (!includeClass)
            excluded.add(instance.classIndex());

        Clustering clustering = clustream.getMicroClusteringResult();
        AutoExpandVector<Cluster> kernels = clustering.getClustering();

        double[] arrayInstance = instance.toDoubleArray();


        for(int i = 0 ; i < kernels.size() ; ++i) {
            double[] clusterCenter = kernels.get(i).getCenter();
            double distance = Clustream.distanceIgnoreNaN(arrayInstance, clusterCenter);
            //////////////////////////////
            if(this.debugModeOption.isSet())
                debugClustreamMicroCluster((ClustreamKernel) kernels.get(i), clusterCenter, distance, true);
            //////////////////////////////
            if(distance < minDistance) {
                minDistance = distance;
                closestCluster = (ClustreamKernel) kernels.get(i);
            }
        }
        ///////////////////////////
        if(this.debugModeOption.isSet())
            debugShowInstance(instance);
        ///////////////////////////

        return closestCluster;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        // instances seen * the number of ensemble members
        return new Measurement[]{
                new Measurement("#pseudo-labeled", this.instancesPseudoLabeled),
                new Measurement("#correct pseudo-labeled", this.instancesCorrectPseudoLabeled),
                new Measurement("accuracy pseudo-labeled", this.instancesCorrectPseudoLabeled / (double) this.instancesPseudoLabeled * 100)
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// DEBUG METHODS //////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void debugShowInstance(Instance instance) {
        System.out.print("Instance: [");
        for(int i = 0 ; i < instance.numAttributes() ; ++i) {
            System.out.print(instance.value(i) + " ");
        }
        System.out.println("]");
    }

    private void debugClustreamMicroCluster(ClustreamKernel cluster, double[] clusterCenter, double distance, boolean showMicroClusterValues) {
        System.out.print("      MicroCluster: " + cluster.getId());
        if(showMicroClusterValues) {
            System.out.print(" [");
            for (int j = 0; j < clusterCenter.length; ++j) {
                System.out.print(String.format("%.4f ", clusterCenter[j]) + " ");
            }
            System.out.print("]");
        }
        System.out.print(" distance to instance: " + String.format("%.4f ",distance) + " classObserver: [ ");

        for(int g = 0 ; g < cluster.classObserver.length ; ++g) {
            System.out.print(cluster.classObserver[g] + " ");
        }
        System.out.print("] maxIndex (vote): " + Utils.maxIndex(cluster.classObserver));
        System.out.println();
    }

    private void debugVotingScheme(Clustering clustering, Instance instance, ClustreamKernel[] topK, boolean showAllClusters) {
        System.out.println("[DEBUG] Voting Scheme: ");
        AutoExpandVector<Cluster> kernels = clustering.getClustering();

        double[] arrayInstance = instance.toDoubleArray();

        System.out.println("   TopK: ");
        for(int z = 0 ; z < topK.length ; ++z) {
            double[] clusterCenter = topK[z].getCenter();
            double distance = Clustream.distanceIgnoreNaN(arrayInstance, clusterCenter);
            debugClustreamMicroCluster(topK[z], clusterCenter, distance, true);
        }

        if(showAllClusters) {
            System.out.println("   All microclusters: ");
            for (int x = 0; x < kernels.size(); ++x) {
                double[] clusterCenter = kernels.get(x).getCenter();
                double distance = Clustream.distanceIgnoreNaN(arrayInstance, clusterCenter);
                debugClustreamMicroCluster((ClustreamKernel) kernels.get(x), clusterCenter, distance, true);
            }
        }
    }
}
