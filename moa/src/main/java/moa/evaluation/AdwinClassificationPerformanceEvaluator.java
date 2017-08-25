/*
 *    WindowClassificationPerformanceEvaluator.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
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

import com.github.javacliparser.IntOption;
import moa.classifiers.core.driftdetection.ADWIN;

/**
 * Classification evaluator that updates evaluation results using an adaptive sliding
 * window.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class AdwinClassificationPerformanceEvaluator extends BasicClassificationPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    protected Estimator newEstimator() {
        return new AdwinEstimator();
    }

    public class AdwinEstimator implements Estimator {

        protected ADWIN adwin;

        public AdwinEstimator() {
            this.adwin = new ADWIN();
        }

        @Override
        public void add(double value) {
            this.adwin.setInput(value);
        }

        @Override
        public double estimation() {
            return this.adwin.getEstimation();
        }

    }

}
