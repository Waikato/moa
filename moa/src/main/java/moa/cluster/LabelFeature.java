package moa.cluster;

import moa.core.DoubleVector;

import java.io.Serializable;

/**
 * Label feature: keeps the weight of each label stored in a cluster.
 * The weight can be simply viewed as the count of a label in a cluster,
 * which is decayed over time by a factor from 0.0 to 1.0
 */
public class LabelFeature implements Serializable {

    /** The vector keeping the weight of each label */
    private DoubleVector counts;

    /** The last time this label feature has been edited */
    private long lastEdit;

    /** The decaying factor */
    private double lambda;

    /**
     * Creates a new label feature
     * @param lambda the decaying factor
     */
    public LabelFeature(double lambda) {
        this.counts = new DoubleVector();
        this.lambda = lambda;
        this.lastEdit = 0;
    }

    /**
     * Creates a new label feature
     * @param timestamp the current timestamp at which this label feature is created
     * @param lambda the decaying factor
     */
    public LabelFeature(long timestamp, double lambda) {
        this(lambda);
        this.lastEdit = timestamp;
    }

    /**
     * Creates a new label feature
     * @param label the label
     * @param timestamp the current timestamp at which this label feature is created
     * @param lambda the decaying factor
     */
    public LabelFeature(double label, long timestamp, double lambda) {
        this(timestamp, lambda);
        this.counts.setValue((int) label, 1);
    }

    /**
     * Creates a new label feature (a copy)
     * @param labelCount the count vector to be copied
     * @param timestamp the current timestamp at which this label is created
     * @param lambda the decaying factor
     */
    public LabelFeature(DoubleVector labelCount, long timestamp, double lambda) {
        this(timestamp, lambda);
        this.counts = (DoubleVector) labelCount.copy();
    }

    /**
     * Increments the weight of a label.
     * First, the weights of all labels are decayed,
     * then we add the given amount to the targeted label
     * @param label the target label
     * @param value the amount of update
     * @param timestamp the timestamp at which this label feature is updated
     */
    public void increment(double label, double value, long timestamp) {
        //update(timestamp);
        double oldValue = this.counts.getValue((int) label);
        counts.setValue((int) label, oldValue + value);
        lastEdit = timestamp;
    }

    /**
     * Updates the weights of all labels (decaying them over time).
     * The decay is computed as follows: <code>w_new = w_old * (2 ^ (-lambda * (timestamp - lastEdit)))</code>
     * @param timestamp the timestamp at which this label feature is updated
     */
    public void update(long timestamp) {
        double[] values = counts.getArrayRef();
        // for each count, update: w = w * 2 ^ (-lambda * (T - lastEdit))
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] * Math.pow(2, -lambda * (timestamp - lastEdit));
        }
    }

    /**
     * Makes a copy of this label feature
     * @return the copy of this label feature
     */
    public LabelFeature copy() {
        return new LabelFeature(this.counts, this.lastEdit, this.lambda);
    }

    /**
     * Gets normalized weights of all the labels
     * @return the array of normalized weights
     */
    public double[] getVotes() {
        DoubleVector copy = (DoubleVector) counts.copy();
        copy.normalize();
        return copy.getArrayRef();
    }

    /**
     * Gets the label with the highest weight.
     * @return the label with the highest weight
     */
    public double getMajorityLabel() {
        return counts.maxIndex();
    }

    /**
     * Accumulates the weights from another label feature.
     * @param lf another label feature
     */
    public void accumulate(LabelFeature lf) {
        // Cannot decay the weight without the timestamp
        double[] _counts = this.counts.getArrayRef();
        for (int i = 0; i < _counts.length; i++) { _counts[i] += lf.counts.getValue(i); }
    }

    /**
     * Accumulates the weights from another label feature,
     * taking into account the timestamp of the update
     * @param lf another label feature
     * @param timestamp the timestamp at which the update is invoked
     */
    public void accumulate(LabelFeature lf, long timestamp) {
        //update(timestamp);
        this.accumulate(lf);
    }

    /**
     * Sets the decay factor of this label feature
     * @param lambda the decay factor
     */
    public void setDecayFactor(double lambda) { this.lambda = lambda; }

    /**
     * Gets the decay factor of this label feature
     * @return the decay factor
     */
    public double getDecayFactor() { return this.lambda; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double v : counts.getArrayRef()) {
            sb.append(v);
            sb.append(" ");
        }
        sb.append("\n");
        return sb.toString();
    }
}
