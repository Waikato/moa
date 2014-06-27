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
 * The Class MultiLabelInstance.
 *
 * @author abifet
 */
   
public class MultiLabelInstance implements Instance {

    /** The weight. */
    protected double weight;
    
    /** The instance data. */
    protected InstanceData instanceData;
    
    /** The class data. */
    protected InstanceData classData;
    
    //Fast implementation without using Objects
    //protected double[] attributeValues;
    //protected double classValue;
    
    /** The instance information. */
    protected InstancesHeader instanceInformation;
    
     /**
     * Instantiates a new single label instance.
     *
     * @param inst the inst
     */
    public MultiLabelInstance( MultiLabelInstance inst) {
        this.weight = inst.weight;
        this.instanceData = inst.instanceData; //copy
        this.classData = inst.classData; //copy
        //this.classValue = inst.classValue;
        //this.attributeValues = inst.attributeValues;
        this.instanceInformation = inst.instanceInformation;
    }
    
    //Dense
    /**
     * Instantiates a new single label instance.
     *
     * @param weight the weight
     * @param res the res
     */
    public MultiLabelInstance(double weight, double[] res, double[] output) {
         this.weight = weight;
         this.instanceData = new DenseInstanceData(res);
         //this.attributeValues = res;
         this.classData = new DenseInstanceData(output);
         //this.classValue = Double.NaN;
    }
    
    //Sparse
    /**
     * Instantiates a new single label instance.
     *
     * @param weight the weight
     * @param attributeValues the attribute values
     * @param indexValues the index values
     * @param numberAttributes the number attributes
     */
    public MultiLabelInstance(double weight, double[] attributeValues, int[] indexValues, int numberAttributes, double[] outputAttributeValues, int[] outputIndexValues, int outputNumberAttributes) {
         this.weight = weight;
         this.instanceData = new SparseInstanceData(attributeValues, indexValues, numberAttributes); //???
         this.classData = new SparseInstanceData(outputAttributeValues, outputIndexValues, outputNumberAttributes);
         //this.classValue = Double.NaN;
    }
    
     /**
     * Instantiates a new single label instance.
     *
     * @param weight the weight
     * @param instanceData the instance data
     */
    public MultiLabelInstance(double weight, InstanceData instanceData, InstanceData outputInstanceData) {
         this.weight = weight;
         this.instanceData = instanceData; //???
         //this.classValue = Double.NaN;
         this.classData = outputInstanceData;
    }
    
     /**
     * Instantiates a new single label instance.
     *
     * @param numAttributes the num attributes
     */
    public MultiLabelInstance(int numAttributes) {
    	this(numAttributes,1);
    }
    
    public MultiLabelInstance(int numAttributes, int numOutputAttributes) {
    this.instanceData = new DenseInstanceData(new double[numAttributes]);
    this.weight = 1;
    this.classData = new DenseInstanceData(new double[numOutputAttributes]);
  }
    
    /**
     * Weight.
     *
     * @return the double
     */  
    @Override
    public double weight() {
        return weight;
    }
    
     /**
     * Sets the weight.
     *
     * @param weight the new weight
     */
    @Override
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
     /**
     * Attribute.
     *
     * @param instAttIndex the inst att index
     * @return the attribute
     */    
    @Override
    public Attribute attribute(int instAttIndex) {
        return this.instanceInformation.attribute(instAttIndex);
    }
    
     /**
     * Delete attribute at.
     *
     * @param i the i
     */
    @Override
    public void deleteAttributeAt(int i) {
        //throw new UnsupportedOperationException("Not yet implemented");
    }

     /**
     * Insert attribute at.
     *
     * @param i the i
     */
    @Override
    public void insertAttributeAt(int i) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
   
     /**
     * Num attributes.
     *
     * @return the int
     */
    @Override
    public int numAttributes() {
        return this.instanceInformation.numAttributes(); 
    }
    
    public int numInputAttributes() {
        return this.instanceInformation.numInputAttributes(); 
    }
    
    public int numOutputAttributes() {
        return this.instanceInformation.numOutputAttributes(); 
    }
    
     /**
     * Value.
     *
     * @param instAttIndex the inst att index
     * @return the double
     */ 
    @Override
    public double value(int instAttIndex) {
        return //attributeValues[instAttIndex]; //
                this.instanceData.value(instAttIndex);
    }

