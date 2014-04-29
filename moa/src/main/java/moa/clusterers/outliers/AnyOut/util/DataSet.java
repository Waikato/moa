/*
 *    DataSet.java
 *
 *    @author F. Sanchez, I. Assent, P. Kranen, C. Baldauf, T. Seidl
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
import java.util.HashMap;
import java.util.Iterator;


/**
 * A set of <code>DataObject</code>s. Internally it uses an <code>ArrayList&lt;DataObject&gt;</code>
 * so it is recommended to use  with care. For speedy operations
 * after the data is collected use {@link #getFeaturesAsArray()}. When access speed is not a factor
 * the iterator can be used to go through the data with a <code>foreach</code> loop.
 * @author Fernando Sanchez Villaamil
 * @see #getFeaturesAsArray()
 */

public class DataSet implements Iterable<DataObject>{

    private int nrOfDimensions;
    private ArrayList<DataObject> dataList;

    /**
     * Creates an empty set.
     * @param nrOfDimensions The dimension the <code>DataObject</code>s should have.
     * @see DataObject
     * @throws praktikum.framework.data.InconsistentDimensionException
     */
    public DataSet(int nrOfDimensions){
        if (nrOfDimensions <= 0) {
            System.err.println("Negative dimension given: " + nrOfDimensions);
        } else {
            this.nrOfDimensions = nrOfDimensions;
            dataList = new ArrayList<DataObject>();
        }
    }

    /**
     * Creates a Set with only the given object. The dimension for the <code>DataSet</code> is
     * given by the given <code>DataObject</code>.
     * @param newData The first <code>DataObject</code> added to the set.
     * @see DataObject
     * @throws praktikum.framework.data.InconsistentDimensionException
     */
    public DataSet(DataObject newData) {
        this.nrOfDimensions = newData.getNrOfDimensions();

        dataList = new ArrayList<DataObject>();
        dataList.add(newData);
    }

    /**
     * Adds a <code>DataObject</code> to the set.
     * @param newData The <code>DataObject</code> to be added.
     * @throws praktikum.framework.data.InconsistentDimensionException This <code>Exception</code> is thrown
     * when the dimension of the object to be added does not fit the rest.
     * @see DataObject
     * @throws praktikum.framework.data.InconsistentDimensionException
     */
    public void addObject(DataObject newData) {

        if (newData.getNrOfDimensions() != nrOfDimensions) {
        	System.err.println("Inconsistent number of dimensions!"); //(this.nrOfDimensions, newData.getNrOfDimensions());
        } else {
        	this.dataList.add(newData);
        }
    }

    /**
     * Adds all objects in the given data set
     * @see addObject(DataObject newData)
     * @param dataSet
     * @throws InconsistentDimensionException
     */
    public void addObject(DataSet dataSet) throws Exception {
    	DataObject[] dataObjects = dataSet.getDataObjectArray();
    	for (int i = 0; i < dataObjects.length; i++) {
			this.addObject(dataObjects[i]);
		}
    }
    
    /**
     * Returns the <code>DataObject</code> at the given position. Use with care.
     * For speedy operations after the data is collected use {@link #getFeaturesAsArray()}.
     * @param index The index position that is to be returned.
     * @return The <code>DataObeject</code> at the given position.
     * @see DataObject
     */
    public DataObject getObject(int index) {
        return dataList.get(index);
    }

    /**
     * Returns the size of the set.
     * @return The size of the set.
     */
    public int size() {
        return this.dataList.size();
    }

    /**
     * Return the dimension of the objects in the <code>DataSet</code>. The <code>DataSet</code>
     * makes sure all objects have the same dimension.
     * @return The dimension of the <code>DataObjects</code> in the <code>DataSet</code>.
     * @see DataObject
     */
    public int getNrOfDimensions() {
        return nrOfDimensions;
    }

