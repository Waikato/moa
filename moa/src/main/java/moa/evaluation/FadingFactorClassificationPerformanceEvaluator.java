/*
 *    FadingFactorClassificationPerformanceEvaluator.java
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

import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;

import com.github.javacliparser.FloatOption;

/**
 * Classification evaluator that updates evaluation results using a fading factor.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class FadingFactorClassificationPerformanceEvaluator extends BasicClassificationPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    public FloatOption alphaOption = new FloatOption("alpha",
            'a', "Fading factor or exponential smoothing factor", .999);

    @Override
    protected Estimator newEstimator() {
        return new FadingFactorEstimator(this.alphaOption.getValue());
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == FadingFactorClassificationPerformanceEvaluator.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    public class FadingFactorEstimator implements Estimator {

        protected double alpha;

        protected double estimation;

        protected double b;

        public FadingFactorEstimator(double a) {
            alpha = a;
            estimation = 0.0;
            b = 0.0;
        }

        @Override
        public void add(double value) {
          if (!Double.isNaN(value)) {
            estimation = alpha * estimation + value;
            b = alpha * b + 1.0;
          }
        }

        @Override
        public double estimation() {
            return b > 0.0 ? estimation / b : 0;
        }

    }

}
