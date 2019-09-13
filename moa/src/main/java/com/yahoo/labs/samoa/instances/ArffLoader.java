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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import moa.core.utils.AttributeDefinitionUtil;

/**
 * The ArffLoader class. Loads an arff file with sparse or dense format.
 */
public class ArffLoader {

    /**
     * The instance information.
     */
    protected InstanceInformation instanceInformation;

    protected InstancesHeader streamHeader;

    /**
     * Stores the indexes which are used. Values are positions read from the file,
     * indexes correspond to Instance indexes.
     */
    protected List<Integer> attributeIndexes;
    
    /**
     * List of loaded attributes. Only includes the attributes which are selected.
     * Indexes are the same as Instance indexes.
     */
    protected List<Attribute> attributes;

    /**
     * The stream tokenizer.
     */
    protected StreamTokenizer streamTokenizer;

   
    /**
     * Instantiates a new arff loader. Constructor for backwards compatibility.
     *
     * @param reader the reader
     * @param size the size
     * @param classAttribute the class attribute
     */
    public ArffLoader(Reader reader, int size, int classAttribute) {
        // size is not used
    	this(reader,String.valueOf(classAttribute));
    }


    /**
     * Instantiates a new arff loader.
     *
     * @param reader the reader
     * @param outputDefinition the string definition of output attributes
     * @param inputDefinition the string definition of input attributes
     */
    public ArffLoader(Reader reader, String outputDefinition, String inputDefinition) {
        BufferedReader br = new BufferedReader(reader);

        // Initialize streamTokenizer
        streamTokenizer = new StreamTokenizer(br);
        streamTokenizer.resetSyntax();
        streamTokenizer.whitespaceChars(0, ' ');
        streamTokenizer.wordChars(' ' + 1, '\u00FF');
        streamTokenizer.whitespaceChars(',', ',');
        streamTokenizer.commentChar('%');
        streamTokenizer.quoteChar('"');
        streamTokenizer.quoteChar('\'');
        streamTokenizer.ordinaryChar('{');
        streamTokenizer.ordinaryChar('}');
        streamTokenizer.eolIsSignificant(true);

        this.instanceInformation = this.getHeader(outputDefinition, inputDefinition);
    }

    /**
     * Instantiates a new arff loader.
     *
     * @param reader the reader
     * @param outputDefinition the string definition of output attributes (others are taken as inputs)
     */
    public ArffLoader(Reader reader, String outputDefinition) {
        BufferedReader br = new BufferedReader(reader);

        //Initialize streamTokenizer
        streamTokenizer = new StreamTokenizer(br);
        streamTokenizer.resetSyntax();
        streamTokenizer.whitespaceChars(0, ' ');
        streamTokenizer.wordChars(' ' + 1, '\u00FF');
        streamTokenizer.whitespaceChars(',', ',');
        streamTokenizer.commentChar('%');
        streamTokenizer.quoteChar('"');
        streamTokenizer.quoteChar('\'');
        streamTokenizer.ordinaryChar('{');
        streamTokenizer.ordinaryChar('}');
        streamTokenizer.eolIsSignificant(true);

        this.instanceInformation = this.getHeader(outputDefinition, AttributeDefinitionUtil.nonIgnoredDefinition);
    }

    /**
     * Returns the attribute at the index in the arff file
     * 
     * @param index arff file index
     * @return corresponding attritbue
     */
    protected Attribute instanceAttribute(int index) {
    	return this.attributes.get(instanceIndex(index));
    }
    
    /**
     * Remaps the arff file intex to the instance index
     * 
     * @param index arff file index
     * @return instance index
     */
    protected int instanceIndex(int index) {
    	return this.attributeIndexes.indexOf(index);
    }
    
    /**
     * Checks whether the arff file index has been selected
     * 
     * @param index arff file index
     * @return
     */
    protected boolean isIndexSelected(int index) {
    	return this.attributeIndexes.contains(index);
    }

