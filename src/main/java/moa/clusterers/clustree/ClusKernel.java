package moa.clusterers.clustree;

import moa.clusterers.clustree.util.*;
import java.util.Arrays;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import weka.core.Instance;

/**
 * Representation of an Entry in the tree
 * @author Fernando Sanchez Villaamil
 */
public class ClusKernel extends CFCluster{
    /**
     * Numeric epsilon.
     */
    public static final double EPSILON = 0.00000001;
    public static final double MIN_VARIANCE = 1e-50; // 1e-100; // 0.0000001;

    /**
     * Counting of the number of N weighted by how much time passes between
     * updates. If this weighted N is under a threshhold, we may consider
     * the cluster irrelevant and we can delete it.
     */
    private double weightedN;

    /**
     * A constructor that makes a Kernel which just represents the given point.
     * @param point The point to be converted into a corresponding Kernel.
     * @param numberClasses The number of classes possible for points in this
     * experiment(<code>Tree</code>).
     */
    public ClusKernel(double[] point, int dim) {
        super(point, dim);
        this.weightedN = 1;
    }

    /**
     * Constructor of the Cluster.
     * @param numberDimensions Dimensionality of the points added that can be
     * added to this cluster
     * @param numberClasses The number of classes possible for points in this
     * experiment(<code>Tree</code>).
     */
    protected ClusKernel(int numberDimensions) {
        super(numberDimensions);
        this.weightedN = 0;
    }

    /**
     * Instantiates a copy of the given cluster.
     * @param other The <code>Cluster</code> of which we make a copy.
     */
    protected ClusKernel(ClusKernel other) {
        super(other);
        this.weightedN = other.getWeightedN();
    }

    /**
     * Adds the given cluster to this cluster, without making this cluster
     * older.
     * @param other
     */
    public void add(ClusKernel other) {
        super.add(other);
        this.weightedN += other.weightedN;
    }

    /**
     * Make this cluster older bei weighting it and add to this cluster the
     * given cluster. If we want to add somethin to the cluster, but don't
     * want to weight it, we should use the function <code>add(Cluster)</code>.
     * @param other The other cluster to be added to this one.
     * @param timeDifference The time elapsed between the last update of the
     * <code>Entry</code> to which this cluster belongs and the update that
     * caused the call to this function.
     * @param negLambda A parameter needed to weight the cluster.
     * @see #add(tree.Kernel)
     */
    protected void aggregate(ClusKernel other, long timeDifference, double negLambda) {
        makeOlder(timeDifference, negLambda);
        add(other);
    }

    /**
     * Make this cluster older. This means multiplying weighted N, LS and SS
     * with a weight factor given by the time difference and the parameter
     * negLambda.
     * @param timeDifference The time elapsed between this current update and
     * the last one.
     * @param negLambda
     */
    protected void makeOlder(long timeDifference, double negLambda) {
        if (timeDifference == 0) {
            return;
        }

        double weightFactor = AuxiliaryFunctions.weight(negLambda, timeDifference);
        this.weightedN *= weightFactor;
        for (int i = 0; i < LS.length; i++) {
            LS[i] *= weightFactor;
            SS[i] *= weightFactor;
        }
    }

    /**
     * Calculate the distance to this other cluster. The other cluster is
     * normaly just a single data point(i.e. N = 1).
     * @param other The other cluster to which the distance is calculated.
     * @return The distance between this cluster and the other.
     */
    protected double calcDistance(ClusKernel other) {
        // TODO: (Fernando, Felix) Adapt the distance function to the new algorithmn.

        double N1 = this.getWeightedN();
        double N2 = other.getWeightedN();

        double[] thisLS = this.LS;
        double[] otherLS = other.LS;

        double res = 0.0;
        for (int i = 0; i < thisLS.length; i++) {
            double substracted = (thisLS[i] / N1) - (otherLS[i] / N2);
            res += substracted * substracted;
        }

        return res;
    }

    /**
     * Returns the weighted number of points in the cluster.
     * @return The weighted number of points in the cluster.
     */
    protected double getWeightedN() {
        return weightedN;
    }

    /**
     * Check if this cluster is empty or not.
     * @return <code>true</code> if the cluster has no data points,
     * <code>false</code> otherwise.
     */
    protected boolean isEmpty() {
        return this.N == 0;
    }

