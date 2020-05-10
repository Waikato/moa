/*
 *    NoChange.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *    @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
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
package moa.classifiers.functions;

import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * NoChange class classifier. It always predicts the last class seen.
 *
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision: 1 $
 */
public class NoChange extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Weather Forecast class classifier: always predicts the last class seen.";
    }

    protected double lastSeenClass;

    @Override
    public void resetLearningImpl() {
        this.lastSeenClass = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.lastSeenClass = inst.classValue();
    }

    public double[] getVotesForInstance(Instance i) {
        double[] votes = new double[i.numClasses()];
        votes[(int) lastSeenClass] = 1.0;
        return votes;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
       
    }

    public boolean isRandomizable() {
        return false;
    }
    
    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == NoChange.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