    /**
     * Creates the InstancesInformation from the arff header and selected input and output definitions
     * 
     * @param outputDefinition the selected output indexes
     * @param inputDefinition the selected input indexes
     * @return the InstancesInformation header
     */
    private InstanceInformation getHeader(String outputDefinition, String inputDefinition) {
        String relation = "file stream";
        List<Attribute> allAttributes = new ArrayList<Attribute>();//JD
        int numAttributes = 0;
        List<Integer> inputIndexes = new ArrayList<Integer>();
        List<Integer> outputIndexes = new ArrayList<Integer>();
        try {
            streamTokenizer.nextToken();
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                //For each line
                if (streamTokenizer.ttype == StreamTokenizer.TT_WORD && streamTokenizer.sval.startsWith("@") == true) {
                    String token = streamTokenizer.sval.toUpperCase();
                    if (token.startsWith("@RELATION")) {
                        streamTokenizer.nextToken();
                        relation = streamTokenizer.sval;
                    } else if (token.startsWith("@ATTRIBUTE")) {
                        streamTokenizer.nextToken();
                        String name = streamTokenizer.sval;
                        if (name == null) {
                            name = Double.toString(streamTokenizer.nval);
                        }
                        streamTokenizer.nextToken();
                        if (streamTokenizer.ttype == '{') {
                            streamTokenizer.nextToken();
                            List<String> attributeLabels = new ArrayList<String>();
                            while (streamTokenizer.ttype != '}') {

                                if (streamTokenizer.sval != null) {
                                    attributeLabels.add(streamTokenizer.sval);
                                } else {
                                    attributeLabels.add(Double.toString(streamTokenizer.nval));
                                }

                                streamTokenizer.nextToken();
                            }
                            allAttributes.add(new Attribute(name, attributeLabels));
                            numAttributes++;
                            /* TODO Hierarchical loading */
//                        } else if (streamTokenizer.sval != null && streamTokenizer.sval.toUpperCase() == "HIERARCHICAL") {
//                        	streamTokenizer.nextToken();
//                        	DAGStructure attributeStructure = new DAGStructure();
//                        	if (streamTokenizer.ttype == '{') {
//                        		while(streamTokenizer.ttype != '}') {
//                        			streamTokenizer.nextToken();
//                        			System.out.println(streamTokenizer.sval);
//                        		}
//                        		
//                        	}
//                        	
                        } else {
                            allAttributes.add(new Attribute(name));
                            numAttributes++;
                        }

                    } else if (token.startsWith("@DATA")) {
                        //System.out.print("END");
                        streamTokenizer.nextToken();
                        break;
                    }
                }
                streamTokenizer.nextToken();
            }
            
            outputIndexes = AttributeDefinitionUtil.parseAttributeDefinition(outputDefinition, numAttributes, null);
            inputIndexes = AttributeDefinitionUtil.parseAttributeDefinition(inputDefinition, numAttributes, outputIndexes);

            attributes = new ArrayList<Attribute>();
            attributeIndexes = new ArrayList<Integer>();
            
            // Keep only used attributes
            for (int i = 0; i < allAttributes.size(); i++)
            	if (inputIndexes.contains(i) || outputIndexes.contains(i)) {
            		attributes.add(allAttributes.get(i));
            		attributeIndexes.add(i);
            	}
            
            inputIndexes = AttributeDefinitionUtil.remapAttributeDefitinion(inputIndexes, attributeIndexes);		
            outputIndexes = AttributeDefinitionUtil.remapAttributeDefitinion(outputIndexes, attributeIndexes);
            
        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new InstanceInformation(relation, attributes, outputIndexes, inputIndexes);
    }

    
    /**
     * Reads instance. It detects if it is dense or sparse.
     *
     * @return the instance
     */
    public Instance readInstance() {
        while (streamTokenizer.ttype == StreamTokenizer.TT_EOL) {
            try {
                streamTokenizer.nextToken();
            } catch (IOException ex) {
                Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (streamTokenizer.ttype == '{') {
            return readInstanceSparse();
        } else {
            return readInstanceDense();
        }

    }

    /**
     * Reads a dense instance from the file.
     *
     * @return the instance
     */
    public Instance readInstanceDense() {
        Instance instance = newDenseInstance(this.instanceInformation.numInputAttributes() + this.instanceInformation.numOutputAttributes());
        int numAttribute = 0; // Instance index
        try {
            while (numAttribute == 0 && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                //For each line
            	int valueNum = 0; // File index
                while (streamTokenizer.ttype != StreamTokenizer.TT_EOL && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                    //For each item
                	if (isIndexSelected(valueNum)) { // Check that the index is included in the selected indexes 
	                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
	                        this.setValue(instance, numAttribute, streamTokenizer.nval, true);
	                        numAttribute++;
	
	                    } else if (streamTokenizer.sval != null && (streamTokenizer.ttype == StreamTokenizer.TT_WORD || streamTokenizer.ttype == 34)) {
	                        boolean isNumeric = this.instanceInformation.attribute(numAttribute).isNumeric();
	                        double value;
	                        if ("?".equals(streamTokenizer.sval)) {
	                            value = Double.NaN;
	                        } else if (isNumeric == true) {
	                            value = Double.valueOf(streamTokenizer.sval).doubleValue();
	                        } else {
	                            value = this.instanceInformation.attribute(numAttribute).indexOfValue(streamTokenizer.sval);
	                        }
	                        this.setValue(instance, numAttribute, value, isNumeric);
	                        numAttribute++;
	                    }
                    }
                	valueNum++;
                    streamTokenizer.nextToken();
                }
                streamTokenizer.nextToken();
            }

        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (numAttribute > 0) ? instance : null;
    }

    /**
     * Reads a sparse instance from the file.
     *
     * @return the instance
     */
    private Instance readInstanceSparse() {
        //Return a sparse Instance
        Instance instance = newSparseInstance(1.0);
        int numAttribute;
        ArrayList<Double> attributeValues = new ArrayList<Double>();
        List<Integer> indexValues = new ArrayList<Integer>();
        try {
        	streamTokenizer.nextToken(); // Remove the '{' char
            //For each line
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOL
                    && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                while (streamTokenizer.ttype != '}') {
                    //For each item
                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        numAttribute = (int) streamTokenizer.nval;
                    } else {
                        numAttribute = Integer.parseInt(streamTokenizer.sval);
                    }
                    streamTokenizer.nextToken(); //The ':' char

                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        if (isIndexSelected(numAttribute))
                        	this.setSparseValue(instance, indexValues, attributeValues, instanceIndex(numAttribute), streamTokenizer.nval, true);

                    } else if (streamTokenizer.sval != null && (streamTokenizer.ttype == StreamTokenizer.TT_WORD || streamTokenizer.ttype == 34)) {
                        if (this.instanceAttribute(numAttribute).isNumeric()) {
                            this.setSparseValue(instance, indexValues, attributeValues, instanceIndex(numAttribute), Double.valueOf(streamTokenizer.sval).doubleValue(), true);
                        } else {
                            this.setSparseValue(instance, indexValues, attributeValues, instanceIndex(numAttribute), instanceAttribute(numAttribute).indexOfValue(streamTokenizer.sval), false);
                        }
                    }
                    streamTokenizer.nextToken();
                }
                streamTokenizer.nextToken(); //Remove the '}' char
            }
            streamTokenizer.nextToken();
        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        int[] arrayIndexValues = new int[attributeValues.size()];
        double[] arrayAttributeValues = new double[attributeValues.size()];
        for (int i = 0; i < arrayIndexValues.length; i++) {
            arrayIndexValues[i] = indexValues.get(i).intValue();
            arrayAttributeValues[i] = attributeValues.get(i).doubleValue();
        }
        instance.addSparseValues(arrayIndexValues, arrayAttributeValues, this.instanceInformation.numInputAttributes() + this.instanceInformation.numOutputAttributes());
        return instance;

    }

