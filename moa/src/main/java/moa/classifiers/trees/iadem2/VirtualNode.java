/*
 *    VirtualNode.java
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
import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import java.util.ArrayList;
import moa.classifiers.trees.iademutils.IademException;
import moa.core.DoubleVector;

public abstract class VirtualNode extends Node {
    private static final long serialVersionUID = 1L;
    protected int attIndex;
    
    protected boolean heuristicMeasureUpdated;
    
    protected IademAttributeSplitSuggestion bestSplitSuggestion = null;
    
    public VirtualNode(IADEM2cTree tree, Node parent, int attIndex) {
        super(tree, parent, new double[0]);
        this.attIndex = attIndex;
        this.heuristicMeasureUpdated = false;
    }

    public IademAttributeSplitSuggestion getBestSplitSuggestion() {
        return bestSplitSuggestion;
    }

    public int getAttIndex() {
        return attIndex;
    }
    

    @Override
    public int getSubtreeNodeCount() {
        return 0;
    } 
    
    @Override
    public ArrayList<LeafNode> getLeaves() {
        return new ArrayList<LeafNode>();
    }
    
    public abstract SplitNode getNewSplitNode(long newInstancesSeen,
            Node parent,
            IademAttributeSplitSuggestion bestSuggestion);
    
    public abstract void updateHeuristicMeasure() throws IademException;

    public abstract DoubleVector computeConditionalProbability(double value);

    public abstract double getPercent();

    public abstract boolean hasInformation();
    
    public double getHeuristicMeasureUpper() throws IademException {
        if (!this.heuristicMeasureUpdated) {
            updateHeuristicMeasure();
        }
        if (this.bestSplitSuggestion == null) {
            return -1;
        }
        return this.bestSplitSuggestion.merit;
    }
    
    public double getHeuristicMeasureLower() throws IademException {
        if (!this.heuristicMeasureUpdated) {
            updateHeuristicMeasure();
        }
        if (this.bestSplitSuggestion == null) {
            return -1;
        }
        return this.bestSplitSuggestion.getMeritLowerBound();
    }

    @Override
    public double[] getClassVotes(Instance inst) {
        return this.classValueDist.getArrayCopy();
    }
}
