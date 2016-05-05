package testers;

import inputstream.BernoulliDistributionGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import sizeof.agent.SizeOfAgent;
import cutpointdetection.ADWIN;
import cutpointdetection.SingDetector;

public class VDriftDetectionOnlineVersion_TruePositiveTester implements Tester {
	int numDriftInstances = 1000;
	// int numDriftInstances = 500000;
	int iterations = 100;

	public final int NOCORRECTION = 0;
	public final int SINE_FUNC = 1;
	public final int SIGMOID_FUNC = 2;

	@Override
	public void doTest() {
		try {
			// int[] numInst = {10000, 50000, 100000, 1000000};
			// int[] numInst = {100000000, 250000000, 500000000};
			int[] numInst = { 1000000 };
			// String[] slopes = { "", "100000", "50000", "10000", "5000",
			// "1000" };
			String[] slopes = { "", "10000", "50000", "100000" };
			// double[] epsilonPrimes = { 0.0025, 0.005, 0.0075, 0.01 };
			// String[] epsilonPrimes = { "0.4", "0.6", "0.8" };
			String[] epsilonPrimes = { "0.0001", "0.0002", "0.0003", "0.0004" };
			// String[] epsilonPrimes = {"0.0006"};
			// double[] linearAlphas = {0.1};
			double[] linearAlphas = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5 };

			double delta = 0.05;

			double[] prob = { 0.2 };

			for (int w = 0; w < linearAlphas.length; w++) {
				System.out.println(linearAlphas[w]);
				for (int q = 0; q < epsilonPrimes.length; q++) {
					System.out.println(epsilonPrimes[q]);
					for (int i = 0; i < numInst.length; i++) {
						// BufferedWriter bWriter = new BufferedWriter(new
						// FileWriter("src\\testers\\Volatility&ChangeDetection\\Test\\"+
						// blockSize + "_"+ DECAY_MODE + "_" + COMPRESSION_MODE
						// + "_" + "L-" + numDriftInstances + "_" + compTerms[e]
						// + "_" + epsilonPrimes[q] + "_" + alphas[w] + "_" +
						// numInst[i] + ".csv"));
						// BufferedWriter bWriter = new BufferedWriter(new
						// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\"
						// +
						// "VaryingMagnitude_abrupt_0.2_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+".csv"));
						// BufferedWriter bWriter = new BufferedWriter(new
						// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\"
						// +
						// "VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_nocorrection.csv"));
						// BufferedWriter bWriter = new BufferedWriter(new
						// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\TP
						// Test\\" +
						// "VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+".csv"));

						BufferedWriter bWriter = new BufferedWriter(
								new FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\SEED\\TP Test\\"
										+ "Rand_VaryingMagnitude_gradual_" + epsilonPrimes[q] + "_sigmoid"
										+ linearAlphas[w] + ".csv"));

						// bWriter.write("Slope,Number of Drifts,Avg Time,Time
						// Stdev,Memory Size");
						bWriter.write(
								"Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
						bWriter.newLine();
						for (int j = 1; j < slopes.length; j++) {
							int[] delays = new int[iterations];
							int totalDrift = 0;
							long[] times = new long[iterations];
							long totalTime = 0;

							// int totalCompCount = 0;
							// int totalCompChecks = 0;
							// int totalWarningCount = 0;

							// int[] checks = new int[iterations];

							int totalSize = 0;
							for (int k = 0; k < iterations; k++) {
								// generateSlopedInput(prob[0], slopes[j],
								// numInst[i], numDriftInstances, k);

								ADWIN adwin = new ADWIN(delta);
								SingDetector sing = new SingDetector(delta, 32, 1, 1, 0.01, 0.8, 75);
								// SingDetector sing = new SingDetector(delta,
								// blockSize, DECAY_MODE, COMPRESSION_MODE,
								// epsilonPrimes[q], alphas[w], compTerms[e]);
								// CutPointDetector detector = adwin;

								// BufferedReader br = new BufferedReader(new
								// FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+epsilonPrimes[q]+"\\"+slopes[j]+"_"+epsilonPrimes[q]+
								// "_" + k + ".csv"));
								// BufferedWriter bw = new BufferedWriter(new
								// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\TP
								// Test\\VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_"
								// + slopes[j] + "_" + k + ".csv"));
								// BufferedReader br = new BufferedReader(new
								// FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_"+epsilonPrimes[q]+"\\v"+slopes[j]+"_Abrupt_0.2_"+epsilonPrimes[q]+
								// "_" + k + ".csv"));
								// BufferedWriter bw = new BufferedWriter(new
								// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_abrupt_0.2_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_"
								// + slopes[j] + "_" + k + ".csv"));

								// BufferedReader br = new BufferedReader(new
								// FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+epsilonPrimes[q]+"\\"+slopes[j]+"_"+epsilonPrimes[q]+
								// "_" + k + ".csv"));
								// BufferedWriter bw = new BufferedWriter(new
								// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_"
								// + slopes[j] + "_" + k + ".csv"));
								// BufferedReader br = new BufferedReader(new
								// FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+epsilonPrimes[q]+"\\"+slopes[j]+"_"+epsilonPrimes[q]+
								// "_" + k + ".csv"));
								// BufferedWriter bw = new BufferedWriter(new
								// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_nocorrection"
								// + "\\driftpoints_" + slopes[j] + "_" + k +
								// ".csv"));

								BufferedReader br = new BufferedReader(new FileReader(
										"D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"
												+ epsilonPrimes[q] + "\\rand_10000000_" + epsilonPrimes[q] + "_" + k
												+ ".csv"));
								// BufferedWriter bw = new BufferedWriter(new
								// FileWriter("src\\testers\\VolatilityPredictingDrift\\Results\\TP
								// Test\\Rand_VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\sigmoid"+linearAlphas[w]+"_driftpoints_"
								// + slopes[j] + "_" + k + ".csv"));

								double relPos = 0.0;
								BufferedReader brpos = new BufferedReader(new FileReader(
										"D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"
												+ epsilonPrimes[q] + "\\rand_10000000_" + epsilonPrimes[q] + "_" + k
												+ "_driftlocations.csv"));
								// ArrayList<Double> list = new
								// ArrayList<Double>();
								// String line2 = "";
								// while ((line2 = brpos.readLine()) != null)
								// {
								// list.add(Double.parseDouble(line2));
								// }
								// list.add(Double.parseDouble(slopes[j]));
								// brpos.close();
								// adwin.setVolatility(Integer.parseInt(slopes[j]));
								sing.setTension(linearAlphas[w]);
								sing.setMode(SIGMOID_FUNC);

								String line = "";

								int c = 0;

								int delay = -1;
								double prevdrift = 0;
								double drift = Double.parseDouble(brpos.readLine()); // list.remove(0);
								brpos.close();
								while ((line = br.readLine()) != null) {
									// if(c == drift)
									// {
									// prevdrift = drift;
									// drift = list.remove(0);
									// }
									relPos = (c - prevdrift) / (drift - prevdrift);
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

									long startTime = System.currentTimeMillis();
									if (sing.setInput(Double.parseDouble(line), delta, relPos) && c >= drift) {
										long endTime = System.currentTimeMillis();
										totalTime = totalTime + (endTime - startTime);
										times[k] = times[k] + endTime - startTime;
										// bw.write(c + "\n");
										delay = c - (int) drift;
										delays[k] = delay;
										if (c <= drift + 1000) {
											totalDrift++;
										}
										c++;
										break;
									}
									long endTime = System.currentTimeMillis();
									totalTime = totalTime + (endTime - startTime);
									times[k] = times[k] + endTime - startTime;
									c++;
								}
								totalSize += SizeOfAgent.fullSizeOf(adwin);
								br.close();
								// bw.close();
							}

							bWriter.write(slopes[j] + ",");
							bWriter.write(totalDrift + ",");
							bWriter.write(totalDrift / (double) iterations + ",");
							bWriter.write(calculateSum(delays) / iterations + ",");
							bWriter.write(calculateStdev(delays, calculateSum(delays) / iterations) + ",");
							bWriter.write((double) totalTime / iterations + ",");
							bWriter.write(calculateStdevLong(times, (double) totalTime / iterations) + ",");
							bWriter.write((double) totalSize / (double) iterations + ",");
							bWriter.newLine();
						}
						bWriter.close();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * 100 DRIFT POINTS
	 * 
	 * @Override public void doTest() { try { //int[] numInst = {10000, 50000,
	 * 100000, 1000000}; //int[] numInst = {100000000, 250000000, 500000000};
	 * int[] numInst = { 1000000 }; //String[] slopes = { "", "100000", "50000",
	 * "10000", "5000", "1000" }; String[] slopes = { "", "10000000" };
	 * //double[] epsilonPrimes = { 0.0025, 0.005, 0.0075, 0.01 }; //String[]
	 * epsilonPrimes = { "0.4", "0.6", "0.8" }; String[] epsilonPrimes =
	 * {"0.0001","0.0002", "0.0003", "0.0004", "0.0006"}; //String[]
	 * epsilonPrimes = {"0.0006"}; //double[] linearAlphas = {0.1}; double[]
	 * linearAlphas = {0.0};
	 * 
	 * double delta = 0.05;
	 * 
	 * double[] prob = { 0.2 };
	 * 
	 * for (int w = 0; w < linearAlphas.length; w++) {
	 * System.out.println(linearAlphas[w]); for (int q = 0; q <
	 * epsilonPrimes.length; q++) { System.out.println(epsilonPrimes[q]); for
	 * (int i = 0; i < numInst.length; i++) { //BufferedWriter bWriter = new
	 * BufferedWriter(new FileWriter(
	 * "src\\testers\\Volatility&ChangeDetection\\Test\\"+ blockSize + "_
	 * "+ DECAY_MODE + "_" + COMPRESSION_MODE + "_" + "L-
	 * " + numDriftInstances + "_" + compTerms[e] + "_" + epsilonPrimes[q] + "_
	 * " + alphas[w] + "_" + numInst[i] + ".csv")); //BufferedWriter bWriter =
	 * new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\" + "
	 * VaryingMagnitude_abrupt_0.
	 * 2_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+".csv"));
	 * //BufferedWriter bWriter = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\" + "
	 * VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_nocorrection.csv"));
	 * //BufferedWriter bWriter = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\" + "
	 * VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+".
	 * csv"));
	 * 
	 * BufferedWriter bWriter = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\RandDrift Point\\" + "
	 * Rand_VaryingMagnitude_gradual_"+epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"_10000000
	 * .csv"));
	 * 
	 * bWriter.write("Slope,Number of Drifts,Avg Time,Time Stdev,Memory Size");
	 * // bWriter.write(
	 * "Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size"
	 * ); bWriter.newLine(); for (int j = 1; j < slopes.length; j++) { // int[]
	 * delays = new int[iterations]; int totalDrift = 0; long[] times = new
	 * long[iterations]; long totalTime = 0;
	 * 
	 * //int totalCompCount = 0; //int totalCompChecks = 0; //int
	 * totalWarningCount = 0;
	 * 
	 * int[] checks = new int[iterations];
	 * 
	 * int totalSize = 0; for (int k = 0; k < iterations; k++) {
	 * //generateSlopedInput(prob[0], slopes[j], numInst[i], numDriftInstances,
	 * k);
	 * 
	 * ADWIN adwin = new ADWIN(delta); //SingDetector sing = new
	 * SingDetector(delta, blockSize, DECAY_MODE, COMPRESSION_MODE,
	 * epsilonPrimes[q], alphas[w], compTerms[e]); //CutPointDetector detector =
	 * adwin;
	 * 
	 * //BufferedReader br = new BufferedReader(new
	 * FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+
	 * epsilonPrimes[q]+"\\"+slopes[j]+"_"+epsilonPrimes[q]+ "_" + k + ".csv"));
	 * //BufferedWriter bw = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"
	 * +epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_" + slopes[j]
	 * + "_" + k + ".csv")); //BufferedReader br = new BufferedReader(new
	 * FileReader(
	 * "src\\testers\\VolatilityPredictingDrift\\Datasets\\Abrupt_0.2_"+
	 * epsilonPrimes[q]+"\\v"+slopes[j]+"_Abrupt_0.2_"+epsilonPrimes[q]+ "_" + k
	 * + ".csv")); //BufferedWriter bw = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_abrupt_0.2_"
	 * +epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_" + slopes[j]
	 * + "_" + k + ".csv"));
	 * 
	 * //BufferedReader br = new BufferedReader(new
	 * FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+
	 * epsilonPrimes[q]+"\\"+slopes[j]+"_"+epsilonPrimes[q]+ "_" + k + ".csv"));
	 * //BufferedWriter bw = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"
	 * +epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\driftpoints_" + slopes[j]
	 * + "_" + k + ".csv")); //BufferedReader br = new BufferedReader(new
	 * FileReader("src\\testers\\VolatilityPredictingDrift\\Datasets\\Gradual_"+
	 * epsilonPrimes[q]+"\\"+slopes[j]+"_"+epsilonPrimes[q]+ "_" + k + ".csv"));
	 * //BufferedWriter bw = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\VaryingMagnitude_gradual_"
	 * +epsilonPrimes[q]+"_nocorrection" + "\\driftpoints_" + slopes[j] + "_" +
	 * k + ".csv"));
	 * 
	 * BufferedReader br = new BufferedReader(new FileReader(
	 * "D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"
	 * +epsilonPrimes[q]+"\\rand_"+slopes[j]+"_"+epsilonPrimes[q]+ "_" + k +
	 * ".csv")); BufferedWriter bw = new BufferedWriter(new FileWriter(
	 * "src\\testers\\VolatilityPredictingDrift\\Results\\RandDrift Point\\VaryingMagnitude_gradual_"
	 * +epsilonPrimes[q]+"_sigmoid"+linearAlphas[w]+"\\sigmoid"+linearAlphas[w]+
	 * "_driftpoints_" + slopes[j] + "_" + k + ".csv"));
	 * 
	 * double relPos = 0.0; BufferedReader brpos = new BufferedReader(new
	 * FileReader(
	 * "D:\\Kauri BackUp Results\\VolatilityPredictingDrift\\Datasets\\Gradual_"
	 * +epsilonPrimes[q]+"\\rand_"+slopes[j]+"_"+epsilonPrimes[q]+ "_" + k +
	 * "_driftlocations.csv")); ArrayList<Double> list = new
	 * ArrayList<Double>(); String line2 = ""; while ((line2 = brpos.readLine())
	 * != null) { list.add(Double.parseDouble(line2)); }
	 * list.add(Double.parseDouble(slopes[j])); brpos.close();
	 * //adwin.setVolatility(Integer.parseInt(slopes[j]));
	 * adwin.setTension(linearAlphas[w]); adwin.setMode(SIGMOID_FUNC);
	 * 
	 * String line = "";
	 * 
	 * int c = 0;
	 * 
	 * int delay = -1; double prevdrift = 0; double drift = list.remove(0);
	 * 
	 * while ((line = br.readLine()) != null) { if(c == drift) { prevdrift =
	 * drift; drift = list.remove(0); } relPos = (c - prevdrift) / (drift -
	 * prevdrift); // String[] pred = line.split(","); // //
	 * if(pred[0].equals(pred[1])) // { // line = "1"; // } // else // { // line
	 * = "0"; // }
	 * 
	 * long startTime = System.currentTimeMillis(); if
	 * (adwin.setInput(Double.parseDouble(line), delta, relPos)) { long endTime
	 * = System.currentTimeMillis(); totalTime = totalTime + (endTime -
	 * startTime); times[k] = times[k] + endTime - startTime; bw.write(c +
	 * "\n"); // delay = c - (numInst[i] - numDriftInstances); // delays[k] =
	 * delay; totalDrift++; c++; continue; } long endTime =
	 * System.currentTimeMillis(); totalTime = totalTime + (endTime -
	 * startTime); times[k] = times[k] + endTime - startTime; c++; } totalSize
	 * += SizeOfAgent.fullSizeOf(adwin); br.close(); bw.close(); }
	 * 
	 * bWriter.write(slopes[j] + ","); bWriter.write(totalDrift + ","); //
	 * bWriter.write(totalDrift / (double) iterations + ","); //
	 * bWriter.write(calculateSum(delays) / totalDrift + ","); //
	 * bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) +
	 * ","); bWriter.write((double) totalTime / iterations + ",");
	 * bWriter.write(calculateStdevLong(times, (double) totalTime / iterations)
	 * + ","); bWriter.write((double) totalSize / (double) iterations + ",");
	 * bWriter.newLine(); } bWriter.close(); } } }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); }
	 * 
	 * }
	 */
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
