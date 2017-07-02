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
 * Class for storing the information of the attributes.
 * It stores only information about discrete attributes and suppose that
 * the default attribute is numeric.
 *
 * @version $Revision: 7 $
 */
public class AttributesInformation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1666190771085863695L;

	/**
     * The attribute information.
     */
    protected List<Attribute> attributes;

    /**
     * The attribute used for default for numerical values
     */
    protected Attribute defaultNumericAttribute;

    public AttributesInformation(AttributesInformation chunk) {
        this.attributes = chunk.attributes;
    }

    public AttributesInformation(List<Attribute> v) {
        this.attributes = v;
    }

    public AttributesInformation() {
        this.attributes = null;
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
        return attributes.get(indexAttribute);
    }

    public void add(Attribute attribute) {
        this.attributes.add(attribute);
    }

    public int indexOfAttribute(Attribute a) {
    	return attributes.indexOf(a);
    }
    
    /**
     * Sets the attribute information.
     *
     * @param v the new attribute information
     */
    public void setAttributes(List<Attribute> v) {
        this.attributes = v;
    }

    private Attribute defaultNumericAttribute() {
        if (this.defaultNumericAttribute == null) {
            this.defaultNumericAttribute = new Attribute("default");
        }
        return this.defaultNumericAttribute;
    }

    public int numAttributes() {
    	return attributes.size();
    }
    
}
