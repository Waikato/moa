/*
 *    Predicates.java
 *    Copyright (C) 2012 University of Porto, Portugal
 *    @author P. Kosina, E. Almeida, J. Gama
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

import com.yahoo.labs.samoa.instances.Instance;

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
