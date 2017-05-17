package moa.classifiers.rules.featureranking.messages;

import moa.core.DoubleVector;

public class MeritCheckMessage implements FeatureRankingMessage {
	protected DoubleVector merits;
	boolean [] learningAttributes;

	public MeritCheckMessage(DoubleVector merits) {
		this.merits = merits;
	}
	
	public MeritCheckMessage(DoubleVector merits, boolean [] learningAttributes) {
		this.merits = merits;
		this.learningAttributes=learningAttributes;
	}
	
	public DoubleVector getMerits() {
		return merits;
	}
	
	
	public boolean [] getLearningAttributes() {
		return learningAttributes;
	}

}
