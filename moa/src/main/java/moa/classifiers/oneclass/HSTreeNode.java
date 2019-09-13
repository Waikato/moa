/*
 *    HSTreeNode.java
 *    Copyright (C) 2018
 *    @author Richard Hugh Moulton
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
 */

package moa.classifiers.oneclass;

import java.io.Serializable;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * A node in an HSTree. Based on the work S. C. Tan, K. M. Ting, and T. F. Liu,
 * “Fast anomaly detection for streaming data,” in IJCAI Proceedings-International
 * Joint Conference on Artificial Intelligence, 2011, vol. 22, no. 1, pp. 1511–1516.
 * 
 * Made use of by HSTrees.java.
 * 
 * @author Richard Hugh Moulton
 *
 */
public class HSTreeNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * The left/right nodes pointed to by this node.
	 */
	private HSTreeNode left, right;
	
	/**
	 * The randomly chosen attribute that this node splits on. In the paper this is 'q'.
	 */
	private int splitAttribute;
	/**
	 * The mass profile of the reference window.
	 */
	private int r;
	/**
	 * The mass profile of the latest window.
	 */
	private int l;
	/**
	 * The dimensionality of the data stream.
	 */
	private int dimensions;
	/**
	 * The depth of this node in the HSTree. The root node is at depth 1.
	 */
	private int depth;
	/**
	 * The midpoint of the split attribute in the workspace of the node.
	 * @see splitAttribute
	 */
	private double splitValue;
	/**
	 * If this node is an internal node: <b>true</b>, if it is a leaf node: <b>false</b>.
	 */
	private boolean internalNode;
	
	/**
	 * Constructor for an HSTreeNode.
	 * 
	 * @param min the minimum values of the attributes for this node's workspace
	 * @param max the maximum values of the attributes for this node's workspace
	 * @param k the depth of this node in the HSTree
	 * @param maxDepth the maximum depth of this HSTree
	 */
	public HSTreeNode(double[] min, double[] max, int k, int maxDepth)
	{
		this.r = 0;
		this.l = 0;
		this.dimensions = min.length;
		this.depth = k;
		
		//If this node is not at the maximum depth level, then create two subordinate nodes.
		if((depth < maxDepth))
		{
			this.internalNode = true;
			this.splitAttribute = (int) Math.floor((Math.random() * (this.dimensions)));
			this.splitValue = (min[this.splitAttribute] + max[this.splitAttribute]) / 2.0;
			
			double temp = max[this.splitAttribute];
			max[this.splitAttribute] = this.splitValue;
			this.left = new HSTreeNode(min, max, k+1, maxDepth);
			
			max[this.splitAttribute] = temp;
			min[this.splitAttribute] = this.splitValue;
			this.right = new HSTreeNode(min, max, k+1, maxDepth);
		}
		// If this node is a leaf node, mark it as such.
		else
		{
			this.splitAttribute = -1;
			this.internalNode = false;
		}
	}

	/**
	 * Update the mass profile of this node.
	 * 
	 * @param inst the instance being passed through the HSTree.
	 * @param referenceWindow if the HSTree is in the initial reference window: <b>true</b>, else: <b>false</b>
	 */
	public void updateMass(Instance inst, boolean referenceWindow)
	{
		if(referenceWindow)
			r++;
		else
			l++;
		
		if(internalNode)
		{
			if(inst.value(this.splitAttribute) > this.splitValue)
				right.updateMass(inst, referenceWindow);
			else
				left.updateMass(inst, referenceWindow);
		}
	}
	
	/**
	 * Update the node's model by setting the latest window's mass profile as the reference window's mass profile,
	 * resetting the latest window's mass profile to zero and updating any subordinates nodes' models.
	 */
	public void updateModel()
	{
		this.r = this.l;
		this.l = 0;
		
		if(internalNode)
		{
			left.updateModel();
			right.updateModel();
		}
	}

	/**
	 * If this node is a leaf node or it has a mass profile of less than sizeLimit, this returns the anomaly score for the argument instance.
	 * Otherwise this node determines which of its subordinate nodes the argument instance belongs to and asks it provide the anomaly score.
	 * 
	 * @param inst the instance being passed through the tree
	 * @param sizeLimit the minimum mass profile for a node to calculate the argument instance's anomaly score
	 * 
	 * @return the argument instance's anomaly score (r * 2^depth)
	 */
	public double score(Instance inst, int sizeLimit)
	{
		double anomalyScore = 0.0;
		
		if(this.internalNode && this.r > sizeLimit)
		{
			if(inst.value(this.splitAttribute) > this.splitValue)
				anomalyScore = right.score(inst, sizeLimit);
			else
				anomalyScore = left.score(inst, sizeLimit);
		}
		else
		{
			anomalyScore = this.r * Math.pow(2.0, this.depth);
		}
		
		return anomalyScore;
	}
	
	/**
	 * Prints this node to string and, if it is an internal node, prints its children nodes as well.
	 * Useful for debugging the entire tree structure.
	 */
	protected void printNode()
	{
		System.out.println(this.depth+", "+this.splitAttribute+", "+this.splitValue+", "+this.r);
		
		if(this.internalNode)
		{
			this.right.printNode();
			this.left.printNode();
		}
	}
}
