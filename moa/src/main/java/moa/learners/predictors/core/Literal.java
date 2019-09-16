package moa.learners.predictors.core;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.AbstractMOAObject;
import moa.learners.predictors.rules.core.Predicate;

public class Literal extends AbstractMOAObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	protected Predicate predicate;

	public Literal(Predicate predicate) {
		this.predicate = predicate;
	}

	public int getAttributeIndex() {
		return this.predicate.getAttributeIndex();
	}

	public boolean evaluate(Instance inst) {
		return predicate.evaluate(inst);
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		predicate.getDescription(sb, indent, null);
	}

	public void getDescription(StringBuilder sb, int indent, InstancesHeader instanceInformation) {
		predicate.getDescription(sb, indent, instanceInformation);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		predicate.getDescription(sb, 1, null);
		return sb.toString();
	}

}
