package moa.classifiers.rules.multilabel.core;

import com.yahoo.labs.samoa.instances.Instance;

import moa.AbstractMOAObject;
import moa.classifiers.rules.core.Predicate;

public class Literal extends AbstractMOAObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected Predicate predicate;
	
	public Literal(Predicate predicate){
		this.predicate=predicate;
	}

	public boolean evaluate(Instance inst) {
		return predicate.evaluate(inst);
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		predicate.getDescription(sb, indent);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		predicate.getDescription(sb, 1);
		return sb.toString();
	}


}
