package moa.classifiers.rules.core.multilabel;

import java.util.LinkedList;
import java.util.List;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;


public class MultiLabelRule extends AbstractMOAObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected List<Literal> literalList = new LinkedList<Literal>();

	protected LearningLiteral learningLiteral;
	protected boolean [] outputsCoveredMask;
	

	protected int ruleNumberID;
	
	public int getRuleNumberID() {
		return ruleNumberID;
	}

	public void setRuleNumberID(int ruleNumberID) {
		this.ruleNumberID = ruleNumberID;
	}
	
	public boolean isCovering(MultiLabelInstance inst) {
		boolean isCovering = true;
		for (Literal l : literalList) {
			if (l.evaluate(inst) == false) {
				isCovering = false;
				break;
			}
		}
		return isCovering;
	}
	
	public boolean[] getOutputsCoveredMask(MultiLabelInstance inst) {
		return outputsCoveredMask;
	}
	
	
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	public boolean updateChangeDetection(MultiLabelInstance instance) {
		return this.learningLiteral.updateChangeDetection(instance);
	}

	public boolean updateAnomalyDetection(MultiLabelInstance instance) {
		return this.learningLiteral.updateAnomalyDetection(instance);
	}

	public void trainOnInstance(MultiLabelInstance instance) {
		learningLiteral.trainOnInstance(instance);
	}

	public double getWeightSeenSinceExpansion() {
		return learningLiteral.getWeightSeenSinceExpansion();
	}

	public LearningLiteral getLearningNode() {
		return learningLiteral;
	}
	
	@Override
	public String toString()
	{
		StringBuilder out = new StringBuilder();
		//TODO: complete rule description
		
		/*int indent = 1;
		StringUtils.appendIndented(out, indent, "Rule Nr." + this.ruleNumberID + " Instances seen:" + this.learningLiteral.getWeightSeenSinceExpansion() + "\n"); // AC
		for (Literal literal : literalList) {
			StringUtils.appendIndented(out, indent, literal.getSplitTest().toString());
			StringUtils.appendIndented(out, indent, " ");
			StringUtils.appendIndented(out, indent, literal.toString());
		}
		DoubleVector pred = new DoubleVector(this.learningLiteral.getSimplePrediction());
		StringUtils.appendIndented(out, 0, " --> y: " + pred.toString());
		StringUtils.appendNewline(out);

		if (this.learningNode instanceof RuleActiveRegressionNode) {
			if(((RuleActiveRegressionNode)this.learningLiteral).perceptron!=null){
				((RuleActiveRegressionNode)this.learningLiteral).perceptron.getModelDescription(out,0 );
				StringUtils.appendNewline(out);
			}
		}*/
		return(out.toString());
	}

	public double [] getCurrentErrors() {
		return null;//learner.getCurrentError();
	}

	public  Prediction getPredictionForInstance(MultiLabelInstance instance) {
		return learningLiteral.getPredictionForInstance(instance);
	}

}
