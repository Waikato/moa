 /*
 *    ORTO.java
 *    Copyright (C) Jožef Stefan Institute, Ljubljana
 *    @author Aljaž Osojnik
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

/* Based on the FIMTDD implementation by Katie de Lange, E. Almeida, J. Gama. See FIMTDD.java.
 *
 * Contact: aljaz.osojnik@ijs.si
 */

package moa.classifiers.trees;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import moa.AbstractMOAObject;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NullAttributeClassObserver;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion;
// import moa.classifiers.core.splitcriteria.SDRSplitCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.core.AutoExpandVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.options.ClassOption;
// import weka.core.Utils;

/*
 * Implementation of ORTO, option tree for data streams.
 */

public class ORTO extends AbstractClassifier implements Regressor{

    private static final long serialVersionUID = 1L;

    //============================== INTERNALS ===============================//
    
    protected Node treeRoot;
    
    private int leafNodeCount = 0;
    private int innerNodeCount = 0;
    private int optionNodeCount = 0;
    
    private int numTrees = 1;

    protected int maxDepth = 0;
    
    protected double inactiveLeafByteSizeEstimate;

    protected double activeLeafByteSizeEstimate;

    protected double byteSizeEstimateOverheadFraction;

    // Store the lowest node (lowest level) in the tree that requires adaptation
    protected ArrayList<InnerNode> nodesToAdapt = new ArrayList<InnerNode>();
    
    protected boolean Adaptable = true;
    
    protected double initLearnRate = 0.1;
    
    protected double learnRateDecay = 0.001;
    
    public int maxID = 0;
    
    private double learnTime = 0.0;
    
    private double predictTime = 0.0;
    
    //============================ END INTERNALS =============================//
    
    //============================= SET OPTIONS ==============================//

    public FloatOption PageHinckleyAlphaOption = new FloatOption(
            "PageHinckleyAlpha",
            'a',
            "The alpha value to use in the Page Hinckley change detection tests.",
            0.005, 0.0, 1.0);
        
    public IntOption PageHinckleyThresholdOption = new IntOption(
            "PageHinckleyThreshold",
            'h',
            "The threshold value to be used in the Page Hinckley change detection tests.",
            50, 0, Integer.MAX_VALUE);
        
    public FloatOption AlternateTreeFadingFactorOption = new FloatOption(
            "AlternateTreeFadingFactor",
            'f',
            "The fading factor to use when deciding if an alternate tree should replace an original.",
            0.995, 0.0, 1.0);
        
    public IntOption AlternateTreeTMinOption = new IntOption(
            "AlternateTreeTMin",
            'y',
            "The Tmin value to use when deciding if an alternate tree should replace an original.",
            150, 0, Integer.MAX_VALUE);
        
    public IntOption AlternateTreeTimeOption = new IntOption(
            "AlternateTreeTime",
            'u',
            "The 'time' (in terms of number of instances) value to use when deciding if an alternate tree should be discarded.",
            1500, 0, Integer.MAX_VALUE);
    
    public FloatOption LearningRatioOption = new FloatOption(
            "LearningRatio",
            'w',
            "Learning ratio to use for training the Perceptrons in the leaves.",
            0.01, 0.0, 1.0);
    
    public FlagOption LearningRatioDecayOrConstOption = new FlagOption(
            "LearningRatioDecayOrConst",
            'j',
            "learning Ratio Decay or const parameter.");
    
    public IntOption MaxTreesOption = new IntOption(
            "MaxTrees",
            'm',
            "The maximum number of trees contained in the option tree.",
            10, 1, Integer.MAX_VALUE);
    
    public IntOption MaxOptionLevelOption = new IntOption(
            "MaxOptionLevel",
            'l',
            "The maximal depth at which option nodes can be created.",
            10, 0, Integer.MAX_VALUE);

    public FloatOption OptionDecayFactorOption = new FloatOption(
            "OptionDecayFactor",
            'd',
            "The option decay factor that determines how many options can be selected at a given level.",
            0.9, 0.0, 1.0);
            
    public ClassOption splitCriterionOption = new ClassOption(
            "splitCriterion",
            's',
            "Split criterion to use.",
            VarianceReductionSplitCriterion.class,
            "VarianceReductionSplitCriterion");
    
    public ClassOption numericEstimatorOption = new ClassOption(
            "numericEstimator",
            'n',
            "Numeric estimator to use.",
            FIMTDDNumericAttributeClassObserver.class,
            "FIMTDDNumericAttributeClassObserver");

    public IntOption gracePeriodOption = new IntOption(
            "gracePeriod",
            'g',
            "The number of instances a leaf should observe between split attempts.",
            200, 0, Integer.MAX_VALUE);
    
    public FloatOption splitConfidenceOption = new FloatOption(
            "splitConfidence",
            'c',
            "The allowable error in split decision, values closer to 0 will take longer to decide.",
            0.0000001, 0.0, 1.0);
    
    public FloatOption tieThresholdOption = new FloatOption(
            "tieThreshold",
            't',
            "Threshold below which a split will be forced to break ties.",
            0.05, 0.0, 1.0);
    
    public FlagOption removePoorAttsOption = new FlagOption(
            "removePoorAtts",
            'p',
            "Disable poor attributes.");
    
    public MultiChoiceOption OptionNodeAggregationOption = new MultiChoiceOption(
            "OptionNodeAggregation",
            'o',
            "The aggregation method used to combine predictions in option nodes.", 
            new String[]{"average", "bestTree"}, new String[]{"Average", "Best tree"}, 0);
    
    public FloatOption OptionFadingFactorOption = new FloatOption(
            "OptionFadingFactor",
            'q',
            "The fading factor used for comparing subtrees of an option node.",
            0.9995, 0.0, 1.0);
    
