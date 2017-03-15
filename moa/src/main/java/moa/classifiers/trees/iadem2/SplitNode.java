/*
 *    SplitNode.java
 *
 *    @author José del Campo-Ávila
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

package moa.classifiers.trees.iadem2;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.ArrayList;
import java.util.Arrays;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.core.AutoExpandVector;

public class SplitNode extends Node {
    private static final long serialVersionUID = 1L;
    
    public InstanceConditionalTest splitTest;
    
    public AutoExpandVector<Node> children = new AutoExpandVector<Node>();

    public SplitNode(IADEM2cTree tree,
            Node parent,
            Node[] children, 
            double[] initialClassCount,
            InstanceConditionalTest splitTest) {
        super(tree, parent, initialClassCount);
        this.splitTest = splitTest;
        this.setChildren(children);
    }

    public InstanceConditionalTest getSplitTest() {
        return splitTest;
    }
    
    public void setChild(Node child, int index) {
        if ((this.splitTest.maxBranches() >= 0)
                && (index >= this.splitTest.maxBranches())) {
            throw new IndexOutOfBoundsException();
        }
        this.children.set(index, child);
    }

    @Override
    public int getSubtreeNodeCount() {
        int count = 1;
        for (Node currentChild : this.children) {
            count += currentChild.getSubtreeNodeCount();
        }
        return count;
    }
    
    @Override
    public ArrayList<LeafNode> getLeaves() {
        ArrayList<LeafNode> leaves = new ArrayList<LeafNode>();
        for (Node currentChild : this.children) {
            leaves.addAll(currentChild.getLeaves());
        }
        return leaves;
    }
    
    public void changeChildren(Node oldChild,
            Node newChild) {
        boolean found = false;
        int pos = 0;
        while ((!found) && (pos < this.children.size())) {
            if (this.children.get(pos).equals(oldChild)) {
                found = true;
                this.children.set(pos, newChild);
            }
            pos++;
        }
    }

    public int instanceChildIndex(Instance inst) {
        return this.splitTest.branchForInstance(inst);
    }

    public Node getChild(int index) {
        return this.children.get(index);
    }

    final public void setChildren(Node[] children) {
        this.children.clear();
        if (children != null) {
            this.children.addAll(Arrays.asList(children));
        }
    }
    
    public void setChild(AutoExpandVector<Node> children) {
        this.children.clear();
        this.children.addAll(children);
    }

    @Override
    public Node learnFromInstance(Instance inst) {
        int childIndex = instanceChildIndex(inst);
        if (childIndex >= 0) {
            Node child = getChild(childIndex);
            if (child != null) {
                return child.learnFromInstance(inst);
            }
        }
        return null;
    }

    @Override
    public double[] getClassVotes(Instance inst) {
        int childIndex = instanceChildIndex(inst);
        // there is no missing value
        if (childIndex >= 0) {
            Node currentChild = getChild(childIndex);
            return currentChild.getClassVotes(inst);
        } else {
            return this.classValueDist.getArrayCopy();
        }
    }
    
    @Override
    public int getChildCount() {
        return this.children.size();
    }
    
    public void removeChild(Node child) {
        this.children.remove(child);
    }
    
    public void addChild(Node child) {
        this.children.add(child);
    }

    @Override
    public void getNumberOfNodes(int[] count) {
        count[0]++;
        for (Node child : children) {
            child.getNumberOfNodes(count);
        }
    }
}
