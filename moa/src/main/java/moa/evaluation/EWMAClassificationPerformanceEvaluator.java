/*
 *    EWMAClassificationPerformanceEvaluator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
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
package moa.evaluation;

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;

import com.github.javacliparser.FloatOption;

import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.core.Utils;

/**
 * Classification evaluator that updates evaluation results using an Exponential Weighted Moving Average.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EWMAClassificationPerformanceEvaluator extends BasicClassificationPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    public FloatOption alphaOption = new FloatOption("alpha",
            'a', "Fading factor or exponential smoothing factor", .01);

    @Override
    protected Estimator newEstimator() {
        return new EWMAEstimator(this.alphaOption.getValue());
    }

    protected Estimator weightCorrect;

    public class EWMAEstimator implements Estimator {

        protected double alpha;

        protected double estimation;

        public EWMAEstimator(double a) {
            alpha = a;
            estimation = 0;
        }

        @Override
        public void add(double value) {
            estimation += alpha * (value - estimation);
        }

        @Override
        public double estimation() {
            return estimation;
        }

    }
}
