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
public interface Instance extends Serializable {

    /**
     * Gets the weight of the instance.
     *
     * @return the weight
     */
    public double weight();

    /**
     * Sets the weight.
     *
     * @param weight the new weight
     */
    public void setWeight(double weight);

    /**
     * Attribute.
     *
     * @param instAttIndex the inst att index
     * @return the attribute
     */
    public Attribute attribute(int instAttIndex);

    /**
     * Index of an Attribute.
     *
     * @param Attribute, the attribute to be found.
     * @return the index of an attribute
     */
    public int indexOfAttribute(Attribute attribute);
    
    /**
     * Delete attribute at.
     *
     * @param i the index
     */
    public void deleteAttributeAt(int i);

    /**
     * Insert attribute at.
     *
     * @param i the index
     */
    public void insertAttributeAt(int i);

    /**
     * Gets the number of attributes.
     *
     * @return the number of attributes
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

    /**
     * Gets the number of values, mainly for sparse instances.
     *
     * @return the number of values
     */
    public int numValues();

    /**
     * Gets the value of a discrete attribute as a string.
     *
     * @param i the i
     * @return the string
     */
    public String stringValue(int i);

    /**
     * Gets the value of an attribute.
     *
     * @param instAttIndex the inst att index
     * @return the double
     */
    public double value(int instAttIndex);

    /**
     * Gets the value of an attribute, given the attribute.
     *
     * @param attribute the attribute
     * @return the double
     */
    public double value(Attribute attribute);

    /**
     * Sets an attribute as missing
     *
     * @param instAttIndex, the attribute's index     
     */
    public void setMissing(int instAttIndex);        
    
    
    /**
     * Sets an attribute as missing
     *
     * @param attribute, the Attribute
     */
    public void setMissing(Attribute attribute);
    
    /**
     * Sets the value of an attribute.
     *
     * @param instAttIndex the index
     * @param value the value
     */
    public void setValue(int instAttIndex, double value);

    
    /**
     * Sets the value of an attribute.
     *
     * @param attribute, the Attribute
     * @param value the value
     */
    public void setValue(Attribute attribute, double value);
    
    /**
     * Checks if an attribute is missing.
     *
     * @param instAttIndex the inst att index
     * @return true, if is missing
     */
    public boolean isMissing(int instAttIndex);

    /**
     * Checks if an attribute is missing.
     *
     * @param attribute, the Attribute
     * @return true, if is missing
     */
    public boolean isMissing(Attribute attribute);
    
    /**
     * Gets the index of the attribute given the index of the array in a sparse
     * representation.
     *
     * @param arrayIndex the index of the array
     * @return the index
     */
    public int index(int arrayIndex);

    /**
     * Gets the value of an attribute in a sparse representation of the
     * instance.
     *
     * @param i the i
     * @return the value
     */
    public double valueSparse(int i);

    /**
     * Checks if the attribute is missing sparse.
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

    /**
     * Gets the number of input attributes.
     *
     * @return the number of input attributes
     */
    public int numInputAttributes();

    /**
     * Gets the number of output attributes.
     *
     * @return the number of output attributes
     */
    public int numOutputAttributes();

    /**
     * Gets the number of output attributes.
     *
     * @return the number of output attributes
     */
    public int numberOutputTargets();

    /**
     * Gets the value of an output attribute.
     *
     * @param attributeIndex the index
     * @return the value
     */
    public double classValue(int attributeIndex);

    /**
     * Sets the value of an output attribute.
     *
     * @param indexClass the output attribute index
     * @param valueAttribute the value of the attribute
     */
    public void setClassValue(int indexClass, double valueAttribute);

    /**
     * Gets an output attribute given its index.
     *
     * @param attributeIndex the index
     * @return the attribute
     */
    public Attribute outputAttribute(int attributeIndex);

    /**
     * Gets an input attribute given its index.
     *
     * @param attributeIndex the index
     * @return the attribute
     */
    public Attribute inputAttribute(int attributeIndex);

    /**
     * Gets the value of an input attribute.
     *
     * @param attributeIndex the index
     * @return the value
     */
    public double valueInputAttribute(int attributeIndex);

    /**
     * Gets the value of an output attribute.
     *
     * @param attributeIndex the index
     * @return the value
     */
    public double valueOutputAttribute(int attributeIndex);

}
