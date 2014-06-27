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
 * The Class SparseMultiLabelInstance.
 *
 * @author abifet
 */
public class SparseMultiLabelInstance extends MultiLabelInstance{
    
     /**
     * Instantiates a new sparse instance.
     *
     * @param d the d
     * @param res the res
     */
    public SparseMultiLabelInstance(double d, double[] res, double[] out) {
         super(d,res, out);
    }
    
     /**
     * Instantiates a new sparse instance.
     *
     * @param inst the inst
     */
    public SparseMultiLabelInstance(MultiLabelInstance inst) {
        super(inst);
    }

     /**
     * Instantiates a new sparse instance.
     *
     * @param numberAttributes the number attributes
     */
    public SparseMultiLabelInstance(double numberAttributes, double numberOutputAttributes) {
      //super(1, new double[(int) numberAttributes-1]); 
      super(1,null,null,(int) numberAttributes, null, null, (int) numberOutputAttributes);  
    }
    
     /**
     * Instantiates a new sparse instance.
     *
     * @param weight the weight
     * @param attributeValues the attribute values
     * @param indexValues the index values
     * @param numberAttributes the number attributes
     */
    public SparseMultiLabelInstance(double weight, double[] attributeValues, int[] indexValues, int numberAttributes, double[] outputAttributeValues, int[] outputIndexValues, int outputNumberAttributes) {
           super(weight,attributeValues,indexValues,outputNumberAttributes, outputAttributeValues,indexValues,outputNumberAttributes);
    }
    
}
