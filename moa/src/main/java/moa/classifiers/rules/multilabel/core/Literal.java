package moa.classifiers.rules.multilabel.core;

import moa.AbstractMOAObject;
import moa.classifiers.rules.core.Predicate;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceInformation;

public class Literal extends AbstractMOAObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected Predicate predicate;
	
	public Literal(Predicate predicate){
		this.predicate=predicate;
	}
	
	
	public int getAttributeIndex(){
		return this.predicate.getAttributeIndex();
	}
	

	public boolean evaluate(Instance inst) {
		return predicate.evaluate(inst);
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		predicate.getDescription(sb, indent, null);
	}
	
	public void getDescription(StringBuilder sb, int indent, InstanceInformation instanceInformation) {
		predicate.getDescription(sb, indent, instanceInformation);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		predicate.getDescription(sb, 1, null);
		return sb.toString();
	}


}
