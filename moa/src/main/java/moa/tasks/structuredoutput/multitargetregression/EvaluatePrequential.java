/*
 *    EvaluatePrequentialMultiTarget.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
package moa.tasks.structuredoutput.multitargetregression;

import java.io.PrintStream;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.evaluation.evaluators.LearningPerformanceEvaluator;
import moa.learners.predictors.MultiTargetRegressor;
import moa.options.ClassOption;
import moa.tasks.AbstractEvaluatePrequential;

/**
 * Task for evaluating a classifier on a stream by testing then training with
 * each example in sequence.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluatePrequential extends AbstractEvaluatePrequential<MultiTargetRegressor>
		implements MultiTargetRegressionMainTask {

	private static final long serialVersionUID = 1L;

	public EvaluatePrequential() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiTargetRegressor.class,
				"mtr.trees.ISOUPTree");
		this.evaluatorOption = new ClassOption("evaluator", 'e',
				"Multi target regression performance evaluation method.", LearningPerformanceEvaluator.class,
				"WindowMultiTargetRegressionPerformanceEvaluator");
	}

	@Override
	public String getPurposeString() {
		return "Evaluates a multi-target regressor on a stream by testing then training with each example in sequence.";
	}

	@Override
	public void printPrediction(PrintStream print, Instance inst, Prediction prediction) {
		print.println(prediction.asPredictionString() + "," + inst.outputAttributesToString());

	}

}
