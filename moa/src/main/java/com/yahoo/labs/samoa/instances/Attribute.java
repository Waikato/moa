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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class Attribute.
 */
public class Attribute implements Serializable {

    /**
     * The is nominal.
     */
    protected boolean isNominal;

    /**
     * The is numeric.
     */
    protected boolean isNumeric;

    /**
     * The is date.
     */
    protected boolean isDate;

    /**
     * The name.
     */
    protected String name;

    /**
     * The attribute values.
     */
    protected List<String> attributeValues;

    /**
     * Gets the attribute values.
     *
     * @return the attribute values
     */
    public List<String> getAttributeValues() {
        return attributeValues;
    }

    /**
     * The index.
     */
    protected int index;

    /**
     * Instantiates a new attribute.
     *
     * @param string the string
     */
    public Attribute(String string) {
        this.name = string;
        this.isNumeric = true;
    }

    /**
     * Instantiates a new attribute.
     *
     * @param attributeName the attribute name
     * @param attributeValues the attribute values
     */
    public Attribute(String attributeName, List<String> attributeValues) {
        this.name = attributeName;
        this.attributeValues = attributeValues;
        this.isNominal = true;
    }

    /**
     * Instantiates a new attribute.
     */
    public Attribute() {
        this("");
    }

    /**
     * Checks if is nominal.
     *
     * @return true, if is nominal
     */
    public boolean isNominal() {
        return this.isNominal;
    }

    /**
     * Name.
     *
     * @return the string
     */
    public String name() {
        return this.name;
    }

    /**
     * Value.
     *
     * @param value the value
     * @return the string
     */
    public String value(int value) {
        return attributeValues.get(value);
    }

    /**
     * Checks if is numeric.
     *
     * @return true, if is numeric
     */
    public boolean isNumeric() {
        return isNumeric;
    }

    /**
     * Num values.
     *
     * @return the int
     */
    public int numValues() {
        if (isNumeric()) {
            return 0;
        } else {
            return attributeValues.size();
        }
    }

    /**
     * Index.
     *
     * @return the int
     */
    public int index() { //RuleClassifier
        return this.index;
    }

    /**
     * Format date.
     *
     * @param value the value
     * @return the string
     */
    String formatDate(double value) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        return sdf.format(new Date((long) value));
    }

    /**
     * Checks if is date.
     *
     * @return true, if is date
     */
    boolean isDate() {
        return isDate;
    }

    /**
     * The values string attribute.
     */
    private Map<String, Integer> valuesStringAttribute;

    /**
     * Index of value.
     *
     * @param value the value
     * @return the int
     */
    public final int indexOfValue(String value) {

        if (isNominal() == false) {
            return -1;
        }
        if (this.valuesStringAttribute == null) {
            this.valuesStringAttribute = new HashMap<String, Integer>();
            int count = 0;
            for (String stringValue : attributeValues) {
                this.valuesStringAttribute.put(stringValue, count);
                count++;
            }
        }
        Integer val = (Integer) this.valuesStringAttribute.get(value);
        if (val == null) {
            return -1;
        } else {
            return val.intValue();
        }
    }
}
