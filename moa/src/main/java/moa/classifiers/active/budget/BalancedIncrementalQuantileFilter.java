/*
 *    BalancedIncrementalQuantileFilter.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
 *    
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers.active.budget;

import java.util.List;

import com.github.javacliparser.IntOption;

import moa.classifiers.active.RingBuffer;

/**
 * This budget manager is an implementation of the balanced incremental 
 * quantile filter proposed in [1]
 *
 *
 * [1]	Kottke D., Krempl G., Spiliopoulou M. 
 * 		(2015) Probabilistic Active Learning in Datastreams. 
 * 		In: Fromont E., De Bie T., van Leeuwen M. (eds) 
 * 		Advances in Intelligent Data Analysis XIV. 
 * 		Lecture Notes in Computer Science, vol 9385. Springer, Cham
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class BalancedIncrementalQuantileFilter extends IncrementalQuantileFilter{

	private static final long serialVersionUID = 1L;
	
	Ranking<Double> r;
	int numProcessedInstances;
	int numAcquiredLabels;
	
	public IntOption toleranceWindowSizeOption = new IntOption("toleranceWindowSize", 't', 
			"The number of instances which are used to balance the number of label acquisitions.",
			100, 1,Integer.MAX_VALUE);

	@Override
	public boolean isAbove(double alScore) {
		Double removedElement = scoreBuffer.add(alScore);
		int windowSize = scoreBuffer.size();
		
		int toleranceWindowSize = Math.min(numProcessedInstances, toleranceWindowSizeOption.getValue());
		
		List<Integer> rankedIndices = r.rank(scoreBuffer, windowSize - 1, removedElement);
		
		int thresholdIdx = (int)(windowSize * (1-budget));
		double threshold = scoreBuffer.get(rankedIndices.get(thresholdIdx));
		
		
		double acquisitionsLeft = getAcquisitionsLeft(budget);
		
		double range = getRange(scoreBuffer, rankedIndices);
		double balancedThreshold = threshold - range * (toleranceWindowSize==0? 0 : acquisitionsLeft/toleranceWindowSize);
		
		boolean decision = alScore >= balancedThreshold;
		
		++numProcessedInstances;
		if (decision) {
			++acquiredLabels;
			++numAcquiredLabels;
		}
		return decision;
	}
	
	/**
	 * Get the number of acquisitions which has to be done to get from the
	 * current budget to the wanted budget.
	 * @param budget the wandted budget
	 * @return the number of acquisitions which is needed to get to the wanted budget
	 */
	private double getAcquisitionsLeft(double budget)
	{
		double acquisitionsLeft = numProcessedInstances * budget - numAcquiredLabels;
		return acquisitionsLeft;
	}
	
	/**
	 * Get the difference between the highest and smallest entry.
	 * If the size of the window is smaller than 2 0 is returned instead.
	 * @param window the window of observed active learning scores
	 * @param rankedIndices the order of indices such that window is ordered
	 * @return the range of the window
	 */
	private double getRange(RingBuffer<Double> window, List<Integer> rankedIndices)
	{
		if(window.size() > 1)
		{
			return window.get(rankedIndices.get(rankedIndices.size() - 1)) - window.get(rankedIndices.get(0));
		}
		else
		{
			return 0;
		}
	}
	
	@Override
	public void resetLearning() {
		super.resetLearning();
		numProcessedInstances = 0;
		numAcquiredLabels = 0;
		this.budget = budgetOption.getValue();
		r = new Ranking<>();
	}
	
	@Override
	public void prepareForUse() {
		super.prepareForUse();
		resetLearning();
	}
}
