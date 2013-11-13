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
public class InstanceInformation implements Serializable{
    
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
    public InstanceInformation(InstanceInformation chunk) {
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
    public InstanceInformation(String st, List<Attribute> v) {
        this.relationName = st;
        this.attributes = v;
    }
    
    /**
     * Instantiates a new instance information.
     */
    public InstanceInformation() {
        this.relationName = null;
        this.attributes = null;
    }
    
    
    //Information Instances
    
    /**
     * Sets the dataset's name.
     *
     * @param string the new dataset's name
     */
    public void setRelationName(String string) {
        this.relationName = string;
    }

    /**
     * Gets the dataset's name.
     *
     * @return the dataset's name
     */
    public String getRelationName() {
        return this.relationName;
    }
    
    /**
     * Class index.
     *
     * @return the int
     */
    public int classIndex() {
        return classIndex; 
    }

    /**
     * Sets the class index.
     *
     * @param classIndex the new class index
     */
    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }
  
    /**
     * Class attribute.
     *
     * @return the attribute
     */
    public Attribute classAttribute() {
        return this.attribute(this.classIndex());
    }

    /**
     * Num attributes.
     *
     * @return the int
     */
    public int numAttributes() {
        return this.attributes.size();
    }

    /**
     * Attribute.
     *
     * @param w the w
     * @return the attribute
     */
    public Attribute attribute(int w) {
        return this.attributes.get(w);
    }
    
    /**
     * Num classes.
     *
     * @return the int
     */
    public int numClasses() {
        return this.attributes.get(this.classIndex()).numValues();
    }
    
    /**
     * Delete attribute at.
     *
     * @param integer the integer
     */
    public void deleteAttributeAt(Integer integer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Insert attribute at.
     *
     * @param attribute the attribute
     * @param i the i
     */
    public void insertAttributeAt(Attribute attribute, int i) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Sets the attribute information.
     *
     * @param v the new attribute information
     */
    public void setAttributes(List<Attribute> v) {
        this.attributes = v;
    }
    
    
}
