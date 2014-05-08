/*
 *    FFIMTDD.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author Katie de Lange, E. Almeida, J. Gama
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

/* Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC, 
 *
 * Contact: jgama@fep.up.pt
 */

package moa.classifiers.trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.AbstractMOAObject;
import moa.classifiers.Classifier;
import moa.classifiers.Regressor;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.functions.FIMTDDPerceptron;
import moa.classifiers.AbstractClassifier;
import moa.core.AutoExpandVector;
import moa.core.Measurement;
import moa.core.SizeOf;
import moa.core.StringUtils;

/*
 * Implementation of FIMTDD, regression and model trees for data streams.
 */

public class FIMTDD extends AbstractClassifier implements Regressor  {

    private static final long serialVersionUID = 1L;
    private long examplesSeen = 0;
    private Node treeRoot;
    private int leafNodeCount = 0;

    public String getPurposeString() {
        return "New implementation of the FIMT-DD tree as described by Ikonomovska et al.";
    }

    public FIMTDD() {}
    
    // Store the lowest node in the tree that requires adaptation
    protected ArrayList<SplitNode> nodesToAdapt = new ArrayList<SplitNode>();

    //region ================ OPTIONS ================

    public ClassOption splitCriterionOption = new ClassOption(
            "splitCriterion",
            's',
            "Split criterion to use.",
            SplitCriterion.class,
            "moa.classifiers.core.splitcriteria.VarianceReductionSplitCriterion");
    
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

