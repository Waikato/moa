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
 * The Class DenseInstance.
 */
public class DenseMultiLabelInstance extends MultiLabelInstance {

    /**
	 * 
	 */
	private static final long serialVersionUID = -615967841180047599L;

	/**
     * Instantiates a new dense instance.
     *
     * @param weight the weight
     * @param res the res
     */
    public DenseMultiLabelInstance(double weight, double[] res, double[] out) {
         super(weight,res, out);
    }
    
    /**
     * Instantiates a new dense instance.
     *
     * @param inst the inst
     */
    public DenseMultiLabelInstance(MultiLabelInstance inst) {
        super(inst);
    }
    
    /**
     * Instantiates a new dense instance.
     *
     * @param inst the inst
     */
    public DenseMultiLabelInstance(Instance inst) {
        super((MultiLabelInstance) inst);
    }
    
    /**
     * Instantiates a new dense instance.
     *
     * @param numberAttributes the number attributes
     */
    public DenseMultiLabelInstance(double numberAttributes, double numberOutputAttributes) {
         super((int) numberAttributes, (int) numberOutputAttributes);
    }
}
