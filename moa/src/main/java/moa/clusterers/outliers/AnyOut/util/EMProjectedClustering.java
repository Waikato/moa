/*
 *    EMProjectedClustering.java
 *
 *    @author I. Assent, P. Kranen, C. Baldauf, T. Seidl
 *    @author G. Piskas, A. Gounaris
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

package moa.clusterers.outliers.AnyOut.util;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

/**
 * Implements clustering via Expectation Maximization
 * but return a clear partitioning of the data, 
 * i.e. a point belongs to only one cluster (the most probable)
 * @author kranen
 *
 */
public class EMProjectedClustering {
	
	private long randomSeed = 1L;

	// the points to cluster
	double[][] pointArray;
	int n;		// the total number of points
	int dim;	// the number of dimensions
	int k;		// the number of requested partitions
	
	// mininum variance (like kernel bandwidth) to avoid division by zero
	private final double minVariance = 0.01;
	// stop criterion: deviation of expectation as minDeviation = 1 - (E_old / E_new)
	private final double minDeviation = 0.01;
	private final int MAXITER = 20;

	// means of the clusters
	double[][] clusterMeans;
	// variance vectors of the clusters
	double[][] clusterVariances;
	
	// probability for the points given a cluster
	double[][] pXgivenC;
	// probabilities for points to belong to a cluster
	double[][] pCgivenX;
	// weight of the clusters
	double[] clusterWeights;
	// probabilities of the points
	double[] pX;

	public  EMProjectedClustering() {}
	
	/**
	 * Performs an EM clustering on the provided data set
	 * !! Only the variances are calculated and used for point assignments !
	 * !!! the number k' of returned clusters might be smaller than k !!! 
	 * @param pointArray the data set as an array[n][d] of n points with d dimensions
	 * @param k the number of requested partitions (!might return less)
	 * @return a mapping int[n][k'] of the n given points to the k' resulting clusters
	 */
	public int[][] getEMClusteringVariances(double[][] pointArray, int k) {
		// initialize field and clustering
		initFields(pointArray, k);
		setInitialPartitions(pointArray, k);
		
		// iterate M and E
		double currentExpectation, newExpectation = 0.0;
		double expectationDeviation;
		int count = 0;
		do{
			currentExpectation = newExpectation;
			// reassign objects
			getNewClusterRepresentation();
			calculateAllProbabilities();
			// calculate new expectation value
			newExpectation = expectation();
			// stop when the deviation is less than minDeviation
			expectationDeviation = 1.0 - (currentExpectation / newExpectation);
			count++;
		} while (expectationDeviation > minDeviation && count < MAXITER);
		
		// return the resulting mapping from points to clusters
		return createProjectedClustering();
	}
	
	public int[][] getEMClusteringVariancesBestChoice(double[][] pointArray, int k, int nrOfChoices) {
		nrOfChoices = Math.max(1, nrOfChoices);
		int[][] bestChoice = null;
		int[][] tmpChoice = null;
		double tmpMeasure, bestMeasure = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i < nrOfChoices; i++) {
			this.randomSeed = i;
			tmpChoice = this.getEMClusteringVariances(pointArray, k);
			tmpMeasure = this.expectation();
			if (tmpMeasure > bestMeasure) {
				bestMeasure = tmpMeasure;
				bestChoice = tmpChoice;
			}
		}
		
