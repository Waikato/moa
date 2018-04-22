/*
 *    IADEM3Tree.java
 *
 *    @author Isvani Frías-Blanco
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

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.Serializable;
import java.util.Arrays;

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.core.AutoExpandVector;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.core.DoubleVector;
import moa.core.Measurement;
import weka.core.Utils;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class Iadem3 extends Iadem2 implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    public IntOption maxNestingLevelOption = new IntOption("maxNestingLevel", 'p',
            "Maximum level of nesting for alternative subtrees (-1 => unbounded).",
            1, -1, Integer.MAX_VALUE);

    public IntOption maxSubtreesPerNodeOption = new IntOption("maxSubtreesPerNode", 'w',
            "Maximum number of alternative subtrees per split node (-1 => unbounded).",
            1, -1, Integer.MAX_VALUE);

    protected final boolean restartAtDrift = true;

    protected int interchangedTrees = 0;
    protected int deletedTrees = 0;
    protected int numTrees = 0;

    protected int lastPrediction = -1,
            lastPredictionInLeaf = -1;
    //
    protected int treeLevel = 0;
    protected AutoExpandVector<Iadem3Subtree> subtreeList = new AutoExpandVector<Iadem3Subtree>();

    protected int currentSplitState = -1;
    protected final int SPLIT_BY_TIE_BREAKING = 0,
            SPLIT_WITH_CONFIDENCE = 1;

    public int numSplitsByBreakingTies = 0;

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{
            new Measurement("tree size (nodes)", this.getNumberOfNodes()),
            new Measurement("tree size (leaves)", this.getNumberOfLeaves()),
            new Measurement("interchanged trees", this.getChangedTrees())
        };
    }
    
    public AbstractChangeDetector getEstimatorCopy() {
        return (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy();
    }

    @Override
    public void createRoot(Instance instance) {

        double[] arrayCont = new double[instance.numClasses()];
        Arrays.fill(arrayCont, 0);

        this.treeRoot = newLeafNode(null, 0, 0, arrayCont, instance);
    }

    public void addSubtree(Iadem3Subtree subtree) {
        this.subtreeList.add(subtree);
    }

    public void removeSubtree(Iadem3Subtree subtree) {
        this.subtreeList.remove(subtree);
    }

    public boolean canCreateSubtree() {
        if (this.maxSubtreesPerNodeOption.getValue() > 0) {
            int count = getNumberOfSubtrees();
            if (count >= this.maxSubtreesPerNodeOption.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public LeafNode newLeafNode(Node parent,
            long instTreeCountSinceVirtual,
            long instNodeCountSinceVirtual,
            double[] initialClassCount,
            Instance instance) {
        switch (this.leafPredictionOption.getChosenIndex()) {
            case 0: {
                return new AdaptiveLeafNode(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        newNumericClassObserver(),
                        this.estimator,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        instance);
            }
            case 1: {
                return new AdaptiveLeafNodeNB(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        newNumericClassObserver(),
                        this.naiveBayesLimit,
                        this.estimator,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        instance);
            }
            case 2: {
                return new AdaptiveLeafNodeNBKirkby(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        newNumericClassObserver(),
                        this.naiveBayesLimit,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        this.estimator,
                        instance);
            }
            default: {
                return new AdaptiveLeafNodeWeightedVote(this,
                        parent,
                        instTreeCountSinceVirtual,
                        instNodeCountSinceVirtual,
                        initialClassCount,
                        newNumericClassObserver(),
                        this.naiveBayesLimit,
                        this.splitTestsOption.getChosenIndex() == 2,
                        this.splitTestsOption.getChosenIndex() == 0,
                        this.estimator,
                        instance);
            }
        }
    }

    public int getTreeLevel() {
        return treeLevel;
    }

    public int getMaxAltSubtreesPerNode() {
        return this.maxSubtreesPerNodeOption.getValue();
    }

    public int getMaxNestingLevels() {
        return this.maxNestingLevelOption.getValue();
    }

    public boolean isRestaurarVectoresPrediccion() {
        return restartAtDrift;
    }

    public int numDeletedTrees() {
        return deletedTrees;
    }

    public int numTrees() {
        int subtreeCount;
        if (this.treeRoot instanceof AdaptiveLeafNode) {
            subtreeCount = 0;
        } else {
            AdaptiveSplitNode nodo = (AdaptiveSplitNode) this.treeRoot;
            subtreeCount = nodo.getNumTrees();
        }
        return subtreeCount;
    }

    public void newTreeChange() {
        interchangedTrees++;
        numTrees--;
    }

    public void newDeletedTree() {
        deletedTrees++;
        numTrees--;
    }

    public int numSubtrees() {
        return tmpNumSubtrees(treeRoot);
    }

    private int tmpNumSubtrees(Node node) {
        int count = 0;
        if (node instanceof AdaptiveSplitNode) {
            count++;
            AutoExpandVector<Iadem3Subtree> subtree = ((AdaptiveSplitNode) node).alternativeTree;
            for (Iadem3Subtree currentSubtree : subtree) {
                count += currentSubtree.numSubtrees();
            }
        }
        if (node instanceof AdaptiveSplitNode) {
            AdaptiveSplitNode nodoAuxiliar = (AdaptiveSplitNode) node;
            for (Node child : nodoAuxiliar.children) {
                count += tmpNumSubtrees(child);
            }
        }
        return count;
    }

    protected boolean hasTree(Node node) {

        boolean ret = false;
        if (node instanceof AdaptiveSplitNode) {
            AdaptiveSplitNode tmp = (AdaptiveSplitNode) node;
            if (tmp.alternativeTree != null) {
                ret = true;
            }
            for (int i = 0; ret == false && i < tmp.children.size(); i++) {
                ret = ret || hasTree(tmp.children.get(i));
            }
        }
        return ret;
    }

    @Override
    public void learnFromInstance(Instance instance)
            throws IademException {
        getClassVotes(instance); // to update lastPrediction in the trees 
        getClassVotesFromLeaf(instance);
        super.learnFromInstance(instance);
    }

    protected void getClassVotesFromLeaf(Instance instance) {
        double[] votes = null;
        Node node = this.treeRoot;
        while (votes == null) {
            if (node instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode splitNode = (AdaptiveSplitNode) node;
                int childIndex = splitNode.instanceChildIndex(instance);
                if (childIndex >= 0) {
                    node = splitNode.getChild(childIndex);
                } else {
                    votes = splitNode.leaf.getClassVotes(instance);
                }
            } else {
                AdaptiveLeafNode leafNode = (AdaptiveLeafNode) node;
                votes = leafNode.getClassVotes(instance);
            }
        }
        this.lastPredictionInLeaf = Utils.maxIndex(votes);
    }

    public void copyTree(Iadem3Subtree arbol) {
        this.treeRoot = arbol.treeRoot;
    }

    void setNewTree() {
        numTrees++;
    }

    public int getChangedTrees() {
        return interchangedTrees;
    }

    @Override
    public double[] getClassVotes(Instance instance) {
        double[] votes = super.getClassVotes(instance);
        this.lastPrediction = Utils.maxIndex(votes);
        return votes;
    }

    public int getNumberOfSubtrees() {
        if (this.treeRoot instanceof AdaptiveSplitNode) {
            return ((AdaptiveSplitNode) this.treeRoot).getNumberOfSubtrees();
        }
        return 0;
    }

    protected Iadem3 getMainTree() {
        return this;
    }

    public void updateNumberOfLeaves(int amount) {
        this.numberOfLeaves += amount;
    }

    public void updateNumberOfNodes(int amount) {
        this.numberOfNodes += amount;
    }

    public void updateNumberOfNodesSplitByTieBreaking(int amount) {
        this.numSplitsByBreakingTies += amount;
    }

    public class AdaptiveLeafNode extends LeafNode implements Serializable {

        private static final long serialVersionUID = 1L;
        protected AbstractChangeDetector estimator;

        public AdaptiveLeafNode(Iadem3 arbol,
                Node parent,
                long instTreeCountSinceVirtual,
                long instNodeCountSinceVirtual,
                double[] initialClassCount,
                IademNumericAttributeObserver numericAttClassObserver,
                AbstractChangeDetector estimator,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                Instance instance) {
            super(arbol,
                    parent, instTreeCountSinceVirtual, instNodeCountSinceVirtual, initialClassCount, numericAttClassObserver, onlyMultiwayTest, onlyBinaryTest, instance);
            if (estimator != null) {
                this.estimator = (AbstractChangeDetector) estimator.copy();
            } else {
                this.estimator = null;
            }
        }

        @Override
        protected void createVirtualNodes(IademNumericAttributeObserver numericAttClassObserver,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                Instance instance) {
            ArrayList<Integer> nominalUsed = nominalAttUsed(instance);
            TreeSet<Integer> sort = new TreeSet<>(nominalUsed);
            for (int i = 0; i < instance.numAttributes(); i++) {
                if (instance.classIndex() != i
                        && instance.attribute(i).isNominal()) {
                    if ((!sort.isEmpty()) && (i == sort.first())) {
                        sort.remove(new Integer(sort.first()));
                        virtualChildren.set(i, null);
                    } else {
                        virtualChildren.set(i, new AdaptiveNominalVirtualNode((Iadem3) tree,
                                this,
                                i,
                                onlyMultiwayTest,
                                onlyBinaryTest));
                    }
                } else if (instance.classIndex() != i
                        && instance.attribute(i).isNumeric()) {
                    virtualChildren.set(i, new AdaptiveNumericVirtualNode((Iadem3) tree,
                            this,
                            i,
                            numericAttClassObserver));
                } else {
                    virtualChildren.set(i, null);
                }
            }
        }

        private void updateCounters(Instance experiencia) {
            double[] classVotes = this.getClassVotes(experiencia);
            boolean trueClass = (Utils.maxIndex(classVotes) == (int) experiencia.classValue());
            if (estimator != null && ((Iadem3) this.tree).restartAtDrift) {
                double error = trueClass == true ? 0.0 : 1.0;
                this.estimator.input(error);
                if (this.estimator.getChange()) {
                    this.restartVariablesAtDrift();
                }
            }
        }

        @Override
        public void attemptToSplit(Instance instance) {
            if (this.classValueDist.numNonZeroEntries() > 1) {
                if (hasInformationToSplit()) {
                    try {
                        this.instSeenSinceLastSplitAttempt = 0;
                        IademAttributeSplitSuggestion bestSplitSuggestion;
                        if (this.instNodeCountSinceReal > 5000) {
                            ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(1);
                            bestSplitSuggestion = getFastSplitSuggestion(instance);
                            if (bestSplitSuggestion != null) {
                                ((Iadem3) this.tree).currentSplitState = ((Iadem3) this.tree).SPLIT_BY_TIE_BREAKING;
                                doSplit(bestSplitSuggestion, instance);
                            }
                        } else {
                            bestSplitSuggestion = getBestSplitSuggestion(instance);
                            if (bestSplitSuggestion != null) {
                                ((Iadem3) this.tree).currentSplitState = ((Iadem3) this.tree).SPLIT_WITH_CONFIDENCE;
                                doSplit(bestSplitSuggestion, instance);
                            }
                        }
                    } catch (IademException ex) {
                        Logger.getLogger(LeafNode.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            updateCounters(inst);
            return super.learnFromInstance(inst);
        }

        @Override
        public AdaptiveLeafNode[] doSplit(IademAttributeSplitSuggestion mejorExpansion, Instance instance) {
            AdaptiveSplitNode splitNode;
            splitNode = (AdaptiveSplitNode) virtualChildren.get(mejorExpansion.splitTest.getAttsTestDependsOn()[0]).getNewSplitNode(instTreeCountSinceReal,
                    parent,
                    mejorExpansion,
                    instance);
            splitNode.setParent(this.parent);
            splitNode.estimator = this.tree.newEstimator();

            if (this.parent == null) {
                tree.setTreeRoot(splitNode);
            } else {
                ((SplitNode) parent).changeChildren(this, splitNode);
            }
            this.tree.newSplit(splitNode.getLeaves().size());
            return null;
        }

        protected void restartVariablesAtDrift() {
            instNodeCountSinceVirtual = 0;

            classValueDist = new DoubleVector();

            instTreeCountSinceReal = 0;
            instNodeCountSinceReal = 0;
            for (int i = 0; i < virtualChildren.size(); i++) {
                if (virtualChildren.get(i) != null) {
                    ((restartsVariablesAtDrift) virtualChildren.get(i)).resetVariablesAtDrift();
                }
            }
        }
    }

    public class AdaptiveLeafNodeNB extends AdaptiveLeafNode {

        private static final long serialVersionUID = 1L;
        protected int limitNaiveBayes;

        public AdaptiveLeafNodeNB(Iadem3 tree,
                Node parent,
                long instTreeCountSinceVirtual,
                long instNodeCountSinceVirtual,
                double[] initialClassCount,
                IademNumericAttributeObserver numericAttClassObserver,
                int limitNaiveBayes,
                AbstractChangeDetector estimator,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                Instance instance) {
            super(tree,
                    parent,
                    instTreeCountSinceVirtual,
                    instNodeCountSinceVirtual,
                    initialClassCount,
                    numericAttClassObserver,
                    estimator,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    instance);
            this.limitNaiveBayes = limitNaiveBayes;
        }

        @Override
        public double[] getClassVotes(Instance inst) {
            double[] votes;
            if (instNodeCountSinceVirtual == 0 || instNodeCountSinceReal < limitNaiveBayes) {
                votes = getMajorityClassVotes(inst);
            } else {
                votes = getNaiveBayesPrediction(inst);
            }
            return votes;
        }

        protected double[] getNaiveBayesPrediction(Instance inst) {
            double[] classDist = getMajorityClassVotes(inst);
            DoubleVector conditionalProbability = null;
            for (int i = 0; i < virtualChildren.size(); i++) {
                VirtualNode virtual = virtualChildren.get(i);
                if (virtual != null && virtual.hasInformation()) {
                    double currentValue = inst.value(i);
                    conditionalProbability = virtual.computeConditionalProbability(currentValue);
                    if (conditionalProbability != null) {
                        for (int j = 0; j < classDist.length; j++) {
                            classDist[j] *= conditionalProbability.getValue(j);
                        }
                    }
                }
            }
            double sum = 0.0;
            for (int i = 0; i < classDist.length; i++) {
                sum += classDist[i];
            }
            if (sum == 0.0) {
                for (int i = 0; i < classDist.length; i++) {
                    classDist[i] = 1.0 / classDist.length;
                }
            } else {
                for (int i = 0; i < classDist.length; i++) {
                    classDist[i] /= sum;
                }
            }
            return classDist;
        }
    }

    public class AdaptiveLeafNodeNBAdaptive extends AdaptiveLeafNodeNB {

        private static final long serialVersionUID = 1L;
        protected AbstractChangeDetector naiveBayesError,
                majorityClassError;

        public AdaptiveLeafNodeNBAdaptive(Iadem3 tree,
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
                    estimator,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    instance);
            this.naiveBayesError = (AbstractChangeDetector) estimator.copy();
            this.majorityClassError = (AbstractChangeDetector) estimator.copy();
        }

        @Override
        public double[] getClassVotes(Instance instance) {
            double mean1 = this.naiveBayesError.getEstimation(),
                    mean2 = this.majorityClassError.getEstimation();
            if (mean1 > mean2) {
                return getMajorityClassVotes(instance);
            } else {
                return getNaiveBayesPrediction(instance);
            }
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            double[] classVote = getMajorityClassVotes(inst);
            double error = (Utils.maxIndex(classVote) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.majorityClassError.input(error);

            classVote = getNaiveBayesPrediction(inst);
            error = (Utils.maxIndex(classVote) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.naiveBayesError.input(error);

            return super.learnFromInstance(inst);
        }

    }

    public class AdaptiveLeafNodeNBKirkby extends AdaptiveLeafNodeNB {

        private static final long serialVersionUID = 1L;
        protected int naiveBayesError,
                majorityClassError;

        public AdaptiveLeafNodeNBKirkby(Iadem3 tree,
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
                    estimator,
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
            double[] classVotes = getMajorityClassVotes(inst);
            double error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.majorityClassError += error;

            classVotes = getNaiveBayesPrediction(inst);
            error = (Utils.maxIndex(classVotes) == (int) inst.classValue()) ? 0.0 : 1.0;
            this.naiveBayesError += error;

            return super.learnFromInstance(inst);
        }

    }

    public class AdaptiveLeafNodeWeightedVote extends AdaptiveLeafNodeNBAdaptive {

        private static final long serialVersionUID = 1L;

        public AdaptiveLeafNodeWeightedVote(Iadem3 tree,
                Node parent,
                long instTreeCountSinceVirtual,
                long instNodeCountSinceVirtual,
                double[] classDist,
                IademNumericAttributeObserver observadorContinuos,
                int naiveBayesLimit,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest,
                AbstractChangeDetector estimator,
                Instance instance) {
            super(tree,
                    parent,
                    instTreeCountSinceVirtual,
                    instNodeCountSinceVirtual,
                    classDist,
                    observadorContinuos,
                    naiveBayesLimit,
                    onlyMultiwayTest,
                    onlyBinaryTest,
                    estimator,
                    instance);
        }

        @Override
        public double[] getClassVotes(Instance instance) {
            double NBweight = 1 - this.naiveBayesError.getEstimation(),
                    MCweight = 1 - this.majorityClassError.getEstimation();
            double[] MC = getMajorityClassVotes(instance),
                    NB = getNaiveBayesPrediction(instance),
                    votes = new double[MC.length];
            for (int i = 0; i < MC.length; i++) {
                votes[i] = MC[i] * MCweight + NB[i] * NBweight;
            }
            return votes;
        }

        protected boolean isSignificantlyGreaterThan(double mean1, double mean2, int n1, int n2) {
            double m = 1.0 / n1 + 1.0 / n2,
                    confidence = 0.001,
                    log = Math.log(1.0 / confidence),
                    bound = Math.sqrt(m * log / 2);
            return mean1 - mean2 > bound;
        }
    }

    public class AdaptiveNominalVirtualNode extends NominalVirtualNode implements Serializable, restartsVariablesAtDrift {

        private static final long serialVersionUID = 1L;

        protected AbstractChangeDetector estimador;

        public AdaptiveNominalVirtualNode(Iadem3 tree,
                Node parent,
                int attID,
                boolean onlyMultiwayTest,
                boolean onlyBinaryTest) {
            super(tree, parent, attID, onlyMultiwayTest, onlyBinaryTest);
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            double attValue = inst.value(attIndex);
            if (Utils.isMissingValue(attValue)) {
            } else {
                updateCountersForChange(inst);
            }
            return super.learnFromInstance(inst);
        }

        private void updateCountersForChange(Instance inst) {
            double[] classVotes = this.getClassVotes(inst);
            boolean trueClass = (Utils.maxIndex(classVotes) == (int) inst.classValue());
            if (estimador != null && ((Iadem3) this.tree).restartAtDrift) {
                double error = trueClass == true ? 0.0 : 1.0;
                this.estimador.input(error);
                if (this.estimador.getChange()) {
                    this.resetVariablesAtDrift();
                }
            }
        }

        @Override
        public SplitNode getNewSplitNode(long counter,
                Node parent,
                IademAttributeSplitSuggestion bestSplit,
                Instance instance) {
            AdaptiveSplitNode splitNode = new AdaptiveSplitNode((Iadem3) this.tree,
                    parent,
                    null,
                    ((LeafNode) this.parent).getMajorityClassVotes(instance),
                    bestSplit.splitTest,
                    ((AdaptiveLeafNode) this.parent).estimator,
                    (AdaptiveLeafNode) this.parent,
                    ((Iadem3) this.tree).currentSplitState);

            Node[] children;
            if (bestSplit.splitTest instanceof NominalAttributeMultiwayTest) {
                children = new Node[instance.attribute(this.attIndex).numValues()];
                for (int i = 0; i < children.length; i++) {
                    long tmpConter = 0;
                    double[] newClassDist = new double[instance.attribute(instance.classIndex()).numValues()];
                    Arrays.fill(newClassDist, 0);
                    for (int j = 0; j < newClassDist.length; j++) {
                        DoubleVector tmpClassDist = nominalAttClassObserver.get(i);
                        double tmpAttClassCounter = tmpClassDist != null ? tmpClassDist.getValue(j) : 0.0;
                        newClassDist[j] = tmpAttClassCounter;
                        tmpConter += newClassDist[j];
                    }
                    children[i] = ((Iadem3) tree).newLeafNode(splitNode,
                            counter,
                            tmpConter,
                            newClassDist,
                            instance);
                }
            } else { // binary split
                children = new Node[2];
                IademNominalAttributeBinaryTest binarySplit = (IademNominalAttributeBinaryTest) bestSplit.splitTest;
                double[] newClassDist = new double[instance.attribute(instance.classIndex()).numValues()];
                double tmpCounter = 0;
                Arrays.fill(newClassDist, 0);
                DoubleVector classDist = nominalAttClassObserver.get(binarySplit.getAttValue());
                for (int i = 0; i < newClassDist.length; i++) {
                    newClassDist[i] = classDist.getValue(i);
                    tmpCounter += classDist.getValue(i);
                }
                children[0] = ((Iadem3) tree).newLeafNode(splitNode,
                        counter,
                        (int) tmpCounter,
                        newClassDist,
                        instance);
                // a la derecha...
                tmpCounter = this.classValueDist.sumOfValues() - tmpCounter;
                for (int i = 0; i < newClassDist.length; i++) {
                    newClassDist[i] = this.classValueDist.getValue(i) - newClassDist[i];
                }
                children[1] = ((Iadem3) tree).newLeafNode(splitNode,
                        counter,
                        (int) tmpCounter,
                        newClassDist,
                        instance);
            }

            splitNode.setChildren(children);

            return splitNode;
        }

        @Override
        public void resetVariablesAtDrift() {
            attValueDist = new DoubleVector();
            nominalAttClassObserver = new AutoExpandVector<DoubleVector>();
            classValueDist = new DoubleVector();
        }
    }

    public class AdaptiveNumericVirtualNode extends NumericVirtualNode implements Serializable, restartsVariablesAtDrift {

        private static final long serialVersionUID = 1L;
        protected IademNumericAttributeObserver altAttClassObserver;
        protected DoubleVector altClassDist;
        protected AbstractChangeDetector estimator;

        public AdaptiveNumericVirtualNode(Iadem3 tree,
                Node parent,
                int attID,
                IademNumericAttributeObserver observadorContinuos) {
            super(tree, parent, attID, observadorContinuos);
        }

        @Override
        public Node learnFromInstance(Instance inst) {
            updateCounters(inst);
            return super.learnFromInstance(inst);
        }

        private void updateCounters(Instance inst) {
            double[] classVotes = this.getClassVotes(inst);
            boolean correct = (Utils.maxIndex(classVotes) == (int) inst.classValue());
            if (this.estimator != null && ((Iadem3) this.tree).restartAtDrift) {
                double error = correct == true ? 0.0 : 1.0;
                this.estimator.input(error);
                if (this.estimator.getChange()) {
                    this.resetVariablesAtDrift();
                }
            }
        }

        private long sum(long[] arr) {
            long s = 0;
            for (int i = 0; i < arr.length; i++) {
                s += arr[i];
            }
            return s;
        }

        @Override
        public SplitNode getNewSplitNode(long counter,
                Node parent,
                IademAttributeSplitSuggestion bestSplit,
                Instance instance) {
            double[] cutPoints = new double[]{bestCutPoint};
            Node[] children = new Node[2]; // a binary split
            long[] newClassDist = numericAttClassObserver.getLeftClassDist(bestCutPoint);
            long sumClassDist = numericAttClassObserver.getValueCount();
            long[] sumAttClassDist = numericAttClassObserver.getClassDist();
            boolean equalsPassesTest = true;
            if (this.numericAttClassObserver instanceof IademVFMLNumericAttributeClassObserver) {
                equalsPassesTest = false;
            }
            AdaptiveSplitNode splitNode = new AdaptiveSplitNode((Iadem3) this.tree,
                    parent,
                    null,
                    ((LeafNode) this.parent).getMajorityClassVotes(instance),
                    new NumericAttributeBinaryTest(this.attIndex, cutPoints[0], equalsPassesTest),
                    ((AdaptiveLeafNode) this.parent).estimator,
                    (AdaptiveLeafNode) this.parent,
                    ((Iadem3) this.tree).currentSplitState);
            long leftClassDist = sum(newClassDist);
            long rightClassDist = sumClassDist - leftClassDist;
            double[] newLeftClassDist = new double[instance.attribute(instance.classIndex()).numValues()];
            double[] newRightClassDist = new double[instance.attribute(instance.classIndex()).numValues()];

            Arrays.fill(newLeftClassDist, 0);
            Arrays.fill(newRightClassDist, 0);

            for (int i = 0; i < newClassDist.length; i++) {
                newLeftClassDist[i] = newClassDist[i];
                newRightClassDist[i] = sumAttClassDist[i] - newLeftClassDist[i];
            }
            splitNode.setChildren(null);
            children[0] = ((Iadem3) tree).newLeafNode(splitNode,
                    counter,
                    leftClassDist,
                    newLeftClassDist,
                    instance);
            children[1] = ((Iadem3) tree).newLeafNode(splitNode,
                    counter,
                    rightClassDist,
                    newRightClassDist,
                    instance);
            splitNode.setChildren(children);
            return splitNode;
        }

        @Override
        public void resetVariablesAtDrift() {
            this.bestSplitSuggestion = null;
            this.heuristicMeasureUpdated = false;
            numericAttClassObserver.reset();
            classValueDist = new DoubleVector();
        }
    }

    public class AdaptiveSplitNode extends SplitNode implements Serializable {

        private static final long serialVersionUID = 1L;

        protected AutoExpandVector<Iadem3Subtree> alternativeTree = new AutoExpandVector<Iadem3Subtree>();
        // Detector de cambio de concepto
        protected AbstractChangeDetector estimator;

        protected int causeOfSplit;

        protected AdaptiveLeafNode leaf;

        public AdaptiveSplitNode(Iadem3 tree,
                Node parent,
                Node[] child,
                double[] freq,
                InstanceConditionalTest splitTest,
                AbstractChangeDetector estimator,
                AdaptiveLeafNode predictionLeaf,
                int causeOfSplit) {
            super(tree, parent, child, freq, splitTest);
            if (estimator != null) {
                this.estimator = (AbstractChangeDetector) estimator.copy();
            } else {
                this.estimator = null;
            }
            this.leaf = predictionLeaf;
            this.leaf.setSplit(false);
            this.causeOfSplit = causeOfSplit;
        }

        @Override
        public Node learnFromInstance(Instance instance) {

            try {
                double thisError = this.estimator.getEstimation(),
                        thisSize = this.estimator.getDelay();
                double leafError = this.leaf.estimator.getEstimation(),
                        leafSize = this.leaf.estimator.getDelay();
                double m = 1.0 / thisSize + 1.0 / leafSize;
                double delta = 0.0001;
                double bound = Math.sqrt(m * Math.log(2.0 / delta) / 2.0);
                double diff = thisError - leafError;
                if (diff > bound && thisSize > 600 && leafSize > 600/**/) {
                    prune();
                    return this.leaf;
                } else if (-diff > bound) {
                    this.leaf.restartVariablesAtDrift();
                    this.leaf.estimator = (AbstractChangeDetector) this.leaf.estimator.copy();
                }
                Node node;
                boolean rightPredicted = ((Iadem3) this.tree).lastPredictionInLeaf == instance.classValue();
                node = checkAlternativeSubtrees(rightPredicted, instance);
                if (node == null) {
                    // no subtree change
                    for (Iadem3Subtree subtree : this.alternativeTree) {
                        try {
                            subtree.learnFromInstance(instance);
                            subtree.incrNumberOfInstancesProcessed();
                        } catch (IademException ex) {
                            Logger.getLogger(AdaptiveSplitNode.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    this.leaf.learnFromInstance(instance);
                    return super.learnFromInstance(instance);
                } else {
                    // subtree change
                    return node.learnFromInstance(instance);
                }
            } catch (IademException ex) {
                Logger.getLogger(AdaptiveSplitNode.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        private Node checkAlternativeSubtrees(boolean acierto, Instance instance) throws IademException {
            if (this.estimator != null) {
                double loss = (acierto == true ? 0.0 : 1.0);
                estimator.input(loss);
                if (estimator.getChange()) {
                    this.createTree(instance);
                }
                for (int i = 0; i < this.alternativeTree.size(); i++) {
                    Iadem3Subtree subtree = alternativeTree.get(i);
                    double treeError = subtree.estimacionValorMedio(),
                            thisError = this.estimator.getEstimation();
                    double bound = IademCommonProcedures.AverageComparitionByHoeffdingCorollary(this.estimator.getDelay(),
                            subtree.windowWidth(),
                            1e-4);

                    if (thisError - treeError > bound/**/) {
                        ((Iadem3) this.tree).interchangedTrees++;
                        return changeTrees(i);
                    } else if (isUseless(i)) {
                        ((Iadem3) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                        ((Iadem3) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                        ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.numSplitsByBreakingTies);
                        i--;
                    } else if (this.estimator.getDelay() > 6000
                            && subtree.windowWidth() > 6000/**/) {
                        {
                            if (treeError - thisError > bound) {
                                this.alternativeTree.remove(i);
                                // update number of nodes
                                ((Iadem3) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                                ((Iadem3) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                                ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.numSplitsByBreakingTies);
                                i--;
                            } else /**/ {
                                int[] countMain = new int[3],
                                        countAlt = new int[3];
                                for (Node child : this.children) {
                                    child.getNumberOfNodes(countMain);
                                }
                                subtree.getNumberOfNodes(countAlt);/**/

                                if (countMain[0] + countMain[1] + 1 > countAlt[0] + countAlt[1]) {
                                    return changeTrees(i);
                                } else {
                                    this.alternativeTree.remove(i);
                                    // update number of nodes
                                    ((Iadem3) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                                    ((Iadem3) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                                    ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.numSplitsByBreakingTies);
                                    i--;
                                }
                            }
                        }/**/

                    }

                }
            }
            return null;
        }

        public boolean isUseless(int i) {
            boolean removed = false;
            Iadem3Subtree subtree = this.alternativeTree.get(i);
            if (subtree.getTreeRoot() instanceof AdaptiveSplitNode) /**/ {
                // change if it already has an alternative subtree 
                AdaptiveSplitNode splitNode = ((AdaptiveSplitNode) subtree.getTreeRoot());

                int nMain = (int) this.estimator.getDelay(),
                        nAlt = (int) subtree.getEstimador().getDelay();
                double errorMain = this.estimator.getEstimation(),
                        errorAlt = subtree.getEstimador().getEstimation();
                double errorDifference = errorAlt - errorMain,
                        absError = Math.abs(errorDifference);
                if (!removed && nMain > 0 && nAlt > 0) {
                    double m = 1.0 / nMain + 1.0 / nAlt;
                    double delta = 1e-4;
                    double bound = Math.sqrt(m * Math.log(2.0 / delta) / 2.0);
                    if (errorDifference > bound) {
                        // alternative tree is too inaccurate
                        this.alternativeTree.remove(i);
                        removed = true;
                    }
                }
                if (!removed) {
                    InstanceConditionalTest condTest = splitNode.splitTest;
                    if (condTest instanceof IademNominalAttributeBinaryTest
                            && this.splitTest instanceof IademNominalAttributeBinaryTest) {
                        IademNominalAttributeBinaryTest altTest = (IademNominalAttributeBinaryTest) condTest,
                                mainTest = (IademNominalAttributeBinaryTest) this.splitTest;
                        if (mainTest.getAttValue() == altTest.getAttValue()
                                && mainTest.getAttsTestDependsOn()[0] == altTest.getAttsTestDependsOn()[0]) {
                            this.alternativeTree.remove(i);
                            removed = true;
                        }
                    } else if (condTest instanceof NominalAttributeMultiwayTest
                            && this.splitTest instanceof NominalAttributeMultiwayTest) {
                        NominalAttributeMultiwayTest altTest = (NominalAttributeMultiwayTest) condTest,
                                mainTest = (NominalAttributeMultiwayTest) this.splitTest;
                        if (mainTest.getAttsTestDependsOn()[0] == altTest.getAttsTestDependsOn()[0]) {
                            this.alternativeTree.remove(i);
                            removed = true;
                        }
                    } else if (condTest instanceof NumericAttributeBinaryTest
                            && this.splitTest instanceof NumericAttributeBinaryTest) {
                        NumericAttributeBinaryTest altTest = (NumericAttributeBinaryTest) condTest,
                                mainTest = (NumericAttributeBinaryTest) this.splitTest;
                        if (mainTest.getAttsTestDependsOn()[0] == altTest.getAttsTestDependsOn()[0]
                                && mainTest.getSplitValue() == altTest.getSplitValue()) {
                            this.alternativeTree.remove(i);
                            removed = true;
                        }
                    }
                }
            }
            return removed;
        }

        private Node changeTrees(int index) {
            for (int i = 0; i < this.alternativeTree.size(); i++) {
                if (i != index) {
                    Iadem3Subtree subtree = this.alternativeTree.get(i);
                    ((Iadem3) this.tree).updateNumberOfLeaves(-subtree.getNumberOfLeaves());
                    ((Iadem3) this.tree).updateNumberOfNodes(-subtree.getNumberOfNodes());
                    ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(-subtree.numSplitsByBreakingTies);
                }
            }
            Iadem3Subtree subtree = this.alternativeTree.get(index);
            // rest nodes of this main tree
            int count[] = new int[3];
            super.getNumberOfNodes(count);
            if (this.causeOfSplit == ((Iadem3) this.tree).SPLIT_BY_TIE_BREAKING) {
                count[2]++;
            }
            ((Iadem3) this.tree).updateNumberOfLeaves(-count[1]);
            ((Iadem3) this.tree).updateNumberOfNodes(-count[0] - count[1]);
            ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(-count[2]);
            //
            AdaptiveSplitNode tmpParent = (AdaptiveSplitNode) this.parent;
            Node newNode = subtree.getTreeRoot();
            ((Iadem3) tree).newTreeChange();
            if (tmpParent == null) {
                ((Iadem3) tree).copyTree(subtree);
            } else {
                for (int i = 0; i < tmpParent.children.size(); i++) {
                    if (tmpParent.children.get(i) == this) {
                        tmpParent.children.set(i, newNode);
                        newNode.parent = tmpParent;
                    }
                }
            }
            updateAttributes(newNode);
            if (newNode instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode splitNode = (AdaptiveSplitNode) newNode;
                for (Iadem3Subtree currentSubtree : splitNode.alternativeTree) {
                    updateSubtreeLevel(currentSubtree.getTreeRoot());
                }
            }
            return newNode;
        }

        void updateAttributes(Node newNode) {
            if (newNode == null) {
                return;
            }
            newNode.setTree(this.tree);
            if (newNode instanceof AdaptiveSplitNode) {
                AdaptiveSplitNode splitNode = (AdaptiveSplitNode) newNode;
                splitNode.leaf.setTree(this.tree);
                for (Node child : splitNode.children) {
                    updateAttributes(child);
                }
            } else if (newNode instanceof AdaptiveLeafNode) {
                AdaptiveLeafNode leafNode = (AdaptiveLeafNode) newNode;
                AutoExpandVector<VirtualNode> virtualChildren = leafNode.getVirtualChildren();
                for (VirtualNode child : virtualChildren) {
                    if (child != null) {
                        child.setTree(this.tree);
                    }
                }
            }
        }

        protected void updateSubtreeLevel(Node node) {
            if (node != null) {
                ((Iadem3) node.getTree()).treeLevel--;
                if (node instanceof AdaptiveSplitNode) {
                    AdaptiveSplitNode splitNode = (AdaptiveSplitNode) node;
                    for (Node child : splitNode.children) {
                        updateSubtreeLevelAux(child);
                    }
                    for (Iadem3Subtree subtree : splitNode.alternativeTree) {
                        updateSubtreeLevel(subtree.getTreeRoot());
                    }
                }
            }
        }

        protected void updateSubtreeLevelAux(Node node) {
            if (node != null) {
                if (node instanceof AdaptiveSplitNode) {
                    AdaptiveSplitNode splitNode = (AdaptiveSplitNode) node;
                    // update level in alternative subtrees
                    for (Iadem3Subtree subtree : splitNode.alternativeTree) {
                        updateSubtreeLevel(subtree.getTreeRoot());
                    }
                    for (Node child : splitNode.children) {
                        updateSubtreeLevelAux(child);
                    }
                }
            }
        }

        void createTree(Instance instance) throws IademException {
            Iadem3 iadem3Tree = ((Iadem3) this.tree);
            if (iadem3Tree.canCreateSubtree()) {
                int maxTreeLevel = iadem3Tree.getMaxNestingLevels();
                int maxAltSubtrees = iadem3Tree.getMaxAltSubtreesPerNode();
                if ((maxTreeLevel == -1 || iadem3Tree.getTreeLevel() < maxTreeLevel)
                        && (maxAltSubtrees == -1 || this.alternativeTree.size() < maxAltSubtrees)) {
                    if (this.estimator != null) {
                        Iadem3Subtree subtree = new Iadem3Subtree(this,
                                iadem3Tree.getTreeLevel() + 1,
                                (Iadem3) this.tree,
                                instance);
                        this.alternativeTree.add(subtree);
                        ((Iadem3) tree).setNewTree();
                    }
                }
            }
        }

        public int getNumTrees() {
            int trees = this.alternativeTree.size() == 0 ? 0 : 1;
            for (Node child : children) {
                if (child instanceof AdaptiveSplitNode) {
                    trees += ((AdaptiveSplitNode) child).getNumTrees();
                }
            }
            for (Iadem3Subtree subtree : this.alternativeTree) {
                if (subtree.getTreeRoot() instanceof AdaptiveSplitNode) {
                    AdaptiveSplitNode node = (AdaptiveSplitNode) subtree.getTreeRoot();
                    trees += node.getNumTrees();
                }
            }
            return trees;
        }

        @Override
        public double[] getClassVotes(Instance observacion) {
            double[] classDist = this.leaf.getClassVotes(observacion);

            double thisError = this.estimator.getEstimation();
            double leafError = this.leaf.estimator.getEstimation();

            int childIndex = instanceChildIndex(observacion);
            if (childIndex >= 0 && thisError < leafError) {
                Node hijo = getChild(childIndex);
                classDist = hijo.getClassVotes(observacion);
            }

            for (Iadem3Subtree subtree : this.alternativeTree) {
                double[] tmp = subtree.getClassVotes(observacion);
                double altWeight = 1.0 - subtree.estimacionValorMedio();
                for (int j = 0; j < classDist.length; j++) {
                    classDist[j] = classDist[j] + tmp[j] * altWeight;
                }
            }
            return classDist;
        }

        @Override
        public int getSubtreeNodeCount() {
            int tmp = super.getSubtreeNodeCount();
            for (Iadem3Subtree subtree : this.alternativeTree) {
                tmp += subtree.getTreeRoot().getSubtreeNodeCount();
            }
            return tmp;
        }

        public double getErrorEstimation() {
            return this.estimator.getEstimation();
        }

        @Override
        public void getNumberOfNodes(int[] count) {
            for (Iadem3Subtree tree : this.alternativeTree) {
                tree.getNumberOfNodes(count);
            }
            if (this.causeOfSplit == ((Iadem3) this.tree).SPLIT_BY_TIE_BREAKING) {
                count[2]++;
            }
            super.getNumberOfNodes(count);
        }

        public int getNumberOfSubtrees() {
            int count = this.alternativeTree.size();
            for (Iadem3Subtree subtree : this.alternativeTree) {
                count += ((Iadem3) subtree).getNumberOfSubtrees();
            }
            for (Node child : children) {
                if (child instanceof AdaptiveSplitNode) {
                    count += ((AdaptiveSplitNode) child).getNumberOfSubtrees();
                }
            }
            return count;
        }

        private void prune() {
            this.leaf.setSplit(true);
            for (Node node = this.parent; node != null; node = node.parent) {
                ((AdaptiveSplitNode) node).leaf.restartVariablesAtDrift();
                this.leaf.estimator = (AbstractChangeDetector) this.leaf.estimator.copy();
            }
            // update tree
            this.leaf.setTree(this.tree);
            AutoExpandVector<VirtualNode> nodeList = this.leaf.getVirtualChildren();
            for (VirtualNode node : nodeList) {
                if (node != null) {
                    node.setTree(this.tree);
                }
            }

            this.leaf.setParent(this.parent);

            if (this.parent == null) {
                this.tree.setTreeRoot(this.leaf);
            } else {
                ((SplitNode) this.parent).changeChildren(this, this.leaf);
            }

            int count[] = new int[3];
            getNumberOfNodes(count);

            ((Iadem3) this.tree).updateNumberOfLeaves(-count[1] + 1);
            ((Iadem3) this.tree).updateNumberOfNodes(-count[0] - count[1] + 1);
            ((Iadem3) this.tree).updateNumberOfNodesSplitByTieBreaking(-count[2]);
        }
    }

    public interface restartsVariablesAtDrift {

        public void resetVariablesAtDrift();
    }
}
