 /*
 *    FFIMTDD.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author Katie de Lange, E. Almeida, J. Gama
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

/* Project Knowledge Discovery from Data Streams, FCT LIAAD-INESC TEC, 
 *
 * Contact: jgama@fep.up.pt
 */

 package moa.classifiers.trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import moa.AbstractMOAObject;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.FIMTDDNumericAttributeClassObserver;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.splitcriteria.SDRSplitCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instance;
 
/*
 * Implementation of FIMTDD, regresion tree for data streams.
 */
 
public class FIMTDD extends HoeffdingTree {

	private static final long serialVersionUID = 1L;

	@Override
	public String getPurposeString() {
		return "Implementation of the FIMT-DD tree as described by Ikonomovska et al.";
	}
		
	// For the moment at least, force the split criterion to be SDRSplitCriterion and the 
	// numeric estimator to be FIMTLDDNumericAttributeClassObserver
	public FIMTDD() {

	    numericEstimatorOption = new ClassOption("numericEstimator",
            'n', "Numeric estimator to use.", FIMTDDNumericAttributeClassObserver.class,
            "FIMTDDNumericAttributeClassObserver");
            
            splitCriterionOption = new ClassOption("splitCriterion",
            's', "Split criterion to use.", SDRSplitCriterion.class,
            "SDRSplitCriterion");
        }
        
        protected DoubleVector splitRatioStatistics = new DoubleVector();
	
        		// Store the lowest node in the tree that requires adaptation
	protected ArrayList<FIMTDDSplitNode> nodesToAdapt = new ArrayList<FIMTDDSplitNode>();
	
	protected boolean Adaptable = true;
	
	protected double initLearnRate = 0.1;
	
	protected double learnRateDecay = 0.001;
        
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
	
	public FloatOption learningRatioOption = new FloatOption("learningRatio", 'w', "Learning ratio to use for training the Perceptrons in the leaves.", 0.01);
	
	public FlagOption learningRatio_Decay_or_Const_Option = new FlagOption("learningRatio_Decay_or_Const", 'j',
			"learning Ratio Decay or const parameter.");
    
    //=============================== CLASSES ================================//
    
     	     /**
     	     * A new interface for nodes to be used in an adaptive setting
     	     */
     	     public interface AdaptationCompatibleNode {    	     	
     	     	
     	     	     public void setParent(Node parent);
     	     	
     	     	     public Node getParent();
     	     }
    
    	     /**
    	     * A modified ActiveLearningNode that uses a Perceptron as the leaf node
    	     * model, and ensures that the class values sent to the attribute observers
    	     * are not truncated to ints if regression is being performed
    	     */
	     public static class FIMTDDActiveLearningNode extends ActiveLearningNode implements AdaptationCompatibleNode{
		
		// Create a Perceptron model that carries out the actual learning in each node
		public FIMTDDPerceptron learningModel = new FIMTDDPerceptron();
		
		// The parent of this particular leaf node
		protected Node parent;
		
		// The statistics for this node:
			// Number of instances that have reached it
			// Sum of y values
			// Sum of squared y values
			protected DoubleVector nodeStatistics;
		
		// The error values for the Page Hinckley test
			// PHmT = the cumulative sum of the errors
			// PHMT = the minimum error value seen so far
		protected double PHmT = 0;
		protected double PHMT = Double.MAX_VALUE; 
		
		/**
		 * Create a new FIMTDDActiveLearningNode
		 */
		public FIMTDDActiveLearningNode(double[] initialClassObservations) {
			super(initialClassObservations);		
			learningModel.resetLearningImpl();
			nodeStatistics = new DoubleVector();
		}
	
		/**
		 * Set the parent node
		 */
		public void setParent(Node parent) {
			this.parent = parent;	
		}
		
		/**
		 * Return the parent node
		 */
		public Node getParent() {
			return parent;
		}
		
		@Override
		public double getWeightSeen() {
			if(nodeStatistics != null)
			{
				return this.nodeStatistics.getValue(0);
			}
			else
			{
				return 0;
			}
		}
		
