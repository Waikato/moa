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

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

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


	RingBuffer<Integer> numAcquisitionsBuffer;

	
	public IntOption toleranceWindowSizeOption = new IntOption("tolreanceWindowSize", 't', 
			"The number of instances which are used to balance the number of label acquisitions.",
			100, 1,Integer.MAX_VALUE);
	
	public BalancedIncrementalQuantileFilter() {
		resetLearning();
	}
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		super.prepareForUseImpl(monitor, repository);
		numAcquisitionsBuffer = new RingBuffer<>(toleranceWindowSizeOption.getValue());
	}
	
	@Override
	public boolean isAbove(double alScore) {
		try
		{
			scoreBuffer.add(alScore);
			
			List<Double> window = scoreBuffer.toList();
			int windowSize = window.size();
			
			List<Integer> toleranceWindow = numAcquisitionsBuffer.toList();
			int toleranceWindowSize = toleranceWindow.size();
			
			Ranking<Double> r = new Ranking<>();
			List<Integer> rankedIndices = r.rank(window, windowSize - 1);
			int thresholdIdx = (int)(windowSize * (1-budget));
			double threshold = window.get(rankedIndices.get(thresholdIdx));
			
			
			double acquisitionsLeft = getAcquisitionsLeft(toleranceWindow, budget);
			double balancedThreshold = threshold - getRange(window, rankedIndices) * acquisitionsLeft/toleranceWindowSize;
			
			boolean decision = alScore >= balancedThreshold;
			
			numAcquisitionsBuffer.add(decision? 1 : 0);
			
			if (decision) {
				++acquiredLabels;
			}
			return decision;
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
		}
		return false;
	}
	
	/**
	 * Get the number of acquisitions which has to be done to get from the
	 * current budget to the wanted budget.
	 * @param toleranceWindow a list where for each time step the number of acquisitions is listed
	 * @param budget the wandted budget
	 * @return the number of acquisitions which is needed to get to the wanted budget
	 */
	private double getAcquisitionsLeft(List<Integer> toleranceWindow, double budget)
	{
		double numProcessedInstances = toleranceWindow.size();
		double numAcquiredLabels = 0;
		for(int i = 0; i < toleranceWindow.size(); ++i)
		{
			numAcquiredLabels += toleranceWindow.get(i);
		}
		return numProcessedInstances * budget - numAcquiredLabels;
	}
	
	/**
	 * Get the difference between the highest and smallest entry.
	 * If the size of the window is smaller than 2 0 is returned instead.
	 * @param window the window of observed active learning scores
	 * @param rankedIndices the order of indices such that window is ordered
	 * @return the range of the window
	 */
	private double getRange(List<Double> window, List<Integer> rankedIndices)
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
		numAcquisitionsBuffer = null;
	}
}
