/*
 *    OzaBoostAdwin.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
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
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Boosting for evolving data streams using ADWIN.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class OzaBoostAdwin extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Boosting for evolving data streams using ADWIN.";
    }
        
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models to boost.", 10, 1, Integer.MAX_VALUE);

    public FlagOption pureBoostOption = new FlagOption("pureBoost", 'p',
            "Boost with weights only; no poisson.");

    public FloatOption deltaAdwinOption = new FloatOption("deltaAdwin", 'a',
            "Delta of Adwin change detection", 0.002, 0.0, 1.0);

    public FlagOption outputCodesOption = new FlagOption("outputCodes", 'o',
            "Use Output Codes to use binary classifiers.");

    public FlagOption sammeOption = new FlagOption("same", 'e',
            "Use Samme Algorithm.");

    protected Classifier[] ensemble;

    protected double[] scms;

    protected double[] swms;

    protected ADWIN[] ADError;

    protected int numberOfChangesDetected;

    protected int[][] matrixCodes;

    protected boolean initMatrixCodes = false;

    protected double logKm1 = 0.0;

    protected int Km1 = 1;

    protected boolean initKm1 = false;

    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = baseLearner.copy();
        }
        this.scms = new double[this.ensemble.length];
        this.swms = new double[this.ensemble.length];
        this.ADError = new ADWIN[this.ensemble.length];
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ADError[i] = new ADWIN((double) this.deltaAdwinOption.getValue());
        }
        this.numberOfChangesDetected = 0;
        if (this.outputCodesOption.isSet()) {
            this.initMatrixCodes = true;
        }
        if (this.sammeOption.isSet()) {
            this.initKm1 = true;
        }

    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        int numClasses = inst.numClasses();
        // Set log (k-1) and (k-1) for SAMME Method
        if (this.sammeOption.isSet()) {
            this.Km1 = numClasses - 1;
            this.logKm1 = Math.log(this.Km1);
            this.initKm1 = false;
        }
        //Output Codes
        if (this.initMatrixCodes == true) {

            this.matrixCodes = new int[this.ensemble.length][inst.numClasses()];
            for (int i = 0; i < this.ensemble.length; i++) {
                int numberOnes;
                int numberZeros;

                do { // until we have the same number of zeros and ones
                    numberOnes = 0;
                    numberZeros = 0;
                    for (int j = 0; j < numClasses; j++) {
                        int result = 0;
                        if (j == 1 && numClasses == 2) {
                            result = 1 - this.matrixCodes[i][0];
                        } else {
                            result = (this.classifierRandom.nextBoolean() ? 1 : 0);
                        }
                        this.matrixCodes[i][j] = result;
                        if (result == 1) {
                            numberOnes++;
                        } else {
                            numberZeros++;
                        }
                    }
                } while ((numberOnes - numberZeros) * (numberOnes - numberZeros) > (this.ensemble.length % 2));

            }
            this.initMatrixCodes = false;
        }


        boolean Change = false;
        double lambda_d = 1.0;
        Instance weightedInst = (Instance) inst.copy();
        for (int i = 0; i < this.ensemble.length; i++) {
            double k = this.pureBoostOption.isSet() ? lambda_d : MiscUtils.poisson(lambda_d * this.Km1, this.classifierRandom);
            if (k > 0.0) {
                if (this.outputCodesOption.isSet()) {
                    weightedInst.setClassValue((double) this.matrixCodes[i][(int) inst.classValue()]);
                }
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }
            boolean correctlyClassifies = this.ensemble[i].correctlyClassifies(weightedInst);
            if (correctlyClassifies) {
                this.scms[i] += lambda_d;
                lambda_d *= this.trainingWeightSeenByModel / (2 * this.scms[i]);
            } else {
                this.swms[i] += lambda_d;
                lambda_d *= this.trainingWeightSeenByModel / (2 * this.swms[i]);
            }

            double ErrEstim = this.ADError[i].getEstimation();
            if (this.ADError[i].setInput(correctlyClassifies ? 0 : 1)) {
                if (this.ADError[i].getEstimation() > ErrEstim) {
                    Change = true;
                }
            }
        }
        if (Change) {
            numberOfChangesDetected++;
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
                this.ADError[imax] = new ADWIN((double) this.deltaAdwinOption.getValue());
                this.scms[imax] = 0;
                this.swms[imax] = 0;
            }
        }
    }

    protected double getEnsembleMemberWeight(int i) {
        double em = this.swms[i] / (this.scms[i] + this.swms[i]);
        if ((em == 0.0) || (em > 0.5)) {
            return this.logKm1;
        }
        return Math.log((1.0 - em) / em) + this.logKm1;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.outputCodesOption.isSet()) {
            return getVotesForInstanceBinary(inst);
        }
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

    public double[] getVotesForInstanceBinary(Instance inst) {
        double combinedVote[] = new double[(int) inst.numClasses()];
        Instance weightedInst = (Instance) inst.copy();
        if (this.initMatrixCodes == false) {
            for (int i = 0; i < this.ensemble.length; i++) {
                //Replace class by OC
                weightedInst.setClassValue((double) this.matrixCodes[i][(int) inst.classValue()]);

                double vote[];
                vote = this.ensemble[i].getVotesForInstance(weightedInst);
                //Binary Case
                int voteClass = 0;
                if (vote.length == 2) {
                    voteClass = (vote[1] > vote[0] ? 1 : 0);
                }
                //Update votes
                for (int j = 0; j < inst.numClasses(); j++) {
                    if (this.matrixCodes[i][j] == voteClass) {
                        combinedVote[j] += getEnsembleMemberWeight(i);
                    }
                }
            }
        }
        return combinedVote;
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
                    this.ensemble != null ? this.ensemble.length : 0),
                    new Measurement("change detections", this.numberOfChangesDetected)
                };
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return this.ensemble.clone();
    }
}
