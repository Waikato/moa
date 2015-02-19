package moa.classifiers.rules.featureranking.messages;



public class WeightedMajorityMessage implements FeatureRankingMessage{
	protected double [] values;

	public WeightedMajorityMessage(double [] values) {
		this.values=values; //copy?
	}
	
	public double [] getValues() {
		return values;
	}
}
