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
import java.util.List;

/**
 * The Class InstanceInformation.
 *
 * @author abifet
 */
public class InstanceDataInformation implements Serializable, InstanceInformationInterface{
    
    //Should we split Instances as a List of Instances, and InformationInstances
    
  /** The dataset's name. */
  protected String relationName;         

  /** The attribute information. */
  protected List<Attribute> attributes;
  
  /** The class index. */
  protected int classIndex;
  

 
    /**
     * Instantiates a new instance information.
     *
     * @param chunk the chunk
     */
    public InstanceDataInformation(InstanceDataInformation chunk) {
        this.relationName = chunk.relationName;
        this.attributes = chunk.attributes;
        this.classIndex = chunk.classIndex;
    }
    
    /**
     * Instantiates a new instance information.
     *
     * @param st the st
     * @param v the v
     */
    public InstanceDataInformation(String st, List<Attribute> v) {
        this.relationName = st;
        this.attributes = v;
    }
    
    /**
     * Instantiates a new instance information.
     */
    public InstanceDataInformation() {
        this.relationName = null;
        this.attributes = null;
    }
    
    
    //Information Instances
    
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setRelationName(java.lang.String)
	 */
    @Override
	public void setRelationName(String string) {
        this.relationName = string;
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#getRelationName()
	 */
    @Override
	public String getRelationName() {
        return this.relationName;
    }
    
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#classIndex()
	 */
    @Override
	public int classIndex() {
        return classIndex; 
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setClassIndex(int)
	 */
    @Override
	public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }
  
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#classAttribute()
	 */
    @Override
	public Attribute classAttribute() {
        return this.attribute(this.classIndex());
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#numAttributes()
	 */
    @Override
	public int numAttributes() {
        return (this.attributes==null)? 0 : this.attributes.size();
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#attribute(int)
	 */
    @Override
	public Attribute attribute(int w) {
        return this.attributes.get(w);
    }
    
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#numClasses()
	 */
    @Override
	public int numClasses() {
        return this.attributes.get(this.classIndex()).numValues();
    }
    
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#deleteAttributeAt(java.lang.Integer)
	 */
    @Override
	public void deleteAttributeAt(Integer integer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#insertAttributeAt(com.yahoo.labs.samoa.instances.Attribute, int)
	 */
    @Override
	public void insertAttributeAt(Attribute attribute, int i) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setAttributes(java.util.List)
	 */
    @Override
	public void setAttributes(List<Attribute> v) {
        this.attributes = v;
    }
    
    
}
