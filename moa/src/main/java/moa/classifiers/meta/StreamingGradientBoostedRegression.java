/*
 *    StreamingGradientBoostedTrees.java
 *    Copyright (C) 2025 University of Waikato, Hamilton, New Zealand
 *    @author Nuwan Gunasekara (nuwan.gunasekara@hh.se)
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
package moa.classifiers.meta;

import moa.classifiers.Regressor;


/**
 * Gradient boosted bagging for evolving data stream regression
 *
 * <p>Streaming Gradient Boosted Regression (SGBR)
 * was developed to adapt gradient boosting for streaming regression using Streaming Gradient Boosted Trees (SGBT).
 * A variant called SGB(Oza), which uses OzaBag bagging regressors as base learners,
 * outperforms existing state-of-the-art methods in both accuracy and efficiency across various drift scenarios.</p>
 *
 * <p>See details in:<br> Nuwan Gunasekara, Bernhard Pfahringer, Heitor Murilo Gomes, Albert Bifet.
 * Gradient boosted bagging for evolving data stream regression.
 * Data Mining and Knowledge Discovery, Springer, 2025.
 * <a href="https://doi.org/10.1007/s10618-025-01147-x">DOI</a>. </p>
 *
 * <p>Parameters:</p> <ul>
 * <li>Same as in SGBT</li>
 * <li>-l : The base learner option is used to employ bagging base learners instead of trees.</li>
 * </ul>
 *
 * @author Nuwan Gunasekara (nuwan dot gunasekara at hh dot se)
 * @version $Revision: 1 $
 */
public class StreamingGradientBoostedRegression extends StreamingGradientBoostedTrees implements Regressor {

    private static final long serialVersionUID = 1L;

    public StreamingGradientBoostedRegression(){
        super();
        // initialise an object with correct default values
        super.baseLearnerOption.setValueViaCLIString("meta.OzaBag -s 10 -l (trees.FIMTDD -s VarianceReductionSplitCriterion -g 50 -c 0.01 -e)");
        super.learningRateOption.setValue(1.0);
        super.numberOfboostingIterations.setValue(10);

        // to support reset to defaults in GUI
        super.baseLearnerOption.setDefaultCLIString("meta.OzaBag -s 10 -l (trees.FIMTDD -s VarianceReductionSplitCriterion -g 50 -c 0.01 -e)");
        super.learningRateOption.setDefault(1.0);
        super.numberOfboostingIterations.setDefaultValue(10);

    }

}
