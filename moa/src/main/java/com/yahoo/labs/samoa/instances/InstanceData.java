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

import java.io.Serializable;

/**
 * The Interface InstanceData.
 *
 * @author abifet
 */
public interface InstanceData extends Serializable {

    /**
     * Num attributes.
     *
     * @return the int
     */
    public int numAttributes();

    /**
     * Value.
     *
     * @param instAttIndex the inst att index
     * @return the double
     */
    public double value(int instAttIndex);

    /**
     * Checks if is missing.
     *
     * @param instAttIndex the inst att index
     * @return true, if is missing
     */
    public boolean isMissing(int instAttIndex);

    /**
     * Num values.
     *
     * @return the int
     */
    public int numValues();

    /**
     * Index.
     *
     * @param i the i
     * @return the int
     */
    public int index(int i);

    /**
     * Value sparse.
     *
     * @param i the i
     * @return the double
     */
    public double valueSparse(int i);

    /**
     * Checks if is missing sparse.
     *
     * @param p1 the p1
     * @return true, if is missing sparse
     */
    public boolean isMissingSparse(int p1);

    /**
     * To double array.
     *
     * @return the double[]
     */
    public double[] toDoubleArray();

    /**
     * Sets the value.
     *
     * @param m_numAttributes the m_num attributes
     * @param d the d
     */
    public void setValue(int m_numAttributes, double d);

    
     /**
     * Deletes an attribute.
     *
     * @param index the indes
     */
    public void deleteAttributeAt(int index);

    /**
     * Inserts an attribute.
     *
     * @param index the indes
     */
    public void insertAttributeAt(int index);
    
   /**
   * Produces a shallow copy of this instance data. 
   * 
   * @return the shallow copy
   */
    public InstanceData copy();

}
