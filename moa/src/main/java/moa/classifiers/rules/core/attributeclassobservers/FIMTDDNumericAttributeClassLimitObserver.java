/*
 *    FIMTDDNumericAttributeClassLimitObserver.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.classifiers.rules.core.attributeclassobservers;

import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;

import com.github.javacliparser.IntOption;


public class FIMTDDNumericAttributeClassLimitObserver extends FIMTDDNumericAttributeClassObserver {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int maxNodes;
	public IntOption maxNodesOption = new IntOption("maxNodesOption", 'z', "Maximum number of nodes", 50, 0, Integer.MAX_VALUE);
    

	protected int numNodes;

	@Override
    public void observeAttributeClass(double attVal, double  classVal, double weight) {
        if (Double.isNaN(attVal)) { //Instance.isMissingValue(attVal)
        } else {
            if (this.root == null) {
        		maxNodes=maxNodesOption.getValue();
                this.root = new FIMTDDNumericAttributeClassLimitObserver.Node(attVal, classVal, weight);
            } else {
                this.root.insertValue(attVal, classVal, weight);
            }
        }
    }
    
    protected class Node extends FIMTDDNumericAttributeClassObserver.Node {
        /**
		 * 
		 */
		private static final long serialVersionUID = -4484141636424708465L;



		public Node(double val, double label, double weight) {
			super(val, label, weight);
		}

        protected Node root = null;
        


        /**
         * Insert a new value into the tree, updating both the sum of values and
         * sum of squared values arrays
         */
        @Override
        public void insertValue(double val, double label, double weight) {
           
		// If the new value equals the value stored in a node, update
		// the left (<=) node information
		if (val == this.cut_point) 
		{
		    this.leftStatistics.addToValue(0,1);
		    this.leftStatistics.addToValue(1,label);
		    this.leftStatistics.addToValue(2,label*label);
		} 
		// If the new value is less than the value in a node, update the
		// left distribution and send the value down to the left child node.
		// If no left child exists, create one
		else if (val < this.cut_point) {
			this.leftStatistics.addToValue(0,1);
			this.leftStatistics.addToValue(1,label);
			this.leftStatistics.addToValue(2,label*label);
			if (this.left == null) {
				if(numNodes<maxNodes){
					this.left = new Node(val, label, weight);
					++numNodes;
				}
			} else {
			    this.left.insertValue(val, label, weight);
			}
		} 
		// If the new value is greater than the value in a node, update the
		// right (>) distribution and send the value down to the right child node.
		// If no right child exists, create one
		else { // val > cut_point
			this.rightStatistics.addToValue(0,1);
			this.rightStatistics.addToValue(1,label);
			this.rightStatistics.addToValue(2,label*label);
			if (this.right == null) {
				if(numNodes<maxNodes){
					this.right = new Node(val, label, weight);
					++numNodes;
				}
			} else {
			    this.right.insertValue(val, label, weight);
			}
		}
        }
    }
}