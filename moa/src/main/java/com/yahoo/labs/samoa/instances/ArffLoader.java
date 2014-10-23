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

/**
 * The Class ArffLoader. Loads an Arff file with sparse or dense format.
 */
public class ArffLoader {

    /**
     * The instance information.
     */
    protected InstanceInformation instanceInformation;

    protected InstancesHeader streamHeader;

    /**
     * The stream tokenizer.
     */
    protected StreamTokenizer streamTokenizer;

    /**
     * Instantiates a new arff loader.
     *
     * @param reader the reader
     * @param size the size
     * @param classAttribute the class attribute
     */
    public ArffLoader(Reader reader, int size, int classAttribute) {
        // size is not used
        this(reader);
        if (classAttribute < 0) {
            this.instanceInformation.setClassIndex(this.instanceInformation.numAttributes() - 1);
            //System.out.print(this.instanceInformation.classIndex());
        } else if (classAttribute > 0) {
            this.instanceInformation.setClassIndex(classAttribute - 1);
        }
    }

    protected Range range;

    /**
     * Instantiates a new arff loader.
     *
     * @param reader the reader
     * @param range
     * @param size the size
     * @param classAttribute the class attribute
     */
    public ArffLoader(Reader reader) {
        this(reader, null);
    }

    /**
     * Instantiates a new arff loader.
     *
     * @param reader the reader
     * @param range
     * @param size the size
     * @param classAttribute the class attribute
     */
    public ArffLoader(Reader reader, Range range) {
        this.range = range;
        BufferedReader br = new BufferedReader(reader);

        //Init streamTokenizer
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

        this.instanceInformation = this.getHeader();

        if (range != null) { //is MultiLabel
            this.instanceInformation.setRangeOutputIndices(range);
        }

    }

    /**
     * Gets the structure.
     *
     * @return the structure
     */
    public InstanceInformation getStructure() {
        return this.instanceInformation;
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
            // return readDenseInstanceSparse();
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
        Instance instance = newDenseInstance(this.instanceInformation.numAttributes());
        //System.out.println(this.instanceInformation.numAttributes());
        int numAttribute = 0;
        try {
            while (numAttribute == 0 && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                //For each line
                while (streamTokenizer.ttype != StreamTokenizer.TT_EOL
                        && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                    //For each item
                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        //System.out.println(streamTokenizer.nval + "Num ");
                        this.setValue(instance, numAttribute, streamTokenizer.nval, true);
                        numAttribute++;

                    } else if (streamTokenizer.sval != null && (streamTokenizer.ttype == StreamTokenizer.TT_WORD
                            || streamTokenizer.ttype == 34)) {
                        //System.out.println(streamTokenizer.sval + "Str");
                        boolean isNumeric = this.instanceInformation.attribute(numAttribute).isNumeric();
                        double value;
                        if ("?".equals(streamTokenizer.sval)) {
                            value = Double.NaN; //Utils.missingValue();
                        } else if (isNumeric == true) {
                            value = Double.valueOf(streamTokenizer.sval).doubleValue();
                        } else {
                            value = this.instanceInformation.attribute(numAttribute).indexOfValue(streamTokenizer.sval);
                        }

                        this.setValue(instance, numAttribute, value, isNumeric);
                        numAttribute++;
                    }
                    streamTokenizer.nextToken();
                }
                streamTokenizer.nextToken();
                //System.out.println("EOL");
            }

        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (numAttribute > 0) ? instance : null;
    }

    protected void setValue(Instance instance, int numAttribute, double value, boolean isNumber) {
        double valueAttribute;

        if (isNumber && this.instanceInformation.attribute(numAttribute).isNominal) {
            valueAttribute = this.instanceInformation.attribute(numAttribute).indexOfValue(Double.toString(value));
            //System.out.println(value +"/"+valueAttribute+" ");

        } else {
            valueAttribute = value;
            //System.out.println(value +"/"+valueAttribute+" ");
        }
        if (this.instanceInformation.classIndex() == numAttribute) {
            setClassValue(instance, valueAttribute);
            //System.out.println(value +"<"+this.instanceInformation.classIndex()+">");
        } else {
        	//if(numAttribute>this.instanceInformation.classIndex())
            //	numAttribute--;
            instance.setValue(numAttribute, valueAttribute);
        }
    }