    public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
            't', "Threshold below which a split will be forced to break ties.",
            0.05, 0.0, 1.0);

    //
    //    public FlagOption removePoorAttsOption = new FlagOption("removePoorAtts",
    //            'r', "Disable poor attributes.");

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
    
    public FlagOption regressionTreeOption = new FlagOption("regressionTree", 'r', "Build a regression tree instead of a model tree.");

    public ClassOption leafModelOption = new ClassOption(
            "leafModel",
            'm',
            "The type of model used in the leaves (not used if the regressionTree flag is set).",
            Regressor.class,
            "moa.classifiers.functions.FIMTDDPerceptron");
        
    //endregion ================ OPTIONS ================
    //region ================ CLASSES ================

    abstract static class Node extends AbstractMOAObject {

        private static final long serialVersionUID = 1L;

        protected double weightSeenAtLastSplitEvaluation;

        // The parent of this particular node
        protected SplitNode parent;
        
        protected AutoExpandVector<FIMTDDNumericAttributeClassObserver> attributeObservers = new AutoExpandVector<FIMTDDNumericAttributeClassObserver>();

        // The error values for the Page Hinckley test
        // PHmT = the cumulative sum of the errors
        // PHMT = the minimum error value seen so far
        protected boolean changeDetection = true;
        protected double PHmT = 0;
        protected double PHMT = Double.MAX_VALUE;

        // The statistics for this node:
        // Number of instances that have reached it
        protected double examplesSeen;
        // Sum of y values
        protected double sumOfValues;
        // Sum of squared y values
        protected double sumOfSquares;
        // Sum of absolute errors
        protected double sumOfAbsErrors;
        // Sum of squared errors
        protected double sumOfSquaredErrors;
        


        public Node() {
        }

        public int calcByteSize() {
            return (int) (SizeOf.sizeOf(this)) + (int) (SizeOf.fullSizeOf(this.attributeObservers));
        }

        public int calcByteSizeIncludingSubtree() {
            return calcByteSize();
        }

        public boolean isLeaf() {
            return true;
        }

        public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent, int parentBranch, AutoExpandVector<SplitNode> alternateNodes) {
            return new FoundNode((LeafNode) this, parent, parentBranch, alternateNodes);
        }

        public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent, int parentBranch) {
            return filterInstanceToLeaf(inst, parent, parentBranch, null);
        }

        public double examplesSeen() {
            return examplesSeen;
        }

        /**
         * Set the parent node
         */
        public void setParent(SplitNode parent) {
            this.parent = parent;    
        }

        /**
         * Return the parent node
         */
        public SplitNode getParent() {
            return parent;
        }

        public void disableChangeDetection() {
            changeDetection = false;
        }

        public void restartChangeDetection() {
            changeDetection = true;
            PHmT = 0;
            PHMT = Integer.MAX_VALUE;
        }

        /**
         * Check to see if the tree needs updating
         */
        public boolean PageHinckleyTest(double error, double threshold) {    
            // Update the cumulative mT sum
            PHmT += error;

            // Update the minimum mT value if the new mT is
            // smaller than the current minimum
            if(PHmT < PHMT) {
                PHMT = PHmT;
            }
            // Return true if the cumulative value - the current minimum is
            // greater than the current threshold (in which case we should adapt)
            return PHmT - PHMT > threshold;
        }

        public void getDescription(StringBuilder sb, int i) {}
        
        public int countLeaves() {
        	return 1;
        }
        
        public void describeSubtree(FIMTDD tree, StringBuilder out, int indent) {
            StringUtils.appendIndented(out, indent, "Leaf");
        }
    }

    /**
     * A modified ActiveLearningNode that uses a Perceptron as the leaf node
     * model, and ensures that the class values sent to the attribute observers
     * are not truncated to ints if regression is being performed
     */
    public static class LeafNode extends Node {

        private static final long serialVersionUID = 1L;

        // Perceptron model that carries out the actual learning in each node
//        public FIMTDDPerceptron learningModel;
        public AbstractClassifier learningModel;

        protected double examplesSeenAtLastSplitEvaluation = 0;

        public double examplesSeenAtLastSplitEvaluation() {
            return examplesSeenAtLastSplitEvaluation;
        }

        public void setExamplesSeenAtLastSplitEvaluation(double seen) {
            examplesSeenAtLastSplitEvaluation = seen;
        }


        /**
         * Create a new LeafNode
         */
        public LeafNode(FIMTDD tree) {
            if (tree.buildingModelTree()) {
            	learningModel = (AbstractClassifier) tree.newLeafModel();
            	learningModel.resetLearningImpl();
            }
            examplesSeen = 0;
            sumOfValues = 0;
            sumOfSquares = 0;
            sumOfAbsErrors = 0;
            sumOfSquaredErrors = 0;
        }

        /**
         * Method to learn from an instance that passes the new instance to the perceptron learner,
         * and also prevents the class value from being truncated to an int when it is passed to the
         * attribute observer
         */
        public void learnFromInstance(Instance inst, FIMTDD tree) {
            // Update the statistics for this node
            // number of instances passing through the node
            examplesSeen += 1;
            
            // sum of y values
            sumOfValues += inst.classValue();
            
            // sum of squared y values
            sumOfSquares += inst.classValue() * inst.classValue();
            
            // sum of absolute errors
            // Normalize values prior to calculating absolute error
            double sd = Math.sqrt((sumOfSquares - sumOfValues * sumOfValues / examplesSeen) / examplesSeen);
            if (sd > 0) sumOfAbsErrors += Math.abs( (inst.classValue() - getPrediction(inst, tree)) / sd);

            // sum of squared errors
            sumOfSquaredErrors = getSquaredError(inst, tree) + sumOfSquaredErrors * tree.AlternateTreeFadingFactorOption.getValue();

            if (tree.buildingModelTree()) learningModel.trainOnInstanceImpl(inst);

            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
                FIMTDDNumericAttributeClassObserver obs = attributeObservers.get(i);
                if (obs == null) {
                    // At this stage all nominal attributes are ignored
                    if (inst.attribute(instAttIndex).isNumeric()) {
                        obs = tree.newNumericClassObserver();
                        this.attributeObservers.set(i, obs);
                    }
                }
                if (obs != null) {
                    obs.observeAttributeClass(inst.value(instAttIndex),inst.classValue(), inst.weight());
                }
            }
        }

        /**
         * Return the best split suggestions for this node using the given split criteria
         */
        public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion, FIMTDD tree) {

            List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();

            // Set the nodeStatistics up as the preSplitDistribution, rather than the observedClassDistribution
            double[] nodeSplitDist = new double[] {examplesSeen, sumOfValues, sumOfSquares, sumOfAbsErrors, sumOfSquaredErrors};

            for (int i = 0; i < this.attributeObservers.size(); i++) {
                FIMTDDNumericAttributeClassObserver obs = this.attributeObservers.get(i);
                if (obs != null) {

                    // AT THIS STAGE NON-NUMERIC ATTRIBUTES ARE IGNORED
                    AttributeSplitSuggestion bestSuggestion = null;
                    if (obs instanceof FIMTDDNumericAttributeClassObserver) {
                        bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion,nodeSplitDist, i, false);
                    }

                    if (bestSuggestion != null) {
                        bestSuggestions.add(bestSuggestion);
                    }
                }
            }
            return bestSuggestions.toArray(new AttributeSplitSuggestion[bestSuggestions.size()]);
        }

        /**
         * Retrieve the class votes using the perceptron learner
         */
        public double getPredictionModel(Instance inst, FIMTDD tree) {
            return learningModel.getVotesForInstance(inst)[0];

        }

        public double getPredictionTargetMean(Instance inst, FIMTDD tree) {
            return (examplesSeen > 0.0) ? sumOfValues / examplesSeen : 0.0;
        }
        
        public double getPrediction(Instance inst, FIMTDD tree) {
        	return (tree.buildingModelTree()) ? getPredictionModel(inst, tree) : getPredictionTargetMean(inst, tree);
        }

        public double[] getClassVotes(Instance inst, FIMTDD tree) {
            return new double[] {getPrediction(inst, tree)};
        }

        /**
         * Return the error for a given instance
         */
        public double getSquaredError(Instance inst, FIMTDD tree) {
            return Math.pow(inst.classValue() - getPrediction(inst, tree), 2);
        }

        /**
         * A method returning the Page-Hinckley error produced on this instance in the leaf node,
         * which will then be back-propagated back through the tree to allow the 
         * Page-Hinckley change detection tests to be updated
         */
        public double getPHError(Instance inst, FIMTDD tree) {
        	if (examplesSeen > 1) {
        		double prediction = getPrediction(inst, tree);
                double sd = Math.sqrt((sumOfSquares - ((sumOfValues * sumOfValues)/examplesSeen))/examplesSeen);
                return Math.abs(((inst.classValue() - prediction)/sd)) //absolutError  
                        -  ((sumOfAbsErrors + Math.abs(((inst.classValue() - prediction)/sd)))  //sumAbsolutError
                                / (examplesSeen + 1)); //number instance seen
            } else return 0;
        }

        /**
         * Returns the squared error, for use in determining if an alternate tree is performing better than an original
         * tree, or if the alternate tree should be deleted
         */
        
        public void checkForSplit(FIMTDD tree) {

            // If it has seen Nmin examples since it was last tested for splitting, attempt a split of this node
            if (examplesSeen - examplesSeenAtLastSplitEvaluation >= tree.gracePeriodOption.getValue()) {
                int index = (parent != null) ? parent.getChildIndex(this) : 0;
                tree.FIMTDD_attemptToSplit(this, parent, index);

                // Take note of how many instances were seen when this split evaluation was made, so we know when to perform the next split evaluation
                examplesSeenAtLastSplitEvaluation = examplesSeen;
            }
            
            
        }
        
        public void describeSubtree(FIMTDD tree, StringBuilder out, int indent) {
            StringUtils.appendIndented(out, indent, "Leaf ");
            out.append(tree.getClassNameString());
            out.append(" = ");
            if (tree.buildingModelTree()) {
            	learningModel.getModelDescription(out, 0);
            } else {
            	out.append(String.format("%.4f", (sumOfValues / examplesSeen)));
                StringUtils.appendNewline(out);
            }
        }

    }

    /**
     * A modified SplitNode method implementing the extra information regarding it's parent,
     * and the ability to track the error rate and perform Page-Hinckley tests
     */
    public static class SplitNode extends Node {

        private static final long serialVersionUID = 1L;

        protected InstanceConditionalTest splitTest;

        protected AutoExpandVector<Node> children = new AutoExpandVector<Node>();

        // Scope for an alternate tree that may need to be grown from this node
        protected Node alternateTree;

        // Keep track of the statistics for loss error calculations
        protected double lossExamplesSeen;
        protected double lossFadedSumOriginal;
        protected double lossFadedSumAlternate;
        protected double lossNumQiTests;
        protected double lossSumQi;
        protected double previousWeight = 0;

        /**
         * Create a new SplitNode
         */
        public SplitNode(InstanceConditionalTest splitTest) {
            this.splitTest = splitTest;
        }

        public void disableChangeDetection() {
            this.changeDetection = false;
            for (Node child : children) {
                child.disableChangeDetection();
           }
        }
        
        protected void setChild(int i, Node child) {
            children.set(i, child);
        }

        public int instanceChildIndex(Instance inst) {
            return this.splitTest.branchForInstance(inst);
        }

        public Node getChild(int i) {
            return children.get(i);
        }

        public int getChildIndex(Node child) {
            return children.indexOf(child);
        }
        
        @Override
        public FoundNode filterInstanceToLeaf(Instance inst, SplitNode parent, int parentBranch, AutoExpandVector<SplitNode> alternateNodes) {
            int childIndex = instanceChildIndex(inst);
            if (alternateTree != null) {
                if (alternateNodes == null) {
                    alternateNodes = new AutoExpandVector<SplitNode>();
                }
                alternateNodes.add(this);
            }
            if (childIndex >= 0) {
                Node child = getChild(childIndex);
                if (child != null) {
                    return child.filterInstanceToLeaf(inst, this, childIndex, alternateNodes);
                }
                return new FoundNode(null, this, childIndex, alternateNodes);
            }
            // This shouldn't happen
            return new FoundNode(null, parent, parentBranch, alternateNodes);
        }

        /**
         * This method allows alternate trees to process the incoming instances and checks if the
         * original tree should be replaced by the alternate tree and whether the alternate tree
         * should be discarded
         */
        public boolean processAlternateTree(Instance inst, FIMTDD tree, LeafNode originalLeaf) {
            // This should always happen
            if (this.alternateTree != null) {
                            	
            	lossExamplesSeen += 1;

                FoundNode foundNode = this.alternateTree.filterInstanceToLeaf(inst,null,-1);
                LeafNode alternateLeaf = foundNode.node;

                alternateLeaf.learnFromInstance(inst, tree);
                
                alternateLeaf.checkForSplit(tree);
                
                if (lossExamplesSeen - previousWeight >= tree.AlternateTreeTMinOption.getValue()) {
                    // Update the weight at which a decision was tested for
                    previousWeight = lossExamplesSeen;

                    double sOrg = lossFadedSumOriginal;
                    double lOrg = originalLeaf.getSquaredError(inst, tree);
                    
                    double sAlt = lossFadedSumAlternate;
                    double lAlt = alternateLeaf.getSquaredError(inst, tree);
                    
                    // Compute the Qi statistics
                    double Qi = Math.log((lOrg + tree.AlternateTreeFadingFactorOption.getValue() * sOrg)/(lAlt + tree.AlternateTreeFadingFactorOption.getValue() * sAlt));
                    double previousQiAverage = lossSumQi / lossNumQiTests;
                    lossNumQiTests += 1;
                    lossSumQi += Qi;
                    double QiAverage = lossSumQi / lossNumQiTests;

                    lossFadedSumOriginal = lOrg + tree.AlternateTreeFadingFactorOption.getValue() * sOrg;
                    lossFadedSumAlternate = lAlt + tree.AlternateTreeFadingFactorOption.getValue() * sAlt;
                    
                    // If appropriate, replace the current tree with the alternate tree
                    if(Qi > 0) {
                        SplitNode parent = this.parent;
                        tree.leafNodeCount += this.alternateTree.countLeaves() - originalLeaf.countLeaves();
                        // Replace the main FIMT-DD tree at a subtree
                        if (parent != null) {
                            Node replacementTree = this.alternateTree;
                            parent.setChild(parent.instanceChildIndex(inst), replacementTree);
                            boolean restart = true;
                            // If there are no alternate trees above this node, restart the change detection
                            while (parent != null && !restart) {
                            	restart = restart && parent.alternateTree == null;
                                parent = parent.getParent();
                            }
                            if (restart) replacementTree.restartChangeDetection();
                            if (replacementTree instanceof SplitNode) ((SplitNode)replacementTree).alternateTree = null;
                        } else { // Or occasionally at the root of the tree
                            tree.treeRoot = this.alternateTree;
                            tree.treeRoot.restartChangeDetection();
                            if (tree.treeRoot instanceof SplitNode) ((SplitNode)tree.treeRoot).alternateTree = null;
                        }
                        return true;
                    }
                    // Otherwise, check if the alternate tree should be discarded
                    else if (QiAverage < previousQiAverage && lossExamplesSeen >= (10 * tree.AlternateTreeTMinOption.getValue()) || lossExamplesSeen >= tree.AlternateTreeTimeOption.getValue())
                    {
                        alternateTree = null;
                        boolean restart = true;
                        // If there are no alternate trees above this node, restart the change detection
                        while (parent != null) {
                            if (parent.alternateTree != null) {
                                restart = false;
                                break;
                            }
                            parent = parent.getParent();
                        }
                        if (restart) restartChangeDetection();
                    }
                }
            }
            
            return false; 
        }
        
        public void initializeAlternateTree(FIMTDD tree) {
            // Start a new alternate tree, beginning with a learning node
            this.alternateTree = tree.newLearningNode();
    
            // Set up the blank statistics
            // Number of instances reaching this node since the alternate tree was started
            lossExamplesSeen = 0;
            // Faded squared error (original tree)
            lossFadedSumOriginal = 0;
            // Faded squared error (alternate tree)
            lossFadedSumAlternate = 0;
            // Number of evaluations of alternate tree
            lossNumQiTests = 0;
            // Sum of Qi values
            lossSumQi = 0;
            // Number of examples at last test
            previousWeight = 0;
            
            // Disable the change detection mechanism bellow this node
            disableChangeDetection();
        }

        
        public int countLeaves() {
        	Stack<Node> stack = new Stack<Node>();
        	stack.addAll(children);
        	int ret = 0;
        	while (!stack.isEmpty()) {
        		Node node = stack.pop();
        		if (node instanceof LeafNode) {
        			ret++;
        		} else if (node instanceof SplitNode) {
        			stack.addAll(((SplitNode) node).children);
        		}
        	}
        	return ret;
        }
        
        @Override
        public void describeSubtree(FIMTDD tree, StringBuilder out, int indent) {
            for (int branch = 0; branch < children.size(); branch++) {
                Node child = getChild(branch);
                if (child != null) {
                    StringUtils.appendIndented(out, indent, "if ");
                    out.append(this.splitTest.describeConditionForBranch(branch,
                            tree.getModelContext()));
                    out.append(": ");
                    StringUtils.appendNewline(out);
                    child.describeSubtree(tree, out, indent + 2);
                }
            }
        }

    }

    public static class FoundNode {

        public LeafNode node;

        public SplitNode parent;

        public int parentBranch;
        
        public AutoExpandVector<SplitNode> alternateNodes;

        public FoundNode(LeafNode node, SplitNode parent, int parentBranch, AutoExpandVector<SplitNode> alternateNodes) {
            this.node = node;
            this.parent = parent;
            this.parentBranch = parentBranch;
            this.alternateNodes = alternateNodes;
        }
    }
    
    //endregion ================ CLASSES ================
    //region ================ METHODS ================
    /**
     * Method for updating (training) the model using a new instance
     */
    public void trainOnInstanceImpl(Instance inst) {
    	examplesSeen++;
        // If no model exists yet, begin with an empty leaf node (the root)
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            leafNodeCount++;
        }

        // Take the current example and traverse it through the tree to a leaf
        FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
        Node leafNode = foundNode.node;

        // If no appropriate leaf already existed, create one
        if (leafNode == null) {
            LeafNode newNode = newLearningNode();
            foundNode.parent.setChild(foundNode.parentBranch, newNode);
            newNode.setParent(foundNode.parent);
            leafNode = newNode;
        }

        if (leafNode instanceof LeafNode) ((LeafNode)leafNode).learnFromInstance(inst, this);
        
        boolean changed = false;
        if (foundNode.alternateNodes != null)
            for (SplitNode nodeWithAlternate : foundNode.alternateNodes) {
                changed = changed || nodeWithAlternate.processAlternateTree(inst, this, (LeafNode)leafNode);
            }
            
        // If the tree has changed recalculate the foundNode
        if (changed) {
            foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
            leafNode = foundNode.node;
        }
        
        // Update the change detection tests on the path traversed in a bottom-up
        // fashion (i.e. using the error from the leaf node)
        if(leafNode instanceof LeafNode) {
            // Retrieve the error for the found leaf node
            Double leafError = ((LeafNode)leafNode).getPHError(inst, this) - this.PageHinckleyAlphaOption.getValue();

            // Back-propagate the error through all the parent nodes
            Node currentNode = leafNode;
            SplitNode parent = currentNode.getParent();
            while(parent != null) {
                // Update the Page-Hinckley error test in the parent node
                // System.out.println(String.format("PHmT: %6.2f PHMT: %6.2f", parent.PHmT, parent.PHMT)); //JD
                if(parent.changeDetection && parent.PageHinckleyTest(leafError, this.PageHinckleyThresholdOption.getValue()) == true) {
                    System.out.println("Drift detected: " + examplesSeen); //JD

                    if(!nodesToAdapt.contains(parent)) {
                        nodesToAdapt.add(parent);
                        nodesToAdapt.remove(parent.getChild(parent.instanceChildIndex(inst)));
                    }    
                }

                // Propagate back to the next parent node
                parent = parent.getParent();
            }
        }

        if(nodesToAdapt.size() > 0) {
            // Adapt the model tree by creating an alternate tree at this node which
            // will be updated as new instances arrive and may, in time, replace the current subtree

            // TODO Test: should always be 1
            for (SplitNode nodeToAdapt : nodesToAdapt) {
                nodeToAdapt.initializeAlternateTree(this);
            }
        } else if (foundNode.alternateNodes == null || foundNode.alternateNodes.size() == 0) {
        	// Growth is disabled for nodes "under" and alternate tree
            // Check that the current node is in fact a leaf node (i.e. that we
            // haven't accidently stopped earlier in the tree)
            if (leafNode instanceof LeafNode) ((LeafNode)leafNode).checkForSplit(this);
        }
        nodesToAdapt = new ArrayList<SplitNode>();
    }

    /**
     * Method used to split a leaf node and generate child nodes, if appropriate
     */
    protected void FIMTDD_attemptToSplit(LeafNode node, SplitNode parent, int parentIndex) {
        // Set the split criterion to use to the SDR split criterion as described by Ikonomovska et al. 
        SplitCriterion splitCriterion = (SplitCriterion) getPreparedClassOption(this.splitCriterionOption);

        // Using this criterion, find the best split per attribute and rank the results
        AttributeSplitSuggestion[] bestSplitSuggestions = node.getBestSplitSuggestions(splitCriterion, this);
        Arrays.sort(bestSplitSuggestions);

        // Declare a variable to determine if any of the splits should be performed
        boolean shouldSplit = false;

        // If only one split was returned, use it
        if (bestSplitSuggestions.length < 2) {
            shouldSplit = bestSplitSuggestions.length > 0;
        } else { // Otherwise, consider which of the splits proposed may be worth trying

            // Determine the Hoeffding bound value, used to select how many instances should be used to make a test decision
            // to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
            double hoeffdingBound = computeHoeffdingBound(1, this.splitConfidenceOption.getValue(), node.examplesSeen());
            // Determine the top two ranked splitting suggestions
            AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
            AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];

            // If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
            // as determined using the Hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
            // particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
            // splitting should occur.
            // Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
            // (default 0.05) is applied to the Hoeffding bound; if the Hoeffding bound is smaller than this limit then the two
            // competing attributes are equally good, and the split will be made on the one with the higher SDR value.
            if ((secondBestSuggestion.merit / bestSuggestion.merit < 1 - hoeffdingBound) || (hoeffdingBound < this.tieThresholdOption.getValue())) {
            	shouldSplit = true;
            }
            // If the splitting criterion was not met, initiate pruning of the E-BST structures in each attribute observer
            else
            {
                for (int i = 0; i < node.attributeObservers.size(); i++) {
                    FIMTDDNumericAttributeClassObserver obs = node.attributeObservers.get(i);
                    if (obs != null) {
                        obs.removeBadSplits(splitCriterion, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound);    
                    }
                }
            }

            // If the user has selected this option, it is also possible to remove poor attributes at this stage
            //                if ((this.removePoorAttsOption != null) && this.removePoorAttsOption.isSet()) {
            //                    Set<Integer> poorAtts = new HashSet<Integer>();
            //                    // scan 1 - add any poor to set
            //                    for (int i = 0; i < bestSplitSuggestions.length; i++) {
            //                        if (bestSplitSuggestions[i].splitTest != null) {
            //                            int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
            //                            if (splitAtts.length == 1) {
            //                                if (((bestSuggestion.merit / secondBestSuggestion.merit) + hoeffdingBound)  < 1) {
            //                                    poorAtts.add(new Integer(splitAtts[0]));
            //                                }
            //                            }
            //                        }
            //                    }
            //                    // scan 2 - remove good ones from set
            //                    for (int i = 0; i < bestSplitSuggestions.length; i++) {
            //                        if (bestSplitSuggestions[i].splitTest != null) {
            //                            int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
            //                            if (splitAtts.length == 1) {
            //                                if (((bestSuggestion.merit / secondBestSuggestion.merit) + hoeffdingBound)  < 1) {
            //                                    poorAtts.remove(new Integer(splitAtts[0]));
            //                                }
            //                            }
            //                        }
            //                    }
            //                    for (int poorAtt : poorAtts) {
            //                        node.disableAttribute(poorAtt);
            //                    }
            //                }
        }

        // If the splitting criterion were met, split the current node using the chosen attribute test, and
        // make two new branches leading to (empty) leaves
        if (shouldSplit) {
            AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];

            SplitNode newSplit = newSplitNode(splitDecision.splitTest);
            for (int i = 0; i < splitDecision.numSplits(); i++) {
            	LeafNode newChild = newLearningNode();
            	if (buildingModelTree() && node.learningModel instanceof FIMTDDPerceptron) {
            		// Copy the splitting node's perceptron to it's children
            		newChild.learningModel = new FIMTDDPerceptron((FIMTDDPerceptron) node.learningModel);
            		newChild.learningModel.setModelContext(getModelContext());
            	}
            	newChild.setParent(newSplit);
            	newSplit.setChild(i, newChild);
            }
            if (parent == null) {
            	treeRoot = newSplit;
            } else {
            	parent.setChild(parentIndex, newSplit);
            	newSplit.setParent(parent);
            }
            leafNodeCount++;
        }
    }

    /**
     * Return an empty LeafNode
     */
    protected LeafNode newLearningNode() {
        return new LeafNode(this);
    }

    /**
     * Return a new SplitNode
     */
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest) {
        return new SplitNode(splitTest);
    }

    /**
     * Gets the index of the attribute in the instance,
     * given the index of the attribute in the learner.
     *
     * @param index the index of the attribute in the learner
     * @param inst the instance
     * @return the index in the instance
     */
    protected static int modelAttIndexToInstanceAttIndex(int index, Instance inst) {
        return inst.classIndex() > index ? index : index + 1;
    }

    protected FIMTDDNumericAttributeClassObserver newNumericClassObserver() {
        return new FIMTDDNumericAttributeClassObserver();
    }

    public static double computeHoeffdingBound(double range, double confidence, double n) {
        return Math.sqrt(( (range * range) * Math.log(1 / confidence)) / (2.0 * n));
    }

    @Override
    public void resetLearningImpl() {
        this.treeRoot = null;
        this.leafNodeCount = 0;
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
    
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{new Measurement("tree size (leaves)", leafNodeCount),}; 
	};
    
    @Override
    public double[] getVotesForInstance(Instance inst) {
        
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            leafNodeCount++;
        }
        
        FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
        Node leafNode = foundNode.node;

        // If no appropriate leaf already existed, create one
        if (leafNode == null) {
            LeafNode newNode = newLearningNode();
            foundNode.parent.setChild(foundNode.parentBranch, newNode);
            newNode.setParent(foundNode.parent);
            leafNode = newNode;
        }
        
        return new double[] {((LeafNode) leafNode).getPrediction(inst, this)};
    }
    
    private Classifier leafModelPrototype;
    
    protected Classifier newLeafModel() {
    	if (leafModelPrototype == null) {
    		leafModelPrototype = (Classifier) getPreparedClassOption(leafModelOption);
    	}
    	Classifier copy = leafModelPrototype.copy();
    	copy.setModelContext(getModelContext());
    	return copy;
    }
    
    public boolean buildingModelTree() {
    	return !regressionTreeOption.isSet();
    }
    
    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        if (treeRoot != null) treeRoot.describeSubtree(this, out, indent);
    }    //endregion ================ METHODS ================
}
