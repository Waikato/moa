package moa.classifiers.rules.core;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

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
		return evaluate((MultiLabelInstance) instance);
	}

	public boolean evaluate(MultiLabelInstance instance) {
		if (instance.isMissing(inputAttributeIndex)) {
			return false;
		}
		boolean evaluation=false;	
		if (instance.valueInputAttribute(inputAttributeIndex)==attributeValue) {
			evaluation=true;
		}
		return (isEqual ? evaluation : (!evaluation));


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
