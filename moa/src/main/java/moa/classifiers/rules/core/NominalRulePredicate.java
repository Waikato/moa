/*
 *    NominalRulePredicate.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
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

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.AbstractMOAObject;
import moa.core.StringUtils;

public class NominalRulePredicate extends AbstractMOAObject implements Predicate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int inputAttributeIndex;
	private double attributeValue;
	private boolean isEqual; 

	public NominalRulePredicate(int inputAttributeIndex, double attributeValue, boolean isEqual) {
		this.inputAttributeIndex=inputAttributeIndex;
		this.attributeValue=attributeValue;
		this.isEqual=isEqual;
	}
	@Override
	public boolean evaluate(Instance instance){
		return evaluate((StructuredInstance) instance);
	}

	public boolean evaluate(StructuredInstance instance) {
		if (instance.isMissing(inputAttributeIndex)) {
			return false;
		}
		boolean evaluation=false;	
		if (instance.valueInputAttribute(inputAttributeIndex)==attributeValue) {
			evaluation=true;
		}
		return (isEqual ? evaluation : (!evaluation));


	}

	public double getAttributeValue() {
		return attributeValue;
	}


	@Override
	public void negateCondition() {
		isEqual=!isEqual;

	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		if(isEqual)
			StringUtils.appendIndented(sb, indent+1, "In" + inputAttributeIndex + " == " + attributeValue);
		else
			StringUtils.appendIndented(sb, indent+1, "In" + inputAttributeIndex + " <> " + attributeValue);
	}

	@Override
	public void getDescription(StringBuilder sb, int indent, InstancesHeader instHeader) {
		if (instHeader != null) {
			if(isEqual)
				StringUtils.appendIndented(sb, indent+1, instHeader.inputAttribute(inputAttributeIndex).name() + " == " + instHeader.inputAttribute(inputAttributeIndex).value((int)attributeValue));
			else
				StringUtils.appendIndented(sb, indent+1, instHeader.inputAttribute(inputAttributeIndex).name() + " <> " + instHeader.inputAttribute(inputAttributeIndex).value((int)attributeValue));
		}
		else 
			getDescription(sb,indent);
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		getDescription(sb,0);
		return sb.toString();
	}

	@Override
	public int getAttributeIndex() {
		return inputAttributeIndex;
	}

	@Override
	public boolean isEqualOrLess() {
		return isEqual;
	}

}
