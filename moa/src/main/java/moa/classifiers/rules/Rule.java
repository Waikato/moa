/*
 *    Rule.java
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

/**
 * Class that stores an arrayList  of predicates of a rule and the observers (statistics).
 * This class implements a function that evaluates a rule.
 *
 * <p>Learning Decision Rules from Data Streams, IJCAI 2011, J. Gama,  P. Kosina </p>
 *
 * @author P. Kosina, E. Almeida, J. Gama
 * @version $Revision: 2 $
 * 
 * 
 */

import java.util.ArrayList;
import moa.AbstractMOAObject;
import moa.classifiers.core.attributeclassobservers.*;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import weka.core.Instance;
//import samoa.instances.Instance;
//import moa.core.Utils;

public class Rule extends AbstractMOAObject{
	
	private static final long serialVersionUID = 1L;
	
    protected  ArrayList<Predicates> predicateSet = new ArrayList<Predicates>();
    
	protected AutoExpandVector<AttributeClassObserver> observers = new AutoExpandVector<AttributeClassObserver>(); //Statistics.
	
	protected AutoExpandVector<AttributeClassObserver> observersGauss = new AutoExpandVector<AttributeClassObserver>(); //Statistics.
	
    protected ArrayList<ArrayList<Double>> attributeStatisticsSupervised = new ArrayList<ArrayList<Double>>();
	
	protected ArrayList<ArrayList<Double>> squaredAttributeStatisticsSupervised = new ArrayList<ArrayList<Double>>();
	
	protected double[] weightAttribute; // The Perception weights. 
    
    protected DoubleVector attributeStatistics = new DoubleVector(); // Statistics used for error calculations.
    
    protected DoubleVector attributesProbability = new DoubleVector(); // Probalility of each attribute.
    
    protected DoubleVector squaredAttributeStatistics = new DoubleVector();
    
    protected DoubleVector obserClassDistrib = new DoubleVector();
    
    protected DoubleVector attributeMissingValues = new DoubleVector(); // for each attribute counts the number of missing values.
    
    protected int instancesSeen = 0;  // The number of instances contributing to this model.
    
    protected int instancesSeenTest = 0;  // The number of instances test seen by the rule.
    
    protected boolean reset=true; // If the model should be reset or not.
    
 // Statistics used for normalize actualClass and predictedClass.
    protected double actualClassStatistics = 0.0;
    
    protected double squaredActualClassStatistics = 0.0;
    
    protected double PHmT = 0; //The cumulative sum of the errors.
    
    protected double PHMT = Double.MAX_VALUE; // The minimum error value seen so far.
    
    protected double XiSum = 0;  //Absolute error.
    
    protected double ValorTargetRule=0; // Target value of the rule.
	
	
	public Rule(Rule x) {
		for (int i = 0; i < x.predicateSet.size(); i++) {
			Predicates pred = new Predicates(x.predicateSet.get(i).getAttributeValue(), x.predicateSet.get(i).getSymbol(), x.predicateSet.get(i).getValue());
			this.predicateSet.add(pred);
		}
		
	}
	
	public Rule() {
		
	}
    
    public boolean ruleEvaluate(Instance inst) {
    	int countTrue = 0;
		boolean ruleEvalu = false;
		for (int i = 0; i < predicateSet.size(); i++) {
			if (predicateSet.get(i).evaluate(inst) == true) {
				countTrue = countTrue + 1;
			}
		}
		if (countTrue == predicateSet.size()) {
			ruleEvalu = true;
		} else {
			ruleEvalu = false;
			}
		return ruleEvalu;
		}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub	
	}
         

}

