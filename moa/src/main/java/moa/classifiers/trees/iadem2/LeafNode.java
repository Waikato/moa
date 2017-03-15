/*
 *    LeafNode.java
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

import moa.classifiers.trees.iademutils.IademAttributeSplitSuggestion;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import moa.classifiers.trees.iademutils.IademException;
import moa.core.AutoExpandVector;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;

public class LeafNode extends Node {

    private static final long serialVersionUID = 1L;
    protected long instNodeCountSinceVirtual;
    protected long instTreeCountSinceReal;
    protected long instNodeCountSinceReal;
    protected AutoExpandVector<VirtualNode> virtualChildren = new AutoExpandVector<>();
    protected boolean allAttUsed;

    protected double instSeenSinceLastSplitAttempt = 0;

    protected boolean split;

    public LeafNode(IADEM2cTree tree,
            Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] initialClassCount,
            IademNumericAttributeObserver numericAttClassObserver,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        super(tree, parent, initialClassCount);
        this.instNodeCountSinceVirtual = instNodeCountSinceVirtual;
        this.instTreeCountSinceReal = 0;
        this.instNodeCountSinceReal = 0;
        this.split = true;
        createVirtualNodes(numericAttClassObserver,
                onlyMultiwayTest,
                onlyBinaryTest);
    }

    public double getInstSeenSinceLastSplitAttempt() {
        return instSeenSinceLastSplitAttempt;
    }

    public void setInstSeenSinceLastSplitAttempt(double instSeenSinceLastSplitAttempt) {
        this.instSeenSinceLastSplitAttempt = instSeenSinceLastSplitAttempt;
    }

    public AutoExpandVector<VirtualNode> getVirtualChildren() {
        return this.virtualChildren;
    }

    public void setVirtualChildren(AutoExpandVector<VirtualNode> virtualChildren) {
        this.virtualChildren = virtualChildren;
    }

    protected void createVirtualNodes(IademNumericAttributeObserver numericObserver,
            boolean onlyMultiwayTest,
            boolean onlyBinaryTest) {
        for (int i = 0; i < this.tree.getProblemDescription().numAttributes(); i++) {
            if (this.tree.getProblemDescription().classIndex() != i
                    && this.tree.getProblemDescription().attribute(i).isNominal()) {
                this.virtualChildren.set(i, new NominalVirtualNode(this.tree,
                        this,
                        i,
                        onlyMultiwayTest,
                        onlyBinaryTest));
            } else if (this.tree.getProblemDescription().classIndex() != i
                    && this.tree.getProblemDescription().attribute(i).isNumeric()) {
                this.virtualChildren.set(i, new NumericVirtualNode(this.tree,
                        this,
                        i,
                        numericObserver));

            } else { // class attribute
                this.virtualChildren.set(i, null);
            }
        }

    }

    protected ArrayList<Integer> nominalAttUsed() {
        SplitNode currentNode = (SplitNode) this.parent;
        ArrayList<Integer> nomAttUsed = new ArrayList<Integer>();
        while (currentNode != null) {
            if (this.tree.problemDescription.attribute(currentNode.splitTest.getAttsTestDependsOn()[0]).isNominal()) {
                nomAttUsed.add(currentNode.splitTest.getAttsTestDependsOn()[0]);
            }
            currentNode = (SplitNode) currentNode.parent;
        }
        return nomAttUsed;
    }

    @Override
    public IADEM2cTree getTree() {
        return this.tree;
    }

    @Override
    public int getSubtreeNodeCount() {
        return 1;
    }

    @Override
    public ArrayList<LeafNode> getLeaves() {
        ArrayList<LeafNode> leaf = new ArrayList<LeafNode>();
        leaf.add(this);
        return leaf;
    }

    public boolean isAllAttUsed() {
        return allAttUsed;
    }

    public void attemptToSplit() {
        if (this.classValueDist.numNonZeroEntries() > 1) {
            if (hasInformationToSplit()) {
                try {
                    this.instSeenSinceLastSplitAttempt = 0;
                    IademAttributeSplitSuggestion bestSplitSuggestion;

                    bestSplitSuggestion = getBestSplitSuggestion();
                    if (bestSplitSuggestion != null) {
                        doSplit(bestSplitSuggestion);
                    }
                } catch (IademException ex) {
                    Logger.getLogger(LeafNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public Node learnFromInstance(Instance inst) {
        this.instNodeCountSinceVirtual += inst.weight();
        this.classValueDist.addToValue((int) inst.value(inst.classIndex()), inst.weight());
        this.instNodeCountSinceReal += inst.weight();
        this.instSeenSinceLastSplitAttempt += inst.weight();
        for (int i = 0; i < inst.numAttributes() - 1; i++) {
            VirtualNode virtual = this.virtualChildren.get(i);
            if (virtual != null) {
                virtual.learnFromInstance(inst);
            }
        }
        if (this.split) {
            attemptToSplit();
        }
        return this;
    }

    protected IademAttributeSplitSuggestion getFastSplitSuggestion() throws IademException {
        int bestAttIndex = -1;
        double bestAttValue = Double.MAX_VALUE;
        for (int i = 0; i < virtualChildren.size(); i++) {
            VirtualNode currentVirtualChild = virtualChildren.get(i);
            if (currentVirtualChild != null) {
                try {
                    currentVirtualChild.updateHeuristicMeasure();
                } catch (IademException e) {
                    throw new IademException("LeafNode", "getFastSplitSuggestion",
                            "Problems when updating measures: \n"
                            + e.getMessage());
                }
                if (currentVirtualChild.getHeuristicMeasureUpper() >= 0) {
                    if (currentVirtualChild.getHeuristicMeasureUpper() < bestAttValue) {
                        bestAttIndex = i;
                        bestAttValue = currentVirtualChild.getHeuristicMeasureUpper();
                    }
                }
            }
        }
        if (bestAttIndex != -1) {
            return virtualChildren.get(bestAttIndex).getBestSplitSuggestion();
        } else {
            return null;
        }
    }

    public IademAttributeSplitSuggestion getBestSplitSuggestion() throws IademException {
        return getBestSplitSuggestionIADEM();
    }

    public LeafNode[] doSplit(IademAttributeSplitSuggestion bestSuggestion) {
        SplitNode splitNode = virtualChildren.get(bestSuggestion.splitTest.getAttsTestDependsOn()[0]).getNewSplitNode(
                this.instTreeCountSinceReal,
                this.parent,
                bestSuggestion);
        splitNode.setParent(this.parent);

        if (this.parent == null) {
            this.tree.setTreeRoot(splitNode);
        } else {
            ((SplitNode) this.parent).changeChildren(this, splitNode);
        }

        this.tree.newSplit(splitNode.getLeaves().size());
        return null;
    }

    @Override
    public double[] getClassVotes(Instance obs) {
        return getMajorityClassVotes();
    }

    public double[] getMajorityClassVotes() {
        InstancesHeader descrProbl = this.tree.problemDescription;
        double[] votes = new double[descrProbl.attribute(descrProbl.classIndex()).numValues()];
        Arrays.fill(votes, 0.0);
        if (instNodeCountSinceVirtual == 0) {
            if (parent != null) {
                ArrayList<LeafNode> siblings = parent.getLeaves();
                siblings.remove(this);
                ArrayList<LeafNode> siblingWithInfo = new ArrayList<LeafNode>();
                long count = 0;
                for (LeafNode currentSibling : siblings) {
                    if (currentSibling.getInstNodeCountSinceVirtual() > 0) {
                        siblingWithInfo.add(currentSibling);
                        count += currentSibling.getInstNodeCountSinceVirtual();
                    }
                }
                if (count > 0) {
                    for (LeafNode currentSibling : siblingWithInfo) {
                        double[] sibVotes = currentSibling.getMajorityClassVotes();
                        double weight = (double) currentSibling.getInstNodeCountSinceVirtual() / (double) count;
                        for (int i = 0; i < votes.length; i++) {
                            votes[i] += weight * sibVotes[i];
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < votes.length; i++) {
                votes[i] = classValueDist.getValue(i) / (double) instNodeCountSinceVirtual;
            }
        }
        // normalize votes
        double voteCount = 0.0;
        for (int i = 0; i < votes.length; i++) {
            voteCount += votes[i];
        }
        if (voteCount == 0.0) {
            for (int i = 0; i < votes.length; i++) {
                votes[i] = 1.0 / (double) votes.length;
            }
        } else {
            for (int i = 0; i < votes.length; i++) {
                votes[i] /= voteCount;
            }
        }
        return votes;
    }

    public long getInstNodeCountSinceVirtual() {
        return instNodeCountSinceVirtual;
    }

    private double percentInCommon(double A_upper, double A_lower, double B_upper,
            double B_lower) {
        double percent;

        // nothing in common
        if ((A_lower >= B_upper) || (A_upper <= B_lower)) {
            percent = 0.0;
        } else { // something in common
            double A_out = 0.0;
            double A_margin = A_upper - A_lower;

            if ((A_upper <= B_upper) && (A_lower >= B_lower)) {
                A_out = 0.0;
            } else if ((A_upper > B_upper) && (A_lower >= B_lower)) {
                A_out = A_upper - B_upper;
            } else if ((A_upper > B_upper) && (A_lower < B_lower)) {
                A_out = (A_upper - B_upper) + (B_lower - A_lower);
            } else if ((A_upper <= B_upper) && (A_lower < B_lower)) {
                A_out = B_lower - A_lower;
            } else {
                // TODO remove next line
                System.out.println("Something is wrong");
            }
            percent = (A_margin - A_out) / A_margin;
        }

        return percent;
    }

    public boolean hasInformationToSplit() {
        return this.instSeenSinceLastSplitAttempt >= this.tree.getGracePeriod();
    }

    public IademAttributeSplitSuggestion getBestSplitSuggestionIADEM() throws IademException {
        int bestAtt;

        int bestAttIndex = -1;
        int secondBestAttIndex = -1;
        double bestAtt_upper = Double.MAX_VALUE;
        double bestAtt_lower = Double.MAX_VALUE;
        double secondBestAtt_upper = Double.MAX_VALUE;
        double secondBestAtt_lower = Double.MAX_VALUE;
        double worstAtt_upper = Double.MIN_VALUE;
        double worstAtt_lower = Double.MIN_VALUE;

        for (int i = 0; i < virtualChildren.size(); i++) {
            VirtualNode currentVirtualChild = virtualChildren.get(i);
            if (currentVirtualChild != null) {
                try {
                    currentVirtualChild.updateHeuristicMeasure();
                } catch (IademException e) {
                    throw new IademException("LeafNode", "getBestSplitSuggestion7",
                            "Problems when updating measures: \n"
                            + e.getMessage());
                }
                // find the best and second-best attributes
                if (currentVirtualChild.getHeuristicMeasureUpper() >= 0) {
                    if ((currentVirtualChild.getHeuristicMeasureUpper() < bestAtt_upper)
                            || ((currentVirtualChild.getHeuristicMeasureUpper() == bestAtt_upper)
                            && (currentVirtualChild.getHeuristicMeasureLower() < bestAtt_lower))) {

                        secondBestAttIndex = bestAttIndex;
                        secondBestAtt_upper = bestAtt_upper;
                        secondBestAtt_lower = bestAtt_lower;

                        bestAttIndex = i;
                        bestAtt_upper = currentVirtualChild.getHeuristicMeasureUpper();
                        bestAtt_lower = currentVirtualChild.getHeuristicMeasureLower();
                    } else if ((currentVirtualChild.getHeuristicMeasureUpper() < secondBestAtt_upper)
                            || ((currentVirtualChild.getHeuristicMeasureUpper() == secondBestAtt_upper)
                            && (currentVirtualChild.getHeuristicMeasureLower() < secondBestAtt_lower))) {
                        secondBestAttIndex = i;
                        secondBestAtt_upper = currentVirtualChild.getHeuristicMeasureUpper();
                        secondBestAtt_lower = currentVirtualChild.getHeuristicMeasureLower();
                    }
                    // find the worst attribute
                    if ((currentVirtualChild.getHeuristicMeasureUpper() > worstAtt_upper)
                            || ((currentVirtualChild.getHeuristicMeasureUpper() == worstAtt_upper)
                            && (currentVirtualChild.getHeuristicMeasureLower() > worstAtt_lower))) {
                        worstAtt_upper = currentVirtualChild.getHeuristicMeasureUpper();
                        worstAtt_lower = currentVirtualChild.getHeuristicMeasureLower();
                    }
                }
            }
        }
        bestAtt = bestAttIndex;
        if (secondBestAttIndex != -1) {
            // percents in common
            // best and worst
            double best_worst = percentInCommon(bestAtt_upper,
                    bestAtt_lower,
                    worstAtt_upper,
                    worstAtt_lower);
            double worst_best = percentInCommon(worstAtt_upper, worstAtt_lower,
                    bestAtt_upper,
                    bestAtt_lower);

            double d = tree.getAttributeDifferentiation();

            boolean similarityBestWorst = (best_worst >= (1.0 - d))
                    && (worst_best >= (1.0 - d));

            boolean similarityWithConfidenceBestWorst = similarityBestWorst
                    && ((bestAtt_upper - bestAtt_lower) <= d);

            boolean differenceBestWorst = (best_worst <= d)
                    || (worst_best <= d);
            if (!similarityWithConfidenceBestWorst && !differenceBestWorst) {
                bestAtt = -1;
            }
        }

        if (bestAtt != -1) {
            VirtualNode bestNode = virtualChildren.get(bestAtt);
            double percent;
            if (bestNode instanceof NumericVirtualNode) {
                percent = 1.0 - bestNode.getPercent();
            } else {
                percent = bestNode.getPercent();
            }
            if (percent > tree.getPercentInCommon()) {
                bestAtt = -1;
            }
        }
        if (bestAtt >= 0) {
            return virtualChildren.get(bestAtt).bestSplitSuggestion;
        }
        return null;
    }

    @Override
    public void getNumberOfNodes(int[] count) {
        count[1]++;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }
}
