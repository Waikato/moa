/*
 *    Metric.java
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

/**
 * Provides methods to calculate different distances of points.
 *
 */
public class Metric {

	/**
	 * Calculates the squared Euclidean length of a point.
	 *
	 * @param pointA
	 *            point
	 * @return the squared Euclidean length
	 */
	public static double distanceSquared(double[] pointA) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			distance += pointA[i] * pointA[i];
		}
		return distance;
	}

	/**
	 * Calculates the Euclidean length of a point.
	 *
	 * @param pointA
	 *            point
	 * @return the Euclidean length
	 */
	public static double distance(double[] pointA) {
		return Math.sqrt(distanceSquared(pointA));
	}

	/**
	 * Calculates the squared Euclidean distance of two points. Starts at dimension
	 * offset + 1 of pointB.
	 *
	 * @param pointA
	 *            first point
	 * @param pointB
	 *            second point
	 * @param offsetB
	 *            start dimension - 1 of pointB
	 * @return the squared Euclidean distance
	 */
	public static double distanceSquared(double[] pointA, double[] pointB,
			int offsetB) {
		assert (pointA.length == pointB.length + offsetB);
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i + offsetB];
			distance += d * d;
		}
		return distance;
	}

	/**
	 * Calculates the Euclidean distance of two points. Starts at dimension offset
	 * + 1 of pointB.
	 *
	 * @param pointA
	 *            first point
	 * @param pointB
	 *            second point
	 * @param offsetB
	 *            start dimension - 1 of pointB
	 * @return the Euclidean distance
	 */
	public static double distance(double[] pointA, double[] pointB, int offsetB) {
		return Math.sqrt(distanceSquared(pointA, pointB, offsetB));
	}

	/**
	 * Calculates the squared Euclidean distance of two points.
	 *
	 * @param pointA
	 *            first point
	 * @param pointB
	 *            second point
	 * @return the squared Euclidean distance
	 */
	public static double distanceSquared(double[] pointA, double[] pointB) {
		assert (pointA.length == pointB.length);
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i];
			distance += d * d;
		}
		return distance;
	}

	/**
	 * Calculates the Euclidean distance of two points.
	 *
	 * @param pointA
	 *            first point
	 * @param pointB
	 *            second point
	 * @return the Euclidean distance
	 */
	public static double distance(double[] pointA, double[] pointB) {
		return Math.sqrt(distanceSquared(pointA, pointB));
	}

	/**
	 * Calculates the squared Euclidean length of a point divided by a scalar.
	 *
	 * @param pointA
	 *            point
	 * @param dA
	 *            scalar
	 * @return the squared Euclidean length
	 */
	public static double distanceWithDivisionSquared(double[] pointA, double dA) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] / dA;
			distance += d * d;
		}
		return distance;
	}

	/**
	 * Calculates the Euclidean length of a point divided by a scalar.
	 *
	 * @param pointA
	 *            point
	 * @param dA
	 *            scalar
	 * @return the Euclidean length
	 */
	public static double distanceWithDivision(double[] pointA, double dA) {
		return Math.sqrt(distanceWithDivisionSquared(pointA, dA));
	}

	/**
	 * Calculates the squared Euclidean distance of the first point divided by a
	 * scalar and another second point.
	 *
	 * @param pointA
	 *            first point
	 * @param dA
	 *            scalar
	 * @param pointB
	 *            second point
	 * @return the squared Euclidean distance
	 */
	public static double distanceWithDivisionSquared(double[] pointA,
			double dA, double[] pointB) {
		assert (pointA.length == pointB.length);
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = (pointA[i] / dA) - pointB[i];
			distance += d * d;
		}
		return distance;
	}

	/**
	 * Calculates the Euclidean distance of the first point divided by a scalar and
	 * another second point.
	 *
	 * @param pointA
	 *            first point
	 * @param dA
	 *            scalar
	 * @param pointB
	 *            second point
	 * @return the Euclidean distance
	 */
	public static double distanceWithDivision(double[] pointA, double dA,
			double[] pointB) {
		return Math.sqrt(distanceWithDivisionSquared(pointA, dA, pointB));
	}

	/**
	 * Calculates the squared Euclidean distance of the first point divided by a
	 * first scalar and another second point divided by a second scalar.
	 *
	 * @param pointA
	 *            first point
	 * @param dA
	 *            first scalar
	 * @param pointB
	 *            second point
	 * @param dB
	 *            second scalar
	 * @return the squared Euclidean distance
	 */
	public static double distanceWithDivisionSquared(double[] pointA,
			double dA, double[] pointB, double dB) {
		assert (pointA.length == pointB.length);
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = (pointA[i] / dA) - (pointB[i] / dB);
			distance += d * d;
		}
		return distance;
	}

	/**
	 * Calculates the Euclidean distance of the first point divided by a first scalar
	 * and another second point divided by a second scalar.
	 *
	 * @param pointA
	 *            first point
	 * @param dA
	 *            first scalar
	 * @param pointB
	 *            second point
	 * @param dB
	 *            second scalar
	 * @return the Euclidean distance
	 */
	public static double distanceWithDivision(double[] pointA, double dA,
			double[] pointB, double dB) {
		return Math.sqrt(distanceWithDivisionSquared(pointA, dA, pointB, dB));
	}

	/**
	 * Calculates the dot product of the point with itself.
	 *
	 * @param pointA
	 *            point
	 * @return the dot product
	 */
	public static double dotProduct(double[] pointA) {
		double product = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			product += pointA[i] * pointA[i];
		}
		return product;
	}

	/**
	 * Calculates the dot product of the first point with a second point.
	 *
	 * @param pointA
	 *            first point
	 * @param pointB
	 *            second point
	 * @return the dot product
	 */
	public static double dotProduct(double[] pointA, double[] pointB) {
		assert (pointA.length == pointB.length);
		double product = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			product += pointA[i] * pointB[i];
		}
		return product;
	}

	/**
	 * Calculates the dot product of the addition of the first and the second
	 * point with the third point.
	 *
	 * @param pointA1
	 *            first point
	 * @param pointA2
	 *            second point
	 * @param pointB
	 *            third point
	 * @return the dot product
	 */
	public static double dotProductWithAddition(double[] pointA1,
			double[] pointA2, double[] pointB) {
		assert (pointA1.length == pointA2.length && pointA1.length == pointB.length);
		double product = 0.0;
		for (int i = 0; i < pointA1.length; i++) {
			product += (pointA1[i] + pointA2[i]) * pointB[i];
		}
		return product;
	}

	/**
	 * Calculates the dot product of the addition of the first and the second
	 * point with the addition of the third and the fourth point.
	 *
	 * @param pointA1
	 *            first point
	 * @param pointA2
	 *            second point
	 * @param pointB1
	 *            third point
	 * @param pointB2
	 *            fourth point
	 * @return the dot product
	 */
	public static double dotProductWithAddition(double[] pointA1,
			double[] pointA2, double[] pointB1, double[] pointB2) {
		assert (pointA1.length == pointA2.length
				&& pointB1.length == pointB2.length && pointA1.length == pointB1.length);
		double product = 0.0;
		for (int i = 0; i < pointA1.length; i++) {
			product += (pointA1[i] + pointA2[i]) * (pointB1[i] + pointB2[i]);
		}
		return product;
	}

}
