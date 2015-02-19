package moa.classifiers.rules.featureranking.messages;


public class BasicRemovedMessage extends BasicLiteralMessage {

	public BasicRemovedMessage(int attributeIndex, double weight) {
		super(attributeIndex, weight);
	}
	
	public BasicRemovedMessage(int attributeIndex) {
		super(attributeIndex);
	}

}
