/*
 *    EvaluateModelMultiTarget.java
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

import moa.learners.MultiTargetRegressor;
import moa.options.ClassOption;

/**
 * Task for evaluating a static model on a stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class EvaluateModelMultiTargetRegression extends AbstractEvaluateModel<MultiTargetRegressor> implements MultiTargetRegressionMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a static multi-target regression model on a stream.";
    }
    
    public EvaluateModelMultiTargetRegression() {
		super(MultiTargetRegressor.class, "moa.classifiers.mtr.trees.ISOUPTree");
	}

	private static final long serialVersionUID = 1L;

}