		return bestChoice;
	}
	
	/**
	 * @param n nrOfPoints
	 * @param dim nrOfDimensions
	 * @param k nrOfClusters
	 */
	private void initFields(double[][] pointArray, int k) {
		this.pointArray = pointArray;
		this.n = pointArray.length;
		this.dim = pointArray[0].length;
		this.k = k;
		
		// means of the clusters
		this.clusterMeans = new double[k][dim];
		// variance vectors of the clusters
		this.clusterVariances = new double[k][dim];
		
		// probability for the points given a cluster
		this.pXgivenC = new double[n][k];
		// probabilities for points to belong to a cluster
		this.pCgivenX = new double[k][n];
		// weight of the clusters
		this.clusterWeights = new double[k];
		// probabilities of the points
		this.pX = new double[n];
	}
	
	/**
	 * creates an initial partitioning
	 * @param pointArray
	 * @param k
	 */
	private void setInitialPartitions(double[][] pointArray, int k) {
		if (pointArray.length < k) {
			System.err.println("cannot cluster less than k points into k clusters...");
			System.exit(0);
		}
		
		// select randomly k initial means
		// TODO: choose more stable initialization (comment: this is run several times (with different random seeds), i.e. the worst case should not occur)
        Random random = new Random(randomSeed);
		TreeSet<Integer> usedPoints = new TreeSet<Integer>();
		int nextIndex;
		for (int i = 0; i < k; i++) {
			nextIndex = ((Double)(Math.floor(random.nextDouble() * n))).intValue();
			if (usedPoints.contains(nextIndex)) {
				i--;
				continue;
			} else {
				for (int d = 0; d < dim; d++) {
					clusterMeans[i][d] = pointArray[nextIndex][d];
				}
			}
		}
		
		// set pCgivenX=1 for the closest C for each point
		int minDistIndex = 0;
		double minDist, currentDist;
		for (int x = 0; x < pointArray.length; x++) {
			minDist = Double.MAX_VALUE;
			for (int i = 0; i < k; i++) {
				currentDist = euclideanDistance(clusterMeans[i], pointArray[x]);
				if (currentDist < minDist) {
					minDist = currentDist;
					minDistIndex = i;
				}
			}
			pCgivenX[minDistIndex][x] = 1.0;
		}
	}
	
	private double euclideanDistance(double[] x, double[] y) {
		double result = 0.0;
		// d(x, y) = sqrt( \sum_{i=1 to d} ( (x_i - y_i)^2 ) )
		for (int i = 0; i < x.length; i++) {
			result += (x[i] - y[i]) * (x[i] - y[i]);
		}
		return Math.sqrt(result);
	}
	
	private void getNewClusterRepresentation() {
		calculateWeights();
		calculateMeans();
		calculateVariances();
	}
	
	private void calculateWeights() {
		// W_i = 1/n * \sum_{x in D} ( P(C_i|x) )
		for (int i = 0; i < k; i++) {
			clusterWeights[i] = 0.0;
			for (int x = 0; x < n; x++) {
				clusterWeights[i] += pCgivenX[i][x];
			}
			clusterWeights[i] /= n;
		}
	}
	
	private void calculateMeans() {
		// per dimension d:
		// mu_i = [\sum_{x in D} ( x * P(C_i|x) )] / [W_i * n]
		for (int i = 0; i < k; i++) {
			for (int d = 0; d < dim; d++) {
				clusterMeans[i][d] = 0.0;
				if (clusterWeights[i] <= 0.0)
					continue;
				for (int x = 0; x < n; x++) {
					clusterMeans[i][d] += pointArray[x][d] * pCgivenX[i][x];
				}
				clusterMeans[i][d] /= (clusterWeights[i] * n);
			}
		}
	}
	
	private void calculateVariances() {
		// per dimension d:
		// sigma_i = [\sum_{x in D} (P(C_i|x) * (x-mu_i)^2)] / ] / [W_i * n]
		double xMinusMu;
		for (int i = 0; i < k; i++) {
			for (int d = 0; d < dim; d++) {
				clusterVariances[i][d] = 0.0;
				if (clusterWeights[i] <= 0.0)
					continue;
				for (int x = 0; x < n; x++) {
					xMinusMu = pointArray[x][d] - clusterMeans[i][d];
					clusterVariances[i][d] += pCgivenX[i][x] * (xMinusMu * xMinusMu);
				}
				if (clusterVariances[i][d] < this.minVariance)
					clusterVariances[i][d] = this.minVariance;
				else 
					clusterVariances[i][d] /= (clusterWeights[i] * n);
			}
		}
	}
	
	private void calculateClassConditionalDensities() {
		// P(x|C) = factor * e^( -0.5 (x-mu) * Sigma^-1 * (x-mu) )
		
		// exponent = -0.5 (x-mu) * Sigma^-1 * (x-mu) )
		// 			= -0.5 ( sum_d ((x_i - mu_i)^2 / var_i) )
		double exponent;
		double xMinusMu;

		// factor = 1 / sqrt( (2*PI)^d * \prod_d(variance) )
		//        = 1 / sqrt((2*PI)^d) * sqrt(\prod_d(variance))
		//        = 1 / (2*PI)^(d/2)   * \prod_d(sqrt(variance))
		double factor;
		
		// for each cluster C_i
		for (int i = 0; i < pXgivenC[0].length; i++) {
			// if the cluster is empty, set P(x|C_i) = 0
			if (clusterWeights[i] <= 0.0) {
				for (int x = 0; x < pXgivenC.length; x++)
					pXgivenC[x][i] = 0.0;
				continue;
			}
			
			// calculate the factor for each cluster
			factor = Math.pow( (2.0 * Math.PI) , ((dim/2.0)) );
			for (int d = 0; d < dim; d++) {
				factor *= Math.sqrt(clusterVariances[i][d]);
			}
			// factor = 1 / sqrt( (2*PI)^d * \prod_d(variance) )
			factor = 1 / factor;
			
			// calculate the density for each point x
			for (int x = 0; x < pXgivenC.length; x++) {
				exponent = 0.0;
				for (int d = 0; d < dim; d++) {
					xMinusMu = pointArray[x][d] - clusterMeans[i][d];
					exponent += (xMinusMu * xMinusMu) / clusterVariances[i][d];
				}
				exponent *= -0.5;
				pXgivenC[x][i] = factor * Math.exp(exponent);
			}
		}
	}
	
	private void calculatePX() {
		// P(x) = \sum_{i=1 to k} ( W_i * P(x|C_i) )
		for (int x = 0; x < n; x++) {
			pX[x] = 0.0;
			for (int i = 0; i < k; i++) {
				pX[x] += clusterWeights[i] * pXgivenC[x][i];
			}
		}
	}
	
	private void calculatePCgivenX() {
		// P(C_i|x) = W_i * P(x|C_i)/P(x)
		for (int i = 0; i < k; i++) {
			for (int x = 0; x < n; x++) {
				if (pX[x] <= 0.0)
					pCgivenX[i][x] = 0.0;
				else
					pCgivenX[i][x] = clusterWeights[i] * pXgivenC[x][i] / pX[x];
			}
		}
	}
	
	private void calculateAllProbabilities() {
		this.calculateClassConditionalDensities();
		this.calculatePX();
		this.calculatePCgivenX();
	}
	
	private double expectation() {
		// E(M) = \sum_{x in D} ( P(x) )
		double result = 0.0;
		for (int x = 0; x < n; x++) {
			//result += Math.log(pX[x]);
			result += pX[x];
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private int[][] createProjectedClustering() {
		TreeSet<Integer> nonEmptyClusters = new TreeSet<Integer>();
		ArrayList<Integer>[] clusterMemberLists = new ArrayList[k];
		for (int i = 0; i < k; i++) {
			clusterMemberLists[i] = new ArrayList<Integer>();
		}
		
		double maxProbability;
		int maxProbIndex = 0;
		// for each point ...
		for (int x = 0; x < n; x++) {
			maxProbability = 0.0;
			// find the most probable cluster
			for (int i = 0; i < k; i++) {
				if (pCgivenX[i][x] > maxProbability) {
					maxProbability = pCgivenX[i][x];
					maxProbIndex = i;
				}
			}
			// and add the point to the cluster's member list
			clusterMemberLists[maxProbIndex].add(x);
			nonEmptyClusters.add(maxProbIndex);
		}
		
		// copy the member lists to an array while removing the empty lists (i.e. empty clusters)
		int[][] result = new int[nonEmptyClusters.size()][];
		int counter = 0;
		for (int i = 0; i < clusterMemberLists.length; i++) {
			if (clusterMemberLists[i].size() > 0) {
				result[counter] = new int[clusterMemberLists[i].size()];
				for (int j = 0; j < clusterMemberLists[i].size(); j++) {
					result[counter][j] = clusterMemberLists[i].get(j);
				}
				counter++;
			}
		}
		return result;
	}
}
