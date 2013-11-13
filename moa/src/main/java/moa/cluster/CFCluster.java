/*
 *    CFCluster.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
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

package moa.cluster;
import java.util.Arrays;
import com.yahoo.labs.samoa.instances.Instance;

/* micro cluster, as defined by Aggarwal et al, On Clustering Massive Data Streams: A Summarization Praradigm 
 * in the book Data streams : models and algorithms, by Charu C Aggarwal
 *  @article{
	title = {Data Streams: Models and Algorithms},
	author = {Aggarwal, Charu C.},
	year = {2007},
	publisher = {Springer Science+Business Media, LLC},
	url = {http://ebooks.ulb.tu-darmstadt.de/11157/},
	institution = {eBooks [http://ebooks.ulb.tu-darmstadt.de/perl/oai2] (Germany)},
}

DEFINITION A micro-clusterfor a set of d-dimensionalpoints Xi,. .Xi,
with t i m e s t a m p s ~. . .T,, is the (2-d+3)tuple (CF2", CFlX CF2t, CFlt, n),
wherein CF2" and CFlX each correspond to a vector of d entries. The definition of each of these entries is as follows:

o For each dimension, the sum of the squares of the data values is maintained
in CF2". Thus, CF2" contains d values. The p-th entry of CF2" is equal to
\sum_j=1^n(x_i_j)^2

o For each dimension, the sum of the data values is maintained in C F l X .
Thus, CFIX contains d values. The p-th entry of CFIX is equal to
\sum_j=1^n x_i_j

o The sum of the squares of the time stamps Ti,. .Tin maintained in CF2t

o The sum of the time stamps Ti, . . .Tin maintained in CFlt.

o The number of data points is maintained in n.

 */
public abstract class CFCluster extends SphereCluster {

	private static final long serialVersionUID = 1L;

	protected double radiusFactor = 1.8;

	/**
	 * Number of points in the cluster.
	 */
	protected double N;
	/**
	 * Linear sum of all the points added to the cluster.
	 */
	public double[] LS;
	/**
	 * Squared sum of all the points added to the cluster.
	 */
	public double[] SS;

	/**
	 * Instantiates an empty kernel with the given dimensionality.
	 * @param dimensions The number of dimensions of the points that can be in
	 * this kernel.
	 */
	public CFCluster(Instance instance, int dimensions) {
		this(instance.toDoubleArray(), dimensions);
	}

	protected CFCluster(int dimensions) {
		this.N = 0;
		this.LS = new double[dimensions];
		this.SS = new double[dimensions];
		Arrays.fill(this.LS, 0.0);
		Arrays.fill(this.SS, 0.0);
	}

	public CFCluster(double [] center, int dimensions) {
		this.N = 1;
		this.LS = center;
		this.SS = new double[dimensions];
		for (int i = 0; i < SS.length; i++) {
			SS[i]=Math.pow(center[i], 2);
		}
	}

	public CFCluster(CFCluster cluster) {
		this.N = cluster.N;
		this.LS = Arrays.copyOf(cluster.LS, cluster.LS.length);
		this.SS = Arrays.copyOf(cluster.SS, cluster.SS.length);
	}

	public void add(CFCluster cluster ) {
		this.N += cluster.N;
		addVectors( this.LS, cluster.LS );
		addVectors( this.SS, cluster.SS );
	}

	public abstract CFCluster getCF();

	/**
	 * @return this kernels' center
	 */
	 @Override
	 public double[] getCenter() {
		 assert (this.N>0);
		 double res[] = new double[this.LS.length];
		 for ( int i = 0; i < res.length; i++ ) {
			 res[i] = this.LS[i] / N;
		 }
		 return res;
	 }


	 @Override
	 public abstract double getInclusionProbability(Instance instance);

	 /**
	  * See interface <code>Cluster</code>
	  * @return The radius of the cluster.
	  */
	 @Override
	 public abstract double getRadius();

	 /**
	  * See interface <code>Cluster</code>
	  * @return The weight.
	  * @see Cluster#getWeight() 
	  */
	 @Override
	 public double getWeight() {
		 return N;
	 }

	 public void setN(double N){
		 this.N = N;
	 }

	 public double getN() {
		 return N;
	 }

	 /**
	  * Adds the second array to the first array element by element. The arrays
	  * must have the same length.
	  * @param a1 Vector to which the second vector is added.
	  * @param a2 Vector to be added. This vector does not change.
	  */
	 public static void addVectors(double[] a1, double[] a2) {
		 assert (a1 != null);
		 assert (a2 != null);
		 assert (a1.length == a2.length) : "Adding two arrays of different "
			 + "length";

		 for (int i = 0; i < a1.length; i++) {
			 a1[i] += a2[i];
		 }
	 }
}