    //============================= END OPTIONS ==============================//
    
    //=============================== CLASSES ================================//
    
    public abstract static class Node extends AbstractMOAObject /*implements AdaptationCompatibleNode*/ {

        private static final long serialVersionUID = 1L;

        public int ID;
        
        protected InnerNode parent;
      
        protected Node alternateTree;
        
        protected boolean Alternate = false;
        
        protected boolean Adaptable = true;

        public Node(int id) {
            this.ID = id;
        }
        
        @Override
        public void getDescription(StringBuilder sb, int indent) {
        }

        public int calcByteSize() {
            return (int) SizeOf.fullSizeOf(this);
        }

        public boolean isLeaf() {
            return true;
        }

        public void calculateDetph(ORTO tree) {
            int level = this.getLevel();
            if (level > tree.maxDepth) {
                tree.maxDepth = level;
            }
        }
        
        public int getLevel() {
            Node target = (Node) this.getParent();
            while (target instanceof OptionNode) {
                target = (Node) target.getParent();
            }
            if (target == null) {
                if (!Alternate) { 
                    // Actual tree root
                    return 0;
                } else {
                    // Root of alternate tree
                    return alternateTree.getLevel();
                }
            } else {
                return target.getLevel() + 1;
            }
        }
        
        /**
        * Set the parent node
        */
        public void setParent(InnerNode parent)
        {
            this.parent = parent;
        }
        
        /**
        * Return the parent node
        */
        public InnerNode getParent()
        {
            return parent;
        }
        
        public void setChild(int parentBranch, Node node) {
        }
        
        public int getChildIndex(Node child) {
            return 0;
        }
        
        public int getNumSubtrees() {
            return 1;
        }
        
        public double[] processInstance(Instance inst, ORTO tree) {
            // The returned values represent (by index):
            // 0: the prediction of the node
            // 1: the faded MSE
            // 2: examples seen
            // 3: the back propagated PH error
            return new double[] {0.0, 0.0, 0.0, 0.0};
        }
        
        public double[] getPrediction(Instance inst, ORTO tree) {
            // The returned values represent (by index):
            // 0: the prediction of the node
            // 1: the faded MSE
            // 2: examples seen
            return new double[] {0.0, 0.0, 0.0};
        }
        
        public void setAdaptable(boolean value) {
            Adaptable = value;
        }
        
        public void setAlternate(boolean value) {
            Alternate = value;
        }
    }

    public abstract static class InnerNode extends Node {
        private static final long serialVersionUID = 1L;
        
        protected AutoExpandVector<Node> children = new AutoExpandVector<Node>();
        
        protected double PHmT = 0;
        protected double PHMT = Double.MAX_VALUE;

        // Keep track of the statistics for loss error calculations
        protected DoubleVector lossStatistics = new DoubleVector();

        protected int weightSeen = 0;
        protected int previousWeight = 0;
        
        public InnerNode(int id) {
            super(id);
        }
        
        public int numChildren() {
            return this.children.size();
        }

        public Node getChild(int index) {
            return this.children.get(index);
        }
        
        public int getChildIndex(Node child) {
            return this.children.indexOf(child);
        }
        
        public void setChild(int index, Node child) {
            this.children.set(index, child);
        }

        public void setAlternateTree(Node tree) {
            this.alternateTree = tree;
        }
        
        public Node getAlternateTree() {
            return this.alternateTree;
        }
        
        public int calcByteSize() {
            return (int) SizeOf.fullSizeOf(this) + (int) SizeOf.fullSizeOf(children);
        }
        
        public void calculateDetph(ORTO tree) {
            if (this.getLevel() > tree.maxDepth) {
                tree.maxDepth = this.getLevel();
            }
            
            for (Node child : children) {
                child.calculateDetph(tree);
            }
        }
        
        /**
         * Check to see if the tree needs updating
         */
        public boolean PageHinckleyTest(double error, double threshold)
        {    
            // Update the cumulative mT sum
            PHmT += error;

            // Update the minimum mT value if the new mT is
            // smaller than the current minimum
            if(PHmT < PHMT)
            {
                PHMT = PHmT;
            }
            // Return true if the cumulative value - the current minimum is
            // greater than the current threshold (in which case we should adapt)
            return PHmT - PHMT > threshold;
        }
        
        public void setAdaptable(boolean value) {
            Adaptable = value;
            
            for (Node child : children) {
                child.setAdaptable(value);
            }
        }
        
        public void setAlternate(boolean value) {
            Alternate = value;
            
            for (Node child : children) {
                child.setAlternate(value);
            }
        }
        
    }
    
    public static class SplitNode extends InnerNode {

        private static final long serialVersionUID = 1L;

        protected InstanceConditionalTest splitTest;
        
        public void setChild(int index, Node child) {
            if ((this.splitTest.maxBranches() >= 0) && (index >= this.splitTest.maxBranches())) {
                throw new IndexOutOfBoundsException();
            }
            this.children.set(index, child);
        }

        public SplitNode(InstanceConditionalTest splitTest, int id) {
            super(id);
            this.splitTest = splitTest;
        }

