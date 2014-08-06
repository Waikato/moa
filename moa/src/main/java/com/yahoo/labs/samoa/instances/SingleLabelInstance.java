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

import com.sun.corba.se.impl.protocol.INSServerRequestDispatcher;

/**
 * The Class SingleLabelInstance.
 *
 * @author abifet
 */
   
public class SingleLabelInstance implements Instance {

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
    public SingleLabelInstance( SingleLabelInstance inst) {
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
    public SingleLabelInstance(double weight, double[] res) {
         this.weight = weight;
         this.instanceData = new DenseInstanceData(res);
         //this.attributeValues = res;
         this.classData = new SingleClassInstanceData();
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
    public SingleLabelInstance(double weight, double[] attributeValues, int[] indexValues, int numberAttributes) {
         this.weight = weight;
         this.instanceData = new SparseInstanceData(attributeValues, indexValues, numberAttributes); //???
         this.classData = new SingleClassInstanceData();
         //this.classValue = Double.NaN;
    }
    
     /**
     * Instantiates a new single label instance.
     *
     * @param weight the weight
     * @param instanceData the instance data
     */
    public SingleLabelInstance(double weight, InstanceData instanceData) {
         this.weight = weight;
         this.instanceData = instanceData; //???
         //this.classValue = Double.NaN;
         this.classData = new SingleClassInstanceData();
    }
    
     /**
     * Instantiates a new single label instance.
     *
     * @param numAttributes the num attributes
     */
    public SingleLabelInstance(int numAttributes) {
    this.instanceData = new DenseInstanceData(new double[numAttributes-1]); //JD
    //m_AttValues = new double[numAttributes];
    /*for (int i = 0; i < m_AttValues.length; i++) {
      m_AttValues[i] = Utils.missingValue();
    }*/
    this.weight = 1;
    this.classData = new SingleClassInstanceData();
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
    
     /**
     * Value.
     *
     * @param instAttIndex the inst att index
     * @return the double
     */ 
    @Override
    public double value(int instAttIndex) {
    	int matchIndex=getMatchingIndex(instAttIndex);
    	if(matchIndex==this.numAttributes()-1)
    		return this.classValue();
    	else
    		return //this.instanceData.value(instAttIndex); //JD - for compatibility with older code (class attribute not the last)
        		this.instanceData.value(matchIndex);
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
                this.instanceData.isMissing(getMatchingIndex(instAttIndex));
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
       SingleLabelInstance inst = new SingleLabelInstance(this);
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
    
    /**
     * Text representation of a SingleLabelInstance.
     */
    public String toString()
    {
    	double [] aux = this.instanceData.toDoubleArray();
    	StringBuffer str= new StringBuffer();
    	for (int i=0; i<aux.length;i++)
    		str.append(aux[i]+" ");
    	str.append("- ");
    	aux = this.classData.toDoubleArray();
    	for (int i=0; i<aux.length;i++)
    		str.append(aux[i]+" ");
    	
    	return str.toString();
    }

  //JD - for compatibility with older code (class attribute not the last)
    /**
    * Makes correspondence between old instance representation and .
    *
    * @param instAttIndex the index value
    * @return "corrected" attribute index
    */
    private int getMatchingIndex(int instAttIndex) {
    	int classIndex=this.classIndex();
    	if (instAttIndex==classIndex)
    		instAttIndex=this.numAttributes()-1;
    	else 
    		if(instAttIndex>classIndex) 
    			instAttIndex--;
		
		return instAttIndex;
	}
    
    
}
