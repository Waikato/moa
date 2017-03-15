/*
 *    Node.java
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
import java.io.Serializable;
import java.util.ArrayList;
import moa.core.DoubleVector;

/**
 * @author José del Campo-Ávila
 *
 */
public abstract class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    protected IADEM2cTree tree;
    
    protected DoubleVector classValueDist;

    public Node parent;

    public DoubleVector getClassValueDist() {
        return classValueDist;
    }

    public void setClassValueDist(DoubleVector classValueDist) {
        this.classValueDist = classValueDist;
    }

    public IADEM2cTree getTree() {
        return tree;
    }

    public void setTree(IADEM2cTree tree) {
        this.tree = tree;
    }

    public Node(IADEM2cTree tree,
            Node parent,
            double[] initialClassCount) {
        this.tree = tree;
        this.parent = parent;
        this.classValueDist = new DoubleVector(initialClassCount);
    }

    public abstract int getSubtreeNodeCount();

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    public abstract Node learnFromInstance(Instance instance);

    public abstract ArrayList<LeafNode> getLeaves();

    public abstract double[] getClassVotes(Instance instance);

    public int getChildCount() {
        return 0;
    }

    public abstract void getNumberOfNodes(int[] count);
}