        public int instanceChildIndex(Instance inst) {
            return this.splitTest.branchForInstance(inst);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
        
        public int getNumSubtrees() {
            int num = 1;
            for (Node child : children) {
                num += child.getNumSubtrees();
            }
            num -= children.size();
            return num;
        }

        public double[] processInstance(Instance inst, ORTO tree) {
            int branch = splitTest.branchForInstance(inst);
            Node child = children.get(branch);
            if (child == null) {
                tree.maxID++;
                child = new ActiveLearningNode(tree.maxID);
                this.setChild(branch, child);
                child.setParent(this);
            }

            double[] processed = child.processInstance(inst, tree);
            
            weightSeen++;
            
            // Convert any nominal attributes to numeric ones???
            
            // If no model exists yet, begin with an empty leaf node (the root)    
            // Take the current example and traverse it through the tree to a leaf
            if (Adaptable) {
                if (this.alternateTree == null) {
                    // Retrieve the error for the found leaf node
                    // currentNode.learnFromInstance(inst, this);
                    double PHerror = processed[3] - tree.PageHinckleyAlphaOption.getValue();
                            
                    // Back-propagate the error through all the parent nodes
                    if(PageHinckleyTest(PHerror, tree.PageHinckleyThresholdOption.getValue())) {
                        if (tree.nodesToAdapt.contains(child)) {
                            tree.nodesToAdapt.remove(child);
                        }
                        tree.nodesToAdapt.add(this);
                    }
                } else if (this.alternateTree != null) {
    
                    // If an alternate tree already exists, check if the current tree should be replaced with it,
                    // or if the alternate tree should be discarded.
                    
                    // this.alternateTree.checkRoot();
                    
                    double[] processedAlt = this.alternateTree.processInstance(inst, tree);
                    
                    // Update the loss statistics for the alternate tree
                    double qAlt = processedAlt[1];
                    double qOrg = processed[1];
                                     
                    // Compute the Qi statistics
                    double Qi = Math.log(qOrg / qAlt);
                    lossStatistics.addToValue(0,1);
                    lossStatistics.addToValue(1,Qi);
                    double QiAverage = lossStatistics.getValue(1) / lossStatistics.getValue(0);
                    
                    if(weightSeen - previousWeight >= tree.AlternateTreeTMinOption.getValue()) {
    
                        // Update the weight at which a decision was tested for
                        previousWeight = weightSeen;
                        
    
                        // If appropriate, replace the current tree with the alternate tree
                        if(Qi > 0) {
                            // Replace the main FIMT-DD tree at a subtree
                            alternateTree.setAdaptable(true);
                            alternateTree.Alternate = false;
                            if(parent != null) {
                                parent.setChild(parent.getChildIndex(this), alternateTree);
                                alternateTree.setParent(parent);
                                tree.numTrees = tree.numTrees - this.getNumSubtrees() + alternateTree.getNumSubtrees();
                                alternateTree.alternateTree = null;
                            } else { // Or occasionally at the root of the tree
                                tree.numTrees = tree.numTrees - this.getNumSubtrees() + alternateTree.getNumSubtrees();
                                tree.treeRoot = alternateTree;
                                alternateTree.alternateTree = null;
                            }
                            tree.removeExcessTrees();
                        }
                        // Otherwise, check if the alternate tree should be discarded
                        else if (QiAverage < lossStatistics.getValue(2) && lossStatistics.getValue(0) >= (10 * tree.AlternateTreeTMinOption.getValue()) || weightSeen >= tree.AlternateTreeTimeOption.getValue()) {
                            // tree.nodesToAdapt.remove(tree.nodesToAdapt.indexOf(this));
                            this.alternateTree = null;
                            setAdaptable(true);
                        }
                        
                        lossStatistics.setValue(2, QiAverage);
                    }
                }
            }
            
            return processed;
            
        }
        
        public double[] getPrediction(Instance inst, ORTO tree) {
            int branch = splitTest.branchForInstance(inst);
            Node child = children.get(branch);
            if (child == null) {
                tree.maxID++;
                child = new ActiveLearningNode(tree.maxID);
                this.setChild(branch, child);
                child.setParent(this);
            }
            return child.getPrediction(inst, tree);
        }
        
    }
   
    public static class OptionNode extends InnerNode {
        
        private static final long serialVersionUID = 1L;
        
        protected double[] optionFFSSL;
        protected double[] optionFFSeen;
        // protected double[] optionBaseFFSSL;
        
        public OptionNode(int id) {
            super(id);
        }
       