		@Override
		/**
		 * Method to learn from an instance that passes the new instance to the perceptron learner,
		 * and also prevents the class value from being truncated to an int when it is passed to the
		 * attribute observer
		 */
		public void learnFromInstance(Instance inst, HoeffdingTree ht) {
		   // this.observedClassDistribution.addToValue((int)inst.classValue(),inst.weight());
		   
		    // Update the statistics for this node
		    	// number of instances passing through the node
		    	nodeStatistics.addToValue(0, 1);
		    	// sum of y values
		    	nodeStatistics.addToValue(1, inst.classValue());
		    	// sum of squared y values
		    	nodeStatistics.addToValue(2, inst.classValue() * inst.classValue());
		    	// sum of absolute errors
		    		// Normalise values prior to calculating absolute error
		    		double sd = Math.sqrt((nodeStatistics.getValue(2) - ((nodeStatistics.getValue(1) * 
				nodeStatistics.getValue(1))/nodeStatistics.getValue(0)))/nodeStatistics.getValue(0));
				double mean = nodeStatistics.getValue(1) / nodeStatistics.getValue(0);
		    	nodeStatistics.addToValue(3, Math.abs(((inst.classValue()-mean)/sd) - ((learningModel.prediction(inst)-mean)/sd)));
		    	// sum of squared errors
		    	nodeStatistics.addToValue(4, Math.pow(this.getError(inst), 2));
		    	
		    	
		    learningModel.trainOnInstanceImpl(inst, (FIMTDD)ht);

		    for (int i = 0; i < inst.numAttributes() - 1; i++) {
			int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
			AttributeClassObserver obs = this.attributeObservers.get(i);
			if (obs == null) {
			    // At this stage all nominal attributes are ignored
			    if(inst.attribute(instAttIndex).isNumeric())
			    {
			    	    obs = ((FIMTDD)ht).newNumericClassObserver();
			    	    this.attributeObservers.set(i, obs);   
			    }
			}
			if(obs != null)
			{
				((FIMTDDNumericAttributeClassObserver)obs).observeAttributeClass(inst.value(instAttIndex),inst.classValue(), inst.weight());
			}
		    }
		}

