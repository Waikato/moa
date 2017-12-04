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
import java.util.*;

import moa.core.Utils;
/**
 * The Class Attribute.
 */
public class Attribute implements Serializable {

    /** The keyword used to denote the start of an arff attribute declaration */
    public final static String ARFF_ATTRIBUTE = "@attribute";

    /** A keyword used to denote a numeric attribute */
    public final static String ARFF_ATTRIBUTE_INTEGER = "integer";

    /** A keyword used to denote a numeric attribute */
    public final static String ARFF_ATTRIBUTE_REAL = "real";

    /** A keyword used to denote a numeric attribute */
    public final static String ARFF_ATTRIBUTE_NUMERIC = "numeric";

    /** The keyword used to denote a string attribute */
    public final static String ARFF_ATTRIBUTE_STRING = "string";

    /** The keyword used to denote a date attribute */
    public final static String ARFF_ATTRIBUTE_DATE = "date";

    /** The keyword used to denote a relation-valued attribute */
    public final static String ARFF_ATTRIBUTE_RELATIONAL = "relational";

    /** The keyword used to denote the end of the declaration of a subrelation */
    public final static String ARFF_END_SUBRELATION = "@end";

    /** Strings longer than this will be stored compressed. */
    private static final int STRING_COMPRESS_THRESHOLD = 200;

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
     * Date format specification for date attributes 
     */
    protected SimpleDateFormat m_DateFormat;
  
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
     *
     * @param attributeName the attribute name
     * @param dateFormat the format of the date used
     */
    public Attribute(String attributeName, String dateFormat) {
        this.name = attributeName;        
        this.valuesStringAttribute = null;
        this.isDate = true;
        
        if (dateFormat != null) {
          m_DateFormat = new SimpleDateFormat(dateFormat);
        } else {
          m_DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        }
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
//    public int index() { //RuleClassifier        
//        return this.index;
//    }

    /**
     * Format date.
     *
     * @param value the value
     * @return the string
     */
    String formatDate(double value) {
        return this.m_DateFormat.format(new Date((long) value));
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
        
        // in case value was not on a list of unique values of nominal attribute yet, add it
        // Hence, the list of values can be extended with new entries arriving in the stream
        if (val == null) {
          int currentValueCount=this.valuesStringAttribute.size();
          this.valuesStringAttribute.put(value,currentValueCount);
          this.attributeValues.add(value);
          return currentValueCount;
        } else {
            return val.intValue();
        }
    }
    
    /**
    * Returns a description of this attribute in ARFF format. Quotes
    * strings if they contain whitespace characters, or if they
     * are a question mark.
     *
     * @return a description of this attribute as a string
     */
    public final String toString() {
    
    StringBuffer text = new StringBuffer();
    
    text.append(ARFF_ATTRIBUTE).append(" ").append(Utils.quote(this.name())).append(" ");
    
    if (this.isNominal){
        text.append('{');
        Enumeration enu =  enumerateValues();
        while (enu.hasMoreElements()) {
            text.append(Utils.quote((String) enu.nextElement()));
            if (enu.hasMoreElements())
                text.append(',');
        }
        text.append('}');
    } else if (this.isNumeric){
        text.append(ARFF_ATTRIBUTE_NUMERIC);
    } else if (this.isDate){
        text.append(ARFF_ATTRIBUTE_DATE).append(" ").append(Utils.quote(m_DateFormat.toPattern()));
    } else{
        text.append("UNKNOW");
    }
    
    return text.toString();
  }
  
    /**
   * Returns an enumeration of all the attribute's values if the
   * attribute is nominal, null otherwise.
   *
   * @return enumeration of all the attribute's values
   */
  public final /*@ pure @*/ Enumeration enumerateValues() {

    if (this.isNominal()) {
      return Collections.enumeration(this.attributeValues);
    }
    return null;
  }
}
