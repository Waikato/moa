package volatilityevaluation;

public class VolatilityPredictionFeatureExtractor {
	private Buffer movingAverage = new Buffer(500);
	private double count = 0.0;

	private double posCount = 0.0;
	private double negCount = 0.0;
	private double posMag = 0.0;
	private double negMag = 0.0;

	private int blockSize = 30;
	private Buffer buffer = new Buffer(blockSize);
	// private double blockMovingAverage = 0.0;
	private double blockCount = 0.0;

	private Reservoir posnegCount = new Reservoir(500);
	private Reservoir posnegMag = new Reservoir(500);

	public void setInput(double input) {
		movingAverage.add(buffer.add(input));
	}

	public void trainOnInput(double input) {
		posnegMag.addElement(input);
	}

	public void extract(double input) {
		// movingAverage = (input + count * movingAverage) / (count + 1);
		// movingAverage.add(input);
		count++;

		// blockMovingAverage = (input + blockCount * blockMovingAverage) /
		// (blockCount + 1);
		// blockCount++;

		movingAverage.add(buffer.add(input));

		if (count % blockSize == 0) {
			double x = buffer.getMean() - movingAverage.getMean();

			if (x >= 0.0) {
				posnegCount.addElement(1);
			} else {
				posnegCount.addElement(0);
			}
			posnegMag.addElement(x);

			/*
			 * double removed = 0.0; if(x >= 0.0) { posMag = (x + posCount *
			 * posMag) / (posCount + 1); posCount++; removed =
			 * posnegCount.add(1); } else { negMag = (x + negCount * negMag) /
			 * (negCount + 1); negCount++; removed = posnegCount.add(0); }
			 */
			// blockCount = 0.0;
		}
	}

	public double getConfidencePrediction() {
		double x = buffer.getMean() - movingAverage.getMean();

		double z = Math.abs((x - posnegMag.getReservoirMean()) / posnegMag.getReservoirStdev());

		if (z < 1.65) {
			return 0.5;
		}
		// else if(z >= 1.15 && z <= 1.65)
		// {
		// return (0.5 + z - 1.15);
		// }
		else {
			return 1.0;
		}
	}

	public String getModel() {
		return posnegMag.getReservoirMean() + "," + posnegMag.getReservoirStdev();
	}

	public Reservoir getReservoir() {
		return posnegMag;
	}

	public String getStats() {

		// double ratio = Math.abs((posCount - negCount) / 2);
		// double mag = Math.abs(posMag + negMag);

		double ratio = posnegCount.getReservoirMean();
		double mag = posnegMag.getReservoirMean();

		// blockCount = 0.0;
		// blockMovingAverage = 0.0;

		return ratio + "," + mag + "\n";
		// return posCount + "," + negCount + "," + posMag + "," + negMag +
		// "\n";

	}

	public void reset() {
		movingAverage.clear();
		buffer.clear();
	}

	public void resetTraining() {
		posnegCount.clear();
		posnegMag.clear();
		count = 0.0;
	}

}
