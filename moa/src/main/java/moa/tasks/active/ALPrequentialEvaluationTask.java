/*
 *    ALPrequentialEvaluationTask.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
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
package moa.tasks.active;

import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.FloatOption;

import moa.classifiers.active.ALClassifier;
import moa.core.ObjectRepository;
import moa.evaluation.ALEvaluator;
import moa.evaluation.LearningCurve;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.tasks.TaskMonitor;

/**
 * This task performs prequential evaluation for an active learning classifier 
 * (testing, then training with each example in sequence).
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ALPrequentialEvaluationTask extends ALMainTask {
	
	private static final long serialVersionUID = 1L;
	
	public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Learner to train.", ALClassifier.class, 
            "moa.classifiers.active.ALZliobaite2011");
	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", ExampleStream.class,
            "generators.RandomTreeGenerator");
	
	public ClassOption prequentialEvaluatorOption = new ClassOption(
			"prequentialEvaluator", 'e',
            "Prequential classification performance evaluation method.",
            ALEvaluator.class,
            "ALBasicClassificationPerformanceEvaluator");
	
	public FloatOption budgetOption = new FloatOption("budget", 't', 
			"Active learner budget.", 0.9);
	
	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}
	
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		/*
		 * TODO Implement prequential evaluation main task
		 * 
		 * Process each sample from the given stream:
		 * 1. Test on sample
		 * 2. Train on sample
		 */
		
		return null;
	}
	
	@Override
	public List<ALTaskThread> getSubtaskThreads() {
		return new ArrayList<ALTaskThread>();
	}
}
