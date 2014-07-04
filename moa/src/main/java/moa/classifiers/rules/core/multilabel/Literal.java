package moa.classifiers.rules.core.multilabel;

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
		
	}


}
