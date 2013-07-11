/*
 *    Predicates.java
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
 * Class that creates and evaluates the predicates
 * 
 * <p>Learning Decision Rules from Data Streams, IJCAI 2011, J. Gama,  P. Kosina </p>
 *
 * @author P. Kosina, E. Almeida, J. Gama
 * @version $Revision: 2 $
 * 
 */

import moa.AbstractMOAObject;
import weka.core.Instance;
//import samoa.instances.Instance;

public class Predicates extends AbstractMOAObject{
	
	private static final long serialVersionUID = 1L;
	
	private double attributeValue;
	
	private double symbol;
	
	private double value;
		
	public Predicates(double attribVal, double symb,double val){
	this.attributeValue = attribVal;
	this.symbol = symb;
	this.value = val;
}
	
	public double getAttributeValue() {
		return this.attributeValue;
	}
	
	public double getSymbol() {
		return this.symbol;
	}
	
	public double getValue() {
		return this.value;
	}
	
	public void setAttributeValue(double attributeValue) {
		this.attributeValue = attributeValue;
	}
	
	public void setSymbol(double symbol) {
		this.symbol = symbol;
	}
	
	public void setValue(double value) {
		this.value = value;
	}

	 public boolean evaluate(Instance inst) {
	    boolean result = false;
	    double attributeValue = inst.value((int) this.attributeValue);
	    if (this.symbol == 0.0 && attributeValue == this.value) {
	        result = true;
	    } else if (this.symbol == -1.0 && attributeValue <= this.value) {
	        result = true;
	    } else if (this.symbol == 1.0 && attributeValue > this.value) {
	        result = true;
	    }
	    return result;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
		
	}
         

}
