/*
 *    RuleClassifier.java
 *    Copyright (C) 2012 University of Porto, Portugal
 *    @author P. Kosina, E. Almeida, J. Gama
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

package moa.classifiers.rules;

import java.util.*; 
import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.attributeclassobservers.*;
import moa.classifiers.core.attributeclassobservers.BinaryTreeNumericAttributeClassObserver.Node;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.FlagOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.MultiChoiceOption;
import weka.core.Instance;
import weka.core.Utils;


/**
 * This classifier learn ordered and unordered rule set from data stream.
 * 
 * <p>Learning Decision Rules from Data Streams, IJCAI 2011, J. Gama,  P. Kosina </p>
 *
 * 
 * <p>Parameters:</p>
 * <ul>
 * <li> -p: Minimum value of p </li>
 * <li> -t: Tie Threshold </li>
 * <li> -c: Split Confidence </li>
 * <li> -g: GracePeriod, the number of instances a leaf should observe between split attempts </li>
 * <li> -o: Prediction function to use. Ex:FirstHit </li>
 * <li> -r: Learn ordered or unordered rule </li>
 * </ul>
 * 
 * @author P. Kosina, E. Almeida, J. Gama
 * @version $Revision: 1 $
 */

public class RuleClassifier extends AbstractClassifier{
	
	  private static final long serialVersionUID = 1L;
	  
	  @Override
	  public String getPurposeString() {
	        return "Rule Classifier.";
	    }
	  
	  protected Instance instance; 
	  
	  protected AutoExpandVector<AttributeClassObserver> attributeObservers;
	  
	  protected AutoExpandVector<AttributeClassObserver> attributeObserversGauss;
	  
	  protected AutoExpandVector<AttributeClassObserver> observersTemp = new AutoExpandVector<AttributeClassObserver>(); 
	  
	  protected DoubleVector observedClassDistribution;
	  
	  protected DoubleVector saveBestEntropy = new DoubleVector();		// Saves the best value of entropy, cut_Point and symbol.
	  
	  protected DoubleVector saveBestEntropyNominalAttrib = new DoubleVector();	 	// Saves the best value of entropy and their cut_Point.
	  
	  protected ArrayList<Integer> ruleClassIndex = new ArrayList<Integer>();		// The index of the class for each rule.
	  
	  protected ArrayList<ArrayList<Double>> saveBestValGlobalEntropy = new ArrayList<ArrayList<Double>>();		// For each attribute contains the best value of entropy and its cutPoint.
	  
	  protected ArrayList<Double> saveBestGlobalEntropy = new ArrayList<Double>();		// For each attribute contains the best value of entropy.
	  
	  protected ArrayList<Double> saveTheBest = new ArrayList<Double>();		// Contains the best attribute. 
	  
	  protected ArrayList<ArrayList<Integer>> majority = new ArrayList<ArrayList<Integer>>();		// Distribution of class for each rule.
	  
	  protected ArrayList<String> ruleClassName = new ArrayList<String>();		// Name of the class for each rule.
	  
	  protected  ArrayList<Rule> ruleSet = new ArrayList<Rule>();
	  
	  double minEntropyTemp = Double.MAX_VALUE;
	  
	  double cutPointTemp = 0.0;
	  
	  double minEntropyNominalAttrib = Double.MAX_VALUE;
	  
	  double symbol = 0.0;
	  
	  int numTotalInstance = 0;
	  
	  int numAttributes = 0;
	  
	  int numClass = 0; 
	 
	  Node root;
	  
	  Predicates pred;  

	  public FloatOption PminOption = new FloatOption("Pmin",
	            'p', "Percentage of the total number of example seen in the node.",
	             0.1, 0.0, 1.0);
	  
	  public FloatOption splitConfidenceOption = new FloatOption(
	            "splitConfidence",
	            'c',
	            "The allowable error in split decision, values closer to 0 will take longer to decide.",
	            0.000001, 0.0, 1.0);
	  
