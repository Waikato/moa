package inputstream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;

public class MixedDriftBernoulliGenerator {
	public void generateInput(double startingMean, int numDrifts, int randomSeed, String fileDir, int length) {
		try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(fileDir, true));

			double sign = Math.random() > 0.5 ? 1.0 : -1.0;
			double currentMean = startingMean;

			for (int i = 0; i < numDrifts; i++) {

				int timeFrame = (int) (Math.random() * 1000) + 1;
				timeFrame = length;
				double magnitude = Math.random();
				magnitude = 0.6;
				boolean boundTest = false;

				while (!boundTest) {
					magnitude = Math.random();
					magnitude = 0.6;

					double condition = currentMean + (sign * magnitude);

					if (condition <= 1.0 && condition >= 0.0) {
						boundTest = true;
						break;
					} else {
						sign *= -1.0;
					}

					condition = currentMean + (sign * magnitude);

					if (condition <= 1.0 && condition >= 0.0) {
						boundTest = true;
						break;
					}
				}
				// double temp = (double) timeFrame / 1000;
				// System.out.println(i + "," + currentMean + "," +
				// sign*magnitude + "," + timeFrame);
				// System.out.println(temp);
				// System.out.println(sign * magnitude / temp);
				
				System.out.println(magnitude + " " + currentMean + " " + timeFrame);
				currentMean = generateSingleDrift(bWriter, currentMean, sign * (magnitude / (double) timeFrame),
						timeFrame, randomSeed);

			}

			bWriter.close();

		} catch (Exception e) {
			System.err.println("error");
		}
	}

	public double generateSingleDrift(BufferedWriter bWriter, double mean, double slope, int numInstances,
			int randomSeed) {
		double[] driftMean = new double[1];
		try {
			driftMean[0] = mean;

			Random RNG = new Random(1);

			BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(driftMean, numInstances,
					randomSeed);
			while (gen.hasNextTransaction()) {
				double rand = RNG.nextGaussian();

				driftMean[0] += (slope + (10 * slope * rand)); // Gaussian
																// Perturbation
				// driftMean[0] += slope;
				// System.out.println(driftMean[0]);

				gen.setMeans(driftMean);
				bWriter.write(gen.getNextTransaction() + "\n");
			}

			driftMean = gen.getMean();
		} catch (Exception e) {
			System.err.println("error");
		}

		return driftMean[0];

	}
}
