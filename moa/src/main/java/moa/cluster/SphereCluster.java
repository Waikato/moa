/*
 *    SphereCluster.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * A simple implementation of the <code>Cluster</code> interface representing
 * spherical clusters. The inclusion probability is one inside the sphere and zero
 * everywhere else.
 *
 */
public class SphereCluster extends Cluster {

	private static final long serialVersionUID = 1L;

	private double[] center;
	private double radius;
	private double weight;


	public SphereCluster(double[] center, double radius) {
		this( center, radius, 1.0 );
	}

	public SphereCluster() {
	}

	public SphereCluster( double[] center, double radius, double weightedSize) {
		this();
		this.center = center;
		this.radius = radius;
		this.weight = weightedSize;
	}

	public SphereCluster(int dimensions, double radius, Random random) {
		this();
		this.center = new double[dimensions];
		this.radius = radius;

		// Position randomly but keep hypersphere inside the boundaries
		double interval = 1.0 - 2 * radius;
		for (int i = 0; i < center.length; i++) {
			this.center[i] = (random.nextDouble() * interval) + radius;
		}
		this.weight = 0.0;
	}


	public SphereCluster(List<?extends Instance> instances, int dimension){
		this();
		if(instances == null || instances.size() <= 0)
			return;

		weight = instances.size();

		Miniball mb = new Miniball(dimension);
		mb.clear();

		for (Instance instance : instances) {
			mb.check_in(instance.toDoubleArray());
		}

		mb.build();
		center = mb.center();
		radius = mb.radius();
		mb.clear();
	}


	/**
	 * Checks whether two <code>SphereCluster</code> overlap based on radius
	 * NOTE: overlapRadiusDegree only calculates the overlap based
	 * on the centers and the radi, so not the real overlap
	 *
	 * TODO: should we do this by MC to get the real overlap???
	 *
	 * @param other
	 * @return
	 */

	public double overlapRadiusDegree(SphereCluster other) {


		double[] center0 = getCenter();
		double radius0 = getRadius();

		double[] center1 = other.getCenter();
		double radius1 = other.getRadius();

		double radiusBig;
		double radiusSmall;
		if(radius0 < radius1){
			radiusBig = radius1;
			radiusSmall = radius0;
		}
		else{
			radiusBig = radius0;
			radiusSmall = radius1;
		}

		double dist = 0;
		for (int i = 0; i < center0.length; i++) {
			double delta = center0[i] - center1[i];
			dist += delta * delta;
		}
		dist = Math.sqrt(dist);

		if(dist > radiusSmall + radiusBig)
			return 0;
		if(dist + radiusSmall <= radiusBig){
			//one lies within the other
			return 1;
		}
		else{
			return (radiusSmall+radiusBig-dist)/(2*radiusSmall);
		}
	}

	public void combine(SphereCluster cluster) {
		double[] center = getCenter();
		double[] newcenter = new double[center.length];
		double[] other_center = cluster.getCenter();
		double other_weight = cluster.getWeight();
		double other_radius = cluster.getRadius();

		for (int i = 0; i < center.length; i++) {
			newcenter[i] = (center[i]*getWeight()+other_center[i]*other_weight)/(getWeight()+other_weight);
		}

		center = newcenter;
		double r_0 = getRadius() + Math.abs(distance(center, newcenter));
		double r_1 = other_radius + Math.abs(distance(other_center, newcenter));
		radius = Math.max(r_0, r_1);
		weight+= other_weight;
	}

	public void merge(SphereCluster cluster) {
		double[] c0 = getCenter();
		double w0 = getWeight();
		double r0 = getRadius();

		double[] c1 = cluster.getCenter();
		double w1 = cluster.getWeight();
		double r1 = cluster.getRadius();

		//vector
		double[] v = new double[c0.length];
		//center distance
		double d = 0;

		for (int i = 0; i < c0.length; i++) {
			v[i] = c0[i] - c1[i];
			d += v[i] * v[i];
		}
		d = Math.sqrt(d);



		double r = 0;
		double[] c = new double[c0.length];

		//one lays within the others
		if(d + r0 <= r1  || d + r1 <= r0){
			if(d + r0 <= r1){
				r = r1;
				c = c1;
			}
			else{
				r = r0;
				c = c0;
			}
		}
		else{
			r = (r0 + r1 + d)/2.0;
			for (int i = 0; i < c.length; i++) {
				c[i] = c1[i] - v[i]/d * (r1-r);
			}
		}

		setCenter(c);
		setRadius(r);
		setWeight(w0+w1);

	}

