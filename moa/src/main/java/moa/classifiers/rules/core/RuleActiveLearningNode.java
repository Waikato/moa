/*
 *    RuleActiveLearningNode.java
 *    Copyright (C) 2014 University of Porto, Portugal
 *    @author A. Bifet, J. Duarte, J. Gama
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
package moa.classifiers.rules.core;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.rules.AbstractAMRules;
import moa.classifiers.rules.driftdetection.PageHinkleyFading;
import moa.classifiers.rules.driftdetection.PageHinkleyTest;
import moa.classifiers.trees.HoeffdingTree;
import moa.classifiers.trees.HoeffdingTree.ActiveLearningNode;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * A modified ActiveLearningNode that uses a Perceptron as the leaf node model,
 * and ensures that the class values sent to the attribute observers are not
 * truncated to ints if regression is being performed
 */
public abstract class RuleActiveLearningNode extends ActiveLearningNode {

    protected PageHinkleyTest pageHinckleyTest;

    protected int predictionFunction;

    protected boolean changeDetection;

    protected Rule owner;
    
    protected boolean [] attributesMask;
    protected int numAttributesSelected;
    

    private static final long serialVersionUID = 9129659494380381126L;

    // The statistics for this node:
    // Number of instances that have reached it
    // Sum of y values
    // Sum of squared y values
    protected DoubleVector nodeStatistics;

    /**
     * Create a new RuleActiveLearningNode
     */
    public RuleActiveLearningNode(double[] initialClassObservations) {
        super(initialClassObservations);
        this.nodeStatistics = new DoubleVector(initialClassObservations);
    }

    public RuleActiveLearningNode() {
        this(new double[0]);
    }
    
    protected AbstractAMRules amRules;


    public RuleActiveLearningNode(Rule.Builder builder) {
        this(builder.statistics);
        this.changeDetection = builder.changeDetection;
        if (builder.changeDetection == false) { 
        	this.pageHinckleyTest = new PageHinkleyFading(builder.threshold, builder.alpha);
        }

        this.amRules = builder.amRules;
        this.predictionFunction = builder.predictionFunction;
        this.owner=builder.getOwner();
        
    }


    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#learnFromInstance(weka.core.Instance)
	 */
	abstract public void learnFromInstance(Instance inst);

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#learnFromInstance(weka.core.Instance, moa.classifiers.trees.HoeffdingTree)
	 */
	@Override
    public void learnFromInstance(Instance inst, HoeffdingTree ht) {
        learnFromInstance(inst);
    }

    protected AttributeClassObserver newNumericClassObserver() {
        //return new FIMTDDNumericAttributeClassObserver();
    	//return new FIMTDDNumericAttributeClassLimitObserver();
    	return (AttributeClassObserver)((AttributeClassObserver)this.amRules.numericObserverOption.getPreMaterializedObject()).copy();
    }


    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#updateStatistics(weka.core.Instance)
	 */
	public void updateStatistics(Instance instance) {
        learnFromInstance(instance);
    }

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getAttributeObservers()
	 */
	public AutoExpandVector<AttributeClassObserver> getAttributeObservers() {
        return this.attributeObservers;
    }

    protected void debug(String string,int level) {
        if (this.amRules.VerbosityOption.getValue() >= level) {
            System.out.println(string);
        }
    }

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getPrediction(weka.core.Instance)
	 */
	public double[] getPrediction(Instance instance) {
        int predictionMode = this.getLearnerToUse(instance, this.predictionFunction);
        return getPrediction(instance, predictionMode);
    }

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getPrediction(weka.core.Instance, int)
	 */
	abstract public double[] getPrediction(Instance instance, int predictionMode);
	

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getNormalizedPrediction(weka.core.Instance)
	 */

	abstract public int getLearnerToUse(Instance instance, int predictionMode);



    

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#computeError(weka.core.Instance)
	 */
	abstract public double computeError(Instance instance);

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#updatePageHinckleyTest(double)
	 */
	public boolean updatePageHinckleyTest(double error) {
        boolean changeDetected = false;
        if (this.changeDetection == false) { 
            changeDetected = pageHinckleyTest.update(error);
        }
        return changeDetected;
    }

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getInstancesSeen()
	 */
	public long getInstancesSeen() {
        return (long) this.getWeightSeen();
    }

   

    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#isAnomaly(weka.core.Instance, double, double, int)
	 */
	abstract public boolean isAnomaly(Instance instance,
            double uniVariateAnomalyProbabilityThreshold,
            double multiVariateAnomalyProbabilityThreshold,
            int numberOfInstanceesForAnomaly); 