		@Override
		/**
		 * Return the best split suggestions for this node using the given split criteria
		 */
		public AttributeSplitSuggestion[] getBestSplitSuggestions(SplitCriterion criterion, HoeffdingTree ht) {
		
			List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();
			
			// Set the nodeStatistics up as the preSplitDistribution, rather than the observedClassDistribution
			double[] nodeSplitDist = this.nodeStatistics.getArrayCopy();
			double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
			
			if (!ht.noPrePruneOption.isSet()) {
				// add null split as an option
				bestSuggestions.add(new AttributeSplitSuggestion(null,new double[0][], criterion.getMeritOfSplit(nodeSplitDist,new double[][]{nodeSplitDist})));
			}
			for (int i = 0; i < this.attributeObservers.size(); i++) {
				AttributeClassObserver obs = this.attributeObservers.get(i);
				if (obs != null) {
					
					// AT THIS STAGE NON-NUMERIC ATTRIBUTES ARE IGNORED
					AttributeSplitSuggestion bestSuggestion = null;
					if(obs instanceof FIMTDDNumericAttributeClassObserver) {
						bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion,nodeSplitDist, i, ht.binarySplitsOption.isSet());
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
		public double getPrediction(Instance inst, HoeffdingTree ht) {
			return learningModel.prediction(inst);
			
		}
		/**
		* Retrieve the class votes using the target mean
		 */
		public double getPredictionTargetMean(Instance inst, HoeffdingTree ht) {
			double valor = 0.0;
			if(this.nodeStatistics.getValue(0) > 0.0){
				valor = this.nodeStatistics.getValue(1)/this.nodeStatistics.getValue(0);
				}
			return valor;
			}
		
		@Override
		public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
			double[] ret = new double[1];
			double perceptronPrediction = getPrediction(inst, ht); // Prediction using perceptron
			double targetMeanPrediction = getPredictionTargetMean(inst, ht); // Prediction using Target Mean
			double perceptronError = Math.abs(inst.classValue() - perceptronPrediction);
			double targetMeanError = Math.abs(inst.classValue() - targetMeanPrediction);
			if(perceptronError < targetMeanError){ // Adaptative strategy
				ret[0] = perceptronPrediction;
			}else{
				ret[0] = targetMeanPrediction;
			}
		
			
		 	return ret;
		}
		
/*		@Override
		public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
			double[] ret = {getPrediction(inst, ht)};
		 	return ret;
		}*/
		
		/**
		 * Return the error for a given instance
		 */
		public double getError(Instance inst)
		{    	
			return inst.classValue() - learningModel.prediction(inst);
		}
		
		/**
		 * A method returning the Page-Hinckley error produced on this instance in the leaf node,
		 * which will then be back-propagated back through the tree to allow the 
		 * Page-Hinckley change detection tests to be updated
		 */
		public double getPHError(Instance inst)
		{
			double sd = Math.sqrt((nodeStatistics.getValue(2) - ((nodeStatistics.getValue(1) * nodeStatistics.getValue(1))/nodeStatistics.getValue(0)))/nodeStatistics.getValue(0));
			double mean = nodeStatistics.getValue(1) / nodeStatistics.getValue(0);
			return Math.abs(((inst.classValue()-mean)/sd) - ((learningModel.prediction(inst)-mean)/sd)) -  ((nodeStatistics.getValue(3) + Math.abs(((inst.classValue()-mean)/sd) - ((learningModel.prediction(inst)-mean)/sd))) / (nodeStatistics.getValue(0)+1));
		}
		
		/**
		 * Returns the squared error, for use in determining if an alternate tree is performing better than an original
		 * tree, or if the alternate tree should be deleted
		 */
		public double getSquaredError()
		{
			return nodeStatistics.getValue(4);
		}
	     }
	    
	     /**
	     * A modified SplitNode method implementing the extra information regarding it's parent,
	     * and the ability to track the error rate and perform Page-Hinckley tests
	     */
	     public static class FIMTDDSplitNode extends SplitNode implements AdaptationCompatibleNode {
			
	     	        // A reference to the parent of this node
	     	     	protected Node parent;
	     	     	
	     	     	// The error values for the Page Hinckley test
			// PHmT = the cumulative sum of the errors
			// PHMT = the minimum error value seen so far
			protected double PHmT = 0;
			protected double PHMT = Double.MAX_VALUE; 
			
			// Scope for an alternate tree that may need to be grown from this node
			protected FIMTDD alternateTree;
			
			// Keep track of the statistics for loss error calculations
			protected DoubleVector lossStatistics = new DoubleVector();
			
			protected int weightSeen = 0;
			protected int previousWeight = 0;
			
			/**
			 * Create a new FIMTDDSplitNode
			 */
			public FIMTDDSplitNode(InstanceConditionalTest splitTest, double[] classObservations) {
			      super(splitTest,classObservations);
			}
			
			/**
			* Set the parent node
			*/
			public void setParent(Node parent)
			{
			      this.parent = parent;	
			}
			
			/**
			* Return the parent node
			*/
			public Node getParent()
			{
			      return parent;
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
			
			/**
			 * This method has been added to the SplitNode subclass to allow alternate trees
			 * to be grown and compared to the current tree, to allow adaptation in the face
			 * of concept drift
			 */
			public void learnFromInstance(Instance inst, FIMTDD ht, boolean growingAltTree) {
			
				if(growingAltTree)
				{
					weightSeen++;
					
					// If no alternate tree exists		
					if (this.alternateTree == null) 
					{
						// Start a new alternate tree, beginning with a learning node
						this.alternateTree = (FIMTDD)ht.copy();
						this.alternateTree.resetLearningImpl();
						this.alternateTree.Adaptable = false;
						
						// Set up the blank statistics
							// Number of instances reaching this node since the alternate tree was started	
							lossStatistics.setValue(0,0);
							// Sum of Qi values
							lossStatistics.setValue(1,0);
							weightSeen = 0;
							previousWeight = 0;
					} 	
					
					// If an alternate tree already exists, check if the current tree should be replaced with it,
					// or if the alternate tree should be discarded.
					else if(weightSeen - previousWeight >= ht.AlternateTreeTMinOption.getValue())
					{
						
						// Update the weight at which a decision was tested for
						previousWeight = weightSeen;
						
						// Update the loss statistics for the alternate tree
						FoundNode foundNode = this.alternateTree.treeRoot.filterInstanceToLeaf(inst,null,-1);
						Node leaf = foundNode.node;
						double sAlt = 0.0;
						double lAlt = 0.0;
							if(leaf instanceof FIMTDDActiveLearningNode)
							{
								sAlt = ((FIMTDDActiveLearningNode)leaf).getSquaredError();
								lAlt = Math.pow(((FIMTDDActiveLearningNode)leaf).getError(inst),2);
							}
						
						// Update the loss statistics for the current tree
						foundNode = this.getChild(this.instanceChildIndex(inst)).filterInstanceToLeaf(inst,null,-1);
						leaf = foundNode.node;
						double sOrg = 0.0;
						double lOrg = 0.0;
							if(leaf instanceof FIMTDDActiveLearningNode)
							{
								sOrg = ((FIMTDDActiveLearningNode)leaf).getSquaredError();
								lOrg = Math.pow(((FIMTDDActiveLearningNode)leaf).getError(inst),2);
							}
						
						// Compute the Qi statistics
						double Qi = Math.log((lOrg + (ht.AlternateTreeFadingFactorOption.getValue() * sOrg))/(lAlt + (ht.AlternateTreeFadingFactorOption.getValue() * lOrg)));
						double previousQiAverage = lossStatistics.getValue(1) / lossStatistics.getValue(0);
						lossStatistics.addToValue(0,1);
						lossStatistics.addToValue(1,Qi);
						double QiAverage = lossStatistics.getValue(1) / lossStatistics.getValue(0);
						
						// If appropriate, replace the current tree with the alternate tree
						if(Qi > 0)
						{
							FIMTDDSplitNode parent = (FIMTDDSplitNode)this.parent;
							//Replace the main FIMT-DD tree at a subtree
							if(parent != null)
							{
								parent.setChild(parent.instanceChildIndex(inst),this.alternateTree.treeRoot);
								ht.nodesToAdapt.remove(this);
								this.alternateTree = null;
							}
							// Or occasionally at the root of the tree
							else
							{
								ht = this.alternateTree;
								ht.nodesToAdapt = new ArrayList<FIMTDDSplitNode>();
								ht.Adaptable = true;
								this.alternateTree = null;
							}

							
						}
						// Otherwise, check if the alternate tree should be discarded
						else if (QiAverage < previousQiAverage && lossStatistics.getValue(0) >= (10 * ht.AlternateTreeTMinOption.getValue()) || weightSeen >= ht.AlternateTreeTimeOption.getValue())
						{
							ht.nodesToAdapt.remove(ht.nodesToAdapt.indexOf(this));
							this.alternateTree = null;
						}
					}
					
					// Learn the alternate tree (if it has not just been discarded)
					if(this.alternateTree != null)
					{
						this.alternateTree.trainOnInstanceImpl(inst);
					}
					 
				
				}
				
				// Determine which of the current children this instance should
				// be sent to, and learn that child
				int childBranch = this.instanceChildIndex(inst);
				Node child = this.getChild(childBranch);
				if (child != null) {
					
					if(child instanceof FIMTDDActiveLearningNode)
					{
						((FIMTDDActiveLearningNode)child).learnFromInstance(inst, ht);
					}
					else
					{
						((FIMTDDSplitNode)child).learnFromInstance(inst,ht, false);	
					}
				}
			}
			
	     }
	     
	     /**
	      * A Perceptron classifier modified to conform to the specifications of Ikonomovska et al.
	      */
	     public static class FIMTDDPerceptron extends AbstractMOAObject{
			
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
			// TODO Auto-generated method stub
		    }
	     	     
	     	    public FIMTDDPerceptron(FIMTDDPerceptron copy)
	     	    {
	     	    		this.weightAttribute = copy.getWeights(); 
	     	    }
	     	    
	     	    public FIMTDDPerceptron()
	     	    {
	     	    	    this.reset = true;
	     	    }
	     	    
	     	    public void setWeights(double[] w)
	     	    {
	     	    	this.weightAttribute = w;	    
	     	    }
	     	    
	     	    public double[] getWeights()
	     	    {
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
		    public void trainOnInstanceImpl(Instance inst, FIMTDD ft) {
			
			// Initialise Perceptron if necessary   
			if (this.reset == true) {
				this.reset = false;
				this.weightAttribute = new double[inst.numAttributes()];
				this.instancesSeen = 0;
				this.attributeStatistics = new DoubleVector();
				this.squaredAttributeStatistics = new DoubleVector();
				for (int j = 0; j < inst.numAttributes(); j++) {
				    weightAttribute[j] = 2 * Math.random() - 1;
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
			if(ft.learningRatio_Decay_or_Const_Option.isSet()){
				learningRatio = ft.learningRatioOption.getValue();
			}else{
				learningRatio = ft.initLearnRate / (1+ instancesSeen*ft.learnRateDecay);
			}
		//	double learningRatio = ft.learningRatioOption.getValue();
			double actualClass = inst.classValue();
			double predictedClass = this.prediction(inst);
			
			//System.out.println("Actual Class: " + actualClass + "  Prediction: " + predictedClass);

			// SET DELTA TO ACTUAL - PREDICTED, NOT PREDICTED - ACTUAL AS SAID IN PAPER
			double delta = actualClass - predictedClass;
			
			for (int j = 0; j < inst.numAttributes() - 1; j++) {
				
				if(inst.attribute(j).isNumeric())
				{
					// Update weights. Ensure attribute values are normalised first
					double sd = Math.sqrt((squaredAttributeStatistics.getValue(j) - ((attributeStatistics.getValue(j) * attributeStatistics.getValue(j))/instancesSeen))/instancesSeen);
					double instanceValue = 0;
					if(sd > 0.0000001) // Limit found in implementation by Ikonomovska et al (2011)
					{
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
			if(this.reset == false)
			{
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
	     
	     

    //=============================== METHODS ================================//
        
    	//================= TRAIN and TEST ================//
    	    /**
	     * Method for updating (training) the model using a new instance
	     */
	    @Override
	    public void trainOnInstanceImpl(Instance inst) {
		
	    	// Convert any nominal attributes to numeric ones???
	    	
	    	// If no model exists yet, begin with an empty leaf node (the root)
	    	if (this.treeRoot == null) {
		    this.treeRoot = newLearningNode();
		    this.activeLeafNodeCount = 1;
		}
		
		// Take the current example and traverse it through the tree to a leaf
		FoundNode foundNode = this.treeRoot.filterInstanceToLeaf(inst, null, -1);
		Node leafNode = foundNode.node;
		
		// If no appropriate leaf already existed, create one
		if (leafNode == null) {
		    FIMTDDActiveLearningNode newNode = newLearningNode();
		    foundNode.parent.setChild(foundNode.parentBranch, newNode);
		    newNode.setParent(foundNode.parent);
		    leafNode = newNode;
		    this.activeLeafNodeCount++;
		}
        		
		// Update the change detection tests on the path traversed in a bottom-up
		// fashion (i.e. using the error from the leaf node)
		if(leafNode instanceof FIMTDDActiveLearningNode)
		{	
			FIMTDDActiveLearningNode currentNode = (FIMTDDActiveLearningNode)leafNode;
			
			// Retrieve the error for the found leaf node
			//currentNode.learnFromInstance(inst, this);
			Double leafError = currentNode.getPHError(inst) - this.PageHinckleyAlphaOption.getValue();
			
			// Back-propagate the error through all the parent nodes
			FIMTDDSplitNode parent = (FIMTDDSplitNode)currentNode.getParent();
			while(parent != null && Adaptable)
			{
				// Update the Page-Hinckley error test in the parent node
				if(parent.PageHinckleyTest(leafError, this.PageHinckleyThresholdOption.getValue()) == true)
				{
					if(!nodesToAdapt.contains(parent))
					{
						nodesToAdapt.add(parent);
						nodesToAdapt.remove(parent.getChild(parent.instanceChildIndex(inst)));
					}	
				}

				// Propagate back to the next parent node					
				parent = (FIMTDDSplitNode)parent.getParent();			
			}
		}

		if(nodesToAdapt.size() > 0 && Adaptable)
		{
			// Adapt the model tree by creating an alternate tree at this node which
			// will be updated as new instances arrive and may, in time, replace the current subtree			
			for(int i = 0; i < nodesToAdapt.size(); i++)
			{
				nodesToAdapt.get(i).learnFromInstance(inst, this, true);
			}
			
		}
		else
		{
			// Check that the current node is in fact a leaf node (i.e. that we
			// haven't accidently stopped earlier in the tree)
			if (leafNode instanceof LearningNode) {	
				
			    LearningNode learningNode = (LearningNode) leafNode;
			    
			    // Update the statistics in this leaf node, based on the new instance
			    learningNode.learnFromInstance(inst, this);
			    
			    // Provided growth is allowed, and this leaf node hasn't been deactivated...
			    if (this.growthAllowed && (learningNode instanceof FIMTDDActiveLearningNode)) {
			    	
			    	// Extract details about this leaf, and the number of examples it has seen
				FIMTDDActiveLearningNode activeLearningNode = (FIMTDDActiveLearningNode) learningNode;			    
				double weightSeen = activeLearningNode.getWeightSeen();
				
				// If it has seen Nmin examples since it was last tested for splitting, attempt a split of this node
				if (weightSeen - activeLearningNode.getWeightSeenAtLastSplitEvaluation() >= this.gracePeriodOption.getValue()) {
					
					FIMTDD_attemptToSplit(activeLearningNode, (FIMTDDSplitNode)foundNode.parent, foundNode.parentBranch);
				    
				    // Take note of how many instances were seen when this split evaluation was made, so we know when to perform the next split evaluation
				    activeLearningNode.setWeightSeenAtLastSplitEvaluation(weightSeen);
				}
			    }
			}

			if (this.trainingWeightSeenByModel% this.memoryEstimatePeriodOption.getValue() == 0) {
			    estimateModelByteSizes();
			}
		}
	    }
	    
	    	    
	    /**
	     * Method used to split a leaf node and generate child nodes, if appropriate
	     */
	    protected void FIMTDD_attemptToSplit(FIMTDDActiveLearningNode node, FIMTDDSplitNode parent, int parentIndex) {
	    		
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
		    } 
		    // Otherwise, consider which of the splits proposed may be worth trying
		    else {
		    	
		    	// Determine the hoeffding bound value, used to select how many instances should be used to make a test decision
		    	// to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
			double hoeffdingBound = computeHoeffdingBound(1,this.splitConfidenceOption.getValue(), node.getWeightSeen());
			
			// Determine the top two ranked splitting suggestions
			AttributeSplitSuggestion bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			AttributeSplitSuggestion secondBestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 2];
			
			splitRatioStatistics.addToValue(0,1);
			splitRatioStatistics.addToValue(1,secondBestSuggestion.merit / bestSuggestion.merit);
			
			// If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
			// as determined using the hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
			// particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
			// splitting should occur.
			// Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
			// (default 0.05) is applied to the hoeffding bound; if the hoeffding bound is smaller than this limit then the two
			// competing attributes are equally good, and the split will be made on the one with the higher SDR value.
			if ((((splitRatioStatistics.getValue(1)/splitRatioStatistics.getValue(0)) + hoeffdingBound)  < 1) || (hoeffdingBound < this.tieThresholdOption.getValue())) {	
				
			    shouldSplit = true;
			    
			}
			// If the splitting criterion was not met, initiate pruning of the E-BST structures in each attribute observer
			else
			{
				for (int i = 0; i < node.attributeObservers.size(); i++) {
					AttributeClassObserver obs = node.attributeObservers.get(i);
					if (obs != null) {
						((FIMTDDNumericAttributeClassObserver)obs).removeBadSplits(splitCriterion, secondBestSuggestion.merit / bestSuggestion.merit, bestSuggestion.merit, hoeffdingBound);	
					}
				}
			}
	
			
			// If the user has selected this option, it is also possible to remove poor attributes at this stage
			if ((this.removePoorAttsOption != null)
				&& this.removePoorAttsOption.isSet()) {
			    Set<Integer> poorAtts = new HashSet<Integer>();
			    // scan 1 - add any poor to set
			    for (int i = 0; i < bestSplitSuggestions.length; i++) {
				if (bestSplitSuggestions[i].splitTest != null) {
				    int[] splitAtts = bestSplitSuggestions[i].splitTest.getAttsTestDependsOn();
				    if (splitAtts.length == 1) {
					if (((bestSuggestion.merit / secondBestSuggestion.merit) + hoeffdingBound)  < 1) {
					    poorAtts.add(new Integer(splitAtts[0]));
					}
				    }
				}
			    }
			    // scan 2 - remove good ones from set
			    for (int i = 0; i < bestSplitSuggestions.length; i++) {
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
				node.disableAttribute(poorAtt);
			    }
			}
		    }
		    
		    // If the splitting criterion were met, split the current node using the chosen attribute test, and
		    // make two new branches leading to (empty) leaves
		    if (shouldSplit) {
		    	    
		    	  
			AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];
			
			// Deactivate this node if the best split was to do nothing
			if (splitDecision.splitTest == null) {
			    // preprune - null wins
			    deactivateLearningNode(node, parent, parentIndex);
			} 
			// Otherwise
			else {
				
			    FIMTDDSplitNode newSplit = newSplitNode(splitDecision.splitTest,
				    node.getObservedClassDistribution());
			    for (int i = 0; i < splitDecision.numSplits(); i++) {
				FIMTDDActiveLearningNode newChild = newLearningNode(splitDecision.resultingClassDistributionFromSplit(i));
				newChild.learningModel = new FIMTDDPerceptron(node.learningModel);
				newChild.setParent(newSplit);
				newSplit.setChild(i, newChild);
			    }
			    this.activeLeafNodeCount--;
			    this.decisionNodeCount++;
			    this.activeLeafNodeCount += splitDecision.numSplits();
			    if (parent == null) {
				this.treeRoot = newSplit;
			    } else {
				parent.setChild(parentIndex, newSplit);
				newSplit.setParent(parent);
			    }
			    
			}
			// Check the memory limits are not being exceeded
			enforceTrackerLimit();
		    }
	    }	
	    
	    /**
	     * Return an empty FIMTDDActiveLearningNode
	     */
	    protected FIMTDDActiveLearningNode newLearningNode() {
	    	    return newLearningNode(new double[0]);
	    }

	    /**
	     * Return a new FIMTDDActiveLearningNode using the initial class observations
	     */
	    protected FIMTDDActiveLearningNode newLearningNode(double[] initialClassObservations) {
	    	    return new FIMTDDActiveLearningNode(initialClassObservations);
	    }
	    
	    /**
	     * Return a new FIMTDDSplitNode
	     */
	    protected FIMTDDSplitNode newSplitNode(InstanceConditionalTest splitTest,double[] classObservations) {
	    	    return new FIMTDDSplitNode(splitTest, classObservations);
	    }
}
