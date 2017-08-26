/*
 *    IADEM2cTree.java
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
package moa.classifiers.trees.iadem;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import java.util.Arrays;

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;

import com.yahoo.labs.samoa.instances.Instance;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

public class Iadem2 extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;
    public ClassOption numericEstimatorOption = new ClassOption("numericEstimator",
            'z', "Numeric estimator to use.", IademNumericAttributeObserver.class,
            "IademGaussianNumericAttributeClassObserver");
    public IntOption gracePeriodOption = new IntOption("gracePeriod", 'n',
            "The number of instances the tree should observe between splitting attempts.",
            100, 1, Integer.MAX_VALUE);
    public MultiChoiceOption splitCriterionOption = new MultiChoiceOption("splitCriterion", 's',
            "Split criterion to use.", new String[]{
                "entropy", "entropy_logVar", "entropy_logVar+Peso", "entropy_Peso", "beta1", "gamma1", "beta2", "gamma2", "beta4", "gamma4"}, new String[]{
                "entropy", "entropy_logVar", "entropy_logVar+Peso", "entropy_Peso", "beta1", "gamma1", "beta2", "gamma2", "beta4", "gamma4"}, 0);
    public FloatOption splitConfidenceOption = new FloatOption("splitConfidence", 'c',
            "The allowable error in split decision, values closer to 0 will take longer to decide.",
            0.01, 0.0, 1.0);
    public MultiChoiceOption splitTestsOption = new MultiChoiceOption("splitChoice", 'i',
            "Methods for splitting leaf nodes.",
            new String[]{
                "onlyBinarySplit", "onlyMultiwaySplit", "bestSplit"}, new String[]{
                "onlyBinary", "onlyMultiway", "bestSplit"},
            2);

    public MultiChoiceOption leafPredictionOption = new MultiChoiceOption("leafPrediction", 'b',
            "Leaf prediction to use.", new String[]{
                "MC", "NB", "NBKirkby", "WeightedVote"},
            new String[]{"MC: Majority class.",
                "NB: Naïve Bayes.",
                "NBKirkby.",
                "WeightedVote: Weighted vote between NB and MC."},
            1);
    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'd',
            "Drift detection method to use.", AbstractChangeDetector.class, "HDDM_A_Test");

    // fixed option...
    public FloatOption attributeDiferentiation = new FloatOption("attritubeDiferentiation", 'a',
            "Attribute differenciation",
            0.1, 0.0, 1.0);

    public final int naiveBayesLimit = 0;
    public final double percentInCommon = 0.75;
    protected int numberOfInstancesProcessed = 0;

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void resetLearningImpl() {
        this.numberOfInstancesProcessed = 0;
        this.treeRoot = null;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.treeRoot == null) {
            IademCommonProcedures.setConfidence(this.splitConfidenceOption.getValue());
            this.estimator = (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy();
            createRoot(inst);
        }
        try {
            learnFromInstance(inst);
        } catch (IademException ex) {
            Logger.getLogger(Iadem2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected IademNumericAttributeObserver newNumericClassObserver() {
        IademNumericAttributeObserver numericClassObserver = (IademNumericAttributeObserver) getPreparedClassOption(this.numericEstimatorOption);
        return (IademNumericAttributeObserver) numericClassObserver.copy();
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{
            new Measurement("tree size (nodes)", this.getNumberOfNodes()),
            new Measurement("tree size (leaves)", this.getNumberOfLeaves())
        };
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.treeRoot == null) {
            DoubleVector classVotes = new DoubleVector();
            double estimation = 1.0 / inst.classAttribute().numValues();
            for (int i = 0; i < inst.classAttribute().numValues(); i++) {
                classVotes.addToValue(i, estimation);
            }
            return classVotes.getArrayCopy();

        }
        DoubleVector predicciones = new DoubleVector(this.treeRoot.getClassVotes(inst));
        return predicciones.getArrayCopy();

    }

    final public static double ERROR_MARGIN = 1.0e-9;
    protected Node treeRoot;
    
    protected AbstractChangeDetector estimator;

    public int numberOfNodes = 1,
            numberOfLeaves = 1;

    public AbstractChangeDetector newEstimator() {
        return (AbstractChangeDetector) this.estimator.copy();
    }

    public void createRoot(Instance instance) {
        double[] arrayCounter = new double[instance.numClasses()];
        Arrays.fill(arrayCounter, 0);

        this.treeRoot = newLeafNode(null, 0, 0, arrayCounter, instance);
    }

    public int getMaxNumberOfBins() {
        return (int) this.newNumericClassObserver().getMaxOfValues();
    }

    public IademNumericAttributeObserver getNumericAttObserver() {
        return newNumericClassObserver();
    }

    public long getNumberOfInstancesProcessed() {
        return numberOfInstancesProcessed;
    }

    public LeafNode newLeafNode(Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] classDist,
            Instance instance) {
        switch (this.leafPredictionOption.getChosenIndex()) {
            case 0: {
                return new LeafNode(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        newNumericClassObserver(),
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        instance);
            }
            case 1: {
                return new LeafNodeNB(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        newNumericClassObserver(),
                        this.naiveBayesLimit,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        instance);
            }
            case 2: {
                return new LeafNodeNBKirkby(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        newNumericClassObserver(),
                        this.naiveBayesLimit,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy(),
                        instance);
            }
            default: {
                return new LeafNodeWeightedVote(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        classDist,
                        newNumericClassObserver(),
                        this.naiveBayesLimit,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy(),
                        instance);
            }
        }
    }

    public double getAttributeDifferentiation() {
        return this.attributeDiferentiation.getValue();
    }

    public IademSplitCriterion getMeasure() throws IademException {
        return new IademSplitCriterion(this.splitCriterionOption.getChosenLabel());
    }

    public void setTreeRoot(Node newRoot) {
        this.treeRoot = newRoot;
    }

    public void learnFromInstance(Instance instance)
            throws IademException {
        this.numberOfInstancesProcessed++;
        this.treeRoot.learnFromInstance(instance);
    }

    public Node getTreeRoot() {
        return this.treeRoot;
    }

    public double[] getClassVotes(Instance instance) {
        return this.treeRoot.getClassVotes(instance);
    }

    public double getPercentInCommon() {
        return this.percentInCommon;
    }

    public int getValuesOfNominalAttributes(int attIndex, Instance instance) {
        return instance.attribute(attIndex).numValues();
    }

    public int getNaiveBayesLimit() {
        return this.naiveBayesLimit;
    }

    public boolean isOnlyMultiwayTest() {
        return this.splitTestsOption.getChosenIndex() == 2;
    }

    public boolean isOnlyBinaryTest() {
        return this.splitTestsOption.getChosenIndex() == 0;
    }

    public void incrNumberOfInstancesProcessed() {
        this.numberOfInstancesProcessed++;
    }

    public void getNumberOfNodes(int[] count) {
        this.treeRoot.getNumberOfNodes(count);
    }

    public void newSplit(int numOfLeaves) {
        this.numberOfLeaves += numOfLeaves - 1;
        this.numberOfNodes += numOfLeaves;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public int getNumberOfLeaves() {
        return numberOfLeaves;
    }

    public void setNumberOfLeaves(int numberOfLeaves) {
        this.numberOfLeaves = numberOfLeaves;
    }

    public abstract class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        protected Iadem2 tree;

        protected DoubleVector classValueDist;

        public Node parent;

        public DoubleVector getClassValueDist() {
            return classValueDist;
        }

        public void setClassValueDist(DoubleVector classValueDist) {
            this.classValueDist = classValueDist;
        }

        public Iadem2 getTree() {
            return tree;
        }

        public void setTree(Iadem2 tree) {
            this.tree = tree;
        }

        public Node(Iadem2 tree,
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

    public class LeafNode extends Node {

        private static final long serialVersionUID = 1L;
        protected long instNodeCountSinceVirtual;
        protected long instTreeCountSinceReal;
        protected long instNodeCountSinceReal;
        protected AutoExpandVector<VirtualNode> virtualChildren = new AutoExpandVector<>();
        protected boolean allAttUsed;

        protected double instSeenSinceLastSplitAttempt = 0;

        protected boolean split;

        public LeafNode(Iadem2 tree,
                Node parent,
                long instTreeCountSinceVirtual,
                long instNodeCountSinceVirtual,
                double[] initialClassCount,
                IademNumericAttributeObserver numericAttClassObserver,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                Instance instance) {
            super(tree, parent, initialClassCount);
            this.instNodeCountSinceVirtual = instNodeCountSinceVirtual;
            this.instTreeCountSinceReal = 0;
            this.instNodeCountSinceReal = 0;
            this.split = true;
            createVirtualNodes(numericAttClassObserver,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    instance);
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
                boolean onlyBinaryTest,
                Instance instance) {
            for (int i = 0; i < instance.numAttributes(); i++) {
                if (instance.classIndex() != i
                        && instance.attribute(i).isNominal()) {
                    this.virtualChildren.set(i, new NominalVirtualNode(this.tree,
                            this,
                            i,
                            onlyMultiwayTest,
                            onlyBinaryTest));
                } else if (instance.classIndex() != i
                        && instance.attribute(i).isNumeric()) {
                    this.virtualChildren.set(i, new NumericVirtualNode(this.tree,
                            this,
                            i,
                            numericObserver));

                } else { // class attribute
                    this.virtualChildren.set(i, null);
                }
            }

        }

        protected ArrayList<Integer> nominalAttUsed(Instance instance) {
            SplitNode currentNode = (SplitNode) this.parent;
            ArrayList<Integer> nomAttUsed = new ArrayList<Integer>();
            while (currentNode != null) {
                if (instance.attribute(currentNode.splitTest.getAttsTestDependsOn()[0]).isNominal()) {
                    nomAttUsed.add(currentNode.splitTest.getAttsTestDependsOn()[0]);
                }
                currentNode = (SplitNode) currentNode.parent;
            }
            return nomAttUsed;
        }

        @Override
        public Iadem2 getTree() {
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

        public void attemptToSplit(Instance instance) {
            if (this.classValueDist.numNonZeroEntries() > 1) {
                if (hasInformationToSplit()) {
                    try {
                        this.instSeenSinceLastSplitAttempt = 0;
                        IademAttributeSplitSuggestion bestSplitSuggestion;

                        bestSplitSuggestion = getBestSplitSuggestion(instance);
                        if (bestSplitSuggestion != null) {
                            doSplit(bestSplitSuggestion, instance);
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
                attemptToSplit(inst);
            }
            return this;
        }

        protected IademAttributeSplitSuggestion getFastSplitSuggestion(Instance instance) throws IademException {
            int bestAttIndex = -1;
            double bestAttValue = Double.MAX_VALUE;
            for (int i = 0; i < virtualChildren.size(); i++) {
                VirtualNode currentVirtualChild = virtualChildren.get(i);
                if (currentVirtualChild != null) {
                    try {
                        currentVirtualChild.updateHeuristicMeasure(instance);
                    } catch (IademException e) {
                        throw new IademException("LeafNode", "getFastSplitSuggestion",
                                "Problems when updating measures: \n"
                                + e.getMessage());
                    }
                    if (currentVirtualChild.getHeuristicMeasureUpper(instance) >= 0) {
                        if (currentVirtualChild.getHeuristicMeasureUpper(instance) < bestAttValue) {
                            bestAttIndex = i;
                            bestAttValue = currentVirtualChild.getHeuristicMeasureUpper(instance);
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

        public IademAttributeSplitSuggestion getBestSplitSuggestion(Instance instance) throws IademException {
            return getBestSplitSuggestionIADEM(instance);
        }

        public LeafNode[] doSplit(IademAttributeSplitSuggestion bestSuggestion, Instance instance) {
            SplitNode splitNode = virtualChildren.get(bestSuggestion.splitTest.getAttsTestDependsOn()[0]).getNewSplitNode(
                    this.instTreeCountSinceReal,
                    this.parent,
                    bestSuggestion,
                    instance);
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
            return getMajorityClassVotes(obs);
        }

        public double[] getMajorityClassVotes(Instance instance) {
            double[] votes = new double[instance.attribute(instance.classIndex()).numValues()];
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
                            double[] sibVotes = currentSibling.getMajorityClassVotes(instance);
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
            return this.instSeenSinceLastSplitAttempt >= this.tree.gracePeriodOption.getValue();
        }

        public IademAttributeSplitSuggestion getBestSplitSuggestionIADEM(Instance instance) throws IademException {
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
                        currentVirtualChild.updateHeuristicMeasure(instance);
                    } catch (IademException e) {
                        throw new IademException("LeafNode", "getBestSplitSuggestion7",
                                "Problems when updating measures: \n"
                                + e.getMessage());
                    }
                    // find the best and second-best attributes
                    if (currentVirtualChild.getHeuristicMeasureUpper(instance) >= 0) {
                        if ((currentVirtualChild.getHeuristicMeasureUpper(instance) < bestAtt_upper)
                                || ((currentVirtualChild.getHeuristicMeasureUpper(instance) == bestAtt_upper)
                                && (currentVirtualChild.getHeuristicMeasureLower(instance) < bestAtt_lower))) {

                            secondBestAttIndex = bestAttIndex;
                            secondBestAtt_upper = bestAtt_upper;
                            secondBestAtt_lower = bestAtt_lower;

                            bestAttIndex = i;
                            bestAtt_upper = currentVirtualChild.getHeuristicMeasureUpper(instance);
                            bestAtt_lower = currentVirtualChild.getHeuristicMeasureLower(instance);
                        } else if ((currentVirtualChild.getHeuristicMeasureUpper(instance) < secondBestAtt_upper)
                                || ((currentVirtualChild.getHeuristicMeasureUpper(instance) == secondBestAtt_upper)
                                && (currentVirtualChild.getHeuristicMeasureLower(instance) < secondBestAtt_lower))) {
                            secondBestAttIndex = i;
                            secondBestAtt_upper = currentVirtualChild.getHeuristicMeasureUpper(instance);
                            secondBestAtt_lower = currentVirtualChild.getHeuristicMeasureLower(instance);
                        }
                        // find the worst attribute
                        if ((currentVirtualChild.getHeuristicMeasureUpper(instance) > worstAtt_upper)
                                || ((currentVirtualChild.getHeuristicMeasureUpper(instance) == worstAtt_upper)
                                && (currentVirtualChild.getHeuristicMeasureLower(instance) > worstAtt_lower))) {
                            worstAtt_upper = currentVirtualChild.getHeuristicMeasureUpper(instance);
                            worstAtt_lower = currentVirtualChild.getHeuristicMeasureLower(instance);
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

    public class LeafNodeNB extends LeafNode {

        private static final long serialVersionUID = 1L;
        protected int naiveBayesLimit;

        public LeafNodeNB(Iadem2 tree,
                Node parent,
                long instTreeCountSinceVirtual,
                long instNodeCountSinceVirtual,
                double[] initialClassVotes,
                IademNumericAttributeObserver numericAttClassObserver,
                int naiveBayesLimit,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                Instance instance) {
            super(tree,
                    parent,
                    instTreeCountSinceVirtual,
                    instNodeCountSinceVirtual,
                    initialClassVotes,
                    numericAttClassObserver,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    instance);
            this.naiveBayesLimit = naiveBayesLimit;
        }

        @Override
        public double[] getClassVotes(Instance inst) {
            double[] classVotes;
            if (instNodeCountSinceVirtual == 0 || instNodeCountSinceReal < naiveBayesLimit) {
                classVotes = getMajorityClassVotes(inst);
            } else {
                classVotes = getNaiveBayesPrediction(inst);
            }
            return classVotes;
        }

        protected double[] getNaiveBayesPrediction(Instance obs) {
            double[] classVotes = getMajorityClassVotes(obs);
            DoubleVector condProbabilities;
            for (int i = 0; i < virtualChildren.size(); i++) {
                VirtualNode currentVirtualNode = virtualChildren.get(i);
                if (currentVirtualNode != null && currentVirtualNode.hasInformation()) {
                    double valor = obs.value(i);
                    condProbabilities = currentVirtualNode.computeConditionalProbability(valor);
                    if (condProbabilities != null) {
                        for (int j = 0; j < classVotes.length; j++) {
                            classVotes[j] *= condProbabilities.getValue(j);
                        }
                    }
                }
            }
            // normalize class votes
            double classVoteCount = 0.0;
            for (int i = 0; i < classVotes.length; i++) {
                classVoteCount += classVotes[i];
            }
            if (classVoteCount == 0.0) {
                for (int i = 0; i < classVotes.length; i++) {
                    classVotes[i] = 1.0 / classVotes.length;
                }
            } else {
                for (int i = 0; i < classVotes.length; i++) {
                    classVotes[i] /= classVoteCount;
                }
            }
            return classVotes;
        }
    }

    public class LeafNodeNBKirkby extends LeafNodeNB {

        private static final long serialVersionUID = 1L;
        protected int naiveBayesError,
                majorityClassError;

        public LeafNodeNBKirkby(Iadem2 tree,
                Node parent,
                long instancesProcessedByTheTree,
                long instancesProcessedByThisLeaf,
                double[] classDist,
                IademNumericAttributeObserver numericAttClassObserver,
                int naiveBayesLimit,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                AbstractChangeDetector estimator,
                Instance instance) {
            super(tree,
                    parent,
                    instancesProcessedByTheTree,
                    instancesProcessedByThisLeaf,
                    classDist,
                    numericAttClassObserver,
                    naiveBayesLimit,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    instance);
            this.naiveBayesError = 0;
            this.majorityClassError = 0;
        }

        @Override
        public double[] getClassVotes(Instance instance) {
            if (naiveBayesError > majorityClassError) {
                return getMajorityClassVotes(instance);
            } else {
                return getNaiveBayesPrediction(instance);
            }
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            // test-then-train
            double[] prediccion = getMajorityClassVotes(inst);
            double error = (Utils.maxIndex(prediccion) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.majorityClassError += error;

            prediccion = getNaiveBayesPrediction(inst);
            error = (Utils.maxIndex(prediccion) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.naiveBayesError += error;

            return super.learnFromInstance(inst);
        }
    }

    public class LeafNodeWeightedVote extends LeafNodeNB {

        private static final long serialVersionUID = 1L;
        protected AbstractChangeDetector naiveBayesError,
                majorityClassError;

        public LeafNodeWeightedVote(Iadem2 tree,
                Node parent,
                long instancesProcessedByTheTree,
                long instancesProcessedByThisLeaf,
                double[] classDist,
                IademNumericAttributeObserver observadorContinuos,
                int naiveBayesLimit,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                AbstractChangeDetector estimator,
                Instance instance) {
            super(tree,
                    parent,
                    instancesProcessedByTheTree,
                    instancesProcessedByThisLeaf,
                    classDist,
                    observadorContinuos,
                    naiveBayesLimit,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    instance);
            this.naiveBayesError = (AbstractChangeDetector) estimator.copy();
            this.majorityClassError = (AbstractChangeDetector) estimator.copy();
        }

        @Override
        public double[] getClassVotes(Instance instance) {
            double NBweight = 1 - this.naiveBayesError.getEstimation(),
                    MCweight = 1 - this.majorityClassError.getEstimation();
            double[] MC = getMajorityClassVotes(instance),
                    NB = getNaiveBayesPrediction(instance),
                    classVotes = new double[MC.length];
            for (int i = 0; i < MC.length; i++) {
                classVotes[i] = MC[i] * MCweight + NB[i] * NBweight;
            }
            return classVotes;
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            // test-then-train
            double[] classVotes = getMajorityClassVotes(inst);
            double error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.majorityClassError.input(error);

            classVotes = getNaiveBayesPrediction(inst);
            error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.naiveBayesError.input(error);
            return super.learnFromInstance(inst);
        }
    }

    public abstract class VirtualNode extends Node {

        private static final long serialVersionUID = 1L;
        protected int attIndex;

        protected boolean heuristicMeasureUpdated;

        protected IademAttributeSplitSuggestion bestSplitSuggestion = null;

        public VirtualNode(Iadem2 tree, Node parent, int attIndex) {
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
                IademAttributeSplitSuggestion bestSuggestion,
                Instance instance);

        public abstract void updateHeuristicMeasure(Instance instance) throws IademException;

        public abstract DoubleVector computeConditionalProbability(double value);

        public abstract double getPercent();

        public abstract boolean hasInformation();

        public double getHeuristicMeasureUpper(Instance instance) throws IademException {
            if (!this.heuristicMeasureUpdated) {
                updateHeuristicMeasure(instance);
            }
            if (this.bestSplitSuggestion == null) {
                return -1;
            }
            return this.bestSplitSuggestion.merit;
        }

        public double getHeuristicMeasureLower(Instance instance) throws IademException {
            if (!this.heuristicMeasureUpdated) {
                updateHeuristicMeasure(instance);
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

    public class NominalVirtualNode extends VirtualNode {

        private static final long serialVersionUID = 1L;

        protected AutoExpandVector<DoubleVector> nominalAttClassObserver = new AutoExpandVector<DoubleVector>();

        protected DoubleVector attValueDist;
        protected boolean onlyMultiwayTest = false;
        protected boolean onlyBinaryTest = false;

        public NominalVirtualNode(Iadem2 tree,
                Node parent,
                int attIndex,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest) {
            super(tree, parent, attIndex);

            this.attValueDist = new DoubleVector();
            this.onlyMultiwayTest = onlyMultiwayTest;
            this.onlyBinaryTest = onlyBinaryTest;
        }

        public AutoExpandVector<DoubleVector> getNominalAttClassObserver() {
            return nominalAttClassObserver;
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            double attValue = inst.value(this.attIndex);
            if (Utils.isMissingValue(attValue)) {
            } else {
                int intAttValue = (int) attValue;

                this.attValueDist.addToValue(intAttValue, inst.weight());
                this.classValueDist.addToValue((int) inst.value(inst.classIndex()), inst.weight());

                DoubleVector valDist = this.nominalAttClassObserver.get(intAttValue);
                if (valDist == null) {
                    valDist = new DoubleVector();
                    this.nominalAttClassObserver.set(intAttValue, valDist);
                }
                int classValue = (int) inst.classValue();
                valDist.addToValue(classValue, inst.weight());

                this.heuristicMeasureUpdated = false;
            }
            return this;
        }

        @Override
        public SplitNode getNewSplitNode(long newTotal,
                Node parent,
                IademAttributeSplitSuggestion bestSuggestion,
                Instance instance) {
            SplitNode splitNode = new SplitNode(this.tree,
                    parent,
                    null,
                    ((LeafNode) this.parent).getMajorityClassVotes(instance),
                    bestSuggestion.splitTest);

            Node[] children;
            if (bestSuggestion.splitTest instanceof IademNominalAttributeMultiwayTest) {
                children = new Node[instance.attribute(this.attIndex).numValues()];
                for (int i = 0; i < children.length; i++) {
                    long count = 0;
                    double[] tmpClassDist = new double[instance.attribute(instance.classIndex()).numValues()];
                    Arrays.fill(tmpClassDist, 0);
                    for (int j = 0; j < tmpClassDist.length; j++) {

                        DoubleVector classCount = nominalAttClassObserver.get(i);
                        double contadorAtributoClase = classCount != null ? classCount.getValue(j) : 0.0;
                        tmpClassDist[j] = contadorAtributoClase;
                        count += tmpClassDist[j];
                    }
                    children[i] = tree.newLeafNode(splitNode,
                            newTotal,
                            count,
                            tmpClassDist,
                            instance);
                }
            } else { // binary split
                children = new Node[2];
                IademNominalAttributeBinaryTest binarySplit = (IademNominalAttributeBinaryTest) bestSuggestion.splitTest;
                // to the left
                double[] tmpClassDist = new double[instance.attribute(instance.classIndex()).numValues()];
                // total de valores
                double tmpCount = 0;
                Arrays.fill(tmpClassDist, 0);
                DoubleVector classDist = nominalAttClassObserver.get(binarySplit.getAttValue());
                if (classDist != null) {
                    for (int i = 0; i < tmpClassDist.length; i++) {
                        tmpClassDist[i] = classDist.getValue(i);
                        tmpCount += classDist.getValue(i);
                    }
                }
                children[0] = tree.newLeafNode(splitNode,
                        newTotal,
                        (int) tmpCount,
                        tmpClassDist,
                        instance);
                // to the right
                tmpCount = this.classValueDist.sumOfValues() - tmpCount;
                for (int i = 0; i < tmpClassDist.length; i++) {
                    tmpClassDist[i] = this.classValueDist.getValue(i) - tmpClassDist[i];
                }
                children[1] = tree.newLeafNode(splitNode,
                        newTotal,
                        (int) tmpCount,
                        tmpClassDist,
                        instance);

            }
            splitNode.setChildren(children);
            return splitNode;
        }

        protected boolean moreThanOneAttValueObserved() {
            int count = 0;
            for (DoubleVector tmpClassDist : this.nominalAttClassObserver) {
                if (tmpClassDist != null) {
                    count++;
                }
                if (count > 1) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void updateHeuristicMeasure(Instance instance) throws IademException {
            if (moreThanOneAttValueObserved()/**/) {
                if (!this.onlyBinaryTest) {
                    updateHeuristicMeasureMultiwayTest(instance);
                }
                if (!this.onlyMultiwayTest) {
                    updateHeuristicMeasureBinaryTest(instance);
                }
            } else {
                this.bestSplitSuggestion = null;
            }
            this.heuristicMeasureUpdated = true;
        }

        public void updateHeuristicMeasureBinaryTest(Instance instance) throws IademException {
            if (!this.heuristicMeasureUpdated) {
                double measureLower, measureUpper;
                IademSplitCriterion measure = tree.getMeasure();
                if (this.bestSplitSuggestion != null
                        && this.bestSplitSuggestion.splitTest instanceof NominalAttributeBinaryTest) {
                    this.bestSplitSuggestion = null;
                }

                int numberOfSplits = 2, // binary test
                        numberOfTests = this.tree.getValuesOfNominalAttributes(this.attIndex, instance);
                int numberOfClasses = instance.attribute(instance.classIndex()).numValues();

                double[][][] classDistPerTestAndSplit_lower = new double[numberOfTests][numberOfSplits][numberOfClasses];
                double[][][] classDistPerTestAndSplit_upper = new double[numberOfTests][numberOfSplits][numberOfClasses];
                computeClassDistBinaryTest(classDistPerTestAndSplit_lower,
                        classDistPerTestAndSplit_upper);

                for (int k = 0; k < numberOfTests; k++) {
                    double[] sumPerSplit_lower = new double[numberOfSplits];
                    double[] availableErrorPerSplit = new double[numberOfSplits];
                    Arrays.fill(sumPerSplit_lower, 0.0);
                    for (int i = 0; i < numberOfSplits; i++) {
                        for (int j = 0; j < numberOfClasses; j++) {
                            sumPerSplit_lower[i] += classDistPerTestAndSplit_lower[k][i][j];
                        }
                        availableErrorPerSplit[i] = 1.0 - sumPerSplit_lower[i];

                        if (availableErrorPerSplit[i] < 0.0) {
                            if (Math.abs(availableErrorPerSplit[i]) < Iadem2.ERROR_MARGIN) {
                                availableErrorPerSplit[i] = 0.0;
                            } else {
                                throw new IademException("NominalVirtualNode", "updateHeuristicMeasureBinaryTest",
                                        "Problems when calculating measures");
                            }
                        }
                    }

                    double[] measurePerSplit_upper = new double[numberOfSplits];

                    double[] valueLevels = new double[numberOfSplits];
                    for (int i = 0; i < numberOfSplits; i++) {
                        ArrayList<Double> lot = new ArrayList<Double>();
                        lot.add(0.0);
                        lot.add(1.0);

                        ArrayList<Integer> hole = new ArrayList<Integer>();
                        hole.add(0);
                        for (int j = 0; j < numberOfClasses; j++) {
                            IademCommonProcedures.insertLotsHoles(lot, hole, classDistPerTestAndSplit_lower[k][i][j],
                                    classDistPerTestAndSplit_upper[k][i][j]);
                        }
                        valueLevels[i] = IademCommonProcedures.computeLevel(lot, hole,
                                availableErrorPerSplit[i]);
                    }
                    for (int i = 0; i < numberOfSplits; i++) {
                        ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                        for (int j = 0; j < numberOfClasses; j++) {
                            double measureProb_uppper = Math.max(valueLevels[i], classDistPerTestAndSplit_lower[k][i][j]);
                            measureProb_uppper = Math.min(classDistPerTestAndSplit_upper[k][i][j], measureProb_uppper);
                            vectorToMeasure.add(measureProb_uppper);
                        }
                        measurePerSplit_upper[i] = measure.doMeasure(vectorToMeasure);
                    }

                    double[] measurePerSplit_lower = new double[numberOfSplits];

                    for (int i = 0; i < numberOfSplits; i++) {
                        double tmpAvailable = availableErrorPerSplit[i];

                        ArrayList<Integer> decOrderClassDist_upper = new ArrayList<Integer>();

                        ArrayList<Integer> unusedClasses = new ArrayList<Integer>();
                        for (int j = 0; j < numberOfClasses; j++) {
                            unusedClasses.add(j);
                        }

                        double auxAvailable = availableErrorPerSplit[i];
                        for (int j = 0; j < numberOfClasses; j++) {
                            if (auxAvailable < 0.0) {
                                if (Math.abs(auxAvailable) < Iadem2.ERROR_MARGIN) {
                                    auxAvailable = 0.0;
                                } else {
                                    throw new IademException("NominalVirtualNode", "updateHeuristicMeasureBinaryTest",
                                            "Problems when calculating measures");
                                }
                            }
                            int classIndex = getClassProbabilities(i, unusedClasses,
                                    classDistPerTestAndSplit_lower[k],
                                    classDistPerTestAndSplit_upper[k],
                                    auxAvailable);
                            double probUp = Math.min(classDistPerTestAndSplit_upper[k][i][classIndex],
                                    classDistPerTestAndSplit_lower[k][i][classIndex] + auxAvailable);

                            auxAvailable -= (probUp - classDistPerTestAndSplit_lower[k][i][classIndex]);

                            unusedClasses.remove(new Integer(classIndex));
                            decOrderClassDist_upper.add(new Integer(classIndex));
                        }

                        ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                        for (int j = 0; j < decOrderClassDist_upper.size(); j++) {
                            int classIndex = decOrderClassDist_upper.get(j);

                            if (tmpAvailable < 0.0) {
                                if (Math.abs(tmpAvailable) < Iadem2.ERROR_MARGIN) {
                                    tmpAvailable = 0.0;
                                } else {
                                    throw new IademException("NominalVirtualNode", "updateHeuristicMeasureBinaryTest",
                                            "Problems when calculating measures");
                                }
                            }
                            double probUp = Math.min(classDistPerTestAndSplit_upper[k][i][classIndex],
                                    classDistPerTestAndSplit_lower[k][i][classIndex] + tmpAvailable);
                            tmpAvailable -= (probUp - classDistPerTestAndSplit_lower[k][i][classIndex]);
                            vectorToMeasure.add(probUp);
                        }

                        measurePerSplit_lower[i] = measure.doMeasure(vectorToMeasure);
                    }

                    double dividendUpper = 0.0;
                    double dividendLower = 0.0;

                    double leftDivUpper = measurePerSplit_upper[0] * attValueDist.getValue(k),
                            leftDivLower = measurePerSplit_lower[0] * attValueDist.getValue(k);
                    double divisor = classValueDist.sumOfValues(),
                            rightTotal = divisor - attValueDist.getValue(k);
                    double rightDivUpper = measurePerSplit_upper[1] * rightTotal,
                            rightDivLower = measurePerSplit_lower[1] * rightTotal;
                    dividendUpper = leftDivUpper + rightDivUpper;
                    dividendLower = leftDivLower + rightDivLower;

                    if (divisor != 0) {
                        measureLower = dividendLower / divisor;
                        measureUpper = dividendUpper / divisor;
                        if (this.bestSplitSuggestion == null) {
                            DoubleVector tmpClassDist = this.nominalAttClassObserver.get(k);
                            if (tmpClassDist != null) { // is it a useful split?
                                NominalAttributeBinaryTest test = new IademNominalAttributeBinaryTest(this.attIndex, k);
                                this.bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                                        new double[0][0],
                                        measureUpper,
                                        measureLower);
                            }
                        } // compete with multiway test 
                        else if (!this.onlyBinaryTest) {
                            if ((measureUpper < this.bestSplitSuggestion.merit)
                                    || (measureUpper == this.bestSplitSuggestion.merit
                                    && measureLower < this.bestSplitSuggestion.getMeritLowerBound())) {
                                DoubleVector tmpClassDist = this.nominalAttClassObserver.get(k);
                                if (tmpClassDist != null) { // is it a useful split?
                                    NominalAttributeBinaryTest test = new IademNominalAttributeBinaryTest(this.attIndex, k);
                                    this.bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                                            new double[0][0],
                                            measureUpper,
                                            measureLower);
                                }
                            }
                        }
                    }
                }

            }
        }

        protected void computeClassDistBinaryTest(double[][][] classDistPerTestAndSplit_lower,
                double[][][] classDistPerTestAndSplit_upper) {
            int numberOfClasses = classDistPerTestAndSplit_lower[0][0].length;
            double estimator, bound;
            double leftTotal = this.classValueDist.sumOfValues();
            for (int currentAttIndex = 0; currentAttIndex < classDistPerTestAndSplit_lower.length; currentAttIndex++) {
                for (int j = 0; j < numberOfClasses; j++) {
                    // compute probabilities in the left branch
                    DoubleVector classCounter = nominalAttClassObserver.get(currentAttIndex);
                    double attClassCounter = classCounter != null ? classCounter.getValue(j) : 0.0;
                    if (attValueDist.getValue(currentAttIndex) != 0) {
                        estimator = attClassCounter / attValueDist.getValue(currentAttIndex);
                        bound = IademCommonProcedures.getIADEM_HoeffdingBound(estimator, attValueDist.getValue(currentAttIndex));
                        classDistPerTestAndSplit_lower[currentAttIndex][0][j] = Math.max(0.0, estimator - bound);
                        classDistPerTestAndSplit_upper[currentAttIndex][0][j] = Math.min(1.0, estimator + bound);
                    } else {
                        classDistPerTestAndSplit_lower[currentAttIndex][0][j] = 0.0;
                        classDistPerTestAndSplit_upper[currentAttIndex][0][j] = 1.0;
                    }
                    // compute probabilities in the right branch
                    attClassCounter = classValueDist.getValue(j) - attClassCounter;
                    double rightTotal = leftTotal - attValueDist.getValue(currentAttIndex);
                    if (rightTotal != 0) {
                        estimator = attClassCounter / rightTotal;
                        bound = IademCommonProcedures.getIADEM_HoeffdingBound(estimator, rightTotal);
                        classDistPerTestAndSplit_lower[currentAttIndex][1][j] = Math.max(0.0, estimator - bound);
                        classDistPerTestAndSplit_upper[currentAttIndex][1][j] = Math.min(1.0, estimator + bound);
                    } else {
                        classDistPerTestAndSplit_lower[currentAttIndex][1][j] = 0.0;
                        classDistPerTestAndSplit_upper[currentAttIndex][1][j] = 1.0;
                    }
                }
            }
        }

        public void updateHeuristicMeasureMultiwayTest(Instance instance) throws IademException {
            if (!this.heuristicMeasureUpdated) {
                this.bestSplitSuggestion = null;
                double measureLower, measureUpper;
                IademSplitCriterion measure = tree.getMeasure();

                int numberOfValues = tree.getValuesOfNominalAttributes(attIndex, instance);
                int numberOfClasses = instance.attribute(instance.classIndex()).numValues();
                double[][] classDist_lower = new double[numberOfValues][numberOfClasses];
                double[][] classDist_upper = new double[numberOfValues][numberOfClasses];

                computeClassDistPerValue(classDist_lower, classDist_upper);

                double[] sumPerValue_lower = new double[numberOfValues];
                double[] availableErrorPerValue = new double[numberOfValues];
                Arrays.fill(sumPerValue_lower, 0.0);
                for (int i = 0; i < numberOfValues; i++) {
                    for (int j = 0; j < numberOfClasses; j++) {
                        sumPerValue_lower[i] += classDist_lower[i][j];
                    }
                    availableErrorPerValue[i] = 1.0 - sumPerValue_lower[i];

                    if (availableErrorPerValue[i] < 0.0) {
                        if (Math.abs(availableErrorPerValue[i]) < Iadem2.ERROR_MARGIN) {
                            availableErrorPerValue[i] = 0.0;
                        } else {
                            throw new IademException("NominalVirtualNode", "updateHeuristicMeasureMultiwayTest",
                                    "Problems when calculating measures");
                        }
                    }
                }

                double[] measuerPerValue_upper = new double[numberOfValues];

                double[] valueLevels = new double[numberOfValues];
                for (int i = 0; i < numberOfValues; i++) {
                    ArrayList<Double> lot = new ArrayList<Double>();
                    lot.add(0.0);
                    lot.add(1.0);

                    ArrayList<Integer> hole = new ArrayList<Integer>();
                    hole.add(0);
                    for (int j = 0; j < numberOfClasses; j++) {
                        IademCommonProcedures.insertLotsHoles(lot, hole, classDist_lower[i][j],
                                classDist_upper[i][j]);
                    }

                    valueLevels[i] = IademCommonProcedures.computeLevel(lot, hole,
                            availableErrorPerValue[i]);
                }

                for (int i = 0; i < numberOfValues; i++) {
                    ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                    for (int j = 0; j < numberOfClasses; j++) {
                        double p_sup_medida = Math.max(valueLevels[i], classDist_lower[i][j]);
                        p_sup_medida = Math.min(classDist_upper[i][j], p_sup_medida);
                        vectorToMeasure.add(p_sup_medida);
                    }
                    measuerPerValue_upper[i] = measure.doMeasure(vectorToMeasure);
                }
                // compute measure lower bounds
                double[] measurePerValue_lower = new double[numberOfValues];
                for (int i = 0; i < numberOfValues; i++) {
                    double availableError = availableErrorPerValue[i];
                    ArrayList<Integer> decOrderClassDist_upper = new ArrayList<Integer>();

                    ArrayList<Integer> unusedClasses = new ArrayList<Integer>();
                    for (int j = 0; j < numberOfClasses; j++) {
                        unusedClasses.add(j);
                    }

                    double auxAvailable = availableErrorPerValue[i];
                    for (int j = 0; j < numberOfClasses; j++) {
                        if (auxAvailable < 0.0) {
                            if (Math.abs(auxAvailable) < Iadem2.ERROR_MARGIN) {
                                auxAvailable = 0.0;
                            } else {
                                throw new IademException("NominalVirtualNode", "updateHeuristicMeasureMultiwayTest",
                                        "Problems when calculating measures");
                            }
                        }

                        int classID = getClassProbabilities(i, unusedClasses, classDist_lower,
                                classDist_upper, auxAvailable);
                        double probUp = Math.min(classDist_upper[i][classID],
                                classDist_lower[i][classID] + auxAvailable);

                        auxAvailable -= (probUp - classDist_lower[i][classID]);

                        unusedClasses.remove(new Integer(classID));
                        decOrderClassDist_upper.add(new Integer(classID));
                    }

                    ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                    for (int j = 0; j < decOrderClassDist_upper.size(); j++) {
                        int classID = decOrderClassDist_upper.get(j);

                        if (availableError < 0.0) {
                            if (Math.abs(availableError) < Iadem2.ERROR_MARGIN) {
                                availableError = 0.0;
                            } else {
                                throw new IademException("NominalVirtualNode", "updateHeuristicMeasureMultiwayTest",
                                        "Problems when calculating measures");
                            }
                        }
                        double probUp = Math.min(classDist_upper[i][classID],
                                classDist_lower[i][classID] + availableError);
                        availableError -= (probUp - classDist_lower[i][classID]);
                        vectorToMeasure.add(probUp);
                    }

                    measurePerValue_lower[i] = measure.doMeasure(vectorToMeasure);
                }

                double dividendUpper = 0.0;
                double dividendLower = 0.0;
                double divisor = 0.0;

                for (int i = 0; i < attValueDist.numValues(); i++) {
                    dividendUpper += (measuerPerValue_upper[i] * attValueDist.getValue(i));
                    dividendLower += (measurePerValue_lower[i] * attValueDist.getValue(i));
                    divisor += attValueDist.getValue(i);
                }
                measureLower = dividendLower / divisor;
                measureUpper = dividendUpper / divisor;

                int maxBranches = instance.attribute(attIndex).numValues();
                IademNominalAttributeMultiwayTest test = new IademNominalAttributeMultiwayTest(this.attIndex, maxBranches);
                bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                        new double[0][0],
                        measureUpper,
                        measureLower);
            }
        }

        private void computeClassDistPerValue(double[][] classDistLower,
                double[][] classDistUpper) {
            double estimator, classDistError;

            int numberOfValues = classDistLower.length;
            int numberOfClasses = classDistLower[0].length;

            for (int i = 0; i < numberOfValues; i++) {
                for (int j = 0; j < numberOfClasses; j++) {
                    if (attValueDist.getValue(i) == 0.0) {
                        classDistLower[i][j] = 0.0;
                        classDistUpper[i][j] = 1.0;
                    } else {
                        DoubleVector classCounter = nominalAttClassObserver.get(i);
                        double attValuePerClassCounter = classCounter != null ? classCounter.getValue(j) : 0.0;
                        estimator = attValuePerClassCounter / attValueDist.getValue(i);
                        classDistError = IademCommonProcedures.getIADEM_HoeffdingBound(estimator, attValueDist.getValue(i));
                        classDistLower[i][j] = Math.max(0.0, estimator - classDistError);
                        classDistUpper[i][j] = Math.min(1.0, estimator + classDistError);
                    }
                }
            }
        }

        private int getClassProbabilities(int attributeValue, ArrayList<Integer> attValueList,
                double[][] classDistPerValueLower, double[][] classDistPerValueUpper,
                double available) {
            int max, tmp;
            double maxProbUp, newProbUp;
            if (attValueList.isEmpty()) {
                return -1;
            } else {
                max = attValueList.get(0);
                maxProbUp = Math.min(classDistPerValueUpper[attributeValue][max],
                        classDistPerValueLower[attributeValue][max] + available);

                for (int i = 1; i < attValueList.size(); i++) {
                    tmp = attValueList.get(i);
                    newProbUp = Math.min(classDistPerValueUpper[attributeValue][tmp],
                            classDistPerValueLower[attributeValue][tmp] + available);
                    if (newProbUp > maxProbUp) {
                        max = tmp;
                        maxProbUp = newProbUp;
                    }
                }
                return max;
            }
        }

        @Override
        public DoubleVector computeConditionalProbability(double valor) {
            int numberOfValues = nominalAttClassObserver.size();
            DoubleVector conditionalProbability = new DoubleVector();

            DoubleVector sumsPerClass = new DoubleVector();
            for (int i = 0; i < numberOfValues; i++) {
                DoubleVector classCounter = nominalAttClassObserver.get(i);
                int numberOfClasses = classCounter != null ? classCounter.numValues() : 0;
                for (int j = 0; j < numberOfClasses; j++) {
                    double attClassCounter = classCounter.getValue(j);
                    sumsPerClass.addToValue(j, attClassCounter);
                }
            }

            for (int i = 0; i < sumsPerClass.numValues(); i++) {
                if (sumsPerClass.getValue(i) != 0.0) {
                    DoubleVector contadorClase = nominalAttClassObserver.get((int) valor);
                    double attClassCounter = contadorClase != null ? contadorClase.getValue(i) : 0.0;
                    conditionalProbability.setValue(i, attClassCounter / sumsPerClass.getValue(i));
                }
            }

            return conditionalProbability;
        }

        @Override
        public double getPercent() {
            double counter = 0;
            double maxInstances = 0;
            for (int i = 0; i < attValueDist.numValues(); i++) {
                counter += attValueDist.getValue(i);
                if (attValueDist.getValue(i) > maxInstances) {
                    maxInstances = attValueDist.getValue(i);
                }
            }
            double maxPercent = maxInstances / counter;
            return maxPercent;
        }

        @Override
        public boolean hasInformation() {
            return true;
        }

        @Override
        public void getNumberOfNodes(int[] count) {
            count[1]++;
        }
    }

    public class NumericVirtualNode extends VirtualNode {

        private static final long serialVersionUID = 1L;
        private static final int MAX_BINS_EQUAL_WIDTH = 10;
        protected IademNumericAttributeObserver numericAttClassObserver;
        protected double bestCutPoint;

        public NumericVirtualNode(Iadem2 tree,
                Node parent,
                int attIndex,
                IademNumericAttributeObserver numericAttClassObs) {
            super(tree, parent, attIndex);
            int numIntervalos = this.tree.getMaxNumberOfBins();
            this.numericAttClassObserver = (IademNumericAttributeObserver) numericAttClassObs.copy();
            this.numericAttClassObserver.setMaxBins(numIntervalos);
            this.bestCutPoint = 0.0;
        }

        public IademNumericAttributeObserver getNumericAttClassObserver() {
            return numericAttClassObserver;
        }

        @Override
        public Node learnFromInstance(Instance instance) {
            this.numericAttClassObserver.addValue(instance.value(this.attIndex),
                    (int) instance.value(instance.classIndex()),
                    instance.weight());
            this.classValueDist.addToValue((int) instance.value(instance.classIndex()), instance.weight());
            this.heuristicMeasureUpdated = false;
            return this;
        }

        long arrSum(long[] arr) {
            long count = 0;
            for (int i = 0; i < arr.length; i++) {
                count += arr[i];
            }
            return count;
        }

        @Override
        public SplitNode getNewSplitNode(long newTotal,
                Node parent,
                IademAttributeSplitSuggestion bestSuggestion,
                Instance instance) {
            double[] cut = new double[]{this.bestCutPoint};

            Node[] children = new Node[2]; // a traditional binary split test for numeric attributes

            long[] newClassVotesAndTotal = this.numericAttClassObserver.getLeftClassDist(this.bestCutPoint);
            long totalLeft = arrSum(newClassVotesAndTotal);

            long total = this.numericAttClassObserver.getValueCount();
            long[] classVotesTotal = this.numericAttClassObserver.getClassDist();
            boolean equalsPassesTest = true;
            if (this.numericAttClassObserver instanceof IademVFMLNumericAttributeClassObserver) {
                equalsPassesTest = false;
            }
            SplitNode splitNode = new SplitNode(this.tree,
                    parent,
                    null,
                    ((LeafNode) this.parent).getMajorityClassVotes(instance),
                    new IademNumericAttributeBinaryTest(this.attIndex,
                            cut[0],
                            equalsPassesTest));

            long newTotalLeft = totalLeft;
            long newTotalRight = total - newTotalLeft;
            double[] newClassVotesLeft = new double[instance.attribute(instance.classIndex()).numValues()];
            double[] newClassVotesRight = new double[instance.attribute(instance.classIndex()).numValues()];

            Arrays.fill(newClassVotesLeft, 0);
            Arrays.fill(newClassVotesRight, 0);

            for (int i = 0; i < newClassVotesAndTotal.length; i++) {
                newClassVotesLeft[i] = newClassVotesAndTotal[i];
                newClassVotesRight[i] = classVotesTotal[i] - newClassVotesLeft[i];
            }

            splitNode.setChildren((Node[]) null);
            children[0] = this.tree.newLeafNode(splitNode, newTotal, newTotalLeft, newClassVotesLeft, instance);
            children[1] = this.tree.newLeafNode(splitNode, newTotal, newTotalRight, newClassVotesRight, instance);

            splitNode.setChildren(children);

            return splitNode;
        }

        @Override
        public void updateHeuristicMeasure(Instance instance) throws IademException {
            if (!this.heuristicMeasureUpdated) {
                if (this.numericAttClassObserver.getNumberOfCutPoints() < 2) {
                    this.bestSplitSuggestion = null;
                } else {
                    IademSplitCriterion measure = ((LeafNode) this.parent).getTree().getMeasure();
                    int numberOfClasses = instance.attribute(instance.classIndex()).numValues();

                    int numberOfSplits = 2;
                    int numberOfCuts = (int) this.numericAttClassObserver.getNumberOfCutPoints();

                    double[] measureLower = new double[numberOfCuts];
                    double[] measureUpper = new double[numberOfCuts];

                    double[][][] classVotesPerCutAndSplit_lower = new double[numberOfCuts][numberOfSplits][numberOfClasses];
                    double[][][] classVotesPerCutAndSplit_upper = new double[numberOfCuts][numberOfSplits][numberOfClasses];

                    double[][] totalPerCutAndSplit = new double[numberOfCuts][numberOfSplits];

                    computeClassVoteBounds(classVotesPerCutAndSplit_lower, classVotesPerCutAndSplit_upper,
                            totalPerCutAndSplit, true);

                    for (int k = 0; k < numberOfCuts; k++) {
                        double[] countPerSplit_lower = new double[numberOfSplits];
                        double[] availableErrorPerSplit = new double[numberOfSplits];
                        Arrays.fill(countPerSplit_lower, 0.0);
                        for (int i = 0; i < numberOfSplits; i++) {
                            for (int j = 0; j < numberOfClasses; j++) {
                                countPerSplit_lower[i] += classVotesPerCutAndSplit_lower[k][i][j];
                            }
                            availableErrorPerSplit[i] = 1.0 - countPerSplit_lower[i];

                            if (availableErrorPerSplit[i] < 0.0) {
                                if (Math.abs(availableErrorPerSplit[i]) < Iadem2.ERROR_MARGIN) {
                                    availableErrorPerSplit[i] = 0.0;
                                } else {
                                    throw new IademException("NumericVirtualNode", "updateHeuristicMeasure",
                                            "Problems when calculating measures");
                                }
                            }
                        }

                        // compute measure upper bounds
                        double[] measurePerSplit_upper = new double[numberOfSplits];

                        double[] splitLevels = new double[numberOfSplits];
                        for (int i = 0; i < numberOfSplits; i++) {
                            ArrayList<Double> lot = new ArrayList<Double>();
                            lot.add(0.0);
                            lot.add(1.0);
                            ArrayList<Integer> hole = new ArrayList<Integer>();
                            hole.add(0);
                            for (int j = 0; j < numberOfClasses; j++) {
                                IademCommonProcedures.insertLotsHoles(lot, hole,
                                        classVotesPerCutAndSplit_lower[k][i][j],
                                        classVotesPerCutAndSplit_upper[k][i][j]);
                            }
                            splitLevels[i] = IademCommonProcedures.computeLevel(lot, hole,
                                    availableErrorPerSplit[i]);
                        }

                        for (int i = 0; i < numberOfSplits; i++) {
                            ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                            for (int j = 0; j < numberOfClasses; j++) {
                                double tmpMeasureUpper = Math.max(splitLevels[i],
                                        classVotesPerCutAndSplit_lower[k][i][j]);
                                tmpMeasureUpper = Math.min(classVotesPerCutAndSplit_upper[k][i][j],
                                        tmpMeasureUpper);
                                vectorToMeasure.add(tmpMeasureUpper);
                            }
                            measurePerSplit_upper[i] = measure.doMeasure(vectorToMeasure);
                        }

                        double[] measurePerSplit_lower = new double[numberOfSplits];

                        for (int i = 0; i < numberOfSplits; i++) {
                            double availableProb = availableErrorPerSplit[i];

                            ArrayList<Integer> decOrderClassVotes_upper = new ArrayList<Integer>();

                            ArrayList<Integer> unusedClasses = new ArrayList<Integer>();
                            for (int j = 0; j < numberOfClasses; j++) {
                                unusedClasses.add(j);
                            }

                            double auxAvailable = availableErrorPerSplit[i];
                            for (int j = 0; j < numberOfClasses; j++) {
                                if (auxAvailable < 0.0) {
                                    if (Math.abs(auxAvailable) < Iadem2.ERROR_MARGIN) {
                                        auxAvailable = 0.0;
                                    } else {
                                        throw new IademException("NodoVirtualContinuo", "actualizaMedida",
                                                "Problems calculating measure");
                                    }
                                }

                                int classIndex = getClassValueProbabilities(i, unusedClasses,
                                        classVotesPerCutAndSplit_lower[k],
                                        classVotesPerCutAndSplit_upper[k], auxAvailable);
                                double probUpper = Math.min(classVotesPerCutAndSplit_upper[k][i][classIndex],
                                        classVotesPerCutAndSplit_lower[k][i][classIndex]
                                        + auxAvailable);

                                auxAvailable -= (probUpper - classVotesPerCutAndSplit_lower[k][i][classIndex]);

                                unusedClasses.remove(new Integer(classIndex));
                                decOrderClassVotes_upper.add(new Integer(classIndex));
                            }

                            ArrayList<Double> vectorToMeasure = new ArrayList<Double>();
                            for (int j = 0; j < decOrderClassVotes_upper.size(); j++) {
                                int classIndex = decOrderClassVotes_upper.get(j);

                                if (availableProb < 0.0) {
                                    if (Math.abs(availableProb) < Iadem2.ERROR_MARGIN) {
                                        availableProb = 0.0;
                                    } else {
                                        throw new IademException("NumericVirtualNode", "updateMeasure",
                                                "Problems when calculating measures");
                                    }
                                }
                                double probUpper = Math.min(classVotesPerCutAndSplit_upper[k][i][classIndex],
                                        classVotesPerCutAndSplit_lower[k][i][classIndex]
                                        + availableProb);
                                availableProb -= (probUpper - classVotesPerCutAndSplit_lower[k][i][classIndex]);
                                vectorToMeasure.add(probUpper);
                            }

                            measurePerSplit_lower[i] = measure.doMeasure(vectorToMeasure);
                        }

                        double dividendUpper = 0.0;
                        double dividendLower = 0.0;
                        double divisor = 0.0;

                        for (int i = 0; i < totalPerCutAndSplit[k].length; i++) {
                            dividendUpper += (measurePerSplit_upper[i] * totalPerCutAndSplit[k][i]);
                            dividendLower += (measurePerSplit_lower[i] * totalPerCutAndSplit[k][i]);
                            divisor += totalPerCutAndSplit[k][i];
                        }

                        measureLower[k] = dividendLower / divisor;
                        measureUpper[k] = dividendUpper / divisor;
                    }

                    ArrayList<Integer> indMinSupMedida = new ArrayList<Integer>();
                    double minSupMedida = measureUpper[0];
                    indMinSupMedida.add(0);

                    for (int i = 1; i < measureUpper.length; i++) {
                        if (measureUpper[i] < minSupMedida) {
                            minSupMedida = measureUpper[i];
                            indMinSupMedida.clear();
                            indMinSupMedida.add(i);
                        } else if (measureUpper[i] == minSupMedida) {
                            indMinSupMedida.add(i);
                        }
                    }

                    Iterator<Integer> iterator = indMinSupMedida.iterator();
                    Integer element = iterator.next();
                    int minMeasureLowerIndex = element;
                    double minMeasureLower = measureLower[element];

                    while (iterator.hasNext()) {
                        element = iterator.next();
                        if (measureLower[element] < minMeasureLower) {
                            minMeasureLower = measureLower[element];
                            minMeasureLowerIndex = element;
                        }
                    }

                    this.bestCutPoint = this.numericAttClassObserver.getCut(minMeasureLowerIndex);
                    boolean equalsPassesTest = true;
                    if (this.numericAttClassObserver instanceof IademVFMLNumericAttributeClassObserver) {
                        equalsPassesTest = false;
                    }
                    IademNumericAttributeBinaryTest test = new IademNumericAttributeBinaryTest(this.attIndex,
                            this.bestCutPoint,
                            equalsPassesTest);
                    this.bestSplitSuggestion = new IademAttributeSplitSuggestion(test,
                            new double[0][0],
                            minSupMedida,
                            minMeasureLower);
                }
                this.heuristicMeasureUpdated = true;
            }
        }

        private void computeClassVoteBounds(double[][][] classVotesPerCutAndSplit_lower,
                double[][][] classVotesPerCutAndSplit_upper,
                double[][] totalPerCutAndSplit,
                boolean withIntervalEstimates) {
            this.numericAttClassObserver.computeClassDistProbabilities(classVotesPerCutAndSplit_lower,
                    classVotesPerCutAndSplit_upper,
                    totalPerCutAndSplit,
                    withIntervalEstimates);
        }

        private int getClassValueProbabilities(int value, ArrayList<Integer> valueList,
                double[][] classValuePerSplitLower, double[][] classValuePerSplitUpper,
                double availableProb) {
            int max, newValue;
            double max_up, newValue_up;
            if (valueList.isEmpty()) {
                return -1;
            } else {
                max = valueList.get(0);
                max_up = Math.min(classValuePerSplitUpper[value][max],
                        classValuePerSplitLower[value][max] + availableProb);

                for (int i = 1; i < valueList.size(); i++) {
                    newValue = valueList.get(i);
                    newValue_up = Math.min(classValuePerSplitUpper[value][newValue],
                            classValuePerSplitLower[value][newValue] + availableProb);
                    if (newValue_up > max_up) {
                        max = newValue;
                        max_up = newValue_up;
                    }
                }
                return max;
            }
        }

        @Override
        public boolean hasInformation() {
            return (this.numericAttClassObserver.getNumberOfCutPoints() > 1);
        }

        private ArrayList<Double> getCuts() {
            return this.numericAttClassObserver.cutPointSuggestion(this.MAX_BINS_EQUAL_WIDTH);
        }

        @Override
        public double getPercent() {
            long[] classVotesLeft = this.numericAttClassObserver.getLeftClassDist(this.bestCutPoint);

            double leftCount = (double) arrSum(classVotesLeft);
            double total = (double) this.numericAttClassObserver.getValueCount();

            double leftPercent = leftCount / total;
            double rightPercent = 1.0 - leftPercent;

            if (rightPercent < leftPercent) {
                return rightPercent;
            } else {
                return leftPercent;
            }
        }

        @Override
        public DoubleVector computeConditionalProbability(double value) {
            ArrayList<Double> cut = getCuts();
            return new DoubleVector(this.numericAttClassObserver.computeConditionalProb(cut, value));
        }

        @Override
        public void getNumberOfNodes(int[] count) {
            count[1]++;
        }
    }

    public class SplitNode extends Node {

        private static final long serialVersionUID = 1L;

        public InstanceConditionalTest splitTest;

        public AutoExpandVector<Node> children = new AutoExpandVector<Node>();

        public SplitNode(Iadem2 tree,
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
}
