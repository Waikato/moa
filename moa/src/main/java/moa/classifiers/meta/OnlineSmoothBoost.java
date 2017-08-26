/*
 *    OnlineSmoothBoost.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
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
package moa.classifiers.meta;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;


/**
 * Incremental on-line boosting with Theoretical Justifications of Shang-Tse Chen,
 * Hsuan-Tien Lin and Chi-Jen Lu.
 *
 * <p>See details in:<br /> </p>
 *
 * <p>Parameters:</p> <ul> <li>-l : Classiﬁer to train</li> <li>-s : The number
 * of models to boost</li> 
 * </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class OnlineSmoothBoost extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Incremental on-line boosting of Shang-Tse Chen, Hsuan-Tien Lin and Chi-Jen Lu.";
    }

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models to boost.", 10, 1, Integer.MAX_VALUE);

    //public FlagOption pureBoostOption = new FlagOption("pureBoost", 'p',
    //        "Boost with weights only; no poisson.");
    
        public FloatOption gammaOption = new FloatOption("gamma",
            'g',
            "The value of the gamma parameter.",
            0.1, 0.0, 1.0);

    protected Classifier[] ensemble;

    protected double[] alpha;
    
    protected double gamma;
    
    protected double theta;

    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        this.alpha = new double[this.ensemble.length];
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = baseLearner.copy();
            this.alpha[i] = 1.0/ (double) this.ensemble.length;
        }
        this.gamma = this.gammaOption.getValue();
       this.theta = this.gamma/(2.0+this.gamma);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        double zt = 0.0;
        double weight = 1.0;
        for (int i = 0; i < this.ensemble.length; i++) {
            zt += (this.ensemble[i].correctlyClassifies(inst) ? 1 : -1) - theta;
                    //normalized_predict(ex.x) * ex.y - theta;
            Instance weightedInst = (Instance) inst.copy();
            weightedInst.setWeight(weight);
            this.ensemble[i].trainOnInstance(weightedInst);
            weight = (zt<=0)? 1.0 : Math.pow(1.0-gamma, zt/2.0);
        }

    }

    protected double getEnsembleMemberWeight(int i) {
        return this.alpha[i];
    }

    public double[] getVotesForInstance(Instance inst) {
               
        DoubleVector combinedVote = new DoubleVector();
        for (int i = 0; i < this.ensemble.length; i++) {
            double memberWeight = getEnsembleMemberWeight(i);
            if (memberWeight > 0.0) {
                DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(inst));
                if (vote.sumOfValues() > 0.0) {
                    vote.normalize();
                    vote.scaleValues(memberWeight);
                    combinedVote.addValues(vote);
                }
            } else {
                break;
            }
        }
        return combinedVote.getArrayRef();
    }

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
