package moa.classifiers.rules.featureranking.messages;

public class WeightedMajorityAddMessage extends WeightedMajorityMessage {

	int numberOfLiterals;
	public WeightedMajorityAddMessage(double[] merits, int numberOfLiterals ) {
		super(merits);
		this.numberOfLiterals=numberOfLiterals;
	}

	public int getNumberOfLiterals() {
		return numberOfLiterals;
	}

}
