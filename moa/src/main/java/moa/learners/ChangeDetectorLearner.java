/*
 *    ChangeDetectorLearner.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
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
 */
package moa.learners;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.core.Measurement;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.options.ClassOption;

/**
 * Class for detecting concept drift and to be used as a learner.<p>data
 *
 * Valid options are:<p>
 *
 * -l classname <br> Specify the full class name of a classifier as the basis
 * for the concept drift classifier.<p>
 *
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class ChangeDetectorLearner extends AbstractClassifier {

    private static final long serialVersionUID = 1L;

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'd',
            "Drift detection method to use.", ChangeDetector.class, "DDM");

    protected ChangeDetector driftDetectionMethod;

    @Override
    public void resetLearningImpl() {
        this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.driftDetectionMethod.input(inst.value(0));

    }

    public double[] getVotesForInstance(Instance inst) {
        return this.driftDetectionMethod.getOutput();
    }

    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        //((AbstractClassifier) this.classifier).getModelDescription(out, indent);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        //return ((AbstractClassifier) this.classifier).getModelMeasurementsImpl();
        return new Measurement[0];
    }
}