        public void resetFF() {
            this.optionFFSSL = new double[this.children.size()];
            this.optionFFSeen = new double[this.children.size()];
            // this.optionBaseFFSSL = new double[this.children.size()];
            for (int i = 0; i < this.children.size(); i++) {
                this.optionFFSSL[i] = 0.0;
                this.optionFFSeen[i] = 0.0;
                // this.optionBaseFFSSL[i] = 0.0;
            }
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
        
        public int getNumSubtrees() {
            int num = 0;
            for (Node child : children) {
                num += child.getNumSubtrees();
            }
            return num;
        }
        
        public int directionForBestTree() {
            int d = 0;
            double tmp = 0.0, min = Double.MAX_VALUE;
            for (int i = 0; i < children.size(); i++) {
                tmp = optionFFSSL[i] / optionFFSeen[i];
                if (tmp < min) {
                    min = tmp;
                    d = i;
                }
            }
            return d;
        }
        
        public double[] getPrediction(Instance inst, ORTO tree) {
            double[][] predictions = new double[this.children.size()][];
            if (tree.OptionNodeAggregationOption.getChosenIndex() != 1) {
                int i = 0;
                for (i = 0; i < this.children.size(); i++) {
                    predictions[i] = this.getChild(i).getPrediction(inst, tree);
                }
                return aggregate(predictions, tree);
            } else {
                int d = directionForBestTree();
                return this.getChild(d).getPrediction(inst, tree);
            }
        }
        
        public double[] processInstance(Instance inst, ORTO tree) {
            double[][] processed = new double[this.numChildren()][];
            int i = 0;
            
            for (i = 0; i < this.numChildren(); i++) {
                processed[i] = this.getChild(i).processInstance(inst, tree); // All the children get to see the instance
            }
            
            double[] prediction;
            
            if (tree.OptionNodeAggregationOption.getChosenIndex() != 1) {
                prediction = aggregate(processed, tree);
            } else {
                prediction = processed[directionForBestTree()];
            }
            
            if (Adaptable) {
                if (this.alternateTree == null) {
                    // Retrieve the error for the found leaf node
                    double PHerror = inst.classValue() - prediction[0];
                            
                    // Back-propagate the error through all the parent nodes
                    if(PageHinckleyTest(PHerror, tree.PageHinckleyThresholdOption.getValue())) {
                        for (Node node : tree.nodesToAdapt) {
                            if (children.contains(node)) {
                                tree.nodesToAdapt.remove(node);
                            }
                        }
                        tree.nodesToAdapt.add(this);
                    }
                } else {
                    // If an alternate tree already exists, check if the current tree should be replaced with it,
                    // or if the alternate tree should be discarded.                  
                    double[] predictionAlt = this.alternateTree.processInstance(inst, tree);

                    // Update the loss statistics for the alternate tree
                    double qOrg = prediction[1];
                    double qAlt = predictionAlt[1];
                                     
                    // Compute the Qi statistics
                    double Qi = Math.log(qOrg / qAlt);
                    lossStatistics.addToValue(0,1);
                    lossStatistics.addToValue(1,Qi);
                    double QiAverage = lossStatistics.getValue(1) / lossStatistics.getValue(0);
                    
                    if (weightSeen - previousWeight >= tree.AlternateTreeTMinOption.getValue()) {
    
                        // Update the weight at which a decision was tested for
                        previousWeight = weightSeen;
    
                        // If appropriate, replace the current tree with the alternate tree
                        if(Qi > 0) {
                            alternateTree.setAdaptable(true);
                            alternateTree.Alternate = false;
                            if (parent != null) { // Replace the main tree at a subtree
                                parent.setChild(parent.getChildIndex(this), alternateTree);
                                tree.numTrees = tree.numTrees - this.getNumSubtrees() + alternateTree.getNumSubtrees();
                                alternateTree.setParent(parent);
                                this.alternateTree = null;
                            } else { // Or occasionally at the root of the tree
                                tree.treeRoot = this.alternateTree;
                                tree.numTrees = tree.numTrees - this.getNumSubtrees() + alternateTree.getNumSubtrees();
                                tree.Adaptable = true;
                                this.alternateTree = null;
                            }
                            tree.removeExcessTrees();
                        }
                        
                        // Otherwise, check if the alternate tree should be discarded
                        else if (QiAverage < lossStatistics.getValue(2) && lossStatistics.getValue(0) >= (10 * tree.AlternateTreeTMinOption.getValue()) || weightSeen >= tree.AlternateTreeTimeOption.getValue()) {
                            // tree.nodesToAdapt.remove(tree.nodesToAdapt.indexOf(this)); ?
                            this.alternateTree = null;
                            setAdaptable(true);
                        }
                        
                        lossStatistics.setValue(2, QiAverage);
                    }
                }
    
                double sqLoss;
                
                if (weightSeen + 1 > tree.gracePeriodOption.getValue() + 50) {
                    for (i = 0; i < this.children.size(); i++) {
                        sqLoss = Math.pow(processed[i][0] - inst.classValue(), 2);
                        optionFFSSL[i] = optionFFSSL[i] * tree.OptionFadingFactorOption.getValue() + sqLoss; 
                        optionFFSeen[i] = optionFFSeen[i] * tree.OptionFadingFactorOption.getValue() + 1;
                    }
                }
            }
            
            weightSeen++;
            
            return prediction;
        }
        
        private double[] aggregate(double[][] predictions, ORTO tree) {
            if (tree.OptionNodeAggregationOption.getChosenIndex() == 0) { // Average
                double[] average = new double[predictions[0].length];
                for (int i = 0; i < predictions[0].length; i++) {
                    average[i] = 0.0;
                }
                for (int i = 0; i < predictions[0].length; i++) {
                    for (int j = 0; j < predictions.length; j++) {
                        average[i] += predictions[j][i];
                    }
                    average[i] = average[i] / predictions.length; 
                }
                
                return average;
            } else {
                assert false : tree.OptionNodeAggregationOption.getChosenLabel();
                return new double[] {0.0};
            }
        }
        
        public double getFFRatio(int childIndex) {
            return optionFFSSL[childIndex] / optionFFSeen[childIndex];
        }
    }

    public static class ActiveLearningNode extends Node {

        private static final long serialVersionUID = 1L;
        
        // Create a Perceptron model that carries out the actual learning in each node
        public ORTOPerceptron learningModel = new ORTOPerceptron();
        
        // The statistics for this node:
        // Sum of y values
        // Sum of squared y values
        protected DoubleVector nodeStatistics = new DoubleVector();
        
        protected DoubleVector splitRatioStatistics = new DoubleVector();
        
        // The error values for the Page Hinckley test
        // PHmT = the cumulative sum of the errors
        // PHMT = the minimum error value seen so far
        protected double PHmT = 0;
        protected double PHMT = Double.MAX_VALUE;
        
        protected int examplesSeenAtLastSplitEvaluation;
        
        protected int examplesSeen = 0;

        protected AutoExpandVector<AttributeClassObserver> attributeObservers = new AutoExpandVector<AttributeClassObserver>();
        
        public ActiveLearningNode(int id) {
            super(id);
            this.learningModel = new ORTOPerceptron();
        }

        @Override
        public int calcByteSize() {
            return super.calcByteSize() + (int) (SizeOf.fullSizeOf(this.attributeObservers)) + (int) (SizeOf.fullSizeOf(this.learningModel));
        }

