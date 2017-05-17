package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import volatilityevaluation.RelativeVolatilityDetector;
import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class VolatilityCutPointTester implements Tester {
	// int numDriftInstances = 1000;
	int numDriftInstances = 500000;
	int iterations = 30;

	private final int LINEAR_DECAY = 1;
	private final int EXPONENTIAL_DECAY = 2;
	private final int FIXED_TERM = 1;
	private final int PARETO = 2;

	private int DECAY_MODE = LINEAR_DECAY;
	private int COMPRESSION_MODE = FIXED_TERM;

	@Override
	public void doTest() {
		try {
			double[] epsilonPrimes = { 0.01 };
			double[] linearAlphas = { 0.8 };
			double[] expAlphas = { 0.01 };
			int[] fixedCompressionTerms = { 75 };
			int[] paretoCompressionTerms = { 200, 400, 600, 800 };

			double[] alphas = null;
			int[] compTerms = null;

			if (DECAY_MODE == LINEAR_DECAY) {
				alphas = linearAlphas;
			} else if (DECAY_MODE == EXPONENTIAL_DECAY) {
				alphas = expAlphas;
			}

			if (COMPRESSION_MODE == FIXED_TERM) {
				compTerms = fixedCompressionTerms;
			} else if (COMPRESSION_MODE == PARETO) {
				compTerms = paretoCompressionTerms;
			}

			int[] blocksizes = { 32, 64, 128, 256 };
			String[] levels = { "", "1", "2", "3", "4", "5", "6", "7" };
			int[] driftpoint = { 0, 100000, 50000, 10000, 5000, 1000, 500, 100 };

			BufferedWriter bWriter = new BufferedWriter(new FileWriter(
					"src\\testers\\Volatility&ChangeDetection\\Results\\VolatilityTest\\ADWIN\\ADWIN_VStreamCutpointSurvey.csv"));
			bWriter.write("Level,Number of Drifts, Stdev of Drifts");
			bWriter.newLine();
			for (int j = 1; j < levels.length; j++) {
				int level = j;
				int trueDriftPoint = driftpoint[level] * 100;

				double delta = 0.05;

				int blockSize = 32;

				for (int e = 0; e < compTerms.length; e++) {
					for (int w = 0; w < alphas.length; w++) {
						for (int q = 0; q < epsilonPrimes.length; q++) {

							int[] delays = new int[iterations];
							int totalDrift = 1;

							for (int k = 0; k < iterations; k++) {
								ADWIN adwin = new ADWIN(delta);
								SingDetector sing = new SingDetector(delta, blockSize, DECAY_MODE, COMPRESSION_MODE,
										epsilonPrimes[q], alphas[w], compTerms[e]);
								
								RelativeVolatilityDetector rv = new RelativeVolatilityDetector(adwin, 32, 0.15);

								BufferedReader br = new BufferedReader(new FileReader(
										"src\\testers\\Volatility&ChangeDetection\\VolatilityStreamDriftPoint\\" + level
												+ "\\VolatilityStream_" + level + "_" + k + ".csv"));
								String line = "";

								int c = 0;
								int driftcount = 0;

								while ((line = br.readLine()) != null) {
									if (adwin.setInput(Double.parseDouble(line))) {
										driftcount++;
									}
								}
								totalDrift += driftcount;
								delays[k] = driftcount;
								br.close();
							}

							bWriter.write(level + "," + (double) totalDrift / (double) iterations + ","
									+ calculateStdev(delays, (double) totalDrift / (double) iterations) + "\n");

						}
					}
				}
			}
			bWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public double calculateStdev(int[] times, double mean) {
		double sum = 0;
		int count = 0;
		for (int i : times) {
			if (i > 0) {
				count++;
				sum += Math.pow(i - mean, 2);
			}
		}
		return Math.sqrt(sum / count);
	}

	public double calculateStdevLong(long[] times, double mean) {
		double sum = 0;
		int count = 0;
		for (Long i : times) {
			if (i > 0) {
				count++;
				sum += Math.pow(i - mean, 2);
			}
		}
		return Math.sqrt(sum / count);
	}

	public double calculateSum(int[] delays) {
		double sum = 0.0;
		for (double d : delays) {
			sum += d;
		}

		return sum;
	}

	public void generateSlopedInput(double driftProb, double slope, int numInstances, int numDriftInstances,
			int randomSeed) {
		try {

			BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\TPData.txt"));

			double[] driftMean = new double[1];
			driftMean[0] = driftProb;
			// System.out.println(driftMean[0]);
			BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(driftMean,
					numInstances - numDriftInstances, randomSeed);
			while (gen.hasNextTransaction()) {
				bWriter.write(gen.getNextTransaction() + "\n");
			}

			BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(driftMean, numDriftInstances,
					randomSeed);
			while (genDrift.hasNextTransaction()) {
				driftMean[0] += slope;
				if (driftMean[0] >= 1.0) {
					driftMean[0] = 1.0;
				}
				// System.out.println(driftMean[0]);
				genDrift.setMeans(driftMean);
				bWriter.write(genDrift.getNextTransaction() + "\n");
			}

			bWriter.close();

		} catch (Exception e) {
			System.err.println("error");
		}
	}

	/*
	 * public void generateInput(double[] driftProb, double driftIncrement, int
	 * numInstances, int numDriftInstances, int randomSeed) { try {
	 * 
	 * BufferedWriter bWriter = new BufferedWriter(new
	 * FileWriter("src\\testers\\TPData.txt"));
	 * 
	 * BernoulliDistributionGenerator gen = new
	 * BernoulliDistributionGenerator(driftProb, numInstances -
	 * numDriftInstances, randomSeed); while(gen.hasNextTransaction()) {
	 * bWriter.write(gen.getNextTransaction() + "\n"); }
	 * 
	 * driftProb[0] += driftIncrement;
	 * 
	 * BernoulliDistributionGenerator genDrift = new
	 * BernoulliDistributionGenerator(driftProb, numDriftInstances, randomSeed);
	 * while(genDrift.hasNextTransaction()) {
	 * bWriter.write(genDrift.getNextTransaction() + "\n"); }
	 * 
	 * driftProb[0] -= driftIncrement;
	 * 
	 * bWriter.close();
	 * 
	 * } catch(Exception e) { System.err.println("error"); } }
	 */
}
