package moa.classifiers.rules.featureranking.messages;

public class WeightedMajorityCleanMessage implements FeatureRankingMessage {
	protected int attribute;
	protected double value;
	

	public WeightedMajorityCleanMessage(int attribute, double value) {
		super();
		this.attribute = attribute;
		this.value = value;
	}
	
	public int getAttribute() {
		return attribute;
	}


	public double getValue() {
		return value;
	}


}