    /**
     * Counts the number of classes that are present in the data set.
     * !!! It does not check whether all classes are contained !!!  
     * @return the number of distinct class labels
     */
    public int getNrOfClasses() {
		HashMap<Integer, Integer> classes = new HashMap<Integer, Integer>();

		for (DataObject currentObject : dataList) {
			if (!classes.containsKey(currentObject.getClassLabel()))
				classes.put(currentObject.getClassLabel(), 1);
		}
    	
		return classes.size();
    }
    
    /**
     * Returns an array with all the features of all the objects in the set. Be aware that it does not copy the features
     * so that any changes to the values of the features in this array will result in changes in
     * the initial <code>DataObject</code>s.
     * @return A <code>double[][]</code> with the features. The first dimension is the feature
     * number, the second the values in the feature.
     * @see DataObject
     */
    public double[][] getFeaturesAsArray() {

        double[][] data = new double[this.size()][this.nrOfDimensions];

        int c = 0;
        for (DataObject d : dataList) {
            data[c] = d.getFeatures();
            c++;
        }

        return data;
    }
    
    /**
     * Returns an array of all the <code>DataObject</code>s in the set. Use for speedy access when the labels
     * and/or the ids are also needed.
     * @return An array with all the <code>DataObject</code>s in the set.
     * @see DataObject
     */
    public DataObject[] getDataObjectArray(){
        DataObject[] res = new DataObject[this.dataList.size()];
        return this.dataList.toArray(res);
    }
    
    /**
     * Separates the objects in this data set according to their class label
     * @return an array of DataSets, one for each class
     */
    public DataSet[] getDataSetsPerClass() throws Exception {
    	DataSet[] dataSetsPerClass = new DataSet[this.getNrOfClasses()];
    	// create a new data set for each class
    	for (int i = 0; i < dataSetsPerClass.length; i++) {
			dataSetsPerClass[i] = new DataSet(this.nrOfDimensions);
		}
    	
    	// fill the data sets
    	for(DataObject currentObject : dataList) {
    		dataSetsPerClass[currentObject.getClassLabel()].addObject(currentObject);
    	}
    	
    	return dataSetsPerClass;
    }
    
    /**
     * Calculates the variance of this data set for each dimension
     * @return double array containing the variance per dimension 
     */
    public double[] getVariances() {
		double N = this.size();
    	double[] LS = new double[this.getNrOfDimensions()];
    	double[] SS = new double[this.getNrOfDimensions()];
		double[] tmpFeatures;
		double[] variances = new double[this.getNrOfDimensions()];

		for (DataObject dataObject : dataList) {
			tmpFeatures = dataObject.getFeatures();
			for (int j = 0; j < tmpFeatures.length; j++) {
				LS[j] += tmpFeatures[j];
				SS[j] += tmpFeatures[j] * tmpFeatures[j];
			}
		}
		
		// sigmaSquared[i] = (SS[i] / N) - ((LS[i] * LS [i]) / (N * N));
		for (int i = 0; i < LS.length; i++) {
			variances[i] = (SS[i] / N - ((LS[i] / N)*(LS[i] / N)));
		}
		
		return variances;
    }
    
    /**
     * An iterator for the set. This allows to use a <code>foreach</code> loop over the <code>DataObject</code>s.
     * This is a nice way of going through the data, but when the access speed is relevant its use
     * is not recommended.
     * @return An iterator for the set.
     * @see DataObject
     */
    public Iterator<DataObject> iterator() {
        return this.dataList.iterator();
    }
    
    /**
     * Returns a <code>String</code> representation of all the <code>DataObject</code>s in the code as a list of
     * the representation implemented for these.
     * @return A <code>String</code> representation of all the elements in the set.
     * @see DataObject
     */
    @Override
    public String toString(){
        String res = "";
        
        for (DataObject dataObject : dataList) {
            res += dataObject.toString() + "\n";
        }
        
        return res;
    }

	/**
	 * resets the ids, so that the set contains ids from 0 to noOfObjects-1
	 */
	public void manipulateIds() {
		int id = 0;
		for (DataObject o : dataList){
			o.setId(id);
			id++;
		}
		
	}

	public void clear() {
		dataList.clear();
	}
}





