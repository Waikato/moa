package moa.classifiers.rules.core;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.StructuredInstance;

import moa.AbstractMOAObject;
import moa.core.StringUtils;

public class NumericRulePredicate extends AbstractMOAObject implements Predicate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int inputAttributeIndex;
	private double attributeValue;
	private boolean isEqualOrLower; 

	public NumericRulePredicate(int inputAttributeIndex, double attributeValue, boolean isEqualOrLower) {
		this.inputAttributeIndex=inputAttributeIndex;
		this.attributeValue=attributeValue;
		this.isEqualOrLower=isEqualOrLower;
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
		if (instance.valueInputAttribute(inputAttributeIndex)<=attributeValue) {
			evaluation=true;
		}
		return (isEqualOrLower ? evaluation : (!evaluation));


	}

	@Override
	public void negateCondition() {
		isEqualOrLower=!isEqualOrLower;
		
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		if(isEqualOrLower)
			StringUtils.appendIndented(sb, indent+1, inputAttributeIndex + " <= " + attributeValue);
		else
			StringUtils.appendIndented(sb, indent+1, inputAttributeIndex + " > " + attributeValue);
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
		return isEqualOrLower;
	}

	public double getAttributeValue() {
		return attributeValue;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent, InstancesHeader header) {
		if(isEqualOrLower)
			StringUtils.appendIndented(sb, indent, InstancesHeader.getAttributeNameString(header, inputAttributeIndex) + " <= " + attributeValue);
		else
			StringUtils.appendIndented(sb, indent, InstancesHeader.getAttributeNameString(header, inputAttributeIndex) + " > " + attributeValue);	}

}
