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

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Class Instances.
 *
 * @author abifet
 */
public class Instances implements Serializable{

    /** The instance information. */
    protected InstanceInformation instanceInformation;
    /**
     * The instances.
     */
    protected List<Instance> instances;
    
    /** The arff. */
    protected ArffLoader arff;

    /**
     * Instantiates a new instances.
     *
     * @param modelContext the model context
     */
    /*public Instances(InstancesHeader modelContext) {
        throw new UnsupportedOperationException("Not yet implemented");
    }*/

    /**
     * Instantiates a new instances.
     *
     * @param chunk the chunk
     */
    public Instances(Instances chunk) {
        this.instanceInformation = chunk.instanceInformation();
        //this.relationName = chunk.relationName;
        //this.attributes = chunk.attributes;
        if (chunk.instances != null) {
            this.instances = chunk.instances;
        } else {
            this.instances = new ArrayList<Instance>();
        }
    }
    
    /**
     * Instantiates a new instances.
     */
    public Instances() {
        //this.instanceInformation = chunk.instanceInformation();
        //this.relationName = chunk.relationName;
        //this.attributes = chunk.attributes;
        //this.instances = chunk.instances;
    }

    /**
     * Instantiates a new instances.
     *
     * @param reader the reader
     * @param size the size
     * @param classAttribute the class attribute
     */
    public Instances(Reader reader, int size, int classAttribute) {
        arff = new ArffLoader(reader, 0, classAttribute);
        this.instanceInformation = arff.getStructure();
        this.instances = new ArrayList<Instance>();
    }

    /**
     * Instantiates a new instances.
     *
     * @param chunk the chunk
     * @param capacity the capacity
     */
    public Instances(Instances chunk, int capacity) {
        this(chunk);
    }

    /**
     * Instantiates a new instances.
     *
     * @param st the st
     * @param v the v
     * @param capacity the capacity
     */
    public Instances(String st, List<Attribute> v, int capacity) {
        this.instanceInformation = new InstanceInformation(st,v);
    }

    /**
     * Instantiates a new instances.
     *
     * @param chunk the chunk
     * @param i the i
     * @param j the j
     */
    public Instances(Instances chunk, int i, int j) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Instantiates a new instances.
     *
     * @param st the st
     * @param v the v
     */
    public Instances(StringReader st, int v) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //Information Instances
    /**
     * Sets the relation name.
     *
     * @param string the new relation name
     */
    public void setRelationName(String string) {
        this.instanceInformation.setRelationName(string);
    }

    /**
     * Gets the relation name.
     *
     * @return the relation name
     */
    public String getRelationName() {
        return this.instanceInformation.getRelationName();
    }

    
    /**
     * Class index.
     *
     * @return the int
     */
    public int classIndex() {
        return this.instanceInformation.classIndex();
    }

    /**
     * Sets the class index.
     *
     * @param classIndex the new class index
     */
    public void setClassIndex(int classIndex) {
        this.instanceInformation.setClassIndex(classIndex);
    }

    /**
     * Class attribute.
     *
     * @return the attribute
     */
    public Attribute classAttribute() {
        return this.instanceInformation.classAttribute();
    }

    /**
     * Num attributes.
     *
     * @return the int
     */
    public int numAttributes() {
        return this.instanceInformation.numAttributes();
    }

    /**
     * Attribute.
     *
     * @param w the w
     * @return the attribute
     */
    public Attribute attribute(int w) {
        return this.instanceInformation.attribute(w);
    }

    /**
     * Num classes.
     *
     * @return the int
     */
    public int numClasses() {
        return this.instanceInformation.numClasses();
    }

    /**
     * Delete attribute at.
     *
     * @param integer the integer
     */
    public void deleteAttributeAt(Integer integer) {
        this.instanceInformation.deleteAttributeAt(integer);
    }

    /**
     * Insert attribute at.
     *
     * @param attribute the attribute
     * @param i the i
     */
    public void insertAttributeAt(Attribute attribute, int i) {
        this.instanceInformation.insertAttributeAt(attribute, i);
    }

    //List of Instances
    /**
     * Instance.
     *
     * @param num the num
     * @return the instance
     */
    public Instance instance(int num) {
        return this.instances.get(num);
    }

    /**
     * Num instances.
     *
     * @return the int
     */
    public int numInstances() {
        return this.instances.size();
    }

    /**
     * Adds the.
     *
     * @param inst the inst
     */
    public void add(Instance inst) {
        this.instances.add(inst.copy());
    }

    /**
     * Randomize.
     *
     * @param random the random
     */
    public void randomize(Random random) {
        for (int j = numInstances() - 1; j > 0; j--) {
            swap(j, random.nextInt(j + 1));
        }
    }

    /**
     * Stratify.
     *
     * @param numFolds the num folds
     */
    public void stratify(int numFolds) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Train cv.
     *
     * @param numFolds the num folds
     * @param n the n
     * @param random the random
     * @return the instances
     */
    public Instances trainCV(int numFolds, int n, Random random) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Test cv.
     *
     * @param numFolds the num folds
     * @param n the n
     * @return the instances
     */
    public Instances testCV(int numFolds, int n) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /*  public Instances dataset() {
     throw new UnsupportedOperationException("Not yet implemented");
     }*/
    /**
     * Mean or mode.
     *
     * @param j the j
     * @return the double
     */
    public double meanOrMode(int j) {
        throw new UnsupportedOperationException("Not yet implemented"); //CobWeb
    }

    /**
     * Read instance.
     *
     * @param fileReader the file reader
     * @return true, if successful
     */
    public boolean readInstance(Reader fileReader) {

        //ArffReader arff = new ArffReader(reader, this, m_Lines, 1);
        Instance inst = arff.readInstance();
        if (inst != null) {
            inst.setDataset(this);
            add(inst);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Delete.
     */
    public void delete() {
        this.instances = new ArrayList<Instance>();
    }

    /**
     * Swap.
     *
     * @param i the i
     * @param j the j
     */
    public void swap(int i, int j) {
        Instance in = instances.get(i);
        instances.set(i, instances.get(j));
        instances.set(j, in);
    }

    /**
     * Instance information.
     *
     * @return the instance information
     */
    private InstanceInformation instanceInformation() {
        return this.instanceInformation;
    }
    
    public Attribute attribute(String name) {
    
    for (int i = 0; i < numAttributes(); i++) {
      if (attribute(i).name().equals(name)) {
	return attribute(i);
      }
    }
    return null;
  }
}
