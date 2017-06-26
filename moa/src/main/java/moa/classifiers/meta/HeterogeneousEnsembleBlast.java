package moa.classifiers.meta;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

public class HeterogeneousEnsembleBlast extends HeterogeneousEnsembleAbstract {

	private static final long serialVersionUID = 1L;

	protected boolean[][] onlineHistory;

	public IntOption windowSizeOption = new IntOption("windowSize", 'w',
			"The window size over which Online Performance Estimation is done.", 1000,
			1, Integer.MAX_VALUE);

	@Override
	public void resetLearningImpl() {
		this.historyTotal = new double[this.ensemble.length];
		this.onlineHistory = new boolean[this.ensemble.length][windowSizeOption
				.getValue()];
		this.instancesSeen = 0;

		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i].resetLearning();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		int wValue = windowSizeOption.getValue();

		for (int i = 0; i < this.ensemble.length; i++) {

			// Online Performance estimation
			double[] votes = ensemble[i].getVotesForInstance(inst);
			boolean correct = (maxIndex(votes) * 1.0 == inst.classValue());

			if (correct && !onlineHistory[i][instancesSeen % wValue]) {
				// performance estimation increases
				onlineHistory[i][instancesSeen % wValue] = true;
				historyTotal[i] += 1.0 / wValue;
			} else if (!correct && onlineHistory[i][instancesSeen % wValue]) {
				// performance estimation decreases
				onlineHistory[i][instancesSeen % wValue] = false;
				historyTotal[i] -= 1.0 / wValue;
			} else {
				// nothing happens
			}

			this.ensemble[i].trainOnInstance(inst);
		}

		instancesSeen += 1;
		if (instancesSeen % gracePerionOption.getValue() == 0) {
			topK = topK(historyTotal, activeClassifiersOption.getValue());
		}
	}
}
