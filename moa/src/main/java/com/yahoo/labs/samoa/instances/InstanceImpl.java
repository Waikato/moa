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

import java.text.SimpleDateFormat;

/**
 * The Class InstanceImpl.
 *
 * @author abifet
 */
public class InstanceImpl implements StructuredInstance {

	private static final long serialVersionUID = 1L;

    /**
     * The weight.
     */
    protected double weight;

    /**
     * The instance data.
     */
    protected InstanceData instanceData;

    /**
     * The instance information.
     */
    protected InstancesHeader instanceHeader;

    /**
     * Instantiates a new instance.
     *
     * @param inst the inst
     */
    public InstanceImpl(InstanceImpl inst) {
        this.weight = inst.weight;
        this.instanceData = inst.instanceData.copy();
        this.instanceHeader = inst.instanceHeader;
    }

    //Dense
    /**
     * Instantiates a new instance.
     *
     * @param weight the weight
     * @param res the res
     */
    public InstanceImpl(double weight, double[] res) {
        this.weight = weight;
        this.instanceData = new DenseInstanceData(res);
    }

    //Sparse
    /**
     * Instantiates a new instance.
     *
     * @param weight the weight
     * @param attributeValues the attribute values
     * @param indexValues the index values
     * @param numberAttributes the number attributes
     */
    public InstanceImpl(double weight, double[] attributeValues, int[] indexValues, int numberAttributes) {
        this.weight = weight;
        this.instanceData = new SparseInstanceData(attributeValues, indexValues, numberAttributes);
    }

    /**
     * Instantiates a new instance.
     *
     * @param weight the weight
     * @param instanceData the instance data
     */
    public InstanceImpl(double weight, InstanceData instanceData) {
        this.weight = weight;
        this.instanceData = instanceData;
    }

    /**
     * Instantiates a new instance.
     *
     * @param numAttributes the num attributes
     */
    public InstanceImpl(int numAttributes) {
        this.instanceData = new DenseInstanceData(new double[numAttributes]); //JD
        this.weight = 1;
    }

    /**
     * Weight.
     *
     * @return the double
     */
    public double weight() {
        return weight;
    }

    /**
     * Sets the weight.
     *
     * @param weight the new weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Attribute.
     *
     * @param index the attribute index
     * @return the attribute
     */
    public Attribute attribute(int index) {
        return this.instanceHeader.attribute(index);
    }

    public int indexOf(Attribute attribute) {
    	return instanceHeader.instanceInformation.attributesInformation.indexOfAttribute(attribute);
    }
    
    /**
     * Delete attribute at.
     *
     * @param i the i
     */
    @Override
    public void deleteAttributeAt(int i) {
        this.instanceData.deleteAttributeAt(i);
    }

    /**
     * Insert attribute at.
     *
     * @param i the i
     */
    @Override
    public void insertAttributeAt(int i) {
    	this.instanceData.insertAttributeAt(i);
    }

    /**
     * Num attributes.
     *
     * @return the int
     */
    public int numAttributes() {
        return this.instanceData.numAttributes();
    }

    /**
     * Value.
     *
     * @param index the attribute index
     * @return the double
     */
    public double value(int index) {
        return this.instanceData.value(index);
    }

    /**
     * Checks if is missing.
     *
     * @param instAttIndex the inst att index
     * @return true, if is missing
     */
    public boolean isMissing(int instAttIndex) {
        return this.instanceData.isMissing(instAttIndex);
    }

    /**
     * Checks if is missing.
     *
     * @param instAttIndex the instance input attribute index
     * @return true, if is missing
     */
    public boolean isInputMissing(int index) {
        return this.instanceData.isMissing(this.instanceHeader.getInputInstanceIndex(index));
    }

    /**
     * Checks if is missing.
     *
     * @param instAttIndex the instance output attribute index
     * @return true, if is missing
     */
    public boolean isOutputMissing(int index) {
        return this.instanceData.isMissing(this.instanceHeader.getOutputInstanceIndex(index));
    }

    /**
     * Num values.
     *
     * @return the int
     */
    public int numValues() {
        return this.instanceData.numValues();
    }

    /**
     * Index.
     *
     * @param i the i
     * @return the int
     */
    public int index(int i) {
        return this.instanceData.index(i);
    }

    /**
     * Value sparse.
     *
     * @param i the i
     * @return the double
     */
    public double valueSparse(int i) {
        return this.instanceData.valueSparse(i);
    }

    /**
     * Checks if is missing sparse.
     *
     * @param p the p
     * @return true, if is missing sparse
     */
    public boolean isMissingSparse(int p) {
        return this.instanceData.isMissingSparse(p);
    }

    /**
     * Value.
     *
     * @param attribute the attribute
     * @return the double
     */
    public double value(Attribute attribute) {
        return value(this.indexOf(attribute));
    }

    /**
     * String value.
     *
     * @param i the i
     * @return the string
     */
    @Override
    public String stringValue(int i) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * To double array.
     *
     * @return the double[]
     */
    @Override
    public double[] toDoubleArray() {
        return this.instanceData.toDoubleArray();
    }

    /**
     * Sets the value.
     *
     * @param numAttribute the num attribute
     * @param d the d
     */
    @Override
    public void setValue(int numAttribute, double d) {
        this.instanceData.setValue(numAttribute, d);
    }

    /**
     * Class value.
     *
     * @return the double
     */
    @Override
    public double classValue() {
        return this.instanceData.value(classIndex());
    }

    /**
     * Class index.
     *
     * @return the int
     */
    @Override
    public int classIndex() {
        return this.instanceHeader.classIndex();

    }

