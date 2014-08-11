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

/**
 * The Class SparseInstance.
 *
 * @author abifet
 */
public class SparseInstance extends InstanceImpl {

    /**
     * Instantiates a new sparse instance.
     *
     * @param d the d
     * @param res the res
     */
    public SparseInstance(double d, double[] res) {
        super(d, res);
    }

    /**
     * Instantiates a new sparse instance.
     *
     * @param inst the inst
     */
    public SparseInstance(InstanceImpl inst) {
        super(inst);
    }

    /**
     * Instantiates a new sparse instance.
     *
     * @param numberAttributes the number attributes
     */
    public SparseInstance(double numberAttributes) {
        super(1, null, null, (int) numberAttributes);
    }

    /**
     * Instantiates a new sparse instance.
     *
     * @param weight the weight
     * @param attributeValues the attribute values
     * @param indexValues the index values
     * @param numberAttributes the number attributes
     */
    public SparseInstance(double weight, double[] attributeValues, int[] indexValues, int numberAttributes) {
        super(weight, attributeValues, indexValues, numberAttributes);
    }

}
