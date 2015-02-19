package moa.classifiers.rules.featureranking.messages;

abstract public class BasicLiteralMessage  implements FeatureRankingMessage{
	private int attributeIndex;
	private double weight=0;

	public BasicLiteralMessage(int attributeIndex) {
		this(attributeIndex,1.0);
	}
	
	public BasicLiteralMessage(int attributeIndex, double weight) {
		this.attributeIndex=attributeIndex;
		this.weight=weight;
	}

	public int getAttributeIndex() {
		return attributeIndex;
	}
	
	public double getWeight() {
		return weight;
	}
}
