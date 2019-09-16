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

	int STRUCTURE_TYPE_SINGLE_TARGET = 0;
	int STRUCTURE_TYPE_MULTI_TARGET = 1;
	int STRUCTURE_TYPE_TIME_SERIES = 2;
	int STRUCTURE_TYPE_HIERARHICAL = 3;

	/**
	 * Gets the weight of the instance.
	 *
	 * @return the weight
	 */
	double weight();

	/**
	 * Sets the weight.
	 *
	 * @param weight the new weight
	 */
	void setWeight(double weight);

	/**
	 * Attribute.
	 *
	 * @param instAttIndex the inst att index
	 * @return the attribute
	 */
	Attribute attribute(int instAttIndex);

	int indexOf(Attribute attribute);

	/**
	 * Index of an Attribute.
	 *
	 * @param Attribute, the attribute to be found.
	 * @return the index of an attribute
	 */
//    public int indexOfAttribute(Attribute attribute);

	/**
	 * Delete attribute at.
	 *
	 * @param i the index
	 */
	void deleteAttributeAt(int i);

	/**
	 * Insert attribute at.
	 *
	 * @param i the index
	 */
	void insertAttributeAt(int i);

	/**
	 * Gets the number of attributes.
	 *
	 * @return the number of attributes
	 */
	int numAttributes();

	/**
	 * Adds the sparse values.
	 *
	 * @param indexValues      the index values
	 * @param attributeValues  the attribute values
	 * @param numberAttributes the number attributes
	 */
	void addSparseValues(int[] indexValues, double[] attributeValues, int numberAttributes);

	/**
	 * Gets the number of values, mainly for sparse instances.
	 *
	 * @return the number of values
	 */
	int numValues();

	/**
	 * Gets the value of a discrete attribute as a string.
	 *
	 * @param i the i
	 * @return the string
	 */
	String stringValue(int i);

	/**
	 * Gets the value of an attribute.
	 *
	 * @param instAttIndex the inst att index
	 * @return the double
	 */
	double value(int instAttIndex);

	/**
	 * Gets the value of an attribute, given the attribute.
	 *
	 * @param attribute the attribute
	 * @return the double
	 */
	double value(Attribute attribute);

	/**
	 * Sets the value of an attribute.
	 *
	 * @param instAttIndex the index
	 * @param value        the value
	 */
	void setValue(int instAttIndex, double value);

	/**
	 * Sets the value of an attribute.
	 *
	 * @param attribute, the Attribute
	 * @param value      the value
	 */
	void setValue(Attribute attribute, double value);

	/**
	 * Checks if an attribute is missing.
	 *
	 * @param instAttIndex the inst att index
	 * @return true, if is missing
	 */
	boolean isMissing(int instAttIndex);

	/**
	 * Checks if an attribute is missing.
	 *
	 * @param attribute, the Attribute
	 * @return true, if is missing
	 */
	boolean isMissing(Attribute attribute);

	/**
	 * Checks if an attribute is missing.
	 *
	 * @param index the instance input attribute index
	 * @return true, if is missing
	 */
	boolean isInputMissing(int index);

	/**
	 * Are any of the output values missing.
	 *
	 * @return true, if any output has missing value
	 */
	boolean missingOutputs();

	/**
	 * Sets an attribute as missing
	 *
	 * @param instAttIndex, the attribute's index
	 */
	void setMissing(int instAttIndex);

	/**
	 * Sets an input attribute as missing
	 *
	 * @param inputAttributeIndex the index of the input attribute
	 */
	void setInputMissing(int inputAttributeIndex);

	/**
	 * Sets an output attribute as missing
	 *
	 * @param outputAttributeIndex the index of the output attribute
	 */
	void setOutputMissing(int outputAttributeIndex);

	/**
	 * Sets an attribute as missing
	 *
	 * @param attribute, the Attribute
	 */
	void setMissing(Attribute attribute);

	/**
	 * Gets the index of the attribute given the index of the array in a sparse
	 * representation.
	 *
	 * @param arrayIndex the index of the array
	 * @return the index
	 */
	int index(int arrayIndex);

	/**
	 * Gets the value of an attribute in a sparse representation of the instance.
	 *
	 * @param i the i
	 * @return the value
	 */
	double valueSparse(int i);

	/**
	 * Checks if the attribute is missing sparse.
	 *
	 * @param p1 the p1
	 * @return true, if is missing sparse
	 */
	boolean isMissingSparse(int p1);

	/**
	 * To double array.
	 *
	 * @return the double[]
	 */
	double[] toDoubleArray();

	/**
	 * Class attribute.
	 *
	 * @return the attribute
	 */
	Attribute classAttribute();

	/**
	 * Class index.
	 *
	 * @return the int
	 */
	int classIndex();

	/**
	 * Class is missing.
	 *
	 * @return true, if successful
	 */
	boolean classIsMissing();

	/**
	 * Class value.
	 *
	 * @return the double
	 */
	double classValue();

	/**
	 * Num classes.
	 *
	 * @return the int
	 */
	int numClasses();

	/**
	 * Sets the class value.
	 *
	 * @param d the new class value
	 */
	void setClassValue(double d);

	/**
	 * Copy.
	 *
	 * @return the instance
	 */
	Instance copy();

	/**
	 * Sets the dataset.
	 *
	 * @param dataset the new dataset
	 */
	void setDataset(InstancesHeader dataset);

	/**
	 * Dataset.
	 *
	 * @return the instances
	 */
	InstancesHeader dataset();

	/**
	 * Gets the number of input attributes.
	 *
	 * @return the number of input attributes
	 */
	int numInputAttributes();

	/**
	 * Gets the number of output attributes.
	 *
	 * @return the number of output attributes
	 */
	int numOutputAttributes();

	/**
	 * Gets an input attribute given its index.
	 *
	 * @param attributeIndex the index
	 * @return the attribute
	 */
	Attribute inputAttribute(int attributeIndex);

	/**
	 * Gets the value of an input attribute.
	 *
	 * @param attributeIndex the index
	 * @return the value
	 */
	double valueInputAttribute(int attributeIndex);

	/**
	 * Checks if an output attribute is missing.
	 *
	 * @param index the instance output attribute index
	 * @return true, if is missing
	 */
	boolean isOutputMissing(int index);

	/**
	 * Gets an output attribute given its index.
	 *
	 * @param attributeIndex the index
	 * @return the attribute
	 */
	Attribute outputAttribute(int attributeIndex);

	/**
	 * Gets the value of an output attribute.
	 *
	 * @param attributeIndex the index
	 * @return the value
	 */
	double classValue(int attributeIndex);

	/**
	 * Sets the value of an output attribute.
	 *
	 * @param indexClass     the output attribute index
	 * @param valueAttribute the value of the attribute
	 */
	void setClassValue(int indexClass, double valueAttribute);

	/**
	 * Gets the value of an output attribute.
	 *
	 * @param attributeIndex the index
	 * @return the value
	 */
	double valueOutputAttribute(int attributeIndex);

	int structureType();

	AttributeStructure getStructure();

	String outputAttributesToString();
}
