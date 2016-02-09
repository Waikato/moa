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

/**
 * Class for storing the header or context of a data stream. It allows to know
 * the number of attributes and classes.
 *
 * @version $Revision: 7 $
 */
public class InstancesHeader extends Instances {

    private static final long serialVersionUID = 1L;

    public InstancesHeader(Instances i) {
        super(i, 0);
    }

    public InstancesHeader() {
        super();
    }

    public static String getClassNameString(InstancesHeader context) {
        if (context == null) {
            return "[class]";
        }
        return "[class:" + context.classAttribute().name() + "]";
    }

    public static String getClassLabelString(InstancesHeader context,
            int classLabelIndex) {
        if ((context == null) || (classLabelIndex >= context.numClasses())) {
            return "<class " + (classLabelIndex + 1) + ">";
        }
        return "<class " + (classLabelIndex + 1) + ":"
                + context.classAttribute().value(classLabelIndex) + ">";
    }

    // is impervious to class index changes - attIndex is true attribute index
    // regardless of class position
    public static String getAttributeNameString(InstancesHeader context,
            int attIndex) {
        if ((context == null) || (attIndex >= context.numAttributes())) {
            return "[att " + (attIndex + 1) + "]";
        }
        int instAttIndex = attIndex < context.classIndex() ? attIndex
                : attIndex + 1;
        return "[att " + (attIndex + 1) + ":"
                + context.attribute(instAttIndex).name() + "]";
    }

    public static String getInputAttributeNameString(InstancesHeader context,
                                                int attIndex) {
        if ((context == null) || (attIndex >= context.numInputAttributes())) {
            return "[att " + (attIndex + 1) + "]";
        }
        int instAttIndex = attIndex;
        return "[att " + (attIndex + 1) + ":"
                + context.inputAttribute(instAttIndex).name() + "]";
    }

    // is impervious to class index changes - attIndex is true attribute index
    // regardless of class position
    public static String getNominalValueString(InstancesHeader context,
            int attIndex, int valIndex) {
        if (context != null) {
            int instAttIndex = attIndex < context.classIndex() ? attIndex
                    : attIndex + 1;
            if ((instAttIndex < context.numAttributes())
                    && (valIndex < context.attribute(instAttIndex).numValues())) {
                return "{val " + (valIndex + 1) + ":"
                        + context.attribute(instAttIndex).value(valIndex) + "}";
            }
        }
        return "{val " + (valIndex + 1) + "}";
    }

    // is impervious to class index changes - attIndex is true attribute index
    // regardless of class position
    public static String getNumericValueString(InstancesHeader context,
            int attIndex, double value) {
        if (context != null) {
            int instAttIndex = attIndex < context.classIndex() ? attIndex
                    : attIndex + 1;
            if (instAttIndex < context.numAttributes()) {
                if (context.attribute(instAttIndex).isDate()) {
                    return context.attribute(instAttIndex).formatDate(value);
                }
            }
        }
        return Double.toString(value);
    }

    public Attribute inputAttribute(int w) {
        return this.instanceInformation.inputAttribute(w);
    }

    public Attribute outputAttribute(int w) {
        return this.instanceInformation.outputAttribute(w);
    }

    public int numInputAttributes() {
        return this.instanceInformation.numInputAttributes();
    }

    public int numOutputAttributes() {
        return this.instanceInformation.numOutputAttributes();
    }

    public InstanceInformation getInstanceInformation() {
        return this.instanceInformation;
    }
}
