/*
 *    LearnModelMultiTarget.java
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
package moa.tasks.structuredoutput.multitargetregression;

import moa.learners.MultiTargetRegressor;
import moa.options.ClassOption;
import moa.tasks.AbstractLearnModel;

/**
 * Task for learning a model without any evaluation.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class LearnModel extends AbstractLearnModel<MultiTargetRegressor> implements MultiTargetRegressionMainTask {

    @Override
    public String getPurposeString() {
        return "Learns a multi-target regression model from a stream.";
    }

    private static final long serialVersionUID = 1L;

    public LearnModel() {
		this.learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiTargetRegressor.class, "moa.classifiers.mtr.trees.ISOUPTree");
	}
    
}
