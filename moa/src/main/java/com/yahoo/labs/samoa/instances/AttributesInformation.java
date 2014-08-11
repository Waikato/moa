package com.yahoo.labs.samoa.instances;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AttributesInformation {

	/** The attribute information. */
	protected List<Attribute> attributes;
	protected List<Integer> indexValues;
	/** The number of attributes. */
	protected int numberAttributes;
        
        /** The attribute used for default for numerical values */
        protected Attribute defaultNumericAttribute;


	public AttributesInformation(AttributesInformation chunk) {
	    this.attributes = chunk.attributes;
	    this.indexValues = chunk.indexValues;
	    this.numberAttributes = chunk.numberAttributes;
	}
	
	public AttributesInformation(List<Attribute> v, List<Integer> i, int numberAttributes) {
            this.attributes = v;
            this.indexValues = i;
            this.numberAttributes = numberAttributes;
        }
        
        public AttributesInformation(List<Attribute> v, int numberAttributes) {
            this.attributes = v;
            this.indexValues = new ArrayList<Integer>(numberAttributes);
            for (int i = 0; i< numberAttributes; i++){
                this.indexValues.add(i);
            }
            this.numberAttributes = numberAttributes;
        }
	
	 public AttributesInformation() {
	        this.attributes = null;
	        this.indexValues = null;
	        this.numberAttributes = 0;
                this.defaultNumericAttribute = null;
	    }
	
	/**
	 * Attribute.
	 *
	 * @param indexAttribute the index Attribute
	 * @return the attribute
	 */
	public Attribute attribute(int indexAttribute) {
                if (this.attributes == null) {
                    //All attributes are numeric
                    return defaultNumericAttribute();
                }
		int location = locateIndex(indexAttribute);
                if (location == -1) {
                    //if there is not attribute information, it is numeric
                    return defaultNumericAttribute();
                }
		return attributes.get(location);
	}
	
	public void add(Attribute attribute, int value){
		this.attributes.add(attribute);
		this.indexValues.add(value);
	}

	/**
	 * Sets the attribute information.
	 *
	 * @param v the new attribute information
	 */
	public void setAttributes(List<Attribute> v) {
	    this.attributes = v;
	}

	/**
	 * Locates the greatest index that is not greater than the given index.
	 * 
	 * @return the internal index of the attribute index. Returns -1 if no index
	 *         with this property could be found
	 */
	public int locateIndex(int index) {
	
	  int min = 0;
	  int max = this.indexValues.size() - 1;
	
	  if (max == -1) {
	    return -1;
	  }
	
	  // Binary search
	  while ((this.indexValues.get(min) <= index) && (this.indexValues.get(max) >= index)) {
	    int current = (max + min) / 2;
	    if (this.indexValues.get(current) > index) {
	      max = current - 1;
	    } else if (this.indexValues.get(current) < index) {
	      min = current + 1;
	    } else {
	      return current;
	    }
	  }
	  if (this.indexValues.get(max) < index) {
	    return max;
	  } else {
	    return min - 1;
	  }
	}

    private Attribute defaultNumericAttribute() {
        if (this.defaultNumericAttribute == null) {
            this.defaultNumericAttribute = new Attribute("default");
        }
        return this.defaultNumericAttribute;
    }

}