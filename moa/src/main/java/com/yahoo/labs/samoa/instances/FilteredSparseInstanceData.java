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
 * The Class SparseInstanceData.
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
     * Value.
     *
     * @param indexAttribute the index attribute
     * @return the double
     */
    @Override
    public double value(int indexAttribute) {
        int location = locateIndex(indexAttribute);
        //return location == -1 ? 0 : this.attributeValues[location];
        //      int index = locateIndex(attIndex);
        if ((location >= 0) && (indexValues[location] == indexAttribute)) {
            return attributeValues[location];
        } else {
            return Double.NaN;
        }
    }
}
