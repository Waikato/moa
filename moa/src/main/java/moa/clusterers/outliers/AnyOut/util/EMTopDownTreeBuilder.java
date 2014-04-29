/*
 *    EMTopDownTreeBuilder.java
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

import moa.clusterers.clustree.ClusKernel;
import moa.clusterers.clustree.Entry;
import moa.clusterers.clustree.Node;

public class EMTopDownTreeBuilder {

	// fields
	private final int maxFanout = 3;
	private boolean ALLOW_KERNELS_IN_INNER_NODES = true;

	// constructors
	public EMTopDownTreeBuilder() {}

	/**
	 * 
	 * @param dataSet All data except the left out class
	 * @return the root node of the resulting tree
	 * @throws Exception
	 */
	public Node buildTree(DataSet dataSet) throws Exception {
		Node rootNode = this.buildTreeRecursively(dataSet, 0);

		return rootNode;
	}

	private Node buildTreeRecursively(DataSet dataSet, int level) throws Exception {
		Node resultNode = null;
		Entry[] entries = new Entry[maxFanout];
		int count = 0;
		// There are two cases where this method returns a leaf node
		// 1) the number of objects in the given data set is smaller than the fanout
		// 2) the data can not be split into at least two data sets with at least two objects each

		// case 1: if size is too small: create leaf node
		if (dataSet.size() <= maxFanout) {
			// create a kernel estimator for each object and insert it into the leaf node
			for(DataObject dataObject : dataSet) {
				// fill entry using the current 'dataObject'
				entries[count] = new Entry(dataSet.getNrOfDimensions());
				entries[count].add(new Entry(dataObject.getNrOfDimensions(),new ClusKernel(dataObject.getFeatures(),dataObject.getFeatures().length), 0));
				count++;
			}
			// else split DataSet
		} else {
			DataSet[] subSets = splitDataSetUsingEM(dataSet, maxFanout);

			// case 2: if the data set could not be split, we insert all objects into a new leaf node
			if (subSets.length == 1) {
				// force all objects into #maxFanout entries
				for(DataObject dataObject : dataSet) {
					Entry newEntry = new Entry(dataObject.getNrOfDimensions(),new ClusKernel(dataObject.getFeatures(),dataObject.getFeatures().length), 0);
					if (count < maxFanout) {
						// fill entry using the current 'dataObject'
						entries[count] = new Entry(dataSet.getNrOfDimensions());
						entries[count].add(newEntry);
					} else {
						// rather than [count % maxFanout] merge it to the closest existing entry
						double tmpDist, minDist = Double.MIN_VALUE; 
						int closestEntry = 0;
						for (int i = 0; i < entries.length; i++) {
							tmpDist = entries[i].calcDistance(newEntry);
							if (tmpDist < minDist) {
								minDist = tmpDist;
								closestEntry = i;
							}
						}
						entries[closestEntry].add(newEntry);
					}
					count++;
				}
			} else {
				// create new nodes from subsets recursively
				Node[] newChildNodes = new Node[subSets.length];
				for (int i = 0; i < newChildNodes.length; i++) {
					if (subSets[i].size() > 1)
						newChildNodes[i] = buildTreeRecursively(subSets[i], level + 1);
					else
						newChildNodes[i] = null;
				}

				// add child nodes (i.e. the corresponding entries) to the result node
				for (int i = 0; i < maxFanout; i++) {
					if (i < newChildNodes.length) {
						if (newChildNodes[i] == null) {
							// subSets[i].size() is either 1 or 0, i.e.
							if (subSets[i].size() == 0) {
								entries[i] = new Entry(dataSet.getNrOfDimensions());
							} else {
								// fill entry using the one dataObject subSets[i].getObject(0)
								entries[i] = new Entry(dataSet.getNrOfDimensions());
								entries[i].add(new Entry(subSets[i].getObject(0).getNrOfDimensions(),new ClusKernel(subSets[i].getObject(0).getFeatures(),subSets[i].getObject(0).getFeatures().length), 0));
							}
						} else {
							// fill entry using newChildNodes[i]
							entries[i] = new Entry(dataSet.getNrOfDimensions());
							entries[i].setChild(newChildNodes[i]);
							for (Entry e : newChildNodes[i].getEntries()){
								entries[i].add(e);
							}
						}
					} else {
						entries[i] = new Entry(dataSet.getNrOfDimensions());
					}
					count++;
				}
			}
		}

		// create remaining entries as empty entries
		for (int i = count; i < maxFanout; i++) {
			entries[i] = new Entry(dataSet.getNrOfDimensions());
		}
		// and add them all to the resulting node
		resultNode = new Node(dataSet.getNrOfDimensions(), level);
		for (Entry e : entries){
			resultNode.addEntry(e,0);
		}

		return resultNode;
	}

	/**
	 * This methods splits the given data set into partitions using the EM algorithm.
	 * The resulting number of partitions is <= nrOfPartitions.
	 * If EM returns only one partition, the data set is split using mean shift.
	 * The size of each resulting partition is AT LEAST 2, 
	 * i.e. partitions with size 1 are merged with the closest remaining partition.
	 * Hence, this method might return only one partition containing the whole data set. 
	 *  
	 * @param dataSet
	 * @param nrOfPartitions
	 * @return an array of DataSets containing the partitions (minimal 1, maximal nrOfPartitions)
	 * @throws Exception
	 */
	private DataSet[] splitDataSetUsingEM(DataSet dataSet, int nrOfPartitions) throws Exception {
		if (dataSet.size() <= 1) throw new Exception("EMsplit needs at least 2 objects!");
		EMProjectedClustering myEM = new EMProjectedClustering();
		// iterate several times and take best solution
		int nrOfIterations = 1;
		// maximum      --> 10 iterations
		// 10^2 objects --> 8 iterations
		// 10^6 objects --> 4 iteration
		// minimum      --> 2 iterations
		//
		// #iterations = max{1, (10 - log_10(#objects)) }
		double log10 = Math.log(dataSet.size()*1.0)/Math.log(10.0);		
		nrOfIterations = Math.max(1, (10 - ((Long) Math.round(log10)).intValue()));
		nrOfIterations = Math.min(10, nrOfIterations);
		int[][] emMapping = myEM.getEMClusteringVariancesBestChoice(dataSet.getFeaturesAsArray(), nrOfPartitions, nrOfIterations);

		DataSet[] subDataSets = new DataSet[emMapping.length];
		for (int i = 0; i < subDataSets.length; i++) {
			subDataSets[i] = new DataSet(dataSet.getNrOfDimensions());
			for (int j = 0; j < emMapping[i].length; j++) {
				subDataSets[i].addObject(dataSet.getObject(emMapping[i][j]));
			}
		}

		////////////////////////////////////////////////////////////////
		// the EM part ends here
		// now we try to create at least 2 partitions
		// and make sure that each partition contains at least 2 objects
		////////////////////////////////////////////////////////////////
		if (subDataSets.length < 2) {
			System.out.println("mean shift split");
			subDataSets = splitDataSetUsingMeanShift(dataSet);
		}

		// decide what to do with kernels in inner nodes
		// by default they are allowed, i.e. no changes are made if the case occurs
		boolean changes = !ALLOW_KERNELS_IN_INNER_NODES;
		while (changes) {
			changes = false;
			for (int i = 0; i < subDataSets.length; i++) {
				if (subDataSets[i].size() == 1) {
					System.out.println("merge singular sets");
					subDataSets = mergeDataSets(subDataSets, i);
					changes = true;
					break;
				}
			}
		}

		return subDataSets;
	}

	private DataSet[] splitDataSetUsingMeanShift(DataSet dataSet) {
		// calculate cluster features
		DataObject[] dataObjects = dataSet.getDataObjectArray();
		int N = dataSet.size();
		int dim = dataObjects[0].getFeatures().length;
		double[] tempFeatures;
		double[] LS = new double[dim];
		double[] SS = new double[dim];
		for (int i = 0; i < dataObjects.length; i++) {
			tempFeatures = dataObjects[i].getFeatures();
			for (int j = 0; j < dim; j++) {
				LS[j] += tempFeatures[j];
				SS[j] += (tempFeatures[j] * tempFeatures[j]);
			}
		}
		// calculate mean and variance per dimension
		double[] sigmaSquared = new double[dim];
		double[] mean = new double[dim];
		for (int i = 0; i < dim; i++) {
			mean[i] = LS[i] / N;
			sigmaSquared[i] = (SS[i] / N) - ((LS[i] * LS [i]) / (N * N));
			if (sigmaSquared[i] <= 0.0) {
				sigmaSquared[i] = 0.1;
			}
		}
		// calculate shifted means
		double[] mean1 = new double[dim];
		double[] mean2 = new double[dim];
		for (int i = 0; i < dim; i++) {
			mean1[i] = mean[i] + Math.sqrt(sigmaSquared[i]);
			mean2[i] = mean[i] - Math.sqrt(sigmaSquared[i]);
		}		
		// assign objects to closest mean in two data set
		DataSet[] subDataSets = new DataSet[2];
		try {
			subDataSets[0] = new DataSet(dataSet.getNrOfDimensions());
			subDataSets[1] = new DataSet(dataSet.getNrOfDimensions());
			double dist1, dist2;
			for (int i = 0; i < dataObjects.length; i++) {
				dist1 = euclideanDistance(dataObjects[i].getFeatures(), mean1);
				dist2 = euclideanDistance(dataObjects[i].getFeatures(), mean2);
				if (dist1 < dist2)
					subDataSets[0].addObject(dataObjects[i]);
				else
					subDataSets[1].addObject(dataObjects[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// if one data set is empty return only one
		if(subDataSets[0].size() == 0) {
			DataSet tmpDS = subDataSets[1];
			subDataSets = new DataSet[1];
			subDataSets[0] = tmpDS;
		} else if (subDataSets[1].size() == 0) {
			DataSet tmpDS = subDataSets[0];
			subDataSets = new DataSet[1];
			subDataSets[0] = tmpDS;
		}

		return subDataSets;
	}

	private DataSet[] mergeDataSets(DataSet[] subDataSets, int setToMerge) throws Exception {
		DataSet[] result = new DataSet[subDataSets.length-1];

		DataSet tmpDataSet;
		int nrOfDim = subDataSets[0].getNrOfDimensions();
		double[] tmpFeatures, tmpMean = new double[nrOfDim];
		DataObject tmpObject = subDataSets[setToMerge].getObject(0);
		int closestSet = 0;
		double tmpDist, shortestDist = Double.MAX_VALUE;
		int newIndex = 0;

		for (int i = 0; i < subDataSets.length; i++) {
			if (i != setToMerge) {
				tmpDataSet = subDataSets[i];
				result[newIndex] = tmpDataSet;
				// calculate mean of current sub data set
				for (int j = 0; j < tmpDataSet.size(); j++) {
					tmpFeatures = tmpDataSet.getObject(j).getFeatures();
					for (int d = 0; d < nrOfDim; d++) {
						tmpMean[d] += tmpFeatures[d];
					}
				}
				for (int d = 0; d < nrOfDim; d++) {
					tmpMean[d] /= tmpDataSet.size();
				}
				// compare distance
				tmpDist = euclideanDistance(tmpMean, tmpObject.getFeatures());
				if (tmpDist < shortestDist) {
					shortestDist = tmpDist;
					closestSet = newIndex;
				}
				newIndex++;
			}
		}

		// add object to closest set and return
		result[closestSet].addObject(tmpObject);
		return result;
	}

	private double euclideanDistance(double[] x, double[] y) {
		double result = 0.0;
		// d(x, y) = sqrt( \sum_{i=1 to d} ( (x_i - y_i)^2 ) )
		for (int i = 0; i < x.length; i++) {
			result += (x[i] - y[i]) * (x[i] - y[i]);
		}
		return Math.sqrt(result);
	}
}
