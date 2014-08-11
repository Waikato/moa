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
 * The Interface Instance.
 *
 * @author abifet
 */

public interface Instance extends Serializable{

    /**
     * Weight.
     *
     * @return the double
     */
    public double weight();
    
    /**
     * Sets the weight.
     *
     * @param weight the new weight
     */
    public void setWeight(double weight);
    
    //Attributes
    /**
     * Attribute.
     *
     * @param instAttIndex the inst att index
     * @return the attribute
     */
    public Attribute attribute(int instAttIndex);
    
    /**
     * Delete attribute at.
     *
     * @param i the i
     */
    public void deleteAttributeAt(int i);
    
    /**
     * Insert attribute at.
     *
     * @param i the i
     */
    public void insertAttributeAt(int i);
    
    /**
     * Num attributes.
     *
     * @return the int
     */
    public int numAttributes();
    
    /**
     * Adds the sparse values.
     *
     * @param indexValues the index values
     * @param attributeValues the attribute values
     * @param numberAttributes the number attributes
     */
    public void addSparseValues(int[] indexValues, double[] attributeValues, int numberAttributes);
    

    //Values
    /**
     * Num values.
     *
     * @return the int
     */
    public int numValues();
    
    /**
     * String value.
     *
     * @param i the i
     * @return the string
     */
    public String stringValue(int i);
    
    /**
     * Value.
     *
     * @param instAttIndex the inst att index
     * @return the double
     */
    public double value(int instAttIndex);
    
    /**
     * Value.
     *
     * @param attribute the attribute
     * @return the double
     */
    public double value(Attribute attribute);
    
    /**
     * Sets the value.
     *
     * @param m_numAttributes the m_num attributes
     * @param d the d
     */
    public void setValue(int m_numAttributes, double d);
    
    /**
     * Checks if is missing.
     *
     * @param instAttIndex the inst att index
     * @return true, if is missing
     */
    public boolean isMissing(int instAttIndex);
    
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
    
    //Class
    /**
     * Class attribute.
     *
     * @return the attribute
     */
    public Attribute classAttribute();
    
    /**
     * Class index.
     *
     * @return the int
     */
    public int classIndex();
    
    /**
     * Class is missing.
     *
     * @return true, if successful
     */
    public boolean classIsMissing();
    
    /**
     * Class value.
     *
     * @return the double
     */
    public double classValue();
    
    /**
     * Num classes.
     *
     * @return the int
     */
    public int numClasses();
    
    /**
     * Sets the class value.
     *
     * @param d the new class value
     */
    public void setClassValue(double d);

    /**
     * Copy.
     *
     * @return the instance
     */
    public Instance copy();

    //Dataset
    /**
     * Sets the dataset.
     *
     * @param dataset the new dataset
     */
    public void setDataset(Instances dataset);
    
    /**
     * Dataset.
     *
     * @return the instances
     */
    public Instances dataset();

           public int numInputAttributes();
    
    public int numOutputAttributes();
    
    public int numberOutputTargets();

	public double classValue(int instAttIndex) ;

	public void setClassValue(int indexClass, double valueAttribute);

	public Attribute outputAttribute(int outputIndex);

	public Attribute inputAttribute(int attributeIndex);

	public double valueInputAttribute(int attributeIndex);

	public double valueOutputAttribute(int attributeIndex);
    
}

