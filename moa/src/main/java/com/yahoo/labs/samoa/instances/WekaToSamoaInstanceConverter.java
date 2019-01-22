/* 
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
import java.util.Enumeration;
import java.util.List;

/**
 * The Class WekaToSamoaInstanceConverter.
 *
 * @author abifet
 */
public class WekaToSamoaInstanceConverter implements Serializable{

    protected Instances samoaInstanceInformation;
    
    /**
     * Samoa instance from weka instance.
     *
     * @param inst the inst
     * @return the instance
     */
    public Instance samoaInstance(weka.core.Instance inst) {
        Instance samoaInstance;
        if (inst instanceof weka.core.SparseInstance) {
            double[] attributeValues = new double[inst.numValues()];
            int[] indexValues = new int[inst.numValues()];
            for (int i = 0; i < inst.numValues(); i++) {
                if (inst.index(i) != inst.classIndex()) {
                    attributeValues[i] = inst.valueSparse(i);
                    indexValues[i] = inst.index(i);
                }
            }
            samoaInstance = new SparseInstance(inst.weight(), attributeValues,
                    indexValues, inst.numAttributes());
        } else {
            samoaInstance = new DenseInstance(inst.weight(), inst.toDoubleArray());
            //samoaInstance.deleteAttributeAt(inst.classIndex());
        }
        if (this.samoaInstanceInformation == null) {
            this.samoaInstanceInformation = this.samoaInstancesInformation(inst.dataset());
        }
        samoaInstance.setDataset(samoaInstanceInformation);

        if(inst.classIndex() >= 0) { // class attribute is present
            samoaInstance.setClassValue(inst.classValue());
        }
        
        return samoaInstance;
    }

     /**
     * Samoa instances from weka instances.
     *
     * @param instances the instances
     * @return the instances
     */
    public Instances samoaInstances(weka.core.Instances instances) {
        Instances samoaInstances = samoaInstancesInformation(instances);
        //We assume that we have only one samoaInstanceInformation for WekaToSamoaInstanceConverter
        this.samoaInstanceInformation = samoaInstances;
        for (int i = 0; i < instances.numInstances(); i++) {
            samoaInstances.add(samoaInstance(instances.instance(i)));
        }
        return samoaInstances;
    }

     /**
     * Samoa instances information.
     *
     * @param instances the instances
     * @return the instances
     */
    public Instances samoaInstancesInformation(weka.core.Instances instances) {
        Instances samoaInstances;
        List<Attribute> attInfo = new ArrayList<Attribute>();
        for (int i = 0; i < instances.numAttributes(); i++) {
            attInfo.add(samoaAttribute(i, instances.attribute(i)));
        }
        samoaInstances = new Instances(instances.relationName(), attInfo, 0);
        
        if(instances.classIndex() >= 0) { // class attribute is present
            samoaInstances.setClassIndex(instances.classIndex());
        }
        
        return samoaInstances;
    }

    
     /**
     * Get Samoa attribute from a weka attribute.
     *
     * @param index the index
     * @param attribute the attribute
     * @return the attribute
     */
    protected Attribute samoaAttribute(int index, weka.core.Attribute attribute) {
        Attribute samoaAttribute;
        if (attribute.isNominal()) {
            Enumeration enu = attribute.enumerateValues();
            List<String> attributeValues = new ArrayList<String>();
            while (enu.hasMoreElements()) {
                attributeValues.add((String) enu.nextElement());
            }
            samoaAttribute = new Attribute(attribute.name(), attributeValues);
        } else {
            samoaAttribute = new Attribute(attribute.name());
        }
        return samoaAttribute;
    }
}
