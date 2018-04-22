/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */
package com.yahoo.labs.samoa.instances;

/**
 * The Class SparseInstanceData.
 *
 * @author abifet
 */
public class SparseInstanceData implements InstanceData {

    /**
     * Instantiates a new sparse instance data.
     *
     * @param attributeValues the attribute values
     * @param indexValues the index values
     * @param numberAttributes the number attributes
     */
    public SparseInstanceData(double[] attributeValues, int[] indexValues, int numberAttributes) {
        this.attributeValues = attributeValues;
        this.indexValues = indexValues;
        this.numberAttributes = numberAttributes;
    }

    /**
     * Instantiates a new sparse instance data.
     *
     * @param length the length
     */
    public SparseInstanceData(int length) {
        this.attributeValues = new double[length];
        this.indexValues = new int[length];
    }

    /**
     * The attribute values.
     */
    protected double[] attributeValues;

    /**
     * Gets the attribute values.
     *
     * @return the attribute values
     */
    public double[] getAttributeValues() {
        return attributeValues;
    }

    /**
     * Sets the attribute values.
     *
     * @param attributeValues the new attribute values
     */
    public void setAttributeValues(double[] attributeValues) {
        this.attributeValues = attributeValues;
    }

    /**
     * Gets the index values.
     *
     * @return the index values
     */
    public int[] getIndexValues() {
        return indexValues;
    }

    /**
     * Sets the index values.
     *
     * @param indexValues the new index values
     */
    public void setIndexValues(int[] indexValues) {
        this.indexValues = indexValues;
    }

    /**
     * Gets the number attributes.
     *
     * @return the number attributes
     */
    public int getNumberAttributes() {
        return numberAttributes;
    }

    /**
     * Sets the number of attributes.
     *
     * @param numberAttributes the new number attributes
     */
    public void setNumberAttributes(int numberAttributes) {
        this.numberAttributes = numberAttributes;
    }

    /**
     * The index values.
     */
    protected int[] indexValues;

    /**
     * The number of attributes.
     */
    protected int numberAttributes;

    /**
     * Gets the number of attributes.
     *
     * @return the int
     */
    @Override
    public int numAttributes() {
        return this.numberAttributes;
    }

    /**
     * Value.
     *
     * @param indexAttribute the index attribute
     * @return the double
     */
    @Override
    public double value(int indexAttribute) {
        int location = locateIndex(indexAttribute);
        //return location == -1 ? 0 : this.attributeValues[location];
        //      int index = locateIndex(attIndex);
        if ((location >= 0) && (indexValues[location] == indexAttribute)) {
            return attributeValues[location];
        } else {
            return 0.0;
        }
    }

    /**
     * Checks if is missing.
     *
     * @param indexAttribute the index attribute
     * @return true, if is missing
     */
    @Override
    public boolean isMissing(int indexAttribute) {
        return Double.isNaN(this.value(indexAttribute));
    }

    /**
     * Num values.
     *
     * @return the int
     */
    @Override
    public int numValues() {
        return this.attributeValues.length;
    }

    /**
     * Index.
     *
     * @param indexAttribute the index attribute
     * @return the int
     */
    @Override
    public int index(int indexAttribute) {
        return this.indexValues[indexAttribute];
    }

    /**
     * Value sparse.
     *
     * @param indexAttribute the index attribute
     * @return the double
     */
    @Override
    public double valueSparse(int indexAttribute) {
        return this.attributeValues[indexAttribute];
    }

    /**
     * Checks if is missing sparse.
     *
     * @param indexAttribute the index attribute
     * @return true, if is missing sparse
     */
    @Override
    public boolean isMissingSparse(int indexAttribute) {
        return Double.isNaN(this.valueSparse(indexAttribute));
    }

    /**
     * To double array.
     *
     * @return the double[]
     */
    @Override
    public double[] toDoubleArray() {
        double[] array = new double[numAttributes()];
        for (int i = 0; i < numValues(); i++) {
            array[index(i)] = valueSparse(i);
        }
        return array;
    }

