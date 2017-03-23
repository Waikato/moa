/*
 *    IncrementalQuantileFilter.java
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

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;

import moa.classifiers.active.RingBuffer;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * This budget manager is an implementation of the incremental quantile filter
 * proposed in [1]
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
public class IncrementalQuantileFilter extends AbstractOptionHandler implements BudgetManager {

	private static final long serialVersionUID = 1L;
	
	protected double budget;
	protected int acquiredLabels;
	protected RingBuffer<Double> scoreBuffer;
	Ranking<Double> r;

	public IntOption windowSizeOption = new IntOption("windowSize", 'w', 
			"The number of previously observed al scores which should be considered.",
			100, 1,Integer.MAX_VALUE);

    public FloatOption budgetOption = new FloatOption("budget",
    		'b', "The budget that should be used by the BudgetManager.",
    		0.1, 0.00, 1.00);
	
	public IncrementalQuantileFilter() {
		resetLearning();
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		scoreBuffer = new RingBuffer<>(windowSizeOption.getValue());
		budget = budgetOption.getValue();
	}

	@Override
	public boolean isAbove(double alScore) {
		try {

			Double removedElement = scoreBuffer.add(alScore);
			
			int windowSize = scoreBuffer.size();
			
			List<Integer> rankedIndices = r.rank(scoreBuffer, windowSize - 1, removedElement);
			int thresholdIdx = (int)(windowSize * (1-budget));
			double threshold = scoreBuffer.get(rankedIndices.get(thresholdIdx));
			
			
			boolean decision = alScore >= threshold;
			
			if (decision) {
				++acquiredLabels;
			}
			return decision;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
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
		r = new Ranking<>();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