    /**
     * Reads an instance sparse and returns a dense one.
     *
     * @return the instance
     */
    private Instance readDenseInstanceSparse() {
        //Returns a dense instance
        Instance instance = newDenseInstance(this.instanceInformation.numAttributes());
        int numAttribute;
        try {
            streamTokenizer.nextToken(); // Remove the '{' char
            //For each line
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOL
                    && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                while (streamTokenizer.ttype != '}') {
                    //For each item
                    numAttribute = (int) streamTokenizer.nval;
                    streamTokenizer.nextToken();

                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        this.setValue(instance, numAttribute, streamTokenizer.nval, true);
                    } else if (streamTokenizer.sval != null && (streamTokenizer.ttype == StreamTokenizer.TT_WORD
                            || streamTokenizer.ttype == 34)) {
                        if (this.instanceAttribute(numAttribute).isNumeric()) {
                            this.setValue(instance, numAttribute, Double.valueOf(streamTokenizer.sval).doubleValue(), true);
                        } else {
                            this.setValue(instance, numAttribute, this.instanceInformation.attribute(numAttribute).indexOfValue(streamTokenizer.sval), false);
                        }
                    }
                    streamTokenizer.nextToken();
                }
                streamTokenizer.nextToken(); //Remove the '}' char
            }
            streamTokenizer.nextToken();
        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return instance;
    }

    /**
     * Sets the appropriate value of the instance
     * 
     * @param instance the instance for which the value is being set
     * @param numAttribute the index of attribute for which the value is being set
     * @param value the value
     * @param isNumber whether the value is a number
     */
    protected void setValue(Instance instance, int numAttribute, double value, boolean isNumber) {
        double valueAttribute;

        // Correct the value for nominal attributes
        if (isNumber && this.instanceInformation.attribute(numAttribute).isNominal()) {
            valueAttribute = this.instanceInformation.attribute(numAttribute).indexOfValue(Double.toString(value));
        } else {
            valueAttribute = value;
        }
        
        instance.setValue(numAttribute, valueAttribute);
    }
    
    /** 
     * Updates the list of indexes and values for the instance.
     * No values are assigned in this method, they are all assigned at once, when the instance is done loading. 
     * 
     * @param instance the instance
     * @param indexValues the assigned indexes
     * @param attributeValues the assigned values
     * @param numAttribute the index of the attribute which is being assigned
     * @param value the value which is being assigned
     * @param isNumber whether the value is a number
     */
    private void setSparseValue(Instance instance, List<Integer> indexValues, List<Double> attributeValues, int numAttribute, double value, boolean isNumber) {
        double valueAttribute;
        if (isNumber && this.instanceInformation.attribute(numAttribute).isNominal()) {
            valueAttribute = this.instanceInformation.attribute(numAttribute).indexOfValue(Double.toString(value));
        } else {
            valueAttribute = value;
        }
        indexValues.add(numAttribute);
        attributeValues.add(valueAttribute);
    }

    protected Instance newSparseInstance(double d, double[] res) {
        Instance inst = new SparseInstance(d, res); //is it dense?
        return inst;
    }
    
    protected Instance newSparseInstance(double d) {
        Instance inst = new SparseInstance(d);
        return inst;
    }

    protected Instance newDenseInstance(int numberAttributes) {
        Instance inst = new DenseInstance(numberAttributes);
        return inst;
    }
}