    /**
     * Sets the value.
     *
     * @param attributeIndex the attribute index
     * @param d the d
     */
    @Override
    public void setValue(int attributeIndex, double d) {
        int index = locateIndex(attributeIndex);
        if (index(index) == attributeIndex) {
            this.attributeValues[index] = d;
        } else {
            // We need to add the value
        }
    }

    /**
     * Locates the greatest index that is not greater than the given index.
     *
     * @return the internal index of the attribute index. Returns -1 if no index
     * with this property could be found
     */
    public int locateIndex(int index) {

        int min = 0;
        int max = this.indexValues.length - 1;

        if (max == -1) {
            return -1;
        }

        // Binary search
        while ((this.indexValues[min] <= index) && (this.indexValues[max] >= index)) {
            int current = (max + min) / 2;
            if (this.indexValues[current] > index) {
                max = current - 1;
            } else if (this.indexValues[current] < index) {
                min = current + 1;
            } else {
                return current;
            }
        }
        if (this.indexValues[max] < index) {
            return max;
        } else {
            return min - 1;
        }
    }

   /**
   * Deletes an attribute at the given position (0 to numAttributes() - 1).
   * 
   * @param position the attribute's position
   */
  @Override
  public void deleteAttributeAt(int position) {

    int index = locateIndex(position);

    this.numberAttributes--;
    if ((index >= 0) && (indexValues[index] == position)) {
      int[] tempIndices = new int[indexValues.length - 1];
      double[] tempValues = new double[attributeValues.length - 1];
      System.arraycopy(indexValues, 0, tempIndices, 0, index);
      System.arraycopy(attributeValues, 0, tempValues, 0, index);
      for (int i = index; i < indexValues.length - 1; i++) {
        tempIndices[i] = indexValues[i + 1] - 1;
        tempValues[i] = attributeValues[i + 1];
      }
      indexValues = tempIndices;
      attributeValues = tempValues;
    } else {
      int[] tempIndices = new int[indexValues.length];
      double[] tempValues = new double[attributeValues.length];
      System.arraycopy(indexValues, 0, tempIndices, 0, index + 1);
      System.arraycopy(attributeValues, 0, tempValues, 0, index + 1);
      for (int i = index + 1; i < indexValues.length; i++) {
        tempIndices[i] = indexValues[i] - 1;
        tempValues[i] = attributeValues[i];
      }
      indexValues = tempIndices;
      attributeValues = tempValues;
    }
  }

    @Override
    public void insertAttributeAt(int position) {
        if ((position< 0) || (position > numAttributes())) {
            throw new IllegalArgumentException("Can't insert attribute: index out "
                    + "of range");
        }
        int index = locateIndex(position);

        this.numberAttributes++;
        if ((index >= 0) && (indexValues[index] == position)) {
            int[] tempIndices = new int[indexValues.length + 1];
            double[] tempValues = new double[attributeValues.length + 1];
            System.arraycopy(indexValues, 0, tempIndices, 0, index);
            System.arraycopy(attributeValues, 0, tempValues, 0, index);
            tempIndices[index] = position;
            tempValues[index] =  Double.NaN; //Missing Value
            for (int i = index; i < indexValues.length; i++) {
                tempIndices[i + 1] = indexValues[i] + 1;
                tempValues[i + 1] = attributeValues[i];
            }
            indexValues = tempIndices;
            attributeValues = tempValues;
        } else {
            int[] tempIndices = new int[indexValues.length + 1];
            double[] tempValues = new double[attributeValues.length + 1];
            System.arraycopy(indexValues, 0, tempIndices, 0, index + 1);
            System.arraycopy(attributeValues, 0, tempValues, 0, index + 1);
            tempIndices[index + 1] = position;
            tempValues[index + 1] =  Double.NaN; //Missing Value
            for (int i = index + 1; i < indexValues.length; i++) {
                tempIndices[i + 1] = indexValues[i] + 1;
                tempValues[i + 1] = attributeValues[i];
            }
            indexValues = tempIndices;
            attributeValues = tempValues;
        }
    }

    @Override
    public InstanceData copy() {
      return new SparseInstanceData(this.attributeValues.clone(),this.indexValues.clone(),this.numberAttributes);   
    }


}