    /**
     * Num classes.
     *
     * @return the int
     */
    @Override
    public int numClasses() {
        return this.instanceHeader.numClasses();
    }

    /**
     * Class is missing.
     *
     * @return true, if successful
     */
    @Override
    public boolean classIsMissing() {
        return this.instanceData.isMissing(classIndex());
    }
    
    public boolean missingOutputs() {
    	if (this.instanceHeader.numOutputAttributes() == 1)
    		return classIsMissing();
    	else {
    		for (int i = 0; i < this.instanceHeader.numOutputAttributes(); i++) {
    			if (this.isOutputMissing(i)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }

    /**
     * Class attribute.
     *
     * @return the attribute
     */
    @Override
    public Attribute classAttribute() {
        return this.instanceHeader.attribute(classIndex());
    }

    /**
     * Sets the class value.
     *
     * @param d the new class value
     */
    @Override
    public void setClassValue(double d) {
        this.setValue(classIndex(), d);
    }

    /**
     * Copy.
     *
     * @return the instance
     */
    @Override
    public Instance copy() {
        InstanceImpl inst = new InstanceImpl(this);
        return inst;
    }

    /**
     * Dataset.
     *
     * @return the instances
     */
    @Override
    public InstancesHeader dataset() {
        return this.instanceHeader;
    }

    /**
     * Sets the dataset.
     *
     * @param dataset the new dataset
     */
    @Override
    public void setDataset(InstancesHeader dataset) {
   		this.instanceHeader = dataset;
    }

    /**
     * Adds the sparse values.
     *
     * @param indexValues the index values
     * @param attributeValues the attribute values
     * @param numberAttributes the number attributes
     */
    @Override
    public void addSparseValues(int[] indexValues, double[] attributeValues, int numberAttributes) {
        this.instanceData = new SparseInstanceData(attributeValues, indexValues, numberAttributes); //???
    }

    /**
     * Text representation of a InstanceImpl.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int index = 0; index < this.numInputAttributes() + this.numOutputAttributes(); index++) {
            if (!this.isMissing(index)) {
                if (this.attribute(index).isNominal()) {
                    int valueIndex = (int) this.value(index);
                    String stringValue = this.attribute(index).value(valueIndex);
                    str.append(stringValue).append(",");
                } else if (this.attribute(index).isNumeric()) {
                    str.append(this.value(index)).append(",");
                } else if (this.attribute(index).isDate()) {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    str.append(dateFormatter.format(this.value(index))).append(",");
                }
            } else {
                str.append("?,");
            }
        }
        return str.toString();
    }

    @Override
    public int numInputAttributes() {
        return this.instanceHeader.numInputAttributes();
    }

    @Override
    public int numOutputAttributes() {
        return this.instanceHeader.numOutputAttributes();
    }

    @Override
    public double classValue(int instAttIndex) {
        return valueOutputAttribute(instAttIndex);
    }

    @Override
    public void setClassValue(int indexClass, double valueAttribute) {
        InstanceInformation instanceInformation = this.instanceHeader.getInstanceInformation();
        this.instanceData.setValue(instanceInformation.outputAttributeIndex(indexClass), valueAttribute);

    }

    @Override
    public Attribute outputAttribute(int outputIndex) {
        InstanceInformation instanceInformation = this.instanceHeader.getInstanceInformation();
        return instanceInformation.outputAttribute(outputIndex);
    }

    @Override
    public Attribute inputAttribute(int attributeIndex) {
        InstanceInformation instanceInformation = this.instanceHeader.getInstanceInformation();
        return instanceInformation.inputAttribute(attributeIndex);
    }

    @Override
    public double valueInputAttribute(int attributeIndex) {
        InstanceInformation instanceInformation = this.instanceHeader.getInstanceInformation();
        return this.instanceData.value(instanceInformation.inputAttributeIndex(attributeIndex));
    }

    @Override
    public double valueOutputAttribute(int attributeIndex) {
        InstanceInformation instanceInformation = this.instanceHeader.getInstanceInformation();
        return this.instanceData.value(instanceInformation.outputAttributeIndex(attributeIndex));
    }

    @Override
    public void setMissing(int instAttIndex) {
        this.setValue(instAttIndex, Double.NaN);
    }

    @Override
    public void setMissing(Attribute attribute) {
        int index = this.indexOf(attribute);
        this.setMissing(index);
    }

    @Override
    public boolean isMissing(Attribute attribute) {
        int index = this.indexOf(attribute);
        return this.isMissing(index);
    }

    @Override
    public void setValue(Attribute attribute, double value) {
        int index = this.indexOf(attribute);
        this.setValue(index, value);
    }
    
    public int structureType() {
    	return (numOutputAttributes() > 1) ? Instance.STRUCTURE_TYPE_MULTI_TARGET : Instance.STRUCTURE_TYPE_SINGLE_TARGET;
    }
    
    public AttributeStructure getStructure() {
    	return null;
    }

	@Override
	public String outputAttributesToString() {
		if (numOutputAttributes() == 1)
			return String.valueOf(this.classValue());
		else {
			String ret = "{";
			for (int i = 0; i < numOutputAttributes(); i++) {
				ret += String.valueOf(this.valueOutputAttribute(i)) + (i != numOutputAttributes() - 1 ? "|" : "");
			}
			ret += "}";
			return ret;
		}
	}

	@Override
	public void setInputMissing(int inputAttributeIndex) {
		this.setMissing(this.instanceHeader.getInputInstanceIndex(inputAttributeIndex));
		
	}

	@Override
	public void setOutputMissing(int outputAttributeIndex) {
		this.setMissing(this.instanceHeader.getOutputInstanceIndex(outputAttributeIndex));
	}

}
