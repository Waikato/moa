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
 * @version $Revision: 1 $
 * 
 * 
 */

import java.util.ArrayList;

import moa.AbstractMOAObject;
import moa.classifiers.core.attributeclassobservers.*;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import weka.core.Instance;

public class Rule extends AbstractMOAObject{
	
	private static final long serialVersionUID = 1L;
	
    protected  ArrayList<Predicates> predicateSet = new ArrayList<Predicates>();
    
	protected AutoExpandVector<AttributeClassObserver> observers = new AutoExpandVector<AttributeClassObserver>(); //statistics
	
	protected AutoExpandVector<AttributeClassObserver> observersGauss = new AutoExpandVector<AttributeClassObserver>(); //statistics
	
	protected DoubleVector obserClassDistrib = new DoubleVector();
	
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

