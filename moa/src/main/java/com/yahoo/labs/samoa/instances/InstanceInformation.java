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
public class InstanceInformation implements InstanceInformationInterface, Serializable{
    
    //protected InstanceDataInformation inputInstanceInformation;

//protected InstanceDataInformation outputInstanceInformation;
    
/** The dataset's name. */
protected String relationName;   

protected AttributesInformation attributesInformation;   

/** The class index. */
protected int classIndex = Integer.MAX_VALUE; //By default is multilabel

/** Range for multi-label instances. */
protected Range range;

public Attribute inputAttribute(int w) {
return this.attributesInformation.attribute(inputAttributeIndex(w));
        //inputInstanceInformation.attributes.get(w);
}

public Attribute outputAttribute(int w) {
return this.attributesInformation.attribute(outputAttributeIndex(w));
    //this.outputInstanceInformation.attributes.get(w);
}
	
	
    
      

  /** The attribute information. */
  //protected List<Attribute> attributes;
  

  

 
    /**
     * Instantiates a new instance information.
     *
     * @param chunk the chunk
     */
    public InstanceInformation(InstanceInformation chunk) {
        this.relationName = chunk.relationName;
        //this.attributes = chunk.attributes;
        //this.inputInstanceInformation = chunk.inputInstanceInformation;
        //this.outputInstanceInformation = chunk.outputInstanceInformation;
        this.attributesInformation = chunk.attributesInformation;
        this.classIndex = chunk.classIndex;
    }
    
    /**
     * Instantiates a new instance information.
     *
     * @param st the st
     * @param v the v
     */
    public InstanceInformation(String st, List<Attribute> input) {
        this.relationName = st;
        this.attributesInformation = new AttributesInformation(input, input.size());
        //this.outputInstanceInformation = new InstanceDataInformation(st,output);
        //this.attributes = v;
    }
    
    /**
     * Instantiates a new instance information.
     */
    public InstanceInformation() {
        this.relationName = null;
        //this.attributes = null;
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
        return this.classIndex; 
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#setClassIndex(int)
	 */
	public void setClassIndex(int classIndex) {
            this.classIndex = classIndex;
		/*if(classIndex<Integer.MAX_VALUE){//classIndex==Integer.MAX_VALUE indicates multilabel
			if(outputInstanceInformation.numAttributes()>0) //JD
			{
				this.inputInstanceInformation.attributes.add(classIndex, outputInstanceInformation.attributes.get(0));
				outputInstanceInformation.attributes.remove(0);
			}
			
			Attribute classAtribute = this.inputAttribute(classIndex);
			List<Attribute> listAttribute = new ArrayList<Attribute>();
			listAttribute.add(classAtribute);
			this.outputInstanceInformation.setAttributes(listAttribute);
			this.inputInstanceInformation.attributes.remove(classAtribute);
			this.classIndex = classIndex;
			}
            */
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
        return  this.attributesInformation.numberAttributes;
                //this.inputInstanceInformation.attributes.size() + this.outputInstanceInformation.attributes.size();
    }

    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#attribute(int)
	 */
	public Attribute attribute(int w) {
    	//TODO: check for single label instances
    	/*int offset = this.inputInstanceInformation.attributes.size();
		if(classIndex<offset){ //Just for single label instances (retro-compatibility)
			if(w==classIndex)
				return this.outputInstanceInformation.attributes.get(0);
			else if(w<classIndex)
				return this.inputInstanceInformation.attributes.get(w);
			else
				return this.inputInstanceInformation.attributes.get(w-1);
				
		}	
		else //is multilabel (classindex=-1) 
			if (w < offset) { 
    		return this.inputInstanceInformation.attributes.get(w);
    	} else {
    		return this.outputInstanceInformation.attributes.get(w-offset);
    	}*/
        return this.attributesInformation.attribute(w);
    }
    
    /* (non-Javadoc)
	 * @see com.yahoo.labs.samoa.instances.InstanceInformationInterface#numClasses()
	 */
	public int numClasses() {
        //return this.outputInstanceInformation.attributes.get(this.classIndex()).numValues(); //JD
        return this.attributesInformation.attribute(classIndex()).numValues();
//this.outputInstanceInformation.attributes.get(0).numValues();
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

    @Override
    public void setAttributes(List<Attribute> v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int inputAttributeIndex(int index) {
        int ret = 0;
        if (classIndex == Integer.MAX_VALUE){//Multi Label
            
        } else { //Single Label
            ret = classIndex() > index ? index : index + 1;
        }
        return ret;
    }    

    public int outputAttributeIndex(int attributeIndex) {
        int ret = 0;
        if (classIndex == Integer.MAX_VALUE){//Multi Label
            
        } else { //Single Label
            ret = classIndex;
        }
        return ret; }

    int numInputAttributes() {
        int ret = 0;
        if (classIndex == Integer.MAX_VALUE){//Multi Label
            
        } else { //Single Label
            ret = this.numAttributes() - 1;
        }
        return ret; 
    }

    int numOutputAttributes() {
        int ret = 0;
        if (classIndex == Integer.MAX_VALUE){//Multi Label
            
        } else { //Single Label
            ret = 1;
        }
        return ret;   
    }

    void setRangeOutputIndices(Range range) {
        this.setClassIndex(Integer.MAX_VALUE);
        this.range = range;
    }
    
    
}
