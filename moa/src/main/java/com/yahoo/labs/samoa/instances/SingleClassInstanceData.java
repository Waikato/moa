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
 * The Class SingleClassInstanceData.
 *
 * @author abifet
 */
public class SingleClassInstanceData implements InstanceData {

    /** The class value. */
    protected double classValue;
    
    /**
     * Num attributes.
     *
     * @return the int
     */    
    @Override
    public int numAttributes() {
        return 1;
    }
    
    /**
     * Value.
     *
     * @param instAttIndex the inst att index
     * @return the double
     */
    @Override
    public double value(int instAttIndex) {
        return classValue;
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
        return 1;
    }
    
    /**
     * Index.
     *
     * @param i the i
     * @return the int
     */
    @Override
    public int index(int i) {
        return 0;
    }
    
    /**
     * Value sparse.
     *
     * @param i the i
     * @return the double
     */
    @Override
    public double valueSparse(int i) {
        return value(i);
    }
    
    /**
     * Checks if is missing sparse.
     *
     * @param indexAttribute the index attribute
     * @return true, if is missing sparse
     */
    @Override
    public boolean isMissingSparse(int indexAttribute) {
        return Double.isNaN(this.value(indexAttribute));
    }

    /**
     * To double array.
     *
     * @return the double[]
     */
    @Override
    public double[] toDoubleArray() {
        double[] array = {this.classValue};
        return array;
    }
    
    /**
     * Sets the value.
     *
     * @param m_numAttributes the m_num attributes
     * @param d the d
     */
    @Override
    public void setValue(int m_numAttributes, double d) {
        this.classValue = d;
    }
    
}
