package moa.classifiers.rules.multilabel.core;


public class MultiLabelRuleClassification extends MultiLabelRule {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public MultiLabelRuleClassification(LearningLiteralClassification learningLiteral) {
		super(learningLiteral);
	}

	public MultiLabelRuleClassification() {
		super();
		learningLiteral=new LearningLiteralClassification();
		
		
	}

	public MultiLabelRuleClassification(int id) {
		this();
		ruleNumberID=id;
	}

}