	@Override
	public double[] getCenter() {
		double[] copy = new double[center.length];
		System.arraycopy(center, 0, copy, 0, center.length);
		return copy;
	}

	public void setCenter(double[] center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius( double radius ) {
		this.radius = radius;
	}

	@Override
	public double getWeight() {
		return weight;
	}

	public void setWeight( double weight ) {
		this.weight = weight;
	}

	@Override
	public double getInclusionProbability(Instance instance) {
		if (getCenterDistance(instance) <= getRadius()) {
			return 1.0;
		}
		return 0.0;
	}

	public double getCenterDistance(Instance instance) {
		double distance = 0.0;
		//get the center through getCenter so subclass have a chance
		double[] center = getCenter();
		for (int i = 0; i < center.length; i++) {
			double d = center[i] - instance.value(i);
			distance += d * d;
		}
		return Math.sqrt(distance);
	}

	public double getCenterDistance(SphereCluster other) {
		return distance(getCenter(), other.getCenter());
	}

	/*
	 * the minimal distance between the surface of two clusters.
	 * is negative if the two clusters overlap
	 */
	public double getHullDistance(SphereCluster other) {
		double distance = 0.0;
		//get the center through getCenter so subclass have a chance
		double[] center0 = getCenter();
		double[] center1 = other.getCenter();
		distance = distance(center0, center1);

		distance = distance - getRadius() - other.getRadius();
		return distance;
	}

	/*
	 */
	/**
	 * When a clusters looses points the new minimal bounding sphere can be
	 * partly outside of the originating cluster. If a another cluster is
	 * right next to the original cluster (without overlapping), the new
	 * cluster can be overlapping with this second cluster. OverlapSave
	 * will tell you if the current cluster can degenerate so much that it
	 * overlaps with cluster 'other'
	 * 
	 * @param other the potentially overlapping cluster
	 * @return true if cluster can potentially overlap
	 */
	public boolean overlapSave(SphereCluster other){
		//use basic geometry to figure out the maximal degenerated cluster
		//comes down to Max(radius *(sin alpha + cos alpha)) which is
		double minDist = Math.sqrt(2)*(getRadius() + other.getRadius());
		double diff = getCenterDistance(other) - minDist;

		if(diff > 0)
			return true;
		else
			return false;
	}

	private double distance(double[] v1, double[] v2){
		double distance = 0.0;
		double[] center = getCenter();
		for (int i = 0; i < center.length; i++) {
			double d = v1[i] - v2[i];
			distance += d * d;
		}
		return Math.sqrt(distance);
	}

	public double[] getDistanceVector(Instance instance){
		return distanceVector(getCenter(), instance.toDoubleArray());
	}

	public double[] getDistanceVector(SphereCluster other){
		return distanceVector(getCenter(), other.getCenter());
	}

	private double[] distanceVector(double[] v1, double[] v2){
		double[] v = new double[v1.length];
		for (int i = 0; i < v1.length; i++) {
			v[i] = v2[i] - v1[i];
		}
		return v;
	}

 
	/**
	 * Samples this cluster by returning a point from inside it.
	 * @param random a random number source
	 * @return a point that lies inside this cluster
	 */
	public Instance sample(Random random) {
		// Create sample in hypersphere coordinates
		//get the center through getCenter so subclass have a chance
		double[] center = getCenter();

		final int dimensions = center.length;

		final double sin[] = new double[dimensions - 1];
		final double cos[] = new double[dimensions - 1];
		final double length = random.nextDouble() * getRadius();

		double lastValue = 1.0;
		for (int i = 0; i < dimensions-1; i++) {
			double angle = random.nextDouble() * 2 * Math.PI;
			sin[i] = lastValue * Math.sin( angle ); // Store cumulative values
			cos[i] = Math.cos( angle );
			lastValue = sin[i];
		}

		// Calculate cartesian coordinates
		double res[] = new double[dimensions];

		// First value uses only cosines
		res[0] = center[0] + length*cos[0];

		// Loop through 'middle' coordinates which use cosines and sines
		for (int i = 1; i < dimensions-1; i++) {
			res[i] = center[i] + length*sin[i-1]*cos[i];
		}

		// Last value uses only sines
		res[dimensions-1] = center[dimensions-1] + length*sin[dimensions-2];

		return new DenseInstance(1.0, res);
	}

	@Override
	protected void getClusterSpecificInfo(ArrayList<String> infoTitle, ArrayList<String> infoValue) {
		super.getClusterSpecificInfo(infoTitle, infoValue);
		infoTitle.add("Radius");
		infoValue.add(Double.toString(getRadius()));
	}


}