    /**
     * Reads a sparse instance.
     *
     * @return the instance
     */
    private Instance readInstanceSparse() {
        //Return a Sparse Instance
        Instance instance = newSparseInstance(1.0); //, null); //(this.instanceInformation.numAttributes() + 1);
        //System.out.println(this.instanceInformation.numAttributes());
        int numAttribute;
        ArrayList<Double> attributeValues = new ArrayList<Double>();
        List<Integer> indexValues = new ArrayList<Integer>();
        try {
            //while (streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
            streamTokenizer.nextToken(); // Remove the '{' char
            //For each line
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOL
                    && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                while (streamTokenizer.ttype != '}') {
                    //For each item
                    //streamTokenizer.nextToken();
                    //while (streamTokenizer.ttype != '}'){
                    //System.out.println(streamTokenizer.nval +"-"+ streamTokenizer.sval);
                    //numAttribute = (int) streamTokenizer.nval;
                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        numAttribute = (int) streamTokenizer.nval;
                    } else {
                        numAttribute = Integer.parseInt(streamTokenizer.sval);
                    }
                    streamTokenizer.nextToken();

                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        //System.out.print(streamTokenizer.nval + " ");
                        this.setSparseValue(instance, indexValues, attributeValues, numAttribute, streamTokenizer.nval, true);
                        //numAttribute++;

                    } else if (streamTokenizer.sval != null && (streamTokenizer.ttype == StreamTokenizer.TT_WORD
                            || streamTokenizer.ttype == 34)) {
                        //System.out.print(streamTokenizer.sval + "-");
                        if (this.auxAttributes.get(numAttribute).isNumeric()) {
                            this.setSparseValue(instance, indexValues, attributeValues, numAttribute, Double.valueOf(streamTokenizer.sval).doubleValue(), true);
                        } else {
                            this.setSparseValue(instance, indexValues, attributeValues, numAttribute, this.instanceInformation.attribute(numAttribute).indexOfValue(streamTokenizer.sval), false);
                        }
                    }
                    streamTokenizer.nextToken();
                }
                streamTokenizer.nextToken(); //Remove the '}' char
            }
            streamTokenizer.nextToken();
            //System.out.println("EOL");
            //}

        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        int[] arrayIndexValues = new int[attributeValues.size()];
        double[] arrayAttributeValues = new double[attributeValues.size()];
        for (int i = 0; i < arrayIndexValues.length; i++) {
            arrayIndexValues[i] = indexValues.get(i).intValue();
            arrayAttributeValues[i] = attributeValues.get(i).doubleValue();
        }
        instance.addSparseValues(arrayIndexValues, arrayAttributeValues, this.instanceInformation.numAttributes());
        return instance;

    }

    private void setSparseValue(Instance instance, List<Integer> indexValues, List<Double> attributeValues, int numAttribute, double value, boolean isNumber) {
        double valueAttribute;
        if (isNumber && this.instanceInformation.attribute(numAttribute).isNominal) {
            valueAttribute = this.instanceInformation.attribute(numAttribute).indexOfValue(Double.toString(value));
        } else {
            valueAttribute = value;
        }
        //if (this.instanceInformation.classIndex() == numAttribute) {
        //    setClassValue(instance, valueAttribute);
        //} else {
            //instance.setValue(numAttribute, valueAttribute);
            indexValues.add(numAttribute);
            attributeValues.add(valueAttribute);
        //}
        //System.out.println(numAttribute+":"+valueAttribute+","+this.instanceInformation.classIndex()+","+value);
    }

    /**
     * Reads an instance sparse and returns a dense one.
     *
     * @return the instance
     */
    private Instance readDenseInstanceSparse() {
        //Returns a dense instance
        Instance instance = newDenseInstance(this.instanceInformation.numAttributes());
        //System.out.println(this.instanceInformation.numAttributes());
        int numAttribute;
        try {
            //while (streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
            streamTokenizer.nextToken(); // Remove the '{' char
            //For each line
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOL
                    && streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                while (streamTokenizer.ttype != '}') {
                    //For each item
                    //streamTokenizer.nextToken();
                    //while (streamTokenizer.ttype != '}'){
                    //System.out.print(streamTokenizer.nval+":");
                    numAttribute = (int) streamTokenizer.nval;
                    streamTokenizer.nextToken();

                    if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        //System.out.print(streamTokenizer.nval + " ");
                        this.setValue(instance, numAttribute, streamTokenizer.nval, true);
                        //numAttribute++;

                    } else if (streamTokenizer.sval != null && (streamTokenizer.ttype == StreamTokenizer.TT_WORD
                            || streamTokenizer.ttype == 34)) {
                        //System.out.print(streamTokenizer.sval + "/"+this.instanceInformation.attribute(numAttribute).indexOfValue(streamTokenizer.sval)+" ");
                        if (this.auxAttributes.get(numAttribute).isNumeric()) {
                            this.setValue(instance, numAttribute, Double.valueOf(streamTokenizer.sval).doubleValue(), true);
                        } else {
                            this.setValue(instance, numAttribute, this.instanceInformation.attribute(numAttribute).indexOfValue(streamTokenizer.sval), false);
                            //numAttribute++;
                        }
                    }
                    streamTokenizer.nextToken();
                }
                streamTokenizer.nextToken(); //Remove the '}' char
            }
            streamTokenizer.nextToken();
            //System.out.println("EOL");
            //}

        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return instance;
    }

    //protected List<Attribute> inputAttributes;
   // protected List<Attribute> outputAttributes;
    
    protected List<Attribute> auxAttributes;

    private InstanceInformation getHeader() {
    	//commented JD
        //this.range.setUpper(10000); //TO DO: Create a new range object with isInRange that does not need the upper limit
        String relation = "file stream";
        //System.out.println("RELATION " + relation);
        //inputAttributes = new ArrayList<Attribute>();
        //outputAttributes = new ArrayList<Attribute>();
        //ArrayList<Attribute> 
        auxAttributes = new ArrayList<Attribute>();//JD
        int numAttributes = 0;
        try {
            streamTokenizer.nextToken();
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                //For each line
                //if (streamTokenizer.ttype == '@') {
                if (streamTokenizer.ttype == StreamTokenizer.TT_WORD && streamTokenizer.sval.startsWith("@") == true) {
                    //streamTokenizer.nextToken();
                    String token = streamTokenizer.sval.toUpperCase();
                    if (token.startsWith("@RELATION")) {
                        streamTokenizer.nextToken();
                        relation = streamTokenizer.sval;
                      //  System.out.println("RELATION " + relation);
                    } else if (token.startsWith("@ATTRIBUTE")) {
                        streamTokenizer.nextToken();
                        String name = streamTokenizer.sval;
                        //System.out.println("* " + name);
                        if (name == null) {
                            name = Double.toString(streamTokenizer.nval);
                        }
                        streamTokenizer.nextToken();
                        String type = streamTokenizer.sval;
                       // System.out.println("* " + name + ":" + type + " ");
                        if (streamTokenizer.ttype == '{') {
                            streamTokenizer.nextToken();
                            List<String> attributeLabels = new ArrayList<String>();
                            while (streamTokenizer.ttype != '}') {

                                if (streamTokenizer.sval != null) {
                                    attributeLabels.add(streamTokenizer.sval);
                                   // System.out.print(streamTokenizer.sval + ",");
                                } else {
                                    attributeLabels.add(Double.toString(streamTokenizer.nval));
                                    //System.out.print(streamTokenizer.nval + ",");
                                }

                                streamTokenizer.nextToken();
                            }
                           // System.out.println();
                            //attributes.add(new Attribute(name, attributeLabels));
                            //commented JD
                           /* if (this.range.isInRange(numAttribute)) {
                             outputAttributes.add(new Attribute(name, attributeLabels));
                             } else {
                             inputAttributes.add(new Attribute(name, attributeLabels));
                             }*/
                            auxAttributes.add(new Attribute(name, attributeLabels));
                            numAttributes++;
                        } else {
                            // Add attribute
                            //commented JD
                        	/*if (this.range.isInRange(numAttribute)) {
                             outputAttributes.add(new Attribute(name));
                             } else {
                             inputAttributes.add(new Attribute(name));
                             }*/
                            auxAttributes.add(new Attribute(name));
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
            if (range != null) {
                this.range.setUpper(numAttributes);
            }
            /*if (range==null) //is single-target. All instances should go to inputAtrributes (see setClassIndex(int) from InstanceInformation )
             inputAttributes=auxAttributes;
             else//is multi-target
             {
             this.range.setUpper(numAttribute);
             for (int i=0; i<auxAttributes.size();i++)
             {
             //if (this.range.isInRange(i))
             //	outputAttributes.add(auxAttributes.get(i));
             //else
             inputAttributes.add(auxAttributes.get(i));
	            	
             }
             }*/

        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        // this.range.setUpper(inputAttributes.size()+outputAttributes.size());
        return new InstanceInformation(relation, auxAttributes);
    }

    protected Instance newSparseInstance(double d, double[] res) {
        Instance inst = new SparseInstance(d, res); //is it dense?
        //inst.setInstanceInformation(this.instanceInformation);
        return inst;
    }
    
    protected Instance newSparseInstance(double d) {
        Instance inst = new SparseInstance(d);
        //inst.setInstanceInformation(this.instanceInformation);
        return inst;
    }

    protected Instance newDenseInstance(int numberAttributes) {
        Instance inst = new DenseInstance(numberAttributes);
        //inst.setInstanceInformation(this.instanceInformation);
        return inst;
    }

    private void setClassValue(Instance instance, double valueAttribute) {
        instance.setValue(this.instanceInformation.classIndex(), valueAttribute);
    }

}
