/*
 *    NumericAttributeBinaryRulePredicate.java
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
package moa.classifiers.rules.core.conditionaltests;

import moa.classifiers.core.conditionaltests.InstanceConditionalBinaryTest;
import moa.classifiers.rules.core.Predicate;
import moa.core.StringUtils;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceInformation;
import com.yahoo.labs.samoa.instances.InstancesHeader;



/**
 * Numeric binary conditional test for instances to use to split nodes in
 * AMRules.
 *
 * @version $Revision: 1 $
 */
public class NumericAttributeBinaryRulePredicate extends InstanceConditionalBinaryTest implements Predicate {

	private static final long serialVersionUID = 1L;

	protected int attIndex;

	protected double attValue;

	protected int operator; // 0 =, 1<=, 2>

	protected boolean state=true;

	public NumericAttributeBinaryRulePredicate(int attIndex, double attValue,
			int operator) {
		this.attIndex = attIndex;
		this.attValue = attValue;
		this.operator = operator;
	}

	@Override
	public void negateCondition() {
		state=!state;

	}

	@Override
	public int branchForInstance(Instance inst) {
		int instAttIndex = this.attIndex < inst.classIndex() ? this.attIndex
				: this.attIndex + 1;
		if (inst.isMissing(instAttIndex)) {
			return -1;
		}
		double v = inst.value(instAttIndex);
		int ret = 0;
		switch (this.operator) {
		case 0:
			ret = (v == this.attValue) ? 0 : 1;
			break;
		case 1:
			ret = (v <= this.attValue) ? 0 : 1;
			break;
		case 2:
			ret = (v > this.attValue) ? 0 : 1;
		}
		return ret;
	}

	/**
	 *
	 */
	 @Override
	 public String describeConditionForBranch(int branch, InstancesHeader context) {
		if ((branch >= 0) && (branch <= 2)) {
			String compareChar = (branch == 0) ? "=" : (branch == 1) ? "<=" : ">";
			return InstancesHeader.getAttributeNameString(context,
					this.attIndex)
					+ ' '
					+ compareChar
					+ InstancesHeader.getNumericValueString(context,
							this.attIndex, this.attValue);
		}
		throw new IndexOutOfBoundsException();
	 }


	 @Override
	 public int[] getAttsTestDependsOn() {
		 return new int[]{this.attIndex};
	 }

	 public double getSplitValue() {
		 return this.attValue;
	 }

	 @Override
	 public boolean evaluate(Instance inst) {
		 if(state)
			 return (branchForInstance(inst) == 0) ;
		 else
			 return (branchForInstance(inst) != 0);
	 }

	 public boolean isEqual(NumericAttributeBinaryRulePredicate predicate) {
		 return (this.attIndex == predicate.attIndex
				 && this.attValue == predicate.attValue
				 && this.operator == predicate.operator);
	 }

	 public boolean isUsingSameAttribute(NumericAttributeBinaryRulePredicate predicate) {
		 return (this.attIndex == predicate.attIndex
				 && this.operator == predicate.operator);
	 }

	 public boolean isIncludedInRuleNode(
			 NumericAttributeBinaryRulePredicate predicate) {
		 boolean ret;
		 if (this.operator == 1) { // <=
				 ret = (predicate.attValue <= this.attValue);
		 } else { // >
			 ret = (predicate.attValue > this.attValue);
		 }

		 return ret;
	 }

	 public void setAttributeValue(
			 NumericAttributeBinaryRulePredicate ruleSplitNodeTest) {
		 this.attValue = ruleSplitNodeTest.attValue;

	 }


	 @Override
	 public void getDescription(StringBuilder sb, int indent) {   
		 String compareChar = (operator == 0) ? "=" : (operator == 1) ? "<=" : ">";
		 StringUtils.appendIndented(sb, indent+1, "In" + attIndex + compareChar + attValue);
	 }

	 @Override
	 public void getDescription(StringBuilder sb, int indent,
			 InstanceInformation instInformation) {
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
		 return attIndex;
	 }

	 @Override
	 public boolean isEqualOrLess() {
		 return state;
	 }

}