        /**
         * Return the best split suggestions for this node using the given split criteria
         */
        public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion, ORTO tree) {
            List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();
            
            // Set the nodeStatistics up as the preSplitDistribution, rather than the observedClassDistribution
            double[] nodeSplitDist = this.nodeStatistics.getArrayCopy();
            
            for (int i = 0; i < this.attributeObservers.size(); i++) {
                AttributeClassObserver obs = this.attributeObservers.get(i);
                if (obs != null) {
                    // AT THIS STAGE NON-NUMERIC ATTRIBUTES ARE IGNORED
                    AttributeSplitSuggestion bestSuggestion = null;
                    if (obs instanceof FIMTDDNumericAttributeClassObserver) {
                        bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion, nodeSplitDist, i, true /*ht.binarySplitsOption.isSet()*/);
                    }
                    if (bestSuggestion != null) {
                        bestSuggestions.add(bestSuggestion);
                    }
                }
            }
            return bestSuggestions.toArray(new AttributeSplitSuggestion[bestSuggestions.size()]);
        }
            

        public void disableAttribute(int attIndex) {
            this.attributeObservers.set(attIndex, new NullAttributeClassObserver());
        }
        
        public double getPHError(Instance inst) {
            double sd = Math.sqrt((nodeStatistics.getValue(2) - ((nodeStatistics.getValue(1) * nodeStatistics.getValue(1))/nodeStatistics.getValue(1)))/examplesSeen);
            double mean = nodeStatistics.getValue(2) / nodeStatistics.getValue(1);
            //     AbsErr(inst) - (SumAbsErr + AbsErr(inst)) / (N + 1)   || SumAbsErr only contains errors for the first N examples and not the last one
            return Math.abs( (inst.classValue() - learningModel.prediction(inst)) / sd ) - ((nodeStatistics.getValue(3) + Math.abs(((inst.classValue()-mean)/sd) - ((learningModel.prediction(inst)-mean)/sd))) / (nodeStatistics.getValue(1)+1));
        }

        /**
         * Returns the squared error, for use in determining if an alternate tree is performing better than an original
         * tree, or if the alternate tree should be deleted
         */
        public double getSquaredError() {
            return nodeStatistics.getValue(4);
        }
        
        /**
         * Return the error for a given instance
         */
        public double getError(Instance inst)
        {
            return inst.classValue() - learningModel.prediction(inst);
        }
        
        public double[] processInstance(Instance inst, ORTO tree) {
            double prediction = getPrediction(inst, tree)[0];
            examplesSeen++;

            // Update the statistics for this node
            // number of instances passing through the node
            nodeStatistics.addToValue(0, 1);
            // sum of y values
            // sum of squared y values
            nodeStatistics.addToValue(2, inst.classValue() * inst.classValue());
            
            // sum of absolute errors
            // Normalize values prior to calculating absolute error
            double sd = Math.sqrt((nodeStatistics.getValue(2) - ((nodeStatistics.getValue(1) * nodeStatistics.getValue(1))/examplesSeen))/examplesSeen);
            double error = this.getError(inst);
            nodeStatistics.addToValue(3, Math.abs(error / sd));
            // sum of squared errors
            // nodeStatistics.addToValue(4, error * error);
            nodeStatistics.setValue(4, nodeStatistics.getValue(4) * tree.AlternateTreeFadingFactorOption.getValue() + error * error);

            double ph = getPHError(inst);
            
            learningModel.trainOnInstanceImpl(inst, tree);

            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
                AttributeClassObserver obs = this.attributeObservers.get(i);
                if (obs == null) {
                    // At this stage all nominal attributes are ignored
                    if (inst.attribute(instAttIndex).isNumeric()) {
                        obs = tree.newNumericClassObserver();
                        this.attributeObservers.set(i, obs);
                    }
                }
                if (obs != null) {
                    ((FIMTDDNumericAttributeClassObserver) obs).observeAttributeClass(inst.value(instAttIndex), inst.classValue(), inst.weight());
                }
            }

