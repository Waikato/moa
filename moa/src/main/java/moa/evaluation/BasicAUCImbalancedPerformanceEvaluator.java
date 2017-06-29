/*
 *    ImbalancedPerformanceEvaluator.java
 *    Copyright (C) 2016 Poznan University of Technology
 *    @author Dariusz Brzezinski (dbrzezinski@cs.put.poznan.pl)
 *    @author Tomasz Pewinski
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package moa.evaluation;

import java.util.TreeSet;

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.tasks.TaskMonitor;

/**
 * Performance measures designed for class imbalance problems. Only to be used
 * for binary classification problems with unweighted instances. Class 0 -
 * majority/negative examples, class 1 - minority, positive examples. AUC
 * calculation as described in (but without windowing): D. Brzezinski, J.
 * Stefanowski, "Prequential AUC: Properties of the Area Under the ROC Curve for
 * Data Streams with Concept Drift", Knowledge and Information Systems, 2017.
 * 
 * @author Dariusz Brzezinski (dbrzezinski at cs.put.poznan.pl)
 */
public class BasicAUCImbalancedPerformanceEvaluator extends AbstractOptionHandler
		implements ClassificationPerformanceEvaluator {

	private static final long serialVersionUID = 1L;

	public FlagOption calculateAUC = new FlagOption("calculateAUC", 'a',
			"Determines whether AUC should be calculated. To calculate AUC, "
					+ "predictions need to be remembered, therefore setting "
					+ "this option for large streams can cause substantial "
					+ "memory usage.");

	protected double totalObservedInstances = 0;
	private Estimator aucEstimator;
	private SimpleEstimator weightMajorityClassifier;
	protected int numClasses;

	public class SimpleEstimator {
		protected double len;

		protected double sum;

		public void add(double value) {
			sum += value;
			len++;
		}

		public double estimation() {
			return sum / len;
		}
	}

	public class Estimator {

		public class Score implements Comparable<Score> {
			/**
			 * Predicted score of the example
			 */
			protected double value;

			/**
			 * Age of example - position in the stream where the example was
			 * added
			 */
			protected int position;

			/**
			 * True if example's true label is positive
			 */
			protected boolean isPositive;

			/**
			 * Constructor.
			 * 
			 * @param value
			 *            score value
			 * 
			 * @param isPositive
			 *            true if the example's true label is positive
			 */
			public Score(double value, int position, boolean isPositive) {
				this.value = value;
				this.isPositive = isPositive;
				this.position = position;
			}

			/**
			 * Sort descending based on score value.
			 */
			@Override
			public int compareTo(Score o) {
				if (o.value < this.value) {
					return -1;
				} else if (o.value > this.value) {
					return 1;
				} else {
					if (!o.isPositive && this.isPositive) {
						return -1;
					} else if (o.isPositive && !this.isPositive) {
						return 1;
					} else {
						if (o.position > this.position) {
							return -1;
						} else if (o.position < this.position) {
							return 1;
						} else {
							return 0;
						}
					}
				}
			}

			@Override
			public boolean equals(Object o) {
				return (o instanceof Score) && ((Score) o).position == this.position;
			}
		}

		protected TreeSet<Score> sortedScores;

		protected int position;

		protected double numPos;

		protected double numNeg;

		protected double correctPredictions;

		protected double correctPositivePredictions;

		protected double[] columnKappa;

		protected double[] rowKappa;

		protected boolean calculateAuc;

		public Estimator(boolean calculateAuc) {
			this.calculateAuc = calculateAuc;
			this.sortedScores = new TreeSet<Score>();

			this.rowKappa = new double[numClasses];
			this.columnKappa = new double[numClasses];
			for (int i = 0; i < numClasses; i++) {
				this.rowKappa[i] = 0.0;
				this.columnKappa[i] = 0.0;
			}

			this.position = 0;
			this.numPos = 0;
			this.numNeg = 0;
			this.correctPredictions = 0;
			this.correctPositivePredictions = 0;
		}

		public void add(double score, boolean isPositive, boolean correctPrediction) {
			// // add new example
			Score newScore = new Score(score, position, isPositive);
			if (this.calculateAuc) {
				sortedScores.add(newScore);
			}
			correctPredictions += correctPrediction ? 1 : 0;
			correctPositivePredictions += correctPrediction && isPositive ? 1 : 0;

			int trueClass = isPositive ? 1 : 0;
			int predictedClass = correctPrediction ? trueClass : Math.abs(trueClass - 1);
			this.rowKappa[predictedClass] += 1;
			this.columnKappa[trueClass] += 1;

			if (newScore.isPositive) {
				numPos++;
			} else {
				numNeg++;
			}

			//// position needs to be always incremented to differentiate
			//// between examples in the red-black tree
			position++;
		}

		public double getAUC() {
			double AUC = 0;
			double c = 0;
			double prevc = 0;
			double lastPosScore = Double.MAX_VALUE;

			if (!this.calculateAuc) {
				return -1;
			}

			if (numPos == 0 || numNeg == 0) {
				return 1;
			}

			for (Score s : sortedScores) {
				if (s.isPositive) {
					if (s.value != lastPosScore) {
						prevc = c;
						lastPosScore = s.value;
					}

					c += 1;
				} else {
					if (s.value == lastPosScore) {
						// tie
						AUC += ((double) (c + prevc)) / 2.0;
					} else {
						AUC += c;
					}
				}
			}

			return AUC / (numPos * numNeg);
		}

		public double getScoredAUC() {
			double AOC = 0;
			double AUC = 0;
			double r = 0;
			double prevr = 0;
			double c = 0;
			double prevc = 0;
			double R_plus, R_minus;
			double lastPosScore = Double.MAX_VALUE;
			double lastNegScore = Double.MAX_VALUE;

			if (!this.calculateAuc) {
				return -1;
			}

			if (numPos == 0 || numNeg == 0) {
				return 1;
			}

			for (Score s : sortedScores) {
				if (s.isPositive) {
					if (s.value != lastPosScore) {
						prevc = c;
						lastPosScore = s.value;
					}

					c += s.value;

					if (s.value == lastNegScore) {
						// tie
						AOC += ((double) (r + prevr)) / 2.0;
					} else {
						AOC += r;
					}
				} else {
					if (s.value != lastNegScore) {
						prevr = r;
						lastNegScore = s.value;
					}

					r += s.value;

					if (s.value == lastPosScore) {
						// tie
						AUC += ((double) (c + prevc)) / 2.0;
					} else {
						AUC += c;
					}
				}
			}

			R_minus = (numPos * r - AOC) / (numPos * numNeg);
			R_plus = (AUC) / (numPos * numNeg);
			return R_plus - R_minus;
		}

		public double getRatio() {
			if (numNeg == 0) {
				return Double.MAX_VALUE;
			} else {
				return numPos / numNeg;
			}
		}

		public double getAccuracy() {
			return totalObservedInstances > 0.0 ? correctPredictions / totalObservedInstances : 0.0;
		}

		public double getKappa() {
			double p0 = getAccuracy();
			double pc = 0.0;

			for (int i = 0; i < numClasses; i++) {
				pc += (this.rowKappa[i] / totalObservedInstances) * (this.columnKappa[i] / totalObservedInstances);
			}

			return (p0 - pc) / (1.0 - pc);
		}

		private double getKappaM() {
			double p0 = getAccuracy();
			double pc = weightMajorityClassifier.estimation();

			return (p0 - pc) / (1.0 - pc);
		}

		public double getGMean() {
			double positiveAccuracy = correctPositivePredictions / numPos;
			double negativeAccuracy = (correctPredictions - correctPositivePredictions) / numNeg;
			return Math.sqrt(positiveAccuracy * negativeAccuracy);
		}

		public double getRecall() {
			return correctPositivePredictions / numPos;
		}
	}

	@Override
	public void reset() {
		reset(this.numClasses);
	}

	public void reset(int numClasses) {
		if (numClasses != 2) {
			throw new RuntimeException("Too many classes (" + numClasses
					+ "). AUC evaluation can be performed only for two-class problems!");
		}

		this.numClasses = numClasses;

		this.aucEstimator = new Estimator(this.calculateAUC.isSet());
		this.weightMajorityClassifier = new SimpleEstimator();
		this.totalObservedInstances = 0;
	}

	@Override
	public void addResult(Example<Instance> exampleInstance, double[] classVotes) {
		InstanceImpl inst = (InstanceImpl) exampleInstance.getData();
		double weight = inst.weight();

		if (inst.classIsMissing() == false) {
			int trueClass = (int) inst.classValue();

			if (weight > 0.0) {
				// // initialize evaluator
				if (totalObservedInstances == 0) {
					reset(inst.dataset().numClasses());
				}
				this.totalObservedInstances += 1;

				//// if classVotes has length == 1, then the negative (0) class
				//// got all the votes
				Double normalizedVote = 0.0;

				//// normalize and add score
				if (classVotes.length == 2) {
					normalizedVote = classVotes[1] / (classVotes[0] + classVotes[1]);
				}

				if (normalizedVote.isNaN()) {
					normalizedVote = 0.0;
				}

				this.aucEstimator.add(normalizedVote, trueClass == 1, Utils.maxIndex(classVotes) == trueClass);
				this.weightMajorityClassifier
						.add((this.aucEstimator.getRatio() <= 1 ? 0 : 1) == trueClass ? weight : 0);
			}
		}
	}

	@Override
	public Measurement[] getPerformanceMeasurements() {
		return new Measurement[] { new Measurement("classified instances", this.totalObservedInstances),
				new Measurement("AUC", this.aucEstimator.getAUC()),
				new Measurement("sAUC", this.aucEstimator.getScoredAUC()),
				new Measurement("Accuracy", this.aucEstimator.getAccuracy()),
				new Measurement("Kappa", this.aucEstimator.getKappa()),
				new Measurement("Pos/Neg ratio", this.aucEstimator.getRatio()),
				new Measurement("G-Mean", this.aucEstimator.getGMean()),
				new Measurement("Recall", this.aucEstimator.getRecall()),
				new Measurement("KappaM", this.aucEstimator.getKappaM()) };

	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		Measurement.getMeasurementsDescription(getPerformanceMeasurements(), sb, indent);
	}

	@Override
	public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
	}

	public Estimator getAucEstimator() {
		return aucEstimator;
	}

	@Override
	public void addResult(Example<Instance> arg0, Prediction arg1) {
		throw new RuntimeException("Designed for scoring classifiers");
	}
}
