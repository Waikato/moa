package moa.clusterers.semisupervised;

import com.yahoo.labs.samoa.instances.Instance;
import moa.clusterers.denstream.MicroCluster;
import moa.clusterers.denstream.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class MicroClusterSSL extends MicroCluster {

    public MicroClusterSSL(double[] center, int dimensions, long creationTimestamp, double lambda, Timestamp currentTimestamp) {
        super(center, dimensions, creationTimestamp, lambda, currentTimestamp);
        this.labelCount = new HashMap<>();
    }

    public MicroClusterSSL(Instance instance, int dimensions, long timestamp, double lambda, Timestamp currentTimestamp) {
        this(instance.toDoubleArray(), dimensions, timestamp, lambda, currentTimestamp);
        if (!instance.classIsMissing() && !instance.classIsMasked()) {
            incrementLabelCount(instance.classValue(), 1);
        }
    }

    /**
     * Inserts a point in the cluster.
     * Updates the count of the point's label in this cluster, if the point is labeled
     * @param instance the instance point
     * @param timestamp the timestamp
     */
    public void insert(Instance instance, long timestamp) {
        super.insert(instance, timestamp);
        if (!instance.classIsMasked() && !instance.classIsMissing()) {
            this.incrementLabelCount(instance.classValue(), 1);
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
    public MicroClusterSSL copy() {
        MicroClusterSSL copy = new MicroClusterSSL(this.LS.clone(), this.LS.length, this.getCreationTime(), this.lambda, this.currentTimestamp);
        copy.setWeight(this.N + 1);
        copy.N = this.N;
        copy.SS = this.SS.clone();
        copy.LS = this.LS.clone();
        copy.lastEditT = this.lastEditT;

        // get the copy of the label count
        for (Map.Entry<Double, Integer> entry : this.labelCount.entrySet()) {
            copy.labelCount.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }
}
