/*
 *    RuleActiveLearningNode.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author E. Almeida, A. Carvalho, J. Gama
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.GaussianNumericAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.classifiers.core.splitcriteria.InfoGainSplitCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.rules.core.splitcriteria.InfoGainAMRulesSplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Utils;



public class RuleActiveClassifierNode extends RuleActiveLearningNode{

	private static final long serialVersionUID = -7141529189840963114L;

	public RuleActiveClassifierNode(double[] initialClassObservations) {
	        super(initialClassObservations);
	    }
	
	    public RuleActiveClassifierNode() {
	        this(new double[0]);
	    }
	
	    public RuleActiveClassifierNode(Rule.Builder builder) {
	        super(builder);
	    }

		@Override
		public double[] getPrediction(Instance instance, int predictionMode) {
	        double[] pred;
	        if (predictionMode == 0) {
	        	predictionMode = this.nbCorrectWeight > this.mcCorrectWeight ? 1:2;
	        }
	        if (predictionMode == 1) {
	        	pred = NaiveBayes.doNaiveBayesPrediction(instance,
	                    this.observedClassDistribution, this.attributeObservers);
	        } else  {
	        	pred = this.observedClassDistribution.getArrayCopy();
	        	
	        } 
	        return pred; 
	    }

		@Override
		public int getLearnerToUse(Instance instance, int predictionMode) {
	        return this.nbCorrectWeight > this.mcCorrectWeight ? 1:2;
	    }

		@Override
		public double computeError(Instance instance) {
			return (Utils.maxIndex(getPrediction(instance)) == (int) instance.classValue()) ? 0: 1;
		}

		@Override
		public boolean isAnomaly(Instance instance,
				double uniVariateAnomalyProbabilityThreshold,
				double multiVariateAnomalyProbabilityThreshold,
				int numberOfInstanceesForAnomaly) {
			// TODO Auto-generated method stub
			return false;
		}



		@Override
		public double[] getSimplePrediction() {
			return this.observedClassDistribution.getArrayCopy();
		}
	
        protected double mcCorrectWeight = 0.0;

        protected double nbCorrectWeight = 0.0;
		
		@Override
        public void learnFromInstance(Instance inst) {
            int trueClass = (int) inst.classValue();
            if (this.observedClassDistribution.maxIndex() == trueClass) {
                this.mcCorrectWeight += inst.weight();
            }
            if (Utils.maxIndex(NaiveBayes.doNaiveBayesPrediction(inst,
                    this.observedClassDistribution, this.attributeObservers)) == trueClass) {
                this.nbCorrectWeight += inst.weight();
            }
            
            // This is from ActiveLearningNode
            if (this.isInitialized == false) {
                this.attributeObservers = new AutoExpandVector<AttributeClassObserver>(inst.numAttributes());
                this.isInitialized = true;
            }
            this.observedClassDistribution.addToValue((int) inst.classValue(),
                    inst.weight());
            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                int instAttIndex = this.amRules.getModelAttIndexToInstanceAttIndex(i, inst);
                AttributeClassObserver obs = this.attributeObservers.get(i);
                if (obs == null) {
                    obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver() : newNumericClassObserver();
                    this.attributeObservers.set(i, obs);
                }
                obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
            }
        }
		
		   protected AttributeClassObserver newNominalClassObserver() {
		        //AttributeClassObserver nominalClassObserver = (AttributeClassObserver) getPreparedClassOption(this.nominalEstimatorOption);
		        //return (AttributeClassObserver) nominalClassObserver.copy();
			   return new NominalAttributeClassObserver();
		    }

		  protected AttributeClassObserver newNumericClassObserver() {
		        //AttributeClassObserver numericClassObserver = (AttributeClassObserver) getPreparedClassOption(this.numericEstimatorOption);
		        //return (AttributeClassObserver) numericClassObserver.copy();
			  return new GaussianNumericAttributeClassObserver(); //BinaryTreeNumericAttributeClassObserver();
		   }

        public double[] getClassVotes(Instance inst) {
            if (this.mcCorrectWeight > this.nbCorrectWeight) {
                return this.observedClassDistribution.getArrayCopy();
            }
            return NaiveBayes.doNaiveBayesPrediction(inst,
                    this.observedClassDistribution, this.attributeObservers);
        }
  
		@Override
		public boolean tryToExpand(double splitConfidence, double tieThreshold) {
			// splitConfidence. Hoeffding Bound test parameter.
	        // tieThreshold. Hoeffding Bound test parameter.
	        
	        SplitCriterion splitCriterion = newSplitCriterion();

	        // Using this criterion, find the best split per attribute and rank the results
	        AttributeSplitSuggestion[] bestSplitSuggestions
	                = this.getBestSplitSuggestions(splitCriterion);
	        Arrays.sort(bestSplitSuggestions);

	        // Declare a variable to determine if any of the splits should be performed
	        boolean shouldSplit = false;
	       
	        /*if (bestSplitSuggestions.length == 0) {
	        	return shouldSplit;
	        }*/

	        // If only one split was returned, use it
	        //if (bestSplitSuggestions.length < 2) {
	        //    shouldSplit = ((bestSplitSuggestions.length > 0) && (bestSplitSuggestions[0].merit > 0));  //JG AC 22-01-2014
	        //    bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
	        //} // Otherwise, consider which of the splits proposed may be worth trying
	        if (bestSplitSuggestions.length < 2) {
                shouldSplit = bestSplitSuggestions.length > 0;
            }
	        else {
	            // Determine the hoeffding bound value, used to select how many instances should be used to make a test decision
	            // to feel reasonably confident that the test chosen by this sample is the same as what would be chosen using infinite examples
	            double hoeffdingBound = computeHoeffdingBound(1, splitConfidence, getWeightSeen());
	            debug("Hoeffding bound " + hoeffdingBound,3);
	            // Determine the top two ranked splitting suggestions
	            bestSuggestion = bestSplitSuggestions[bestSplitSuggestions.length - 1];
	            AttributeSplitSuggestion secondBestSuggestion
	                    = bestSplitSuggestions[bestSplitSuggestions.length - 2];

	            debug("Merits: " + secondBestSuggestion.merit + " " + bestSuggestion.merit,3);

	            // If the upper bound of the sample mean for the ratio of SDR(best suggestion) to SDR(second best suggestion),
	            // as determined using the hoeffding bound, is less than 1, then the true mean is also less than 1, and thus at this
	            // particular moment of observation the bestSuggestion is indeed the best split option with confidence 1-delta, and
	            // splitting should occur.
	            // Alternatively, if two or more splits are very similar or identical in terms of their splits, then a threshold limit
	            // (default 0.05) is applied to the hoeffding bound; if the hoeffding bound is smaller than this limit then the two
	            // competing attributes are equally good, and the split will be made on the one with the higher SDR value.
	            
	             /*if ((((splitRatioStatistics.getValue(1)/splitRatioStatistics.getValue(0)) + hoeffdingBound)  < 1) 
	             || (hoeffdingBound < tieThreshold)) {		
	             System.out.println("Expanded ");
	             shouldSplit = true;    
	             }	*/	
	            
                if ((bestSuggestion.merit - secondBestSuggestion.merit > hoeffdingBound)
                        || (hoeffdingBound < tieThreshold)) {
                    shouldSplit = true;
                }
	            /*if (bestSuggestion.merit > 0) {
	                if ((((secondBestSuggestion.merit / bestSuggestion.merit) + hoeffdingBound) < 1)
	                        || (hoeffdingBound < tieThreshold)) {
	                    debug("Expanded ");
	                    shouldSplit = true;
	                    //}		

	                }
	            }*/
	        }

			if (shouldSplit == true) {
	            AttributeSplitSuggestion splitDecision = bestSplitSuggestions[bestSplitSuggestions.length - 1];

	            // Decide best node to keep: one with lower variance
	            double minValue = Double.MAX_VALUE;
	            
	            double[] branchMerits = InfoGainAMRulesSplitCriterion.computeBranchSplitMerits(bestSuggestion.resultingClassDistributions);
	            for (int i = 0; i < bestSuggestion.numSplits(); i++) {
	                
	                double value = branchMerits[i];
	                if (value < minValue) {
	                    minValue = value;
	                    splitIndex = i;
	                    statisticsNewRuleActiveLearningNode = bestSuggestion.resultingClassDistributionFromSplit(i);
	                }
	            }
	            statisticsBranchSplit = splitDecision.resultingClassDistributionFromSplit(splitIndex);
	            statisticsOtherBranchSplit = bestSuggestion.resultingClassDistributionFromSplit(splitIndex == 0 ? 1 : 0);
			}
			return shouldSplit;
		}

		private SplitCriterion newSplitCriterion() {
			return new InfoGainSplitCriterion();
		}

		@Override
		public void initialize(RuleActiveLearningNode oldLearningNode) {
			    this.observedClassDistribution = new DoubleVector(oldLearningNode.getObservedClassDistribution());
	            this.weightSeenAtLastSplitEvaluation = getWeightSeen();
	            this.isInitialized = false;
		}
		
		
		public AttributeSplitSuggestion[] getBestSplitSuggestions(
                SplitCriterion criterion) {
            List<AttributeSplitSuggestion> bestSuggestions = new LinkedList<AttributeSplitSuggestion>();
            double[] preSplitDist = this.observedClassDistribution.getArrayCopy();
            //if (!ht.noPrePruneOption.isSet()) {
                // add null split as an option
                bestSuggestions.add(new AttributeSplitSuggestion(null,
                        new double[0][], criterion.getMeritOfSplit(
                        preSplitDist,
                        new double[][]{preSplitDist})));
            //}
            for (int i = 0; i < this.attributeObservers.size(); i++) {
                AttributeClassObserver obs = this.attributeObservers.get(i);
                if (obs != null) {
                    AttributeSplitSuggestion bestSuggestion = obs.getBestEvaluatedSplitSuggestion(criterion,
                            preSplitDist, i, true); // true means binary splits
                    if (bestSuggestion != null) {
                        bestSuggestions.add(bestSuggestion);
                    }
                }
            }
            return bestSuggestions.toArray(new AttributeSplitSuggestion[bestSuggestions.size()]);
        }
		
		@Override
        public double getWeightSeen() {
            return this.observedClassDistribution.sumOfValues();
        }

		@Override
		public double getCurrentError() {
			// TODO Not implemented yet for classification
			return -1;
		}
	    
}
