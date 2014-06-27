package com.yahoo.labs.samoa.instances;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Range;

public class MultiTargetArffLoader extends ArffLoader {

    public MultiTargetArffLoader(Reader reader) {
		super(reader);
		// TODO Auto-generated constructor stub
	}

	public MultiTargetArffLoader(Reader reader, Range range) {
		super(reader, range);
	}

	@Override
	protected Instance newSparseInstance(double d, double[] res) {
		return new SparseMultiLabelInstance(d, res, res) ; // TODO
 	}
    
	@Override
    protected Instance newDenseInstance(int i) {
		this.range.setUpper(this.instanceInformation.numAttributes());
		int numberOuputAttributes = range.getSelection().length;
 		
		return new DenseMultiLabelInstance(i-numberOuputAttributes, numberOuputAttributes);
	}
    
    protected SparseInstanceInformation getSparseHeader() {

        String relation = "file stream";
        //System.out.println("RELATION " + relation);
        //attributes = new ArrayList<Attribute>();
        AttributesInformation attributesInformation = new AttributesInformation();
        int index = 0;
        try {
            streamTokenizer.nextToken();
            while (streamTokenizer.ttype != StreamTokenizer.TT_EOF) {
                //For each line
                //if (streamTokenizer.ttype == '@') {
            	int incrementIndex = 1;
            	if (streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
            		incrementIndex = (int) streamTokenizer.nval;
            		streamTokenizer.nextToken();
            	}
                if (streamTokenizer.ttype == StreamTokenizer.TT_WORD && streamTokenizer.sval.startsWith("@") == true) {
                    //streamTokenizer.nextToken();
                    String token = streamTokenizer.sval.toUpperCase();
                    if (token.startsWith("@RELATION")) {
                        streamTokenizer.nextToken();
                        relation = streamTokenizer.sval;
                        System.out.println("RELATION " + relation);
                    } else if (token.startsWith("@ATTRIBUTE")) {
                        streamTokenizer.nextToken();
                        String name = streamTokenizer.sval;
                        //System.out.println("* " + name);
                        if (name == null) {
                            name = Double.toString(streamTokenizer.nval);
                        }
                        streamTokenizer.nextToken();
                        String type = streamTokenizer.sval;
                        System.out.println("* " + name + ":" + type + " ");
                        if (streamTokenizer.ttype == '{') {
                            streamTokenizer.nextToken();
                            List<String> attributeLabels = new ArrayList<String>();
                            while (streamTokenizer.ttype != '}') {

                                if (streamTokenizer.sval != null) {
                                    attributeLabels.add(streamTokenizer.sval);
                                    System.out.print(streamTokenizer.sval + ",");
                                } else {
                                    attributeLabels.add(Double.toString(streamTokenizer.nval));
                                    System.out.print(streamTokenizer.nval + ",");
                                }

                                streamTokenizer.nextToken();
                            }
                            System.out.println();
                            //attributes.add(new Attribute(name, attributeLabels));
                            attributesInformation.add(new Attribute(name, attributeLabels), index);
                            index += incrementIndex;
                        } else {
                            // Add attribute
                            //attributes.add(new Attribute(name));
                        	attributesInformation.add(new Attribute(name), index);
                        	index += incrementIndex;
                        }

                    } else if (token.startsWith("@DATA")) {
                        System.out.print("END");
                        streamTokenizer.nextToken();
                        break;
                    }
                }
                streamTokenizer.nextToken();
            }

        } catch (IOException ex) {
            Logger.getLogger(ArffLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new SparseInstanceInformation(relation, attributesInformation);
    }
    
    private int indexClass = 0;
    private int indexAttribute = 0;
    
    @Override
    protected void setValue(Instance inst, int numAttribute, double value, boolean isNumber) {
    	MultiLabelInstance instance = (MultiLabelInstance) inst;
    	if (numAttribute == 0) {
    		this.indexClass = 0;
    		this.indexAttribute = 0;
    	}
        double valueAttribute;
        if (isNumber && this.instanceInformation.attribute(numAttribute).isNominal) {
            valueAttribute = this.instanceInformation.attribute(numAttribute).indexOfValue(Double.toString(value));
            //System.out.println(value +"/"+valueAttribute+" ");
                        
        } else {
            valueAttribute = value;
            //System.out.println(value +"/"+valueAttribute+" ");
        }
        if (this.range.isInRange(numAttribute)) {
            //instance.setClassValue(valueAttribute);
            //System.out.println(value +"<"+this.instanceInformation.classIndex()+">");
        	instance.setClassValue(this.indexClass, valueAttribute);
        	this.indexClass++;
        } else {
            instance.setValue(this.indexAttribute, valueAttribute);
            this.indexAttribute++;
        }
    }
    
}