     /**
     * Checks if is missing.
     *
     * @param instAttIndex the inst att index
     * @return true, if is missing
     */
    @Override
    public boolean isMissing(int instAttIndex) {
        return //Double.isNaN(value(instAttIndex)); //
                this.instanceData.isMissing(instAttIndex);
    }

     /**
     * Num values.
     *
     * @return the int
     */
    @Override
    public int numValues() {
        return //this.attributeValues.length; //
        this.instanceData.numValues();
    }

     /**
     * Index.
     *
     * @param i the i
     * @return the int
     */
    @Override
    public int index(int i) {
        return //i; //
        this.instanceData.index(i);
    }

     /**
     * Value sparse.
     *
     * @param i the i
     * @return the double
     */
    @Override
    public double valueSparse(int i) {
        return this.instanceData.valueSparse(i);
    }
    
    /**
     * Checks if is missing sparse.
     *
     * @param p the p
     * @return true, if is missing sparse
     */
    @Override
    public boolean isMissingSparse(int p) {
        return this.instanceData.isMissingSparse(p);
    }
    
     /**
     * Value.
     *
     * @param attribute the attribute
     * @return the double
     */
    @Override
    public double value(Attribute attribute) {
       return value(attribute.index());
    
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
        return //this.attributeValues; //
                this.instanceData.toDoubleArray();
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
        //this.attributeValues[numAttribute] = d;
    }
    
     /**
     * Class value.
     *
     * @return the double
     */        
    @Override
    public double classValue() {
        return this.classData.value(0); 
        //return classValue;
    }

     /**
     * Class index.
     *
     * @return the int
     */
    @Override
    public int classIndex() {
        return instanceInformation.classIndex();
    }

     /**
     * Num classes.
     *
     * @return the int
     */
    @Override
    public int numClasses() {
        return this.instanceInformation.numClasses();
    }

     /**
     * Class is missing.
     *
     * @return true, if successful
     */
    @Override
    public boolean classIsMissing() {
        return //Double.isNaN(this.classValue);//
        this.classData.isMissing(0);
    }

    /**
     * Class attribute.
     *
     * @return the attribute
     */    
    @Override
    public Attribute classAttribute() {
        return this.instanceInformation.attribute(0);
    }
    
    /**
     * Sets the class value.
     *
     * @param d the new class value
     */
    @Override
    public void setClassValue(double d) {
        this.classData.setValue(0, d);
        //this.classValue = d;
    }
    
    /**
     * Copy.
     *
     * @return the instance
     */
    @Override
    public Instance copy() {
       MultiLabelInstance inst = new MultiLabelInstance(this);
       return inst;
    }
    
    /**
     * Dataset.
     *
     * @return the instances
     */
    @Override
    public Instances dataset() {
        return this.instanceInformation;
    }

     /**
     * Sets the dataset.
     *
     * @param dataset the new dataset
     */ 
    @Override
    public void setDataset(Instances dataset) {
        this.instanceInformation = new InstancesHeader(dataset);
    }

     /**
     * Adds the sparse values.
     *
     * @param indexValues the index values
     * @param attributeValues the attribute values
     * @param numberAttributes the number attributes
     */
    public void addSparseValues(int[] indexValues, double[] attributeValues, int numberAttributes) {
         this.instanceData = new SparseInstanceData(attributeValues, indexValues, numberAttributes); //???
    }

	public int numberOutputTargets() {
		return this.classData.numAttributes();
	}

	public double classValue(int instAttIndex) {
		return this.classData.value(instAttIndex);
	}

	public InstanceData classValues() {
		return this.classData;
	}

	public void setClassValue(int indexClass, double valueAttribute) {
		this.classData.setValue(indexClass, valueAttribute);
		
	}

	public Attribute outputAttribute(int outputIndex) {
		return this.instanceInformation.outputAttribute(outputIndex);
	}

	public Attribute inputAttribute(int attributeIndex) {
		return this.instanceInformation.inputAttribute(attributeIndex);
	}

	public double valueInputAttribute(int attributeIndex) {
		return this.instanceData.value(attributeIndex);
	}

	public double valueOutputAttribute(int attributeIndex) {
		return this.classData.value(attributeIndex);
	}


}