    //Attribute probability
    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#computeProbability(double, double, double)
	 */
	public double computeProbability(double mean, double sd, double value) {
        double probability = 0.0;
        
      /* double diff = value - mean;
        if (sd > 0.0) {
            double k = (Math.abs(value - mean) / sd);
            if (k > 1.0) {
                probability = 1.0 / (k * k); // Chebyshev's inequality
            } else {
                probability = Math.exp(-(diff * diff / (2.0 * sd * sd)));
            }
        }*/
        
        if (sd > 0.0) {
            double k = (Math.abs(value - mean) / sd); // One tailed variant of Chebyshev's inequality
        	probability= 1.0 / (1+k*k); //cantelli's (one-tailed)
        	double var=Math.pow(sd, 2);
        	probability= 2*var/(var+Math.pow(Math.abs(value - mean), 2));
        	//probability= 1.0 / (1+k*k);
            //probability= 1.0 / (k*k); //chebyshev
        	//normal distribution
        	//double diff = value - mean;
        	//probability = Math.exp(-(diff * diff / (2.0 * sd * sd)));
        }
        
        return probability;
    }

    
    protected AttributeSplitSuggestion bestSuggestion = null;
    
    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getSplitIndex()
	 */
	public int getSplitIndex() {
		return splitIndex;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#setSplitIndex(int)
	 */
	public void setSplitIndex(int splitIndex) {
		this.splitIndex = splitIndex;
	}

	protected int splitIndex = 0;
    
    /* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getBestSuggestion()
	 */
	public AttributeSplitSuggestion getBestSuggestion() {
		return bestSuggestion;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#setBestSuggestion(moa.classifiers.core.AttributeSplitSuggestion)
	 */
	public void setBestSuggestion(AttributeSplitSuggestion bestSuggestion) {
		this.bestSuggestion = bestSuggestion;
	}

	protected double[] statisticsNewRuleActiveLearningNode = null;

	protected double[] statisticsBranchSplit = null;
	
	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getStatisticsBranchSplit()
	 */
	public double[] getStatisticsBranchSplit() {
		return statisticsBranchSplit;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#setStatisticsBranchSplit(double[])
	 */
	public void setStatisticsBranchSplit(double[] statisticsBranchSplit) {
		this.statisticsBranchSplit = statisticsBranchSplit;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getStatisticsNewRuleActiveLearningNode()
	 */
	public double[] getStatisticsNewRuleActiveLearningNode() {
		return statisticsNewRuleActiveLearningNode;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#setStatisticsNewRuleActiveLearningNode(double[])
	 */
	public void setStatisticsNewRuleActiveLearningNode(
			double[] statisticsNewRuleActiveLearningNode) {
		this.statisticsNewRuleActiveLearningNode = statisticsNewRuleActiveLearningNode;
	}

	protected double[] statisticsOtherBranchSplit;
    
	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getStatisticsOtherBranchSplit()
	 */
	public double[] getStatisticsOtherBranchSplit() {
		return statisticsOtherBranchSplit;
	}

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#setStatisticsOtherBranchSplit(double[])
	 */
	public void setStatisticsOtherBranchSplit(double[] statisticsOtherBranchSplit) {
		this.statisticsOtherBranchSplit = statisticsOtherBranchSplit;
	}
	
	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#tryToExpand(double, double)
	 */
	abstract public boolean tryToExpand(double splitConfidence, double tieThreshold);
    
	public static double computeHoeffdingBound(double range, double confidence,
            double n) {
        return Math.sqrt(((range * range) * Math.log(1.0 / confidence))
                / (2.0 * n));
    }

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#initialize(moa.classifiers.rules.RuleActiveLearningNode)
	 */
	abstract public void initialize(RuleActiveLearningNode oldLearningNode);
		

	/* (non-Javadoc)
	 * @see moa.classifiers.rules.RuleActiveLearningNodeInterface#getSimplePrediction()
	 */
	abstract public double[] getSimplePrediction();

	
	public DoubleVector getNodeStatistics(){
		return this.nodeStatistics;
	}

	public boolean updateChangeDetection(double error) {
		if(changeDetection==false){
			return  pageHinckleyTest.update(error);
		}
		else
			return false;
	}

	abstract public double getCurrentError();
}