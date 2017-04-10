package moa.classifiers.rules.featureranking.messages;


public class RuleExpandedMessage implements FeatureRankingMessage {
	private int attributeIndex;
	private boolean isSpecialization;

	public int getAttributeIndex() {
		return attributeIndex;
	}

	public RuleExpandedMessage(int attributeIndex) {
		this.attributeIndex = attributeIndex;
		setSpecialization(false);
	}
	
	public RuleExpandedMessage(int attributeIndex, boolean isSpecialization) {
		this.attributeIndex = attributeIndex;
		this.setSpecialization(isSpecialization);
	}

	public boolean isSpecialization() {
		return isSpecialization;
	}

	protected void setSpecialization(boolean isSpecialization) {
		this.isSpecialization = isSpecialization;
	}


}