            // If it has seen Nmin examples since it was last tested for splitting, attempt a split of this node
            if (examplesSeen - examplesSeenAtLastSplitEvaluation >= tree.gracePeriodOption.getValue()) {
                // Set the split criterion to use to the SDR split criterion as described by Ikonomovska et al. 
                SplitCriterion splitCriterion = (SplitCriterion) tree.getPreparedClassOption(tree.splitCriterionOption);

                // Using this criterion, find the best split per attribute and rank the results
                AttributeSplitSuggestion[] bestSplitSuggestions = getBestSplitSuggestions(splitCriterion, tree);
                List<AttributeSplitSuggestion> acceptedSplits = new LinkedList<AttributeSplitSuggestion>();
                Arrays.sort(bestSplitSuggestions);

                // Declare a variable to determine the number of splits to be performed
                int numSplits = 0;

                // If only one split was returned, use it
                if (bestSplitSuggestions.length == 1) {
                    numSplits = 1;
                    acceptedSplits.add(bestSplitSuggestions[0]);
                } else if (bestSplitSuggestions.length > 1) { // Otherwise, consider which of the splits proposed may be worth trying

                    // Determine the Hoeffding bound value, used to select how many instances should be used to make a test decision
                    // to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
                    double hoeffdingBound = computeHoeffdingBound(1, tree.splitConfidenceOption.getValue(), examplesSeen);
                    
                    // Determine the top two ranked splitting suggestions
                    AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
                    AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];

                    // assert false : bestSuggestion.merit;
                    
                    // If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
                    // as determined using the Hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
                    // particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
                    // splitting should occur.
                    // Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
                    // (default 0.05) is applied to the Hoeffding bound; if the Hoeffding bound is smaller than this limit then the two
                    // competing attributes are equally good, and the split will be made on the one with the higher SDR value.
                    if (secondBestSuggestion.merit / bestSuggestion.merit < 1 - hoeffdingBound) {
                        numSplits = 1;
                        acceptedSplits.add(bestSuggestion);
                    } else if (tree.numTrees < tree.MaxTreesOption.getValue() && getLevel() <= tree.MaxOptionLevelOption.getValue()) {
                        for (AttributeSplitSuggestion suggestion : bestSplitSuggestions) {
                            if (suggestion.merit / bestSuggestion.merit >= 1 - hoeffdingBound) {
                                numSplits++;
                                acceptedSplits.add(suggestion);
                            }
                        }
                    } else if (hoeffdingBound < tree.tieThresholdOption.getValue()) {
                        numSplits = 1;
                        acceptedSplits.add(bestSplitSuggestions[0]);
                    } else { // If the splitting criterion was not met, initiate pruning of the E-BST structures in each attribute observer
                        for (int i = 0; i < attributeObservers.size(); i++) {
                            AttributeClassObserver obs = attributeObservers.get(i);
                            if (obs != null) {
                                ((FIMTDDNumericAttributeClassObserver) obs).removeBadSplits(splitCriterion, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound);    
                            }
                        }
                    }
                    // If the user has selected this option, it is also possible to remove poor attributes at this stage
                    if ((tree.removePoorAttsOption != null) && tree.removePoorAttsOption.isSet()) {
                        Set<Integer> poorAtts = new HashSet<Integer>();
                        for (int i = 0; i < bestSplitSuggestions.length; i++) { // scan 1 - add any poor to set
                            if (bestSplitSuggestions[i].splitTest != null) {
                                int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
                                if (splitAtts.length == 1) {
                                    if (((bestSuggestion.merit / secondBestSuggestion.merit) + hoeffdingBound)  < 1) {
                                        poorAtts.add(new Integer(splitAtts[0]));
                                    }
                                }
                            }
                        }                     
                        for (int i = 0; i < bestSplitSuggestions.length; i++) { // scan 2 - remove good ones from set
                            if (bestSplitSuggestions[i].splitTest != null) {
                                int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
                                if (splitAtts.length == 1) {
                                    if (((bestSuggestion.merit / secondBestSuggestion.merit) + hoeffdingBound)  < 1) {
                                        poorAtts.remove(new Integer(splitAtts[0]));
                                    }
                                }
                            }
                        }
                        for (int poorAtt : poorAtts) {
                            this.disableAttribute(poorAtt);
                        }
                    }
                }
                
                // assert numSplits == 0 : numSplits;

                // If the splitting criterion were met, split the current node using the chosen attribute test, and
                // make two new branches leading to (empty) leaves
                if (numSplits > 0) {

                    double optionFactor = numSplits * Math.pow(tree.OptionDecayFactorOption.getValue(), (double) getLevel());

                    // Deactivate this node if the best split was to do nothing
                    if (numSplits == 1 || optionFactor < 2.0 || tree.MaxTreesOption.getValue() - tree.numTrees  <= 1) {
                        AttributeSplitSuggestion splitDecision = acceptedSplits.get(0);
                        tree.maxID++;
                        SplitNode newSplit = new SplitNode(splitDecision.splitTest, tree.maxID);
                        newSplit.Adaptable = Adaptable;
                        for (int i = 0; i < splitDecision.numSplits(); i++) {
                            tree.maxID++;
                            ActiveLearningNode newChild = new ActiveLearningNode(tree.maxID);
                            newChild.setParent(newSplit);
                            newChild.Adaptable = Adaptable;
                            newSplit.setChild(i, newChild);
                        }
                        tree.leafNodeCount--;
                        tree.innerNodeCount++;
                        tree.leafNodeCount += splitDecision.numSplits();
                        if (parent == null) {
                            tree.treeRoot = newSplit;
                        } else {
                            parent.setChild(parent.getChildIndex(this), newSplit);
                            newSplit.setParent(parent);
                        }
                    } else {
                        tree.maxID++;
                        OptionNode optionNode = new OptionNode(tree.maxID);
                        optionNode.Adaptable = Adaptable;
                        tree.leafNodeCount--;
                        int j = 0;
                        
                        for (AttributeSplitSuggestion splitDecision : acceptedSplits) {
                            if (j > optionFactor || tree.MaxTreesOption.getValue() - tree.numTrees <= 0) {
                                break;
                            }
                            tree.maxID++;
                            SplitNode newSplit = new SplitNode(splitDecision.splitTest, tree.maxID);
                            newSplit.Adaptable = Adaptable;
                            for (int i = 0; i < splitDecision.numSplits(); i++) {
                                tree.maxID++;
                                ActiveLearningNode newChild = new ActiveLearningNode(tree.maxID);
                                newChild.setParent(newSplit);
                                newSplit.setChild(i, newChild);
                                newChild.Adaptable = Adaptable;
                            }
                            
                            tree.leafNodeCount += splitDecision.numSplits();
                            tree.innerNodeCount++;
                            tree.numTrees++;

                            newSplit.setParent(optionNode);
                            optionNode.setChild(j, newSplit);
                            j++;
                        }
                        
                        tree.innerNodeCount++;
                        tree.optionNodeCount++;
                       
                        if (parent == null) {
                            tree.treeRoot = optionNode;
                        } else {
                            parent.setChild(parent.getChildIndex(this), optionNode);
                            optionNode.setParent(parent);
                        }
                        
                        optionNode.resetFF();
                    }
                }

