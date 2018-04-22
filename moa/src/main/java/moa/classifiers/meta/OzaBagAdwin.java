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

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;
import com.github.javacliparser.IntOption;

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
public class OzaBagAdwin extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Bagging for evolving data streams using ADWIN.";
    }    
    
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models in the bag.", 10, 1, Integer.MAX_VALUE);

    protected Classifier[] ensemble;

    protected ADWIN[] ADError;

    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = baseLearner.copy();
        }
        this.ADError = new ADWIN[this.ensemble.length];
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ADError[i] = new ADWIN();
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        boolean Change = false;
        for (int i = 0; i < this.ensemble.length; i++) {
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            if (k > 0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }
            boolean correctlyClassifies = this.ensemble[i].correctlyClassifies(inst);
            double ErrEstim = this.ADError[i].getEstimation();
            if (this.ADError[i].setInput(correctlyClassifies ? 0 : 1)) {
                if (this.ADError[i].getEstimation() > ErrEstim) {
                    Change = true;
                }
            }
        }
        if (Change) {
            double max = 0.0;
            int imax = -1;
            for (int i = 0; i < this.ensemble.length; i++) {
                if (max < this.ADError[i].getEstimation()) {
                    max = this.ADError[i].getEstimation();
                    imax = i;
                }
            }
            if (imax != -1) {
                this.ensemble[imax].resetLearning();
                //this.ensemble[imax].trainOnInstance(inst);
                this.ADError[imax] = new ADWIN();
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        DoubleVector combinedVote = new DoubleVector();
        for (int i = 0; i < this.ensemble.length; i++) {
            DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(inst));
            if (vote.sumOfValues() > 0.0) {
                vote.normalize();
                combinedVote.addValues(vote);
            }
        }
        return combinedVote.getArrayRef();
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
                    this.ensemble != null ? this.ensemble.length : 0)};
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return this.ensemble.clone();
    }
}
