package moa.classifiers.meta;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

public class HeterogeneousEnsembleBlastFadingFactors
		extends HeterogeneousEnsembleAbstract {

	private static final long serialVersionUID = 1L;

	// Makes the model slightly more fault tolerant.
	// Sometimes one of the base-models crashes, resulting
	// in the meta-algorithm to crash as well. We ignore
	// it when this happens only occasionally.
	private static final int MAX_TOLLERATED_TRAINING_ERRROS = 100;

	private int trainingErrors;

	public FloatOption alphaOption = new FloatOption("alpha", 'a',
			"The fading factor.", 0.999, 0, 1);

	@Override
	public void resetLearningImpl() {
		this.historyTotal = new double[this.ensemble.length];
		for (int i = 0; i < this.ensemble.length; ++i) {
			this.historyTotal[i] = 1.0;
		}

		this.instancesSeen = 0;
		this.trainingErrors = 0;
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i].resetLearning();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {

		for (int i = 0; i < this.ensemble.length; i++) {

			// Online Performance estimation
			double[] votes = ensemble[i].getVotesForInstance(inst);
			boolean correct = (maxIndex(votes) * 1.0 == inst.classValue());

			historyTotal[i] = historyTotal[i] * alphaOption.getValue();
			if (correct) {
				historyTotal[i] += 1 - alphaOption.getValue();
			}
			try {
				this.ensemble[i].trainOnInstance(inst);
			} catch (RuntimeException e) {
				this.trainingErrors += 1;

				if (trainingErrors > MAX_TOLLERATED_TRAINING_ERRROS) {
					throw new RuntimeException(
							"Too much training errors! Latest: " + e.getMessage());
				}
			}
		}

		instancesSeen += 1;
		if (instancesSeen % gracePerionOption.getValue() == 0) {
			topK = topK(historyTotal, activeClassifiersOption.getValue());
		}
	}
}
