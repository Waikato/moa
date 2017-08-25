/*
 * 
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
 * The Class DenseInstanceData.
 */
public class DenseInstanceData implements InstanceData{

    /**
     * Instantiates a new dense instance data.
     *
     * @param array the array
     */
    public DenseInstanceData(double[] array) {
       this.attributeValues = array;
    }
    
    /**
     * Instantiates a new dense instance data.
     *
     * @param length the length
     */
    public DenseInstanceData(int length) {
       this.attributeValues = new double[length];
    }
    
    /**
     * Instantiates a new dense instance data.
     */
    public DenseInstanceData() {
       this(0);
    }
    
    /** The attribute values. */
    protected double[] attributeValues;

    /**
     * Num attributes.
     *
     * @return the int
     */
    @Override
    public int numAttributes() {
        return this.attributeValues.length;
    }

    /**
     * Value.
     *
     * @param indexAttribute the index attribute
     * @return the double
     */
    @Override
    public double value(int indexAttribute) {
        return this.attributeValues[indexAttribute];
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
        return numAttributes();
    }

    /**
     * Index.
     *
     * @param indexAttribute the index attribute
     * @return the int
     */
    @Override
    public int index(int indexAttribute) {
        return indexAttribute;
    }

    /**
     * Value sparse.
     *
     * @param indexAttribute the index attribute
     * @return the double
     */
    @Override
    public double valueSparse(int indexAttribute) {
        return value(indexAttribute);
    }

    /**
     * Checks if is missing sparse.
     *
     * @param indexAttribute the index attribute
     * @return true, if is missing sparse
     */
    @Override
    public boolean isMissingSparse(int indexAttribute) {
        return isMissing(indexAttribute);
    }

    /**
     * To double array.
     *
     * @return the double[]
     */
    @Override
    public double[] toDoubleArray() {
        return attributeValues.clone();
    }

    /**
     * Sets the value.
     *
     * @param attributeIndex the attribute index
     * @param d the d
     */
    @Override
    public void setValue(int attributeIndex, double d) {
        this.attributeValues[attributeIndex] = d;
    }

    @Override
    public void deleteAttributeAt(int index) {
        
        double[] newValues = new double[attributeValues.length - 1];

        System.arraycopy(attributeValues, 0, newValues, 0, index);
        if (index < attributeValues.length - 1) {
          System.arraycopy(attributeValues, index + 1, newValues, index,
              attributeValues.length - (index + 1));
        }
        attributeValues = newValues;
    }

    @Override
    public void insertAttributeAt(int index) {
        if ((index< 0) || (index > numAttributes())) {
                throw new IllegalArgumentException("Can't insert attribute: index out "
                        + "of range");
            }
        double[] newValues = new double[attributeValues.length + 1];

        System.arraycopy(attributeValues , 0, newValues, 0, index);
        newValues[index] = Double.NaN; //Missing Value
        System.arraycopy(attributeValues , index, newValues, index + 1,
                attributeValues .length - index);
        attributeValues  = newValues;

    }

    @Override
    public InstanceData copy() {
        return new DenseInstanceData(this.attributeValues.clone());
    }
       
}