                // Take note of how many instances were seen when this split evaluation was made, so we know when to perform the next split evaluation
                examplesSeenAtLastSplitEvaluation = examplesSeen;
            }
            
            return new double[] {prediction, nodeStatistics.getValue(4), examplesSeen, ph};
        }
        
        public double[] getPrediction(Instance inst, ORTO tree) {
            return new double[] {this.learningModel.prediction(inst), this.nodeStatistics.getValue(4), this.examplesSeen};
        }
        
    }
    
    /**
     * A Perceptron classifier modified to conform to the specifications of Ikonomovska et al.
     */
    public static class ORTOPerceptron extends AbstractMOAObject {
        private static final long serialVersionUID = 1L;
        
        // The Perception weights 
        protected double[] weightAttribute; 
        
        // Statistics used for error calculations
        protected DoubleVector attributeStatistics = new DoubleVector();
        protected DoubleVector squaredAttributeStatistics = new DoubleVector();
        
        // The number of instances contributing to this model
        protected int instancesSeen = 0;
        
        // If the model should be reset or not
        protected boolean reset;
        
        @Override
        public void getDescription(StringBuilder sb, int indent) {
        //TODO Auto-generated method stub
        }
        
        public ORTOPerceptron(ORTOPerceptron copy) {
                this.weightAttribute = copy.getWeights();
        }
        
        public ORTOPerceptron() {
                this.reset = true;
        }
        
        public void setWeights(double[] w) {
            this.weightAttribute = w;
        }
        
        public double[] getWeights() {
            return this.weightAttribute;
        }
                
        /**
         * A method to reset the model
         */
        public void resetLearningImpl() {
            this.reset = true;
        }
        
        /**
         * Update the model using the provided instance
         */
        public void trainOnInstanceImpl(Instance inst, ORTO ft) {
            // Initialize Perceptron if necessary   
            if (this.reset == true) {
                this.reset = false;
                this.weightAttribute = new double[inst.numAttributes()];
                this.instancesSeen = 0;
                this.attributeStatistics = new DoubleVector();
                this.squaredAttributeStatistics = new DoubleVector();
                for (int j = 0; j < inst.numAttributes(); j++) {
                    weightAttribute[j] = 2 * ft.classifierRandom.nextDouble() - 1;
                }
            }
            
            // Update attribute statistics
            instancesSeen++;
            for(int j = 0; j < inst.numAttributes() -1; j++)
            {
                attributeStatistics.addToValue(j, inst.value(j));    
                squaredAttributeStatistics.addToValue(j, inst.value(j)*inst.value(j));
            }
            
            // Update weights
            
            double learningRatio = 0.0;
            if(ft.LearningRatioDecayOrConstOption.isSet()){
                learningRatio = ft.LearningRatioOption.getValue();
            } else {
                learningRatio = ft.initLearnRate / (1 + instancesSeen * ft.learnRateDecay);
            }
            // double learningRatio = ft.learningRatioOption.getValue();
            double actualClass = inst.classValue();
            double predictedClass = this.prediction(inst);
            
            // SET DELTA TO ACTUAL - PREDICTED, NOT PREDICTED - ACTUAL AS SAID IN PAPER
            double delta = actualClass - predictedClass;
            
            for (int j = 0; j < inst.numAttributes() - 1; j++) {
                
                if (inst.attribute(j).isNumeric()) {
                    // Update weights. Ensure attribute values are normalized first
                    double sd = Math.sqrt((squaredAttributeStatistics.getValue(j) - ((attributeStatistics.getValue(j) * attributeStatistics.getValue(j))/instancesSeen))/instancesSeen);
                    double instanceValue = 0;
                    if (sd > 0.0000001) { // Limit found in implementation by Ikonomovska et al (2011)
                        instanceValue = (inst.value(j) - (attributeStatistics.getValue(j)/instancesSeen))/(3*sd);
                    }
                    this.weightAttribute[j] += learningRatio * delta * instanceValue;
                }
            }
            this.weightAttribute[inst.numAttributes() - 1] += learningRatio * delta;
        }
        
        /**
         * Output the prediction made by this perceptron on the given instance
         */
        public double prediction(Instance inst)
        {
            double prediction = 0;
            if (this.reset == false) {
                for (int j = 0; j < inst.numAttributes() - 1; j++) {
                    if(inst.attribute(j).isNumeric())
                    {    
                        prediction += this.weightAttribute[j] * inst.value(j);
                    }
                } 
                prediction += this.weightAttribute[inst.numAttributes() - 1];
            }
            
            // Return prediction to 3dp
            return (double)Math.round(prediction * 1000) / 1000;
        }
    }

    //============================= END CLASSES ==============================//
    
    //=============================== METHODS ================================//
        
    @Override
    public String getPurposeString() {
        return "Implementation of the ORTO tree as described by Ikonomovska et al.";
    }
    
    // For the moment at least, force the split criterion to be SDRSplitCriterion and the 
    // numeric estimator to be FIMTLDDNumericAttributeClassObserver
    public ORTO() {
//        numericEstimatorOption = new ClassOption("numericEstimator",
//                'n', "Numeric estimator to use.", FIMTDDNumericAttributeClassObserver.class,
//                "FIMTDDNumericAttributeClassObserver");
                
        splitCriterionOption = new ClassOption("splitCriterion",
                's', "Split criterion to use.", VarianceReductionSplitCriterion.class,
                "VarianceReductionSplitCriterion");
    }

    @Override
    public void resetLearningImpl() {
        this.treeRoot = null;
        this.numTrees = 1;
        this.innerNodeCount = 0;
        this.leafNodeCount = 0;
        this.optionNodeCount = 0;
        this.maxID = 0;
        
        this.learnTime = 0.0;
        this.predictTime = 0.0;
    }
    
    public boolean isRandomizable() {
        return true;
    }

    protected void checkRoot() {
        if (treeRoot == null) {
            maxID++;
            treeRoot = new ActiveLearningNode(maxID);
            leafNodeCount = 1;
        }
    }
    
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        // if (this.treeRoot != null) {
        //    this.treeRoot.calculateDetph(this);
        // }
        return new Measurement[]{ 
                    new Measurement("number of subtrees", this.numTrees),
                    new Measurement("tree size (nodes)", this.leafNodeCount + this.innerNodeCount),
                    new Measurement("tree size (leaves)", this.leafNodeCount),
                    new Measurement("number of option nodes", this.optionNodeCount),
                    // new Measurement("tree depth", this.maxDepth),
                    // new Measurement("option count", this.countOptions()),
                    // new Measurement("learning time", learnTime),
                    // new Measurement("prediction time", predictTime),
                    /*new Measurement("tree depth", measureTreeDepth()),
                    new Measurement("active leaf byte size estimate",
                    this.activeLeafByteSizeEstimate),
                    new Measurement("inactive leaf byte size estimate",
                    this.inactiveLeafByteSizeEstimate),
                    new Measurement("byte size estimate overhead",
                    this.byteSizeEstimateOverheadFraction),
                    new Measurement("maximum prediction paths used",
                    this.maxPredictionPaths) */ };
    }

    public int calcByteSize() {
        int size = (int) SizeOf.sizeOf(this);
        if (this.treeRoot != null) {
            size += this.treeRoot.calcByteSize();
        }
        return size;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        
        if (this.treeRoot != null) {
            double start = System.nanoTime();
            double[] out = {this.treeRoot.getPrediction(inst, this)[0]};
            predictTime += System.nanoTime() - start;
            
            return out;
        }
        return new double[0];
    }
    
    //================= TRAIN and TEST ================//
    /**
     * Method for updating (training) the model using a new instance
     */
    @Override
    public void trainOnInstanceImpl(Instance inst) {
    
        double start = System.nanoTime();
        
        checkRoot();

        treeRoot.processInstance(inst, this);
                        
        for (InnerNode node : nodesToAdapt) {
            if (node.Adaptable) {
                maxID++;
                node.alternateTree = new ActiveLearningNode(maxID);
                node.alternateTree.Adaptable = false;
                node.alternateTree.Alternate = true;
                node.alternateTree.alternateTree = node;
                
                node.setAdaptable(false);
                
                // Reset the node statistics
                node.lossStatistics.setValue(0,0);
                node.lossStatistics.setValue(1,0);
                node.lossStatistics.setValue(2,0);
                node.PHmT = 0;
                node.PHMT = Double.MAX_VALUE; // TODO Does this reset too?
                node.weightSeen = 0;
                node.previousWeight = 0;
            }
        }
        nodesToAdapt = new ArrayList<InnerNode>();
        
        learnTime += System.nanoTime() - start;
    }
    
    protected AttributeClassObserver newNumericClassObserver() {
        AttributeClassObserver numericClassObserver = (AttributeClassObserver) getPreparedClassOption(numericEstimatorOption); // FIXME fix this
        // AttributeClassObserver observer = new FIMTDDNumericAttributeClassObserver();
        return (AttributeClassObserver) numericClassObserver;
    }

    public static double computeHoeffdingBound(double range, double confidence, double n) {
        return Math.sqrt(((range * range) * Math.log(1.0 / confidence)) / (2.0 * n));
    }
    
    protected Node findWorstOption() {
        Stack<Node> stack = new Stack<Node>();
        stack.add(this.treeRoot);

        double ratio = Double.MIN_VALUE;
        Node out = null;
        
        while (!stack.empty()) {
            Node node = stack.pop();
            if (node.parent instanceof OptionNode) {
                OptionNode myParent = (OptionNode) node.parent;
                int myIndex = myParent.getChildIndex(node);
                double myRatio = myParent.getFFRatio(myIndex); 
                
                if (myRatio > ratio) {
                    ratio = myRatio;
                    out = node;
                }
            }
            if (node instanceof InnerNode) {
                for (Node child : ((InnerNode) node).children) {
                    stack.add(child);
                }
            }
        }

        return out;
    }
    
    protected void removeExcessTrees() {
        while (numTrees > MaxTreesOption.getValue()) {
            Node option = findWorstOption();
            OptionNode parent = (OptionNode) option.parent;
            int index = parent.getChildIndex(option);
  
            if (parent.children.size() == 2) {
                parent.children.remove(index);
                
                for (Node chld : parent.children) {
                    chld.parent = parent.parent;
                    parent.parent.setChild(parent.parent.getChildIndex(parent), chld);
                }
            } else {
                AutoExpandVector<Node> children = new AutoExpandVector<Node>();
                double[] optionFFSSL = new double[parent.children.size() - 1];
                double[] optionFFSeen = new double[parent.children.size() - 1];
                
                int seen = 0;
                
                for (int i = 0; i < parent.children.size() - 1; i++) {
                    if (parent.getChild(i) != option) {
                        children.add(parent.getChild(i));
                        optionFFSSL[i] = parent.optionFFSSL[i + seen];
                        optionFFSeen[i] = parent.optionFFSeen[i + seen];
                    } else {
                        seen = 1;
                    }
                }
                
                parent.children = children;
                parent.optionFFSSL = optionFFSSL;
                parent.optionFFSeen = optionFFSeen;
                
                assert parent.children.size() == parent.optionFFSSL.length;
            }
  
            numTrees--;
        }
    }
  
}