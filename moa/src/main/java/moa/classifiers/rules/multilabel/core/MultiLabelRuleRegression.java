package moa.classifiers.rules.multilabel.core;

public class MultiLabelRuleRegression extends MultiLabelRule {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MultiLabelRuleRegression(LearningLiteralRegression learningLiteral) {
		super(learningLiteral);
	}

	public MultiLabelRuleRegression() {
		super();
		learningLiteral=new LearningLiteralRegression();
		
		
	}

	public MultiLabelRuleRegression(int id) {
		this();
		ruleNumberID=id;
	}

}
