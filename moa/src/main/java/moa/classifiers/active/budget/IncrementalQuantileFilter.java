package moa.classifiers.active.budget;

import java.util.List;

import com.github.javacliparser.IntOption;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class IncrementalQuantileFilter extends AbstractOptionHandler implements BudgetManager {

	private static final long serialVersionUID = 1L;
	
	double budget;
	int acquiredLabels;
	RingBuffer<Double> scoreBuffer;

	public IntOption windowSizeOption = new IntOption("WindowSize", 'w', 
			"The number of previously observed al scores which should be considered.",
			100, 1,Integer.MAX_VALUE);

	public IncrementalQuantileFilter() {
		resetLearning();
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		scoreBuffer = new RingBuffer<>(windowSizeOption.getValue());
	}

	@Override
	public boolean isAbove(double alScore) {
		scoreBuffer.add(alScore);
		
		List<Double> window = scoreBuffer.toList();
		int windowSize = window.size();
		
		Ranking<Double> r = new Ranking<>();
		int rank = r.rank(window, windowSize - 1);
		int thresholdIdx = (int)(windowSize * (1-budget));
		
		boolean decision = rank >= thresholdIdx;
		
		if (decision) {
			++acquiredLabels;
		}
		return decision;
	}

	@Override
	public void setBudget(double budget) {
		this.budget = budget;
	}

	@Override
	public int getLastLabelAcqReport() {
		int tmpAcquiredLabels = acquiredLabels;
		acquiredLabels = 0;
		return tmpAcquiredLabels;
	}

	@Override
	public void resetLearning() {
		acquiredLabels = 0;
		budget = 0;
		scoreBuffer = null;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
