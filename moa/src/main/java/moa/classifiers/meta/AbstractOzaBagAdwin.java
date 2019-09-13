/*
 *    OzaBagAdwin.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
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
package moa.classifiers.meta;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.AbstractClassifier;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.learners.Classifier;
import moa.learners.InstanceLearner;

/**
 * Bagging for evolving data streams using ADWIN.
 *
 * <p>ADWIN is a change detector and estimator that solves in
 * a well-speciﬁed way the problem of tracking the average of
 * a stream of bits or real-valued numbers. ADWIN keeps a
 * variable-length window of recently seen items, with the property
 * that the window has the maximal length statistically consistent
 * with the hypothesis “there has been no change in the average value
 * inside the window”.<br />
 * More precisely, an older fragment of the window is dropped if and only
 * if there is enough evidence that its average value differs from that of
 * the rest of the window. This has two consequences: one, that change
 * reliably declared whenever the window shrinks; and two, that at any time
 * the average over the existing window can be reliably taken as an estimation
 * of the current average in the stream (barring a very small or very recent
 * change that is still not statistically visible). A formal and quantitative
 * statement of these two points (a theorem) appears in<p>
 *
 * Albert Bifet and Ricard Gavaldà. Learning from time-changing data
 * with adaptive windowing. In SIAM International Conference on Data Mining,
 * 2007.</p>
 * <p>ADWIN is parameter- and assumption-free in the sense that it automatically
 * detects and adapts to the current rate of change. Its only parameter is a
 * conﬁdence bound δ, indicating how conﬁdent we want to be in the algorithm’s
 * output, inherent to all algorithms dealing with random processes. Also
 * important, ADWIN does not maintain the window explicitly, but compresses it
 * using a variant of the exponential histogram technique. This means that it
 * keeps a window of length W using only O(log W) memory and O(log W) processing
 * time per item.<br />
 * ADWIN Bagging is the online bagging method of Oza and Rusell with the
 * addition of the ADWIN algorithm as a change detector and as an estimator for
 * the weights of the boosting method. When a change is detected, the worst
 * classiﬁer of the ensemble of classiﬁers is removed and a new classiﬁer is
 * added to the ensemble.</p>
 * <p>See details in:<br />
 * [BHPKG] Albert Bifet, Geoff Holmes, Bernhard Pfahringer, Richard Kirkby,
 * and Ricard Gavaldà . New ensemble methods for evolving data streams.
 * In 15th ACM SIGKDD International Conference on Knowledge Discovery and
 * Data Mining, 2009.</p>
 * <p>Example:</p>
 * <code>OzaBagAdwin -l HoeffdingTreeNBAdaptive -s 10</code>
 * <p>Parameters:</p> <ul>
 * <li>-l : Classiﬁer to train</li>
 * <li>-s : The number of models in the bag</li> </ul>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractOzaBagAdwin<MLTask extends InstanceLearner> extends AbstractOzaBag<MLTask> implements CapabilitiesHandler {

    private static final long serialVersionUID = 1L;

    public AbstractOzaBagAdwin(Class<MLTask> MLTask, String defaultCLIString) {
    	super(MLTask, defaultCLIString);
    }

    @Override
    public String getPurposeString() {
        return "Bagging for evolving data streams using ADWIN.";
    }    
    
    protected ADWIN[] ADError;

    @Override
    public void resetLearningImpl() {
    	super.resetLearningImpl();
        this.ADError = new ADWIN[this.ensemble.size()];
        for (int i = 0; i < this.ensemble.size(); i++) {
            this.ADError[i] = new ADWIN();
        }
    }

    public abstract double getAdwinError(Instance inst, int i);

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        boolean Change = false;
        for (int i = 0; i < this.ensemble.size(); i++) {
            int k = MiscUtils.poisson(6.0, this.classifierRandom);
            if (k > 0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble.get(i).trainOnInstance(weightedInst);
            }
            double ErrEstim = this.ADError[i].getEstimation();
            if (this.ADError[i].setInput(this.getAdwinError(inst, i))) {
                if (this.ADError[i].getEstimation() > ErrEstim) {
                    Change = true;
                }
            }
        }
        if (Change) {
            double max = 0.0;
            int imax = -1;
            for (int i = 0; i < this.ensemble.size(); i++) {
                if (max < this.ADError[i].getEstimation()) {
                    max = this.ADError[i].getEstimation();
                    imax = i;
                }
            }
            if (imax != -1) {
                this.ensemble.get(imax).resetLearning();
                this.ensemble.get(imax).setRandomSeed(this.ensemble.get(imax).getRandomSeed() * 2);
                this.ensemble.get(imax).trainOnInstance(inst);
                this.ADError[imax] = new ADWIN();
            }
        }
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{new Measurement("ensemble size",
                    this.ensemble != null ? this.ensemble.size() : 0)};
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == OzaBagAdwin.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
