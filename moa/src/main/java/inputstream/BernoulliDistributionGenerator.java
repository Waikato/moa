package inputstream;

import java.util.Random;

public class BernoulliDistributionGenerator implements InputStreamInterface {

	private double[] driftPtProb;
	private int instancesPerDrift;
	private int tCount;

	private Random RNG;

	public BernoulliDistributionGenerator(double[] means, int instancesPerDrift, int randomSeed) {
		this.driftPtProb = means;
		this.instancesPerDrift = instancesPerDrift;
		this.tCount = 0;

		this.RNG = new Random(randomSeed);
		// this.RNG = new Random();
	}

	@Override
	public boolean hasNextTransaction() {
		return tCount < (instancesPerDrift * driftPtProb.length);
	}

	@Override
	public String getNextTransaction() {
		int value = 0;

		int driftPt = tCount / instancesPerDrift;

		if (RNG.nextDouble() < driftPtProb[driftPt]) {
			value = 1;
		}

		tCount++;
		return value + "";
	}

	public int getCurrentTID() {
		return tCount;
	}

	public void setMeans(double[] means) {
		this.driftPtProb = means;
	}

	public double[] getMean() {
		return this.driftPtProb;
	}

}
