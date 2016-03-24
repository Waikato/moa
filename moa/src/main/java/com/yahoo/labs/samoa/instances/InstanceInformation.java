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
import java.util.ArrayList;
import java.util.List;

import moa.AbstractMOAObject;

/**
 * The Class InstanceInformation.
 *
 * @author abifet
 */
public class InstanceInformation implements Serializable {

    /**
     * The dataset's name.
     */
    protected String relationName;

    protected AttributesInformation attributesInformation;
    
    protected List<Integer> outputIndexes;
    protected List<Integer> inputIndexes;
    
    
    public Attribute inputAttribute(int w) {
        return this.attributesInformation.attribute(inputAttributeIndex(w));
    }

    public Attribute outputAttribute(int w) {
        return this.attributesInformation.attribute(outputAttributeIndex(w));
    }

    
    
    /**
     * Instantiates a new instance information.
     *
     * @param chunk the chunk
     */
    public InstanceInformation(InstanceInformation chunk) {
        this.relationName = chunk.relationName;
        this.outputIndexes = chunk.outputIndexes;
        this.inputIndexes = chunk.inputIndexes;
        this.attributesInformation = chunk.attributesInformation;
    }

    /**
     * Instantiates a new instance information.
     *
     * @param st the st
     * @param v the v
     */
    public InstanceInformation(String st, List<Attribute> attributes, List<Integer> outputIndexes, List<Integer> inputIndexes) {
        this.relationName = st;
        this.outputIndexes = outputIndexes;
        this.inputIndexes = inputIndexes;
        this.attributesInformation = new AttributesInformation(attributes, attributes.size());
    }
    
    public InstanceInformation(String st, List<Attribute> input, List<Integer> indexes) {
        this.relationName = st;
        this.outputIndexes = new ArrayList<Integer>();
        this.inputIndexes = new ArrayList<Integer>();
        this.attributesInformation = new AttributesInformation(input, indexes, indexes.size());
    }

    public InstanceInformation(String st, List<Attribute> input) {
        this.relationName = st;
        this.outputIndexes = new ArrayList<Integer>();
        this.inputIndexes = new ArrayList<Integer>();
        List<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < input.size(); i++) {
        	indexes.add(i);
        }
        this.attributesInformation = new AttributesInformation(input, indexes, indexes.size());
    }
    
    /**
     * Instantiates a new instance information.
     */
    public InstanceInformation() {
        this.relationName = null;
        this.inputIndexes = null;
        this.outputIndexes = null;
        this.attributesInformation = null;
    }

    //Information Instances
    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setRelationName(java.lang.String)
     */
    public void setRelationName(String string) {
        this.relationName = string;
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#getRelationName()
     */
    public String getRelationName() {
        return this.relationName;
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#classIndex()
     */
    public int classIndex() {
    	if (this.outputIndexes.size() > 1)
    		throw new UnsupportedOperationException("This instance has multiple output attributes.");
        return this.outputIndexes.get(0);
    }

    public void setClassIndex(int index) {
    	this.outputIndexes = new ArrayList<Integer>();
    	this.outputIndexes.add(index);
    }
    

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#classAttribute()
     */
    public Attribute classAttribute() {
        return this.attribute(this.classIndex());
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#numAttributes()
     */
    public int numAttributes() {
        return this.attributesInformation.numberAttributes;
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#attribute(int)
     */
    public Attribute attribute(int w) {
        return this.attributesInformation.attribute(w);
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#numClasses()
     */
    public int numClasses() {
        return this.attributesInformation.attribute(classIndex()).numValues();
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#deleteAttributeAt(java.lang.Integer)
     */
    public void deleteAttributeAt(Integer integer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* (non-Javadoc)
     * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#insertAttributeAt(com.yahoo.labs.samoa.instances.Attribute, int)
     */
    public void insertAttributeAt(Attribute attribute, int i) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setAttributes(List<Attribute> v) {
    	if(this.attributesInformation==null)
    		this.attributesInformation= new AttributesInformation();
        this.attributesInformation.setAttributes(v);
    }

    public int inputAttributeIndex(int index) {
    	// Returns actual instance attribute index!
        return inputIndexes.get(index);
    }

    public int outputAttributeIndex(int index) {
    	// Returns actual instance attribute index!
    	return outputIndexes.get(index);    
    }

    public int numInputAttributes() {
        return inputIndexes.size();
    }

    public int numOutputAttributes() {

    	return outputIndexes.size();
    }

	public void setAttributes(List<Attribute> v, List<Integer> indexValues) {
    	if(this.attributesInformation==null)
    		this.attributesInformation= new AttributesInformation();
        this.attributesInformation.setAttributes(v,indexValues);
	}

	public void setOutputIndexes(List<Integer> outputIndexes) {
		this.outputIndexes = outputIndexes;
	}
	
	public void setInputIndexes(List<Integer> inputIndexes) {
		this.inputIndexes = inputIndexes;
	}

	/** 
	 * Sets all non-output attributes as input attributes.
	 */
	public void setInputIndexes() {
		this.inputIndexes = new ArrayList<Integer>();
		for (int i = 0; i < attributesInformation.indexValues.size(); i++) {
			if (!outputIndexes.contains(attributesInformation.indexValues.get(i))) {
				inputIndexes.add(attributesInformation.indexValues.get(i));
			}
		}
	}

}
