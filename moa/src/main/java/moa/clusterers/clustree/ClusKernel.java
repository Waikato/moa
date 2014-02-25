/*
 *    ClusKernel.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Sanchez Villaamil (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.clusterers.clustree;

import moa.clusterers.clustree.util.*;
import java.util.Arrays;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Representation of an Entry in the tree
 */
public class ClusKernel extends CFCluster{
    /**
     * Numeric epsilon.
     */
    public static final double EPSILON = 0.00000001;
    public static final double MIN_VARIANCE = 1e-50; // 1e-100; // 0.0000001;


    /**
     * Counting of the number of N as normal N is weighted by how much time passes between
     * updates. If weighted N is under a threshhold, we may consider
     * the cluster irrelevant and we can delete it.
     */

    private double totalN;

    /**
     * A constructor that makes a Kernel which just represents the given point.
     * @param point The point to be converted into a corresponding Kernel.
     * @param numberClasses The number of classes possible for points in this
     * experiment(<code>Tree</code>).
     */
    public ClusKernel(double[] point, int dim) {
        super(point, dim);
        this.totalN = 1;
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
        this.totalN = 0;
    }

    /**
     * Instantiates a copy of the given cluster.
     * @param other The <code>Cluster</code> of which we make a copy.
     */
    protected ClusKernel(ClusKernel other) {
        super(other);
        this.totalN = other.getTotalN();
    }

    /**
     * Adds the given cluster to this cluster, without making this cluster
     * older.
     * @param other
     */
    public void add(ClusKernel other) {
        super.add(other);
        this.totalN += other.totalN;
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

        //double weightFactor = AuxiliaryFunctions.weight(negLambda, timeDifference);
        assert (negLambda < 0);
        assert (timeDifference > 0);
        double weightFactor = Math.pow(2.0, negLambda * timeDifference);

        this.N *= weightFactor;
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
    public double calcDistance(ClusKernel other) {
        // TODO: (Fernando, Felix) Adapt the distance function to the new algorithmn.

        double N1 = this.getWeight();
        double N2 = other.getWeight();

        double[] thisLS = this.LS;
        double[] otherLS = other.LS;

        double res = 0.0;
        for (int i = 0; i < thisLS.length; i++) {
            double substracted = (thisLS[i] / N1) - (otherLS[i] / N2);
            res += substracted * substracted;
        }

        // TODO INFO: added sqrt to the computation [PK 10.09.10] 
        return Math.sqrt(res);
    }

    /**
     * Returns the weighted number of points in the cluster.
     * @return The weighted number of points in the cluster.
     */
    private double getTotalN() {
        return totalN;
    }

    /**
     * Check if this cluster is empty or not.
     * @return <code>true</code> if the cluster has no data points,
     * <code>false</code> otherwise.
     */
    protected boolean isEmpty() {
        return this.totalN == 0;
    }

    /**
     * Remove all points from this cluster.
     */
    protected void clear() {
        this.totalN = 0;
        this.N = 0.0;
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
        this.totalN = other.totalN;
        this.N = other.N;
        //AuxiliaryFunctions.overwriteDoubleArray(this.LS, other.LS);
        //AuxiliaryFunctions.overwriteDoubleArray(this.SS, other.SS);
        assert (LS.length == other.LS.length);
        System.arraycopy(other.LS, 0, LS, 0, LS.length);
        assert (SS.length == other.SS.length);
        System.arraycopy(other.SS, 0, SS, 0, SS.length);
    }

    @Override
    public double getWeight() {
        return this.N;
    }


    @Override
    public CFCluster getCF(){
        return this;
    }


    /**
     * @return this kernels' center
     */
    public double[] getCenter() {
        assert (!this.isEmpty());
        double res[] = new double[this.LS.length];
        double weightedSize = this.getWeight();
        for (int i = 0; i < res.length; i++) {
            res[i] = this.LS[i] / weightedSize;
        }
        return res;
    }

//    @Override
//    public double getInclusionProbability(Instance instance) {
//
//        double dist = calcNormalizedDistance(instance.toDoubleArray());
//        double res = AuxiliaryFunctions.distanceProbabilty(dist, LS.length);
//        assert (res >= 0.0 && res <= 1.0) : "Bad confidence " + res + " for"
//                + " distance " + dist;
//
//        return res;
//    }

    @Override
    public double getInclusionProbability(Instance instance) {
        //trivial cluster
        if(N == 1){
            double distance = 0.0;
            for (int i = 0; i < LS.length; i++) {
                double d = LS[i] - instance.value(i);
                distance += d * d;
            }
            distance = Math.sqrt(distance);
            if( distance < EPSILON )
                return 1.0;
            return 0.0;
        }
        else{
            double dist = calcNormalizedDistance(instance.toDoubleArray());
            if(dist <= getRadius()){
                return 1;
            }
            else{
                return 0;
            }
//            double res = AuxiliaryFunctions.distanceProbabilty(dist, LS.length);
//            return res;
        }
    }

    /**
     * See interface <code>Cluster</code>
     * @return The radius of the cluster.
     * @see Cluster#getRadius()
     */
    @Override
    public double getRadius() {
        //trivial cluster
        if(N == 1) return 0;

        return getDeviation()*radiusFactor;
    }


    private double getDeviation(){
        double[] variance = getVarianceVector();
        double sumOfDeviation = 0.0;
        for (int i = 0; i < variance.length; i++) {
            double d = Math.sqrt(variance[i]);
            sumOfDeviation += d;
        }
        return sumOfDeviation / variance.length;
    }


    public double[] getVarianceVector() {
        double[] res = new double[this.LS.length];
        for (int i = 0; i < this.LS.length; i++) {
            double ls = this.LS[i];
            double ss = this.SS[i];

            double lsDivN = ls / this.getWeight();
            double lsDivNSquared = lsDivN * lsDivN;
            double ssDivN = ss / this.getWeight();
            res[i] = ssDivN - lsDivNSquared;

            // Due to numerical errors, small negative values can occur.
            // We correct this by settings them to almost zero.
            if (res[i] <= 0.0) {
                if (res[i] > -EPSILON) {
                    res[i] = MIN_VARIANCE;
                }
            }
            else{

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
    //???????
    private double calcNormalizedDistance(double[] point) {
        double[] variance = getVarianceVector();
        double[] center = getCenter();
        double res = 0.0;

        for (int i = 0; i < center.length; i++) {
            double diff = center[i] - point[i];
            res += (diff * diff);// variance[i];
        }
        return Math.sqrt(res);
    }


}
