/*
 *    EvaluateModelRegression.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.predictions.Prediction;

import moa.core.Example;
import moa.core.ObjectRepository;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.RegressionPerformanceEvaluator;
import moa.learners.Classifier;
import moa.learners.MultiTargetRegressor;
import moa.learners.Regressor;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;

/**
 * Task for evaluating a static model on a stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class EvaluateModelRegression extends AbstractEvaluateModel<Regressor> implements RegressionMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a static model on a stream.";
    }

    private static final long serialVersionUID = 1L;

    public EvaluateModelRegression() {
		super(Regressor.class, "moa.classifiers.trees.FIMTDD");
	}

}
