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
 * The Class FilteredSparseInstanceData.
 *
 * This class is an extension to the original SparseInstanceData.
 * It has been created to be used with feature selection
 * algorithms for data streams.
 * In contrast to SparseInstanceData objects, missing values are
 * represented as NaNs instead of 0s (zeros).
 * This allows learners to skip features with missing values.
 *
 * @author Jean Paul Barddal
 */
public class FilteredSparseInstanceData extends SparseInstanceData {


    /**
     * Instantiates a new sparse instance data.
     *
     * @param attributeValues  the attribute values
     * @param indexValues      the index values
     * @param numberAttributes the number attributes
     */
    public FilteredSparseInstanceData(double[] attributeValues, int[] indexValues, int numberAttributes) {
        super(attributeValues, indexValues, numberAttributes);
    }


    /**
     * Value of the attribute in the indexAttribute position.
     * If this value is absent, a NaN value (marker of missing value)
     * is returned, otherwise this method returns the actual value.
     *
     * @param indexAttribute the index attribute
     * @return the double
     */
    @Override
    public double value(int indexAttribute) {
        int location = locateIndex(indexAttribute);
        if ((location >= 0) && (indexValues[location] == indexAttribute)) {
            return attributeValues[location];
        } else {
            // returns a NaN value which represents missing values instead of a 0 (zero)
            return Double.NaN;
        }
    }
}
