package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import volatilityevaluation.VolatilityPredictionFeatureExtractor;

import cutpointdetection.ADWIN;

public class VDriftRealWorld implements Tester {
	private final int NOCORRECTION = 0;
	private final int SINE_FUNC = 1;
	private final int SIGMOID_FUNC = 2;

	double tension = 0.5;

	@Override
	public void doTest() {
		try {

			// BufferedWriter bWriter = new BufferedWriter(new
			// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\online_powersupply.csv"));

			ADWIN adwin = new ADWIN(0.05);

			BufferedReader br = new BufferedReader(
					new FileReader("D:\\Kauri BackUp Results\\Categorization\\Streams\\powersupply.csv"));
			String line = "";

			int c = 0, x = 0;
			double volatility = 0.00;
			double vCount = 0;

			double relPos = 0.0;
			adwin.setTension(tension);
			adwin.setMode(SINE_FUNC);

			VolatilityPredictionFeatureExtractor extractor = new VolatilityPredictionFeatureExtractor();

			ArrayList<Double> list = new ArrayList<Double>();

			// double[] powersupply = new
			// double[]{767,991,5151,5759,6111,6335,8063,8639,10751,13983,15199,15615,18079,19423,22815,23135,23391,24991,25663,28351,28543,29887,29928};
			// for(double d : powersupply){list.add(d);}

			double prevdrift = 0;
			// double drift = list.remove(0);
			// double drift = 767;

			for (int k = 0; k < 10; k++) {
				BufferedReader br2 = new BufferedReader(new FileReader(
						"src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_0.0001\\100000_0.0001_" + k
								+ ".csv"));

				int c2 = 0;
				String line2 = "";
				while ((line2 = br2.readLine()) != null) {
					extractor.extract(Double.parseDouble(line2));
					c2++;
					if (c2 > 100000) {
						break;
					}
				}
				br2.close();
			}

			while ((line = br.readLine()) != null) {
				/*
				 * if(c == drift) { prevdrift = drift; drift = c + volatility; }
				 */
				// relPos = (c - prevdrift) / (drift - prevdrift);
				// String[] pred = line.split(",");
				//
				// if(pred[0].equals(pred[1]))
				// {
				// line = "1";
				// }
				// else
				// {
				// line = "0";
				// }
				extractor.setInput(Double.parseDouble(line));
				double pred = extractor.getConfidencePrediction();
				if (adwin.setInput(Double.parseDouble(line), 0.05, pred)) {
					if (c - prevdrift > 200) {
						prevdrift = c;
						System.out.print(c + "\n");
					}
					// bWriter.write(c+"\n");
				}
				c++;
			}

			br.close();

			// bWriter.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Volatility Mean
	// @Override
	public void doTest2() {
		try {

			// BufferedWriter bWriter = new BufferedWriter(new
			// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\volatility2_powersupply_realdrift_sigmoid"+tension+".csv"));

			ADWIN adwin = new ADWIN(0.05);

			BufferedReader br = new BufferedReader(
					new FileReader("D:\\Kauri BackUp Results\\Categorization\\Streams\\powersupply.csv"));
			String line = "";

			int c = 0, x = 0;
			double volatility = 0.00;
			double vCount = 0;

			double relPos = 0.0;
			// adwin.setTension(tension);
			// adwin.setMode(SIGMOID_FUNC);

			// ArrayList<Double> list = new ArrayList<Double>();

			// double[] powersupply = new
			// double[]{767,991,5151,5759,6111,6335,8063,8639,10751,13983,15199,15615,18079,19423,22815,23135,23391,24991,25663,28351,28543,29887,29928};
			// for(double d : powersupply){list.add(d);}

			double prevdrift = 0;
			// double drift = list.remove(0);
			double drift = 767;

			while ((line = br.readLine()) != null) {
				// relPos = (c - prevdrift) / (drift - prevdrift);

				if (adwin.setInput(Double.parseDouble(line)))
				// if (adwin.setInput(Double.parseDouble(line), 0.05, relPos))
				{
					if (c - prevdrift > 200) {
						// volatility += c - prevdrift;
						vCount++;

						prevdrift = c;
						// drift = c + volatility;
						System.out.print(c + "\n");
					}
					// bWriter.write(c+"\n");
				}
				c++;
			}

			br.close();

			// bWriter.close();

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