	  public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
	            't', "Threshold below which a split will be forced to break ties.",
	             0.05, 0.0, 1.0);
	  
	  public IntOption gracePeriodOption = new IntOption(
		        "gracePeriod",'g', "The number of instances a leaf should observe between split attempts.",
		         200, 0, Integer.MAX_VALUE);
	  
	  public MultiChoiceOption predictionFunctionOption = new MultiChoiceOption(
	            "predictionFunctionOption", 'z', "The prediction function to use.", new String[]{
	            "firstHit", "weightedSum", "weightedMax"}, new String[]{
	            "first Hit",  "weighted Sum", "weighted Max"}, 0);
	  
	  public FlagOption orderedRulesOption = new FlagOption("orderedRules", 'r',
	            "orderedRules."); 
	  
	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] votes = new double[observedClassDistribution.numValues()];
		switch (this.predictionFunctionOption.getChosenIndex()) {
		case 0:
			votes = firstHit(inst);
			break; 
        case 1:
        	votes = weightedSum(inst);
        	break; 
        case 2:
        	votes = weightedMax(inst);
        	break;
		  }
		return votes; 
	}
	
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
		}
	
	@Override
	public void resetLearningImpl() {
		this.observedClassDistribution = new DoubleVector();
        this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
        this.attributeObserversGauss = new AutoExpandVector<AttributeClassObserver>();	
	}
	
	public double getWeightSeen() {
		return this.observedClassDistribution.sumOfValues(); 
      }

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		int numInstanciaObservers = 0;
		int countRuleFiredTrue = 0;
		int remainder = (int)Double.MAX_VALUE;
		boolean ruleFired = false;
		instance = inst;
		numAttributes = instance.numAttributes()-1;
		numClass = instance.numClasses();
		numTotalInstance = numTotalInstance + 1;
		for (int j = 0; j < ruleSet.size(); j++) {
			if (ruleSet.get(j).ruleEvaluate(inst) == true) {
				countRuleFiredTrue = countRuleFiredTrue + 1;
		    	saveBestValGlobalEntropy = new ArrayList<ArrayList<Double>>();
		    	saveBestGlobalEntropy = new ArrayList<Double>();
			    saveTheBest = new ArrayList<Double>();
			    minEntropyTemp = Double.MAX_VALUE;
				minEntropyNominalAttrib = Double.MAX_VALUE;
				ruleSet.get(j).obserClassDistrib.addToValue((int) inst.classValue(), inst.weight());
				for (int i = 0; i < inst.numAttributes() - 1; i++) {
					int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
		            AttributeClassObserver obs = ruleSet.get(j).observers.get(i);		// Nominal and binary tree.
		            AttributeClassObserver obsGauss = ruleSet.get(j).observersGauss.get(i);		// Gaussian.
		            if (obs == null) {
		                obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver()
		                        : newNumericClassObserver();
		                ruleSet.get(j).observers.set(i, obs);     
		            }
		            if (obsGauss == null) {
		                obsGauss = inst.attribute(instAttIndex).isNumeric() ? newNumericClassObserver2():null;
		                ruleSet.get(j).observersGauss.set(i, obsGauss);     
		            }
		            obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
		            if (inst.attribute(instAttIndex).isNumeric()) {
		            	obsGauss.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
		            }  
	            }
		    	int count = getCount(inst, ruleSet.get(j).predicateSet);		// Check if a given instance satisfies the rule.
		        if (count == ruleSet.get(j).predicateSet.size()) {
		        	getRuleClass(inst, j);		// For each rule gets the respective classes distributions.
		        }
		        numInstanciaObservers = observersNumberInstance( inst, ruleSet.get(j).observers);		// Number of instances for this rule observers.
		        if (numInstanciaObservers != 0 && gracePeriodOption.getValue() != 0) {
		        	remainder = (numInstanciaObservers) % (gracePeriodOption.getValue());    
		        }
		        if (remainder == 0){
		        	observersTemp = ruleSet.get(j).observers;
			        theBestAttributes(inst,observersTemp);		// The best value of entropy for each attribute.
			    	Collections.sort(saveBestGlobalEntropy); 
		            boolean HB = checkBestAttrib(numInstanciaObservers, inst, ruleSet.get(j).observers);		// Check if the best attribute value is really the best.
		    		if (HB == true) {
		    			double attributeValue = saveTheBest.get(3);
		    			double symbol = saveTheBest.get(2);		// =, <=, > (0.0, -1.0, 1.0).
		    			double value = saveTheBest.get(0);		// Value of the attribute.
		    			pred = new Predicates(attributeValue, symbol, value);
		    			int countPred = 0;
		    			for (int i = 0; i < ruleSet.get(j).predicateSet.size(); i++) {		// Checks if the new predicate is not yet in the predicateSet. 
	    					 if (pred.getSymbol() == 0.0) {		// Nominal Attribute.
	    						 if (ruleSet.get(j).predicateSet.get(i).getAttributeValue() != pred.getAttributeValue()) {
	    							 countPred = countPred + 1;
	    						 }
	    					 }
	    					 else {
	    						 if (ruleSet.get(j).predicateSet.get(i).getAttributeValue() != pred.getAttributeValue() 
	    								 || ruleSet.get(j).predicateSet.get(i).getSymbol() != pred.getSymbol() 
	    								 || ruleSet.get(j).predicateSet.get(i).getValue() != pred.getValue()) {
	    							 countPred = countPred+1;
	    							 }
	    						 }
	    					 }
		    			if (countPred == ruleSet.get(j).predicateSet.size()) {
		    				int countDifPred = 0;
	    					ArrayList<Predicates> predicSetTemp = new ArrayList<Predicates>();
	    					for (int x = 0; x < ruleSet.get(j).predicateSet.size(); x++) {
	    						predicSetTemp.add(ruleSet.get(j).predicateSet.get(x));
	    					 }
	    					predicSetTemp.add(pred);
	    					for (int f = 0; f < ruleSet.size(); f++) {
	    						int countDifPredTemp = 0;
	    						if (ruleSet.get(f).predicateSet.size() == predicSetTemp.size()) {
	    							for(int x = 0; x < ruleSet.get(f).predicateSet.size(); x++) {
	    								if (ruleSet.get(f).predicateSet.get(x).getAttributeValue() == predicSetTemp.get(x).getAttributeValue() 
	    										&& ruleSet.get(f).predicateSet.get(x).getSymbol() == predicSetTemp.get(x).getSymbol() 
	    										&& ruleSet.get(f).predicateSet.get(x).getValue() == predicSetTemp.get(x).getValue()) {
	    									countDifPredTemp = countDifPredTemp+1;
	    									}
	    								}
	    							if (countDifPredTemp == predicSetTemp.size()) {
	    								break;
	    								}else{
	    									countDifPred = countDifPred + 1;
	    									}
	    							}else{
 							countDifPred = countDifPred + 1;
 							}
	    						}
	    					if (countDifPred == ruleSet.size()) {
	    						if (pred.getSymbol() == 0.0) {
	    							ruleSet.get(j).predicateSet.add(pred);
	    	    					majority.get(j).clear(); 
	    		    				ruleSet.get(j).obserClassDistrib = new DoubleVector();
	    			    			ruleSet.get(j).observers = new AutoExpandVector<AttributeClassObserver>();
	    			    			ruleSet.get(j).observersGauss = new AutoExpandVector<AttributeClassObserver>();
	    			    			} else if (pred.getSymbol() == 1.0) {
	    			    				int countIqualPred = 0;
	    			    				for (int f = 0; f < ruleSet.get(j).predicateSet.size(); f++) {
	    			    					if (pred.getAttributeValue() == ruleSet.get(j).predicateSet.get(f).getAttributeValue()
	    			    							&& pred.getSymbol() == ruleSet.get(j).predicateSet.get(f).getSymbol()) {
	    			    						countIqualPred = countIqualPred + 1;
	    			    						if (pred.getValue() > ruleSet.get(j).predicateSet.get(f).getValue()) {
	    			    							ruleSet.get(j).predicateSet.remove(f);
	    	    									ruleSet.get(j).predicateSet.add(pred);
	    	    				    				majority.get(j).clear();
	    	    				    				ruleSet.get(j).obserClassDistrib=new DoubleVector();
	    	    				    				ruleSet.get(j).observers=new AutoExpandVector<AttributeClassObserver>();
	    	    				    				ruleSet.get(j).observersGauss=new AutoExpandVector<AttributeClassObserver>();
	    	    				    				}
	    			    						}
	    			    					}
	    			    				if (countIqualPred == 0) {
	    			    					ruleSet.get(j).predicateSet.add(pred);
	    	    							majority.get(j).clear();
	    	    				    		ruleSet.get(j).obserClassDistrib=new DoubleVector();
	    	    				    		ruleSet.get(j).observers=new AutoExpandVector<AttributeClassObserver>();
	    	    				    		ruleSet.get(j).observersGauss=new AutoExpandVector<AttributeClassObserver>();
	    	    				    		}
	    			    				}else{
	    	    						 int countIqualPred = 0;
	    	    						 for (int f = 0; f < ruleSet.get(j).predicateSet.size(); f++) {
	    	    							 if (pred.getAttributeValue() == ruleSet.get(j).predicateSet.get(f).getAttributeValue()
	    	    									 && pred.getSymbol() == ruleSet.get(j).predicateSet.get(f).getSymbol()) {
	    	    								 countIqualPred = countIqualPred + 1;
	    	    								 if (pred.getValue() < ruleSet.get(j).predicateSet.get(f).getValue()) {
	    	    									 ruleSet.get(j).predicateSet.remove(f);
	    	    									 ruleSet.get(j).predicateSet.add(pred);
	    	    									 majority.get(j).clear();
	    	    				    				 ruleSet.get(j).obserClassDistrib = new DoubleVector();
	    	    				    				 ruleSet.get(j).observers = new AutoExpandVector<AttributeClassObserver>();
	    	    				    				 ruleSet.get(j).observersGauss = new AutoExpandVector<AttributeClassObserver>();
	    	    				    				 }
	    	    								 }
	    	    							 }
	    	    						 if (countIqualPred == 0) {
	    	    							 ruleSet.get(j).predicateSet.add(pred);
	    	    							 majority.get(j).clear();
	    	    				    		 ruleSet.get(j).obserClassDistrib=new DoubleVector();
	    	    				    		 ruleSet.get(j).observers=new AutoExpandVector<AttributeClassObserver>();
	    	    				    		 ruleSet.get(j).observersGauss=new AutoExpandVector<AttributeClassObserver>();
	    	    				    		 }
	    	    						 }
	    						}
	    					}
		    			}
		    		}
		        if (orderedRulesOption.isSet()) {		// Ordered rules
		        	break;
		        	}
		        }
			}
		if (countRuleFiredTrue > 0) {
			ruleFired = true;
			}else{
				ruleFired = false;
		    }
		if (ruleFired == false) {
			saveBestValGlobalEntropy = new ArrayList<ArrayList<Double>>();
			saveBestGlobalEntropy = new ArrayList<Double>();
			saveTheBest = new ArrayList<Double>();
			minEntropyTemp = Double.MAX_VALUE;
			minEntropyNominalAttrib = Double.MAX_VALUE;
			this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
			for (int i = 0; i < inst.numAttributes() - 1; i++) {
				int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
				AttributeClassObserver obs = this.attributeObservers.get(i);
			    AttributeClassObserver obsGauss = this.attributeObserversGauss.get(i);
			    if (obs == null) {
			    	obs = inst.attribute(instAttIndex).isNominal() ? newNominalClassObserver()
			    			: newNumericClassObserver();
			    	this.attributeObservers.set(i, obs); 
			    	}
			    if (obsGauss == null) {
			    	obsGauss = inst.attribute(instAttIndex).isNumeric() ? newNumericClassObserver2():null;
			    	this.attributeObserversGauss.set(i, obsGauss);
			    	}
			    obs.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
			    if (inst.attribute(instAttIndex).isNumeric()) {
			    	obsGauss.observeAttributeClass(inst.value(instAttIndex), (int) inst.classValue(), inst.weight());
			    	}
			    }
			numInstanciaObservers=observersNumberInstance( inst, this.attributeObservers);
			if (numInstanciaObservers != 0 && gracePeriodOption.getValue() != 0) {
				remainder = (numInstanciaObservers) % (gracePeriodOption.getValue());
				}
			if (remainder == 0) {
				observersTemp = this.attributeObservers;
				theBestAttributes(inst,observersTemp);  
		    	Collections.sort(saveBestGlobalEntropy);         
	            boolean HB = checkBestAttrib(numInstanciaObservers, inst, this.attributeObservers);
	            if (HB == true) {
	            	int countDifRule = 0;
	            	double attributeValue = saveTheBest.get(3);
	    			double symbol = saveTheBest.get(2);		// =, <=, > : (0.0, -1.0, 1.0).
	    			double value = saveTheBest.get(0);		// Value of the attribute
	    			pred = new Predicates(attributeValue, symbol, value);
	    			Rule Rl = new Rule();		// Create Rule.
	    			Rl.predicateSet.add(pred);
	    			for (int i = 0; i < ruleSet.size(); i++) {
	    				if (ruleSet.get(i).predicateSet.size() == 1) {
	    					if (ruleSet.get(i).predicateSet.get(0).getAttributeValue() != Rl.predicateSet.get(0).getAttributeValue()
	    							|| ruleSet.get(i).predicateSet.get(0).getSymbol() != Rl.predicateSet.get(0).getSymbol()
	    							|| ruleSet.get(i).predicateSet.get(0).getValue() != Rl.predicateSet.get(0).getValue()) {
	    						countDifRule = countDifRule + 1;
	    						}
	    					} else if (ruleSet.get(i).predicateSet.size() > 1){
	    						countDifRule = countDifRule + 1;
	    						}
	    				}
	    			if (countDifRule == ruleSet.size()|| ruleSet.isEmpty()) {
	    				ruleSet.add(Rl);
	    				majority.add(new ArrayList<Integer>());
	    				if (Rl.predicateSet.get(0).getSymbol() == -1.0 ||Rl.predicateSet.get(0).getSymbol() == 1.0) {
	    					double posClassDouble = saveTheBest.get(4); 
	    					int posClassInt = (int)posClassDouble;
	    					ruleClassIndex.add(posClassInt);
	    					String classe = inst.classAttribute().value(posClassInt);
	    					ruleClassName.add(classe);
	    					}
	    				}
	    			this.attributeObservers = new AutoExpandVector<AttributeClassObserver>(); 
	    			this.attributeObserversGauss = new AutoExpandVector<AttributeClassObserver>();
	    			}
	            }
			}
		}
	
	@Override	
	public void getModelDescription(StringBuilder out, int indent) {
		StringUtils.appendNewline(out);
        StringUtils.appendIndented(out, indent, "Number of Rule: " + ruleSet.size());
        StringUtils.appendNewline(out);
        StringUtils.appendNewline(out);
        for (int k = 0; k < ruleSet.size(); k++) {
        	StringUtils.appendIndented(out, indent, "Rule "+(k+1)+": ");
        	for (int i = 0; i < ruleSet.get(k).predicateSet.size(); i++) {
        		if (ruleSet.get(k).predicateSet.size() == 1) {
        			if (ruleSet.get(k).predicateSet.get(i).getSymbol() == 0.0) {
    					String nam = instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
    					String val = instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).value((int)ruleSet.get(k).predicateSet.get(i).getValue());
    					StringUtils.appendIndented(out, indent, nam+" = "+val+" --> "+getRuleMajorityClass(k,instance));
    					StringUtils.appendNewline(out);
    					} else if (ruleSet.get(k).predicateSet.get(i).getSymbol() == -1.0){
    						String nam=instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
    						StringUtils.appendIndented(out, indent, nam+" <= "+ruleSet.get(k).predicateSet.get(i).getValue()+" --> "+ruleClassName.get(k));
    					    StringUtils.appendNewline(out);
    					    } else {
    					    	String nam=instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
    			                StringUtils.appendIndented(out, indent, nam+" > "+ruleSet.get(k).predicateSet.get(i).getValue()+" --> "+ruleClassName.get(k));
    					        StringUtils.appendNewline(out);
    					        }
        			} else {
        				if (ruleSet.get(k).predicateSet.get(i).getSymbol() == 0.0) {
        					String nam=instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
    					    String val=instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).value((int)ruleSet.get(k).predicateSet.get(i).getValue());
    					    StringUtils.appendIndented(out, indent, nam+" = "+val+" ");
    					    } else if (ruleSet.get(k).predicateSet.get(i).getSymbol()==-1.0){
    					    	String nam=instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
    					        StringUtils.appendIndented(out, indent, nam+" <= "+ruleSet.get(k).predicateSet.get(i).getValue()+" ");
    					        } else {
    					        	String nam=instance.attribute((int)ruleSet.get(k).predicateSet.get(i).getAttributeValue()).name();
    					        	StringUtils.appendIndented(out, indent, nam+" > "+ruleSet.get(k).predicateSet.get(i).getValue()+" ");
    					        	}
        				if (i < ruleSet.get(k).predicateSet.size() - 1) {
        					StringUtils.appendIndented(out, indent, "and ");
        					} else {
        						int count = getCountNominalAttrib(ruleSet.get(k).predicateSet);
        						if ((ruleSet.get(k).predicateSet.get(i).getSymbol() == 0.0) || (count != 0)) {
        							StringUtils.appendIndented(out, indent, " --> "+getRuleMajorityClass(k,instance));
                    		        StringUtils.appendNewline(out);
                    		        } else {
                    		        	StringUtils.appendIndented(out, indent, " --> "+ruleClassName.get(k));
                    		            StringUtils.appendNewline(out);
                    		            }
        						}
        				}
        		}
        	StringUtils.appendNewline(out);
        	}
        }
	
	@Override
	public boolean isRandomizable() {
		return false;
	}
	
	public int getCountNominalAttrib(ArrayList<Predicates> predicateSet) {
		int count = 0;
		for (int i = 0; i < predicateSet.size(); i++) {
			if (predicateSet.get(i).getSymbol() == 0.0) {
				count = count + 1;
				break;
			}
		}
		return count;
	}
	
	// This function gives the best value of entropy for each attribute
	public void theBestAttributes(Instance instance, 
			AutoExpandVector<AttributeClassObserver> observersParameter) {
		for(int z = 0; z < instance.numAttributes() - 1; z++){
			int instAttIndex = modelAttIndexToInstanceAttIndex(z, instance);
			ArrayList<Double> attribBest = new ArrayList<Double>();
			if(instance.attribute(instAttIndex).isNominal()){
				minEntropyNominalAttrib=Double.MAX_VALUE;	
				AutoExpandVector<DoubleVector> attribNominal = ((NominalAttributeClassObserver)observersParameter.get(z)).attValDistPerClass;
				findBestValEntropyNominalAtt(attribNominal);		// The best value (lowest entropy) of a nominal attribute.
	            attribBest.add(saveBestEntropyNominalAttrib.getValue(0));
	            attribBest.add(saveBestEntropyNominalAttrib.getValue(1));
	            attribBest.add(saveBestEntropyNominalAttrib.getValue(2));
	            saveBestValGlobalEntropy.add(attribBest);
	            saveBestGlobalEntropy.add(saveBestEntropyNominalAttrib.getValue(1));
	            } else {
	            	root=((BinaryTreeNumericAttributeClassObserver)observersParameter.get(z)).root;
					mainFindBestValEntropy(root);		// The best value (lowest entropy) of a numeric attribute.
		            attribBest.add(saveBestEntropy.getValue(0));
		            attribBest.add(saveBestEntropy.getValue(1));
		            attribBest.add(saveBestEntropy.getValue(2));
		            attribBest.add(saveBestEntropy.getValue(4));
		            saveBestValGlobalEntropy.add(attribBest);
		            saveBestGlobalEntropy.add(saveBestEntropy.getValue(1));
		            }
			}
		}
	
	public double  nominalAttributeEntropy(ArrayList<Double> valorDistClassE) {
		double entropy = 0.0;
		double sum = 0.0;
		for (double d : valorDistClassE) {
			if (d > 0.0) {
				entropy -= d * Utils.log2(d);
				sum += d;
				}
			}
		return sum > 0.0 ? (entropy + sum * Utils.log2(sum)) / sum : 0.0;
		}
	
	public double entropy(DoubleVector ValorDistClassE) {
		double entropy = 0.0;
		double sum = 0.0;
		for (double d : ValorDistClassE.getArrayCopy()) {
			if (d > 0.0) {
				entropy -= d * Utils.log2(d);
				sum += d;
				}
			}
		return sum > 0.0 ? (entropy + sum * Utils.log2(sum)) / sum : 0.0;
		}
	
	// Get the best value of entropy and its cutPoint for a numeric attribute
	public void findBestValEntropy(Node node, DoubleVector classCountL, DoubleVector classCountR,
			boolean status, double minEntropy, ArrayList<Double> parentCCLeft ){
		double classCountLEntropy;		// Entropy of class count left.
		double classCountREntropy;		// Entropy of class count right.
		if (root != null) {
			double numInst = root.classCountsLeft.sumOfValues() + root.classCountsRight.sumOfValues();
			if (node != null) {
				int numClass=0;
				DoubleVector classCountLTemp = new DoubleVector(); 
				DoubleVector classCountRTemp = new DoubleVector(); 
				DoubleVector parentCCL = new DoubleVector();
				ArrayList<Double> parentCCLParameter = new ArrayList<Double> ();
				for (int f = 0; f < node.classCountsLeft.numValues(); f++) {
					parentCCLParameter.add(node.classCountsLeft.getValue(f));
					}
				for (int p = 0; p < parentCCLeft.size(); p++) {
					parentCCL.addToValue(p, parentCCLeft.get(p));
					}
				if (classCountL.numValues() >= classCountR.numValues()) {
					numClass = classCountL.numValues();
				} else {
					numClass = classCountR.numValues();
				}
				// Counting the real class count left and the real class count right.
				if (node.cut_point != root.cut_point) {
					for (int i = 0; i < numClass; i++) {
						if (status == true) {		// Left node.
							double parentss = parentCCL.getValue(i) - (node.classCountsLeft.getValue(i) 
									+ node.classCountsRight.getValue(i));
						    double left = classCountL.getValue(i) 
						    		- node.classCountsRight.getValue(i) - parentss;
						    double right = classCountR.getValue(i) 
						    		+ node.classCountsRight.getValue(i) + parentss;
						    classCountLTemp.addToValue(i, left);
					        classCountRTemp.addToValue(i, right);
					        }
						if (status == false) {		// Right node.
							double left = classCountL.getValue(i)+ node.classCountsLeft.getValue(i);
							double right = classCountR.getValue(i)- node.classCountsLeft.getValue(i);
							classCountLTemp.addToValue(i, left);
							classCountRTemp.addToValue(i, right);
							}
						}
					} else {
						classCountLTemp = classCountL;
						classCountRTemp = classCountR;
						}
				double classCountLSum = classCountLTemp.sumOfValues();
				double classCountRSum = classCountRTemp.sumOfValues();
				// The entropy value for all nodes except for the root.
				if ((classCountLSum > PminOption.getValue()*numInst) 
						&& (classCountRSum > PminOption.getValue() * numInst)) {
					classCountLEntropy = entropy( classCountLTemp);
					classCountREntropy = entropy(classCountRTemp);
					if (((classCountLSum / numInst) * classCountLEntropy + (classCountRSum / numInst)* classCountREntropy) <= minEntropy) {
						minEntropyTemp = (classCountLSum / numInst) * classCountLEntropy 
								+ (classCountRSum / numInst) * classCountREntropy;
						cutPointTemp = node.cut_point;
						if (classCountLEntropy <= classCountREntropy) {
							symbol = -1.0;
							double value = 0.0;
							double index = 0.0;
							for (int h = 0; h < numClass; h++) {
								if (value <= classCountLTemp.getValue(h)) {
									value = classCountLTemp.getValue(h);
					    			index = (double)h;
					    			}
								}
							saveBestEntropy.setValue(0, cutPointTemp);
					    	saveBestEntropy.setValue(1, classCountLEntropy);
					    	saveBestEntropy.setValue(2, symbol);
					    	saveBestEntropy.setValue(4, index);
					    	} else {
					    		symbol = 1.0;
					    		double value = 0.0;
					    		double index = 0.0;
					    		for (int h = 0; h < numClass; h++) {
					    			if (value <= classCountRTemp.getValue(h)) {
					    				value = classCountRTemp.getValue(h);
					    				index = (double)h;
					    				}
					    			}
					    		saveBestEntropy.setValue(0, cutPointTemp);
						    	saveBestEntropy.setValue(1, classCountREntropy);
						    	saveBestEntropy.setValue(2, symbol);
						    	saveBestEntropy.setValue(4, index);
						    	}
						}
					}
				findBestValEntropy(node.left ,classCountLTemp , classCountRTemp,
						true, minEntropyTemp, parentCCLParameter);
				findBestValEntropy(node.right ,classCountLTemp , classCountRTemp,
						false, minEntropyTemp, parentCCLParameter);
				}
			}
		}
	
	public void mainFindBestValEntropy(Node root) {
		if (root != null) {
			ArrayList<Double> parentClassCL=new  ArrayList<Double>();
			DoubleVector classCountL = root.classCountsLeft; //class count left
			DoubleVector classCountR = root.classCountsRight; //class count left
			double numInst = root.classCountsLeft.sumOfValues() + root.classCountsRight.sumOfValues();
			double classCountLSum = root.classCountsLeft.sumOfValues();
			double classCountRSum = root.classCountsRight.sumOfValues();
			double classCountLEntropy = entropy(classCountL);
			double classCountREntropy = entropy(classCountR);
			minEntropyTemp = ( classCountLSum / numInst) * classCountLEntropy 
					+ (classCountRSum / numInst)* classCountREntropy;
			for (int f = 0; f < root.classCountsLeft.numValues(); f++) {
				parentClassCL.add(root.classCountsLeft.getValue(f));
				}
			findBestValEntropy(root ,classCountL , classCountR, true, minEntropyTemp, parentClassCL);
			}
		}
	
	public void findBestValEntropyNominalAtt(AutoExpandVector<DoubleVector> attrib) {
		double count;
		double sumValue = 0.0;
		int classNumVal = 0;
		ArrayList<ArrayList<Double>> distClassValue =  new ArrayList<ArrayList<Double>>();
		saveBestEntropyNominalAttrib = new DoubleVector();
		for (int v = 0; v < attrib.size(); v++) {
			if (attrib.get(v)!= null) {
				if (attrib.get(v).numValues() > classNumVal) {
					classNumVal=attrib.get(v).numValues();
					}
				}
			}
		for (int d = 0; d < attrib.size(); d++) {		 // For each class gets their values.
			distClassValue.add(new ArrayList<Double>());
			count = 0.0;
			if (attrib.get(d) != null) {
				for (int e = 0; e < classNumVal; e++) {	
	      		double valor = attrib.get(d).getValue(e);
	      		count = count + valor;
	      		distClassValue.get(d).add(valor);
	      		}
				}
			}
		for (int l = 0; l < distClassValue.size(); l++) {
			if (distClassValue.get(l).isEmpty()) {
				for (int h = 0; h < classNumVal; h++){
					distClassValue.get(l).add(0.0);
					}
				}
			}
		for (int i = 0; i < distClassValue.get(0).size(); i++) {
			ArrayList<Double> saveVal = new ArrayList<Double>();
			for (int j = 0; j < distClassValue.size(); j++){
				double valor = distClassValue.get(j).get(i) ;
				saveVal.add(valor);
				}
			sumValue = getSumOfValue(saveVal);
			double entropyVal = nominalAttributeEntropy(saveVal);
			if (entropyVal <= minEntropyNominalAttrib) {
				minEntropyNominalAttrib = entropyVal;
				saveBestEntropyNominalAttrib.setValue(0, i);
				saveBestEntropyNominalAttrib.setValue(1, entropyVal);
				saveBestEntropyNominalAttrib.setValue(2, 0.0);
				}
			}
		}
	
	public double getSumOfValue(ArrayList<Double> values) {
		double sum = 0.0;
		for (int w = 0; w < values.size(); w++) {
			sum = sum + values.get(w);
    		}
		return sum;
		}
	
	//Hoeffding Bound 
	public  double ComputeHoeffdingBound(double range, double confidence,
		            double n) {
		return Math.sqrt(((range * range) * Math.log(1.0 / confidence))
		                / (2.0 * n));
		}
	
	public boolean checkBestAttrib(double n, Instance inst,
			AutoExpandVector<AttributeClassObserver> observerss){
		double h0 = globalEntropy(inst, observerss);
		boolean  isTheBest = false;
		double bestEntropy = saveBestGlobalEntropy.get(0);
		double secondBestEntropy = saveBestGlobalEntropy.get(1);
	    double range = Utils.log2(numClass);
		double hoeffdingBound = ComputeHoeffdingBound(range, splitConfidenceOption.getValue(), n);
		if ((h0 > bestEntropy) && ((secondBestEntropy - bestEntropy > hoeffdingBound)
				|| (hoeffdingBound < tieThresholdOption.getValue()))) {
			for (int i = 0; i < saveBestValGlobalEntropy.size(); i++) {
				if (bestEntropy ==(saveBestValGlobalEntropy.get(i).get(1))) {
					saveTheBest.add(saveBestValGlobalEntropy.get(i).get(0)); 
					saveTheBest.add(saveBestValGlobalEntropy.get(i).get(1));
					saveTheBest.add(saveBestValGlobalEntropy.get(i).get(2)); 
					saveTheBest.add((double)i);
					if (saveTheBest.get(2) != 0.0) {
						saveTheBest.add(saveBestValGlobalEntropy.get(i).get(3)); 
						}
					}
				}
			isTheBest = true;
			} else {
				isTheBest = false;
				}
		return isTheBest;
		}
	
	protected int observersNumberInstance(Instance inst, 
			AutoExpandVector<AttributeClassObserver> observerss) {
		int numberInstance = 0;
		for (int z = 0; z < inst.numAttributes() - 1; z++) {
			numberInstance = 0;
			int instAttIndex = modelAttIndexToInstanceAttIndex(z, inst);
			if (inst.attribute(instAttIndex).isNumeric()) {
				Node rootNode = ((BinaryTreeNumericAttributeClassObserver) observerss.get(z)).root;
				if (rootNode != null) {
					numberInstance = (int) (rootNode.classCountsLeft.sumOfValues() + rootNode.classCountsRight.sumOfValues());
					break;
					}
				} else {
					AutoExpandVector<DoubleVector> nominalAttrib = ((NominalAttributeClassObserver) observerss.get(z)).attValDistPerClass;
					for (int d = 0; d < nominalAttrib.size(); d++) {
						if(nominalAttrib.get(d) != null){
							for (int e = 0; e < nominalAttrib.get(d).numValues(); e++) {
								double value = nominalAttrib.get(d).getValue(e);
								numberInstance = numberInstance + (int)value;
								}
							}
						}
					}
			}
		return numberInstance;
		}
	
	protected Integer getCount(Instance inst,  ArrayList<Predicates> PredSet){
		int count = 0;
		for (int y = 0; y < PredSet.size(); y++) {
			int i =(int) (PredSet.get(y).getAttributeValue());
			int instAttIndex = modelAttIndexToInstanceAttIndex(i, inst);
			if (inst.attribute(instAttIndex).isNominal()) {
				if (PredSet.get(y).getSymbol() == 0.0) {
					String val = inst.attribute((int)PredSet.get(y).getAttributeValue()).value((int)PredSet.get(y).getValue());
					if ((val == inst.stringValue(i))
							&& (inst.attribute(i).index() == inst.attribute((int)PredSet.get(y).getAttributeValue()).index())) {
							count = count + 1;
							}
					}
				} else {
					if (PredSet.get(y).getSymbol() == -1.0) {
						if ((inst.value(i) <= PredSet.get(y).getValue()) 
								&& (inst.attribute(i).index() == inst.attribute((int)PredSet.get(y).getAttributeValue()).index())) {
								count = count+1;
								}
						} else if (PredSet.get(y).getSymbol() == 1.0) {
							if((inst.value(i) > PredSet.get(y).getValue()) 
									&& (inst.attribute(i).index() == inst.attribute((int)PredSet.get(y).getAttributeValue()).index())) {
									count = count+1;
									}
							}
					}
			}
		return count;
		}
	
	protected void getRuleClass(Instance inst, int ruleIndex) {
		double classID = inst.classValue();
		int pos = (int)(classID);
		if (majority.get(ruleIndex).isEmpty()) {
			for ( int i = 0; i < inst.numClasses(); i++) {
				majority.get(ruleIndex).add(0);
				}
			}
		majority.get(ruleIndex).set(pos, majority.get(ruleIndex).get(pos) + 1);
		}
	
	protected String getRuleMajorityClass(int ruleIndex, Instance inst) {
		ArrayList<Integer>ruleClassOrdered = new ArrayList<Integer>();
		ArrayList<Integer>ruleClass = new ArrayList<Integer>();
		for (int j = 0; j < majority.get(ruleIndex).size(); j++) {
			ruleClassOrdered.add(majority.get(ruleIndex).get(j));
			ruleClass.add(majority.get(ruleIndex).get(j));
			}
		Collections.sort(ruleClassOrdered);
		int maiorValor = ruleClassOrdered.get(ruleClassOrdered.size()-1);
		int posMaxValor = ruleClass.indexOf(maiorValor);
		String classe = inst.classAttribute().value(posMaxValor);
		return classe;
		}
	
	protected ArrayList<Double> oberversDistrib(Instance inst,
			AutoExpandVector<AttributeClassObserver> observerss) {
		ArrayList<Double> classDistrib=new ArrayList<Double>();
		for (int z = 0; z < inst.numAttributes() - 1; z++) {
			classDistrib = new ArrayList<Double>();
			int instAttIndex = modelAttIndexToInstanceAttIndex(z, inst);
			if (inst.attribute(instAttIndex).isNumeric()) {
				if(observerss.get(z)!=null){
				Node rootNode = ((BinaryTreeNumericAttributeClassObserver) observerss.get(z)).root;
				if (rootNode != null) {
					double sum;
					for (int i = 0; i <rootNode.classCountsRight.numValues(); i++) {
						sum=rootNode.classCountsLeft.getValue(i)+rootNode.classCountsRight.getValue(i);
						classDistrib.add(sum);
						}
					break;
					}
				}
				} else {
					if(observerss.get(z)!=null){
					AutoExpandVector<DoubleVector> atribNominal = ((NominalAttributeClassObserver) observerss.get(z)).attValDistPerClass;
					for (int d = 0; d < atribNominal.size(); d++) {
						double sumValue = 0;
						if (atribNominal.get(d) != null) {
							for (int e = 0; e < atribNominal.get(d).numValues(); e++) {
								double value = atribNominal.get(d).getValue(e);
								sumValue = sumValue + (int)value;
								}
							classDistrib.add(sumValue);
							}
						}
					break;
					}
				}
			}
		return classDistrib;
	}
	

	
	protected double globalEntropy(Instance inst,AutoExpandVector<AttributeClassObserver> observerss) {
		ArrayList<Double> classDistrib = oberversDistrib(inst, observerss);
		double globalEntropy = nominalAttributeEntropy(classDistrib);
		return globalEntropy;
		}
	
	protected double[] oberversDistribProb(Instance inst,
			AutoExpandVector<AttributeClassObserver> observerss) {
		double[] votes = new double[observedClassDistribution.numValues()];
		double sum = 0;
		ArrayList<Double> classDistrib = oberversDistrib(inst, observerss);
		for (int k = 0; k < classDistrib.size(); k++) {
			sum = sum + classDistrib.get(k);
			}
		for (int z = 0; z < classDistrib.size(); z++) {
			votes[z] = classDistrib.get(z) / sum;
			}
		return votes;
		}
	
	// The following three functions are used for the prediction 
	protected double[] firstHit(Instance inst) {
		boolean fired = false;
		int countFired = 0;
		double[] votes = new double[observedClassDistribution.numValues()];
		for (int j = 0; j < ruleSet.size(); j++) {
			if (ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				for (int z = 0; z < majority.get(j).size(); z++) {
					votes[z] = ruleSet.get(j).obserClassDistrib.getValue(z) 
							/ ruleSet.get(j).obserClassDistrib.sumOfValues();
					}
				return votes;
			//	break;
				}
			}
		if (countFired > 0) {
			fired = true;
			} else {
				fired = false;
				}
		if (fired == false) {
			votes = oberversDistribProb(inst, this.attributeObservers);
			}
		return votes;
		}
	
	protected double[] weightedMax(Instance inst) {
		int countFired = 0;
		boolean fired = false;
		double highest = 0.0;
		double[] votes = new double[observedClassDistribution.numValues()];
		ArrayList<Double> ruleSetVotes = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> majorityProb = new ArrayList<ArrayList<Double>>();
		for (int j = 0; j < ruleSet.size(); j++) {
			ArrayList<Double> ruleProb = new ArrayList<Double>();
			if (ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired+1;
				for (int z = 0; z < majority.get(j).size(); z++) {
					ruleSetVotes.add(ruleSet.get(j).obserClassDistrib.getValue(z) / ruleSet.get(j).obserClassDistrib.sumOfValues());
		    	    ruleProb.add(ruleSet.get(j).obserClassDistrib.getValue(z) / ruleSet.get(j).obserClassDistrib.sumOfValues());
		    	    }
				majorityProb.add(ruleProb);
				}
			}
		if (countFired > 0) {
			fired = true;
			Collections.sort(ruleSetVotes); 
		    highest = ruleSetVotes.get(ruleSetVotes.size() - 1);
		    for (int t = 0; t < majorityProb.size(); t++) {
				 for(int m = 0; m < majorityProb.get(t).size(); m++) {
					 if (majorityProb.get(t).get(m) == highest) {
						 for (int h = 0; h < majorityProb.get(t).size(); h++) {
							 votes[h] = majorityProb.get(t).get(h);
							 }
						 break;
						 }
					 }
				 }
		    } else {
		    	fired = false;
		    	}
		if (fired == false) {
			votes = oberversDistribProb(inst, this.attributeObservers);
			}
		return votes;
		}
	
	protected double[] weightedSum(Instance inst) {
		boolean fired = false;
		int countFired = 0;
		double[] votes = new double[observedClassDistribution.numValues()];
		ArrayList<Double> weightSum = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> majorityProb = new ArrayList<ArrayList<Double>>();
		for (int j = 0; j < ruleSet.size(); j++) {
			ArrayList<Double> ruleProb = new ArrayList<Double>();
			if (ruleSet.get(j).ruleEvaluate(inst) == true) {
				countFired = countFired + 1;
				for (int z = 0; z < majority.get(j).size(); z++) {
					ruleProb.add(ruleSet.get(j).obserClassDistrib.getValue(z) / ruleSet.get(j).obserClassDistrib.sumOfValues());
					}
				majorityProb.add(ruleProb);
				}
			}
		if (countFired > 0) {
			fired = true;
			for (int m = 0; m < majorityProb.get(0).size(); m++) {
				double sum = 0.0;
				for (int t = 0; t < majorityProb.size(); t++){
					sum = sum + majorityProb.get(t).get(m);
					}
				weightSum.add(sum);
				}
			for (int h = 0; h < weightSum.size(); h++) {
				votes[h] = weightSum.get(h) / majorityProb.size();
				}
			} else {
				fired = false;
				}
		if (fired == false) {
			votes = oberversDistribProb(inst, this.attributeObservers);
			}
		return votes;
		}
	
	protected AttributeClassObserver newNominalClassObserver() {
		return new NominalAttributeClassObserver();
    }
 
    protected AttributeClassObserver newNumericClassObserver() {
    	return new BinaryTreeNumericAttributeClassObserver();
    }
    protected AttributeClassObserver newNumericClassObserver2() {
    	return new GaussianNumericAttributeClassObserver();
		    }
    
    public void manageMemory(int currentByteSize, int maxByteSize) {
        // TODO Auto-generated method stub
    }
    }
