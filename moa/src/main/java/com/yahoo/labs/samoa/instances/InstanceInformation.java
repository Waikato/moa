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

/**
 * The Class InstanceInformation.
 *
 * @author abifet
 */
public class InstanceInformation implements Serializable{
    
    
	protected InstanceDataInformation inputInstanceInformation;
	
	protected InstanceDataInformation outputInstanceInformation;
	
	public Attribute inputAttribute(int w) {
        return this.inputInstanceInformation.attributes.get(w);
    }

	public Attribute outputAttribute(int w) {
        return this.outputInstanceInformation.attributes.get(w);
    }
	
	
    
  /** The dataset's name. */
  protected String relationName;         

  /** The attribute information. */
  //protected List<Attribute> attributes;
  
  /** The class index. */
  protected int classIndex;
  

 
    /**
     * Instantiates a new instance information.
     *
     * @param chunk the chunk
     */
    public InstanceInformation(InstanceInformation chunk) {
        this.relationName = chunk.relationName;
        //this.attributes = chunk.attributes;
        this.inputInstanceInformation = chunk.inputInstanceInformation;
        this.outputInstanceInformation = chunk.outputInstanceInformation;
        this.classIndex = chunk.classIndex;
    }
    
    /**
     * Instantiates a new instance information.
     *
     * @param st the st
     * @param v the v
     */
    public InstanceInformation(String st, List<Attribute> input, List<Attribute> output) {
        this.relationName = st;
        this.inputInstanceInformation = new InstanceDataInformation(st,input);
        this.outputInstanceInformation = new InstanceDataInformation(st,output);
        //this.attributes = v;
    }
    
    /**
     * Instantiates a new instance information.
     */
    public InstanceInformation() {
        this.relationName = null;
        //this.attributes = null;
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
        return classIndex; 
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setClassIndex(int)
	 */
	public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
        Attribute classAtribute = this.inputAttribute(classIndex);
        List<Attribute> listAttribute = new ArrayList<Attribute>();
        listAttribute.add(classAtribute);
        this.outputInstanceInformation.setAttributes(listAttribute);
        this.inputInstanceInformation.attributes.remove(classAtribute);
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
        return this.inputInstanceInformation.attributes.size() + this.outputInstanceInformation.attributes.size();
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#attribute(int)
	 */
	public Attribute attribute(int w) {
    	//TODO: check for single label instances
    	int offset = this.inputInstanceInformation.attributes.size();
    	if (w < offset) { 
    		return this.inputInstanceInformation.attributes.get(w);
    	} else {
    		return this.outputInstanceInformation.attributes.get(w-offset);
    	}
    }
    
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#numClasses()
	 */
	public int numClasses() {
        return this.outputInstanceInformation.attributes.get(this.classIndex()).numValues();
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

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setAttributes(java.util.List)
	 */
   /* @Override
	public void setAttributes(List<Attribute> v) {
        this.attributes = v;
    }*/
    
    
}
