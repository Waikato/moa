/*
 *    CoresetKMeans.java
 *    Copyright (C) 2015 TU Dortmund University, Germany
 *    @author Jan Stallmann (jan.stallmann@tu-dortmund.de)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.clusterers.kmeanspm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides methods to execute the k-means and k-means++ algorithm with a
 * clustering.
 *
 * Citation: David Arthur, Sergei Vassilvitskii:
 * k-means++: the advantages of careful seeding.
 * SODA 2007: 1027-1035
 *
 */
public class CoresetKMeans {

	/**
	 * Generates the initial centroids like the k-means++ algorithm.
	 *
	 * @param k
	 *            number of centroids
	 * @param input
	 *            input clustering
	 * @param random
	 *            instance to generate a stream of pseudorandom numbers
	 * @return the generated centroids
	 */
	public static List<double[]> generatekMeansPlusPlusCentroids(int k,
			List<double[]> input, Random random) {
		int n = input.size();
		assert (n > 0);
		int d = input.get(0).length - 1;
		assert (k <= n);

		List<double[]> centerValue = new ArrayList<double[]>(k);
		// Selects and copies the first centroid
		double[] lastCenter = new double[d];
		System.arraycopy(input.get(random.nextInt(n)), 1, lastCenter, 0, d);
		centerValue.add(lastCenter);

		double[] distance = new double[n];
		for (int j = 0; j < n; j++) {
			distance[j] = Double.POSITIVE_INFINITY;
		}
		for (int i = 1; i < k; i++) {
			// Selects the next centroid
			double sum = 0.0;
			Iterator<double[]> jIter = input.iterator();
			for (int j = 0; j < n; j++) {
				double[] point = jIter.next();
				distance[j] = Math
						.min(distance[j],
								point[0]
										* Metric.distanceSquared(lastCenter,
												point, 1));
				sum += distance[j];

			}
			int candidate = 0;
			if (sum > 0) {
				double nextCenterValue = sum * random.nextDouble();
				double currentValue = distance[0];
				while (!(nextCenterValue < currentValue)) {
					currentValue += distance[++candidate];
				}
			}
			// Copies the selected centroid
			lastCenter = new double[d];
			System.arraycopy(input.get(candidate), 1, lastCenter, 0, d);
			centerValue.add(lastCenter);
		}

		return centerValue;
	}

	/**
	 * Executes the k-means algorithm with the given initial centroids until the
	 * costs converges.
	 *
	 * @param centroids
	 *            initial centroids
	 * @param input
	 *            input clustering
	 * @return the k-means costs
	 */
	public static double kMeans(List<double[]> centroids, List<double[]> input) {
		int k = centroids.size();
		assert (k > 0);
		int d = centroids.get(0).length;
		int size = input.size();

		double[][] center = new double[k][];
		Iterator<double[]> iIter = centroids.iterator();
		for (int i = 0; i < k; i++) {
			center[i] = iIter.next();
		}

		double[][] newCenter = new double[k][d];
		double[] newCenterWeight = new double[k];
		int[] nearestCluster = new int[size];
		boolean converged;
		do {
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < d; j++) {
					newCenter[i][j] = 0.0;
				}
				newCenterWeight[i] = 0.0;
			}
			Iterator<double[]> lIter = input.iterator();
			for (int l = 0; l < size; l++) {
				// Calculates the distance from all points to all centroids
				double[] point = lIter.next();
				assert (d == point.length - 1);
				double minDistance = Double.POSITIVE_INFINITY;
				int closestCluster = -1;
				for (int i = 0; i < k; i++) {
					double distance = Metric.distance(center[i], point, 1);
					if (distance < minDistance) {
						closestCluster = i;
						minDistance = distance;
					}
				}

				// Sums up all points for the new centroids
				assert (closestCluster >= 0 && closestCluster < k);
				for (int j = 0; j < d; j++) {
					newCenter[closestCluster][j] += point[0] * point[j + 1];
				}
				newCenterWeight[closestCluster] += point[0];
				nearestCluster[l] = closestCluster;
			}

			// Calculates the new centroids
			converged = true;
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < d; j++) {
					if (newCenterWeight[i] != 0.0) {
						double newValue = newCenter[i][j] / newCenterWeight[i];
						if (newValue != center[i][j]) {
							converged = false;
						}
						center[i][j] = newValue;
					}
				}
			}
		} while (!converged);
		// Replaces the old centroids with the new ones
		for (int i = 0; i < k; i++) {
			centroids.set(i, center[i]);
		}

		// Calculates the costs of the solution
		double costs = 0.0;
		Iterator<double[]> lIter = input.iterator();
		for (int l = 0; l < size; l++) {
			double[] point = lIter.next();
			costs += point[0]
					* Metric.distanceSquared(center[nearestCluster[l]], point,
							1);
		}
		return costs;
	}

}
