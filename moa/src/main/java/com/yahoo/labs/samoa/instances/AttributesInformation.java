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
 * Class for storing the information of the attributes.
 * It stores only information about discrete attributes and suppose that
 * the default attribute is numeric.
 *
 * @version $Revision: 7 $
 */
public class AttributesInformation implements Serializable {

    /**
     * The attribute information.
     */
    protected Attribute[] attributes;
    protected int[] indexValues;
    /**
     * The number of attributes.
     */
    protected int numberAttributes;

    /**
     * The attribute used for default for numerical values
     */
    protected Attribute defaultNumericAttribute;

    public AttributesInformation(AttributesInformation chunk) {
        this.attributes = chunk.attributes.clone();
        this.indexValues = chunk.indexValues.clone();
        this.numberAttributes = chunk.numberAttributes;
    }

    public AttributesInformation(Attribute[] v, int[] i, int numberAttributes) {
        this.attributes = v;
        this.indexValues = i;
        this.numberAttributes = numberAttributes;
    }

    public AttributesInformation(Attribute[] v, int numberAttributes) {
        this.attributes = v;
        this.indexValues = new int[numberAttributes];
        for (int i = 0; i < numberAttributes; i++) {
            this.indexValues[i]=i;
        }
        this.numberAttributes = numberAttributes;
    }

    public AttributesInformation(List<Attribute> v, int numberAttributes) {
        this.attributes = new Attribute[numberAttributes];
        this.indexValues = new int[numberAttributes];
        for (int i = 0; i < numberAttributes; i++) {
            this.indexValues[i]=i;
            this.attributes[i]= v.get(i);
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
        return attributes[location];
    }

    /*public void add(Attribute attribute, int value) {
        this.attributes.add(attribute);
        this.indexValues.add(value);
    }*/

    /**
     * Sets the attribute information.
     *
     * @param v the new attribute information
     */
    public void setAttributes(Attribute[] v) {
        this.attributes = v;
        this.numberAttributes=v.length;
        this.indexValues = new int[numberAttributes];
        for (int i = 0; i < numberAttributes; i++) {
            this.indexValues[i]=i;
        }
    }

    /**
     * Locates the greatest index that is not greater than the given index.
     *
     * @return the internal index of the attribute index. Returns -1 if no index
     * with this property could be found
     */
    public int locateIndex(int index) {

        int min = 0;
        int max = this.indexValues.length - 1;

        if (max == -1) {
            return -1;
        }

        // Binary search
        while ((this.indexValues[min] <= index) && (this.indexValues[max] >= index)) {
            int current = (max + min) / 2;
            if (this.indexValues[current] > index) {
                max = current - 1;
            } else if (this.indexValues[current] < index) {
                min = current + 1;
            } else {
                return current;
            }
        }
        if (this.indexValues[max] < index) {
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

	public void setAttributes(Attribute[] v, int[] indexValues) {
	        this.attributes = v;
	        this.numberAttributes=v.length;
	        this.indexValues=indexValues;
	}

    public void deleteAttributeAt(int position) {

        int index = locateIndex(position);

        this.numberAttributes--;
        if ((index >= 0) && (indexValues[index] == position)) {
            int[] tempIndices = new int[indexValues.length - 1];
            Attribute[] tempValues = new Attribute[attributes.length - 1];
            System.arraycopy(indexValues, 0, tempIndices, 0, index);
            System.arraycopy(attributes, 0, tempValues, 0, index);
            for (int i = index; i < indexValues.length - 1; i++) {
                tempIndices[i] = indexValues[i + 1] - 1;
                tempValues[i] = attributes[i + 1];
            }
            indexValues = tempIndices;
            attributes = tempValues;
        } else {
            int[] tempIndices = new int[indexValues.length];
            Attribute[] tempValues = new Attribute[attributes.length];
            System.arraycopy(indexValues, 0, tempIndices, 0, index + 1);
            System.arraycopy(attributes, 0, tempValues, 0, index + 1);
            for (int i = index + 1; i < indexValues.length; i++) {
                tempIndices[i] = indexValues[i] - 1;
                tempValues[i] = attributes[i];
            }
            indexValues = tempIndices;
            attributes = tempValues;
        }
    }

    public void insertAttributeAt(Attribute attribute, int position) {
        if ((position< 0) || (position > this.numberAttributes)) {
            throw new IllegalArgumentException("Can't insert attribute: index out "
                    + "of range");
        }
        int index = locateIndex(position);

        this.numberAttributes++;
        if ((index >= 0) && (indexValues[index] == position)) {
            int[] tempIndices = new int[indexValues.length + 1];
            Attribute[] tempValues = new Attribute[attributes.length + 1];
            System.arraycopy(indexValues, 0, tempIndices, 0, index);
            System.arraycopy(attributes, 0, tempValues, 0, index);
            tempIndices[index] = position;
            tempValues[index] =  attribute;
            for (int i = index; i < indexValues.length; i++) {
                tempIndices[i + 1] = indexValues[i] + 1;
                tempValues[i + 1] = attributes[i];
            }
            indexValues = tempIndices;
            attributes = tempValues;
        } else {
            int[] tempIndices = new int[indexValues.length + 1];
            Attribute[] tempValues = new Attribute[attributes.length + 1];
            System.arraycopy(indexValues, 0, tempIndices, 0, index + 1);
            System.arraycopy(attributes, 0, tempValues, 0, index + 1);
            tempIndices[index + 1] = position;
            tempValues[index + 1] =  attribute;
            for (int i = index + 1; i < indexValues.length; i++) {
                tempIndices[i + 1] = indexValues[i] + 1;
                tempValues[i + 1] = attributes[i];
            }
            indexValues = tempIndices;
            attributes = tempValues;
        }
    }

    /* DENSE
    public void deleteAttributeAt(Integer position) {
        if ((position < 0) || (position >= attributes.size())) {
            throw new IllegalArgumentException("Index out of range");
        }

        ArrayList<Attribute> newList = new ArrayList<Attribute>(attributes.size() - 1);
        for (int i = 0 ; i < position; i++) {
            Attribute att = attributes.get(i);
            newList.add(att);
        }
        for (int i = position + 1; i < attributes.size(); i++) {
            Attribute newAtt = (Attribute) attributes.get(i);
            newList.add(newAtt);
        }
        attributes = newList;
    }

    public void insertAttributeAt(Attribute att, int position) {

        if ((position < 0) || (position > attributes.size())) {
            throw new IllegalArgumentException("Index out of range");
        }

        ArrayList<Attribute> newList = new ArrayList<Attribute>(attributes.size() + 1);
        for (int i = 0 ; i < position; i++) {
            Attribute oldAtt = attributes.get(i);
            newList.add(oldAtt);
        }
        newList.add(att);
        for (int i = position; i < attributes.size(); i++) {
            Attribute newAtt = (Attribute) attributes.get(i);
            newList.add(newAtt);
        }
        attributes = newList;

    }*/

}
