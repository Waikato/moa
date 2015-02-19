package moa.classifiers.rules.featureranking.messages;


public class BasicAddedMessage extends BasicLiteralMessage {
	public BasicAddedMessage(int attributeIndex, double weight) {
		super(attributeIndex, weight);
	}
	
	public BasicAddedMessage(int attributeIndex) {
		super(attributeIndex);
	}
}
