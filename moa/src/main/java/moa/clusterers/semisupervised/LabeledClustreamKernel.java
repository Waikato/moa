package moa.clusterers.semisupervised;

import com.yahoo.labs.samoa.instances.Instance;
import moa.cluster.CFCluster;
import moa.clusterers.clustream.ClustreamKernel;
import moa.core.DoubleVector;

import java.util.HashMap;
import java.util.Map;

/**
 * An extension of <code>ClustreamKernel</code>. Apart from the common statistics (linear sum, squared sum, etc.),
 * it also keeps track of the label counts of the instances classified in this cluster
 */
public class LabeledClustreamKernel extends ClustreamKernel {

    /** Keeps track of the count of the class labels */
    private Map<Double, Integer> labelCount;

    private final double EPSILON = 0.2;

    /**
     * Creates a new labeled clustream kernel. Initializes the label count of instance's label to 1
     * @param instance an instance point
     * @param dimensions instance dimension
     * @param timestamp timestamp
     * @param t
     * @param m
     */
    public LabeledClustreamKernel(Instance instance, int dimensions, long timestamp, double t, int m) {
        super(instance, dimensions, timestamp, t, m);

        // initializes the label count
        labelCount = new HashMap<>();
        try {
            if (!instance.classIsMissing()) {
                incrementLabelCount(instance.classValue(), 1);
            }
        } catch (NullPointerException e) { /* shhh say nothing... */ }

    }

    public LabeledClustreamKernel(ClustreamKernel cluster, double t, int m) {
        super(cluster, t, m);

        // copy the label count
        if (cluster instanceof LabeledClustreamKernel) {
            this.labelCount = ((LabeledClustreamKernel) cluster).labelCount; // or a copy?
        }
    }

    @Override
    public void insert(Instance instance, long timestamp) {
        super.insert(instance, timestamp);

        // update the label count
        if (!instance.classIsMissing()) incrementLabelCount(instance.classValue(), 1);
    }

    @Override
    public void add(CFCluster other) {
        super.add(other);

        // accumulate the label count
        if (other instanceof LabeledClustreamKernel) {
            LabeledClustreamKernel lc = (LabeledClustreamKernel) other;
            for (Map.Entry<Double, Integer> entry : lc.labelCount.entrySet()) {
                this.incrementLabelCount(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Increments the count of a given label in this cluster
     * @param label the class label
     * @param amount the amount to increment
     */
    public void incrementLabelCount(Double label, int amount) {
        if (labelCount.containsKey(label)) {
            labelCount.put(label, labelCount.get(label) + amount);
        } else {
            labelCount.put(label, amount);
        }
    }

    /**
     * Decrements the count of a given label in this cluster
     * @param label the class label
     * @param amount the amount to increment
     */
    public void decrementLabelCount(Double label, int amount) {
        if (labelCount.containsKey(label)) {
            labelCount.put(label, labelCount.get(label) - amount);
        } else {
            labelCount.put(label, amount);
        }
    }

    @Override
    public double getInclusionProbability(Instance instance) {
        double distance = ClustreamSSL.distance(instance.toDoubleArray(), this.getCenter());
        // problem: radius of a CFCluster is 0.0 (not explicitly declared)
        // return (distance < this.radiusFactor ? 1.0 : 0.0);
        return (distance < EPSILON ? 1.0 : 0.0);
    }

    public double[] getLabelVotes() {
        // TODO properly normalize the probability
        DoubleVector votes = new DoubleVector();
        for (Map.Entry<Double, Integer> entry : this.labelCount.entrySet()) {
            votes.addToValue(entry.getKey().intValue(), entry.getValue());
        }
        votes.normalize();
        return votes.getArrayRef();
    }

    public Map<Double, Integer> getLabelCount() { return this.labelCount; }
}