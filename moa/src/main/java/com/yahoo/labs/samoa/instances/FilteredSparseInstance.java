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
 * The Class FilteredSparseInstance.
 *
 * This class is an extension to the original SparseInstance.
 * It has been created to be used with feature selection
 * algorithms for data streams.
 * In contrast to SparseInstance objects, missing values are
 * represented as NaNs instead of 0s (zeros).
 * This allows learners to skip features with missing values.
 *
 * @author Jean Paul Barddal
 */
public class FilteredSparseInstance extends SparseInstance {

    /**
     * Instantiates a new sparse instance.
     *
     * @param d         the d
     * @param res       the res
     */
    public FilteredSparseInstance(double d, double[] res) {
        super(d, res);
    }


    /**
     * Instantiates a new sparse instance.
     *
     * @param inst the inst
     */
    public FilteredSparseInstance(InstanceImpl inst) {
        super(inst);
    }

    /**
     * Instantiates a new sparse instance.
     *
     * @param numberAttributes the number attributes
     */
    public FilteredSparseInstance(double numberAttributes) {
        super(numberAttributes);
    }

    /**
     * Instantiates a new sparse instance.
     *
     * @param weight           the weight
     * @param attributeValues  the attribute values
     * @param indexValues      the index values
     * @param numberAttributes the number attributes
     */
    public FilteredSparseInstance(double weight, double[] attributeValues, int[] indexValues, int numberAttributes) {
        super(numberAttributes);
        this.weight = weight;
        this.instanceData = new FilteredSparseInstanceData(attributeValues, indexValues, numberAttributes);
    }

}
