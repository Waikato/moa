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

import java.util.ArrayList;
import java.io.Serializable;

/**
 * The Class SamoaToWekaInstanceConverter.
 *
 * @author abifet
 */
public class SamoaToWekaInstanceConverter implements Serializable {

    /** The weka instance information. */
    protected weka.core.Instances wekaInstanceInformation;

     /**
     * Weka instance.
     *
     * @param inst the inst
     * @return the weka.core. instance
     */
    public weka.core.Instance wekaInstance(Instance inst) {
        weka.core.Instance wekaInstance;
        if (((InstanceImpl) inst).instanceData instanceof SparseInstanceData) {
            InstanceImpl instance = (InstanceImpl) inst;
            SparseInstanceData sparseInstanceData = (SparseInstanceData) instance.instanceData;
            wekaInstance = new weka.core.SparseInstance(instance.weight(), sparseInstanceData.getAttributeValues(),
                    sparseInstanceData.getIndexValues(), sparseInstanceData.getNumberAttributes());
            /*if (this.wekaInstanceInformation == null) {
                this.wekaInstanceInformation = this.wekaInstancesInformation(inst.dataset());
            }
            wekaInstance.insertAttributeAt(inst.classIndex());
            wekaInstance.setDataset(wekaInstanceInformation);
            wekaInstance.setClassValue(inst.classValue());
            //wekaInstance.setValueSparse(wekaInstance.numAttributes(), inst.classValue());*/
        } else {
            Instance instance = inst;
            wekaInstance = new weka.core.DenseInstance(instance.weight(), instance.toDoubleArray());
           /* if (this.wekaInstanceInformation == null) {
                this.wekaInstanceInformation = this.wekaInstancesInformation(inst.dataset());
            }
            //We suppose that the class is the last attibute. We should deal when this is not the case
            wekaInstance.insertAttributeAt(inst.classIndex());
            wekaInstance.setDataset(wekaInstanceInformation);
            wekaInstance.setClassValue(inst.classValue());*/
        }
        if (this.wekaInstanceInformation == null) {
            this.wekaInstanceInformation = this.wekaInstancesInformation(inst.dataset());
        }
        //wekaInstance.insertAttributeAt(inst.classIndex());
        wekaInstance.setDataset(wekaInstanceInformation);
        if (inst.numOutputAttributes() == 1){
            wekaInstance.setClassValue(inst.classValue());
        }
        
        return wekaInstance;
    }
    
     /**
     * Weka instances.
     *
     * @param instances the instances
     * @return the weka.core. instances
     */
    public weka.core.Instances wekaInstances(Instances instances) {
        weka.core.Instances wekaInstances = wekaInstancesInformation(instances);
        //We assume that we have only one WekaInstanceInformation for SamoaToWekaInstanceConverter
        this.wekaInstanceInformation = wekaInstances;
        for (int i = 0; i < instances.numInstances(); i++) {
            wekaInstances.add(wekaInstance(instances.instance(i)));
        }
        return wekaInstances;
    }

     /**
     * Weka instances information.
     *
     * @param instances the instances
     * @return the weka.core. instances
     */
    public weka.core.Instances wekaInstancesInformation(Instances instances) {
        weka.core.Instances wekaInstances;
        ArrayList<weka.core.Attribute> attInfo = new ArrayList<weka.core.Attribute>();
        for (int i = 0; i < instances.numAttributes(); i++) {
            attInfo.add(wekaAttribute(i, instances.attribute(i)));
        }
        wekaInstances = new weka.core.Instances(instances.getRelationName(), attInfo, 0);
        if (instances.instanceInformation.numOutputAttributes() == 1){
            wekaInstances.setClassIndex(instances.classIndex());
        }
        else {
            //Assign a classIndex to a MultiLabel instance for compatibility reasons
            wekaInstances.setClassIndex(instances.instanceInformation.numOutputAttributes()-1); //instances.numAttributes()-1); //Last
        }
        //System.out.println(attInfo.get(3).name());
        //System.out.println(attInfo.get(3).isNominal());
        //System.out.println(wekaInstances.attribute(3).name());
        //System.out.println(wekaInstances.attribute(3).isNominal());
        return wekaInstances;
    }

     /**
     * Weka attribute.
     *
     * @param index the index
     * @param attribute the attribute
     * @return the weka.core. attribute
     */
    protected weka.core.Attribute wekaAttribute(int index, Attribute attribute) {
        weka.core.Attribute wekaAttribute;
        if (attribute.isNominal()) {
            wekaAttribute = new weka.core.Attribute(attribute.name(), attribute.getAttributeValues(), index);
          
        } else {
            wekaAttribute = new weka.core.Attribute(attribute.name(), index);
        }
        //System.out.println(wekaAttribute.name());
        //System.out.println(wekaAttribute.isNominal());
        return wekaAttribute;
    }
}
