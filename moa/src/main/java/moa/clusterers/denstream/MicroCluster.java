/*
 *    MicroCluster.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Wels (moa@cs.rwth-aachen.de)
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
package moa.clusterers.denstream;

import moa.cluster.CFCluster;
import com.yahoo.labs.samoa.instances.Instance;

public class MicroCluster extends CFCluster {

    private long lastEditT = -1;
    private long creationTimestamp = -1;
    private double lambda;
    private Timestamp currentTimestamp;

    public MicroCluster(double[] center, int dimensions, long creationTimestamp, double lambda, Timestamp currentTimestamp) {
        super(center, dimensions);
        this.creationTimestamp = creationTimestamp;
        this.lastEditT = creationTimestamp;
        this.lambda = lambda;
        this.currentTimestamp = currentTimestamp;
    }

    public MicroCluster(Instance instance, int dimensions, long timestamp, double lambda, Timestamp currentTimestamp) {
        this(instance.toDoubleArray(), dimensions, timestamp, lambda, currentTimestamp);
    }

    public void insert(Instance instance, long timestamp) {
        N++;
        super.setWeight(super.getWeight() + 1);
        this.lastEditT = timestamp;

        for (int i = 0; i < instance.numValues(); i++) {
            LS[i] += instance.value(i);
            SS[i] += instance.value(i) * instance.value(i);
        }
    }

    public long getLastEditTimestamp() {
        return lastEditT;
    }

    private double[] calcCF2(long dt) {
        double[] cf2 = new double[SS.length];
        for (int i = 0; i < SS.length; i++) {
            cf2[i] = Math.pow(2, -lambda * dt) * SS[i];
        }
        return cf2;
    }

    private double[] calcCF1(long dt) {
        double[] cf1 = new double[LS.length];
        for (int i = 0; i < LS.length; i++) {
            cf1[i] = Math.pow(2, -lambda * dt) * LS[i];
        }
        return cf1;
    }

    @Override
    public double getWeight() {
        return getWeight(currentTimestamp.getTimestamp());
    }

    private double getWeight(long timestamp) {
        long dt = timestamp - lastEditT;
        return (N * Math.pow(2, -lambda * dt));
    }

    public long getCreationTime() {
        return creationTimestamp;
    }

    @Override
    public double[] getCenter() {
        return getCenter(currentTimestamp.getTimestamp());
    }

    private double[] getCenter(long timestamp) {
        long dt = timestamp - lastEditT;
        double w = getWeight(timestamp);
        double[] res = new double[LS.length];
        for (int i = 0; i < LS.length; i++) {
            res[i] = LS[i];
            res[i] *= Math.pow(2, -lambda * dt);
            res[i] /= w;
        }
        return res;
    }

    @Override
    public double getRadius() {
        return getRadius(currentTimestamp.getTimestamp())*radiusFactor;
    }

    public double getRadius(long timestamp) {
        long dt = timestamp - lastEditT;
        double[] cf1 = calcCF1(dt);
        double[] cf2 = calcCF2(dt);
        double w = getWeight(timestamp);
        double max = 0;
        double sum = 0;
        for (int i = 0; i < SS.length; i++) {
            double x1 = cf2[i] / w;
            double x2 = Math.pow(cf1[i] / w, 2);
            //sum += Math.pow(x1 - x2,2);
            sum += (x1 - x2);
            if (Math.sqrt(x1 - x2) > max) {
                max = Math.sqrt(x1 - x2);
            }
        }
        return max;
    }

    @Override
    public MicroCluster copy() {
        MicroCluster copy = new MicroCluster(this.LS.clone(), this.LS.length, this.getCreationTime(), this.lambda, this.currentTimestamp);
        copy.setWeight(this.N + 1);
        copy.N = this.N;
        copy.SS = this.SS.clone();
        copy.LS = this.LS.clone();
        copy.lastEditT = this.lastEditT;
        return copy;
    }

    @Override
    public double getInclusionProbability(Instance instance) {
        if (getCenterDistance(instance) <= getRadius()) {
            return 1.0;
        }
        return 0.0;
    }

    @Override
    public CFCluster getCF(){
        CFCluster cf = copy();
        double w = getWeight();
        cf.setN(w);
        return cf;
    }
}
