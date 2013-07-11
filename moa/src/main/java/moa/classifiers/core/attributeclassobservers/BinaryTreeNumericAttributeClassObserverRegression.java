/*
 *    BinaryTreeNumericAttributeClassObserverRegression.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author E. Almeida, J. Gama
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 *    
 */

package moa.classifiers.core.attributeclassobservers;

import java.io.Serializable;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Class for observing the class data distribution for a numeric attribute using a binary tree.
 * This observer monitors the class distribution of a given attribute.
 *
 * <p>Learning Adaptive Model Rules from High-Speed Data Streams, ECML 2013, E. Almeida, C. Ferreira, P. Kosina and J. Gama; </p>
 *
 * @author E. Almeida, J. Gama
 * @version $Revision: 2$
 */
public class BinaryTreeNumericAttributeClassObserverRegression extends AbstractOptionHandler
        implements NumericAttributeClassObserver {

    public static final long serialVersionUID = 1L;
	
        public class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        public double cut_point;
        
        public double[] lessThan; //This array maintains statistics for the instance reaching the node with attribute values less than or iqual to the cutpoint.
        
        public double[] greaterThan; //This array maintains statistics for the instance reaching the node with attribute values greater than to the cutpoint.

        public Node left;

        public Node right;

        public Node(double val, double target) {
            this.cut_point = val;
            this.lessThan = new double[3];
            this.greaterThan = new double[3];
            this.lessThan[0] = target; //The sum of their target attribute values.
            this.lessThan[1] = target * target; //The sum of the squared target attribute values.
            this.lessThan[2] = 1.0; //A counter of the number of instances that have reached the node.
            this.greaterThan[0] = 0.0;
            this.greaterThan[1] = 0.0;
            this.greaterThan[2] = 0.0;
      }

        public void insertValue(double val, double target) {
            if (val == this.cut_point) {
            	 this.lessThan[0] = this.lessThan[0] + target;
                 this.lessThan[1] = this.lessThan[1] + (target * target);
                 this.lessThan[2] = this.lessThan[2] + 1;
            } else if (val <= this.cut_point) {
            	 this.lessThan[0] = this.lessThan[0] + target;
                 this.lessThan[1] = this.lessThan[1] + (target * target);
                 this.lessThan[2] = this.lessThan[2] + 1;
                if (this.left == null) {
                    this.left = new Node(val, target);
                } else {
                    this.left.insertValue(val, target);
                }
            } else {
            	 this.greaterThan[0] = this.greaterThan[0] + target;
                 this.greaterThan[1] = this.greaterThan[1] + (target*target);
                 this.greaterThan[2] = this.greaterThan[2] + 1;
                if (this.right == null) {
                	
                   this.right = new Node(val, target);
                } else {
                   this.right.insertValue(val, target);
                }
            }
        }
    }

    public Node root1 = null;
    
    public void observeAttributeTarget(double attVal, double target){
    	 if (Double.isNaN(attVal)) {
         } else {
             if (this.root1 == null) {
                 this.root1 = new Node(attVal, target);
             } else {
                 this.root1.insertValue(attVal, target);
             }
         }
    }

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
       
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal,
            int classVal) {
        // TODO: NaiveBayes broken until implemented
        return 0.0;
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
            SplitCriterion criterion, double[] preSplitDist, int attIndex,
            boolean binaryOnly) {
        return searchForBestSplitOption(this.root1, null, null, null, null, false,
                criterion, preSplitDist, attIndex);
    }

    protected AttributeSplitSuggestion searchForBestSplitOption(
            Node currentNode, AttributeSplitSuggestion currentBestOption,
            double[] actualParentLeft,
            double[] parentLeft, double[] parentRight, boolean leftChild,
            SplitCriterion criterion, double[] preSplitDist, int attIndex) {
       
        return currentBestOption;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}