    /**
     * Remove all points from this cluster.
     */
    protected void clear() {
        this.N = 0;
        this.weightedN = 0.0;
        Arrays.fill(this.LS, 0.0);
        Arrays.fill(this.SS, 0.0);
    }

    /**
     * Overwrites the LS, SS and weightedN in this cluster to the values of the 
     * given cluster but adds N and classCount of the given cluster to this one.
     * This function is useful when the weight of an entry becomes to small, and
     * we want to forget the information of the old points.
     * @param other The cluster that should overwrite the information.
     */
    protected void overwriteOldCluster(ClusKernel other) {
        this.N = other.N;
        this.weightedN = other.weightedN;
        AuxiliaryFunctions.overwriteDoubleArray(this.LS, other.LS);
        AuxiliaryFunctions.overwriteDoubleArray(this.SS, other.SS);

    }

    @Override
    public double getWeight() {
        return this.weightedN;
    }



    /**
     * @return this kernels' center
     */
    public double[] getCenter() {
        assert (!this.isEmpty());
        double res[] = new double[this.LS.length];
        double weightedSize = this.getWeightedN();
        for (int i = 0; i < res.length; i++) {
            res[i] = this.LS[i] / weightedSize;
        }
        return res;
    }

    @Override
    public double getInclusionProbability(Instance instance) {

        double dist = calcNormalizedDistance(instance.toDoubleArray());
        double res = AuxiliaryFunctions.distanceProbabilty(dist, LS.length);
        assert (res >= 0.0 && res <= 1.0) : "Bad confidence " + res + " for"
                + " distance " + dist;

        return res;
    }

    /**
     * See interface <code>Cluster</code>
     * @return The radius of the cluster.
     * @see Cluster#getRadius()
     */
    public double getRadius() {
        double[] squaredVarianceVector = this.getSquaredVarianceVector();

        // The value with which every component of the squared root of every
        // variance vector component is multiplied.
        // TODO: weight MUST depend on #dimensions! (follow cumulative gamma function!)
        // SEE: http://en.wikipedia.org/wiki/Incomplete_gamma_function
        // SEE: http://ieeexplore.ieee.org/iel5/8819/27916/01246282.pdf
        // Numerical calculation: http://algolist.manual.ru/maths/count_fast/gamma_function.php
        final double componentWeight = 1;

        // Use standart deviation to calculate average radius
        double sumOfVariances = 0.0;
        for (int i = 0; i < squaredVarianceVector.length; i++) {
            double d = squaredVarianceVector[i];
            sumOfVariances += componentWeight * Math.sqrt(d);
        }

        return 1.6*(sumOfVariances / squaredVarianceVector.length);
    }



    public double[] getSquaredVarianceVector() {
        double[] res = new double[this.LS.length];
        for (int i = 0; i < this.LS.length; i++) {
            double ls = this.LS[i];
            double ss = this.SS[i];

            double lsDivN = ls / this.weightedN;
            double lsDivNSquared = lsDivN * lsDivN;
            double ssDivN = ss / this.weightedN;
            res[i] = ssDivN - lsDivNSquared;

            // Due to numerical errors, small negative values can occur.
            // We correct this by settings them to almost zero.
            if (res[i] <= 0.0) {
                if (res[i] > -EPSILON) {
                    res[i] = MIN_VARIANCE * MIN_VARIANCE;
                } else {
                    assert (false) : "Bad variance " + res[i]
                            + ", weighted N is " + getWeightedN();
                }
            }
        }
        return res;
    }

    /**
     * Calculate the normalized euclidean distance (Mahalanobis distance for
     * distribution w/o covariances) to a point.
     * @param other The point to which the distance is calculated.
     * @return The normalized distance to the cluster center.
     *
     * TODO: check whether WEIGHTING is correctly applied to variances
     */
    private double calcNormalizedDistance(double[] point) {
        assert (this.LS.length == point.length);
        double N1 = this.getWeightedN();
        double[] thisLS = this.LS;
        double[] squaredVariances = this.getSquaredVarianceVector();
        double res = 0.0;

        for (int i = 0; i < thisLS.length; i++) {
            double substracted = (thisLS[i] / N1) - (point[i]);
            res += (substracted * substracted) / squaredVariances[i];
        }
        return Math.sqrt(res);
    }


}
