/*
 *    BOLE.java
 *    Copyright (C) 2015 Santos, Barros
 *    @authors Silas G. T. C. Santos (sgtcs@cin.ufpe.br)
 *             Roberto Souto Maior de Barros (roberto@cin.ufpe.br) 
 *    @version $Version: 1 $
 *
 *    Evolved from ADOB.java
 *    Copyright (C) 2014 Santos, Goncalves, Barros
 *    @author Silas G. T. C. Santos (sgtcs@cin.ufpe.br)
 *            Paulo M. Goncalves Jr. (paulomgj@gmail.com)
 *            Roberto S. M. Barros (roberto@cin.ufpe.br)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * Boosting-like Online Learning Ensemble (BOLE).
 * 
 * published as:
 *     Roberto Souto Maior de Barros, Silas Garrido T. de Carvalho Santos, 
 *     and Paulo Mauricio Goncalves Jr.: 
 *     A Boosting-like Online Learning Ensemble. 
 *     In Proceedings of IEEE International Joint Conference 
 *     on Neural Networks (IJCNN), Vancouver, Canada, 2016.
 *     DOI: 10.1109/IJCNN.2016.7727427
 */

package moa.classifiers.meta;

import moa.classifiers.MultiClassClassifier;
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

public class BOLE extends AbstractClassifier implements MultiClassClassifier {
    
    private static final long serialVersionUID = 1L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class,
            "drift.SingleClassifierDrift -l trees.HoeffdingTree -d (DDM -n 7 -w 1.2 -o 1.95)");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models to boost.", 10, 1, Integer.MAX_VALUE);

    public FlagOption pureBoostOption = new FlagOption("pureBoost", 
            'p', "Boost with weights only; no poisson.");

    public FlagOption breakVotesOption = new FlagOption("breakVotes",
            'b', "Break Votes? unchecked=no, checked=yes");

    public FloatOption errorBoundOption = new FloatOption("errorBound", 
            'e', "Error bound percentage for allowing experts to vote.",
            0.5, 0.1, 1.0);

    public FloatOption weightShiftOption = new FloatOption("weightShift", 
            'w', "Weight shift associated with the error bound.",
            0.0, 0.0, 5.0);
    
    private double memberWeight;
    private double key_acc; 
    private int key_position, i, j;
    private int maxAcc, minAcc, pos;
    private double lambda_d, k;
    private boolean correct, okay;
    private double em, Bm;

    protected Classifier[] ensemble;
    protected int[] orderPosition;
    protected double[] scms;
    protected double[] swms;

    @Override
    public String getPurposeString() {
        return "Boosting-like Online Learning Ensemble (BOLE)";
    }

    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        this.orderPosition = new int[this.ensemble.length];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = baseLearner.copy();
            this.orderPosition[i] = i;
        }
        this.scms = new double[this.ensemble.length];
        this.swms = new double[this.ensemble.length];
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
	// Calculates current accuracy of experts
        double[] acc = new double[this.ensemble.length];
        for (i = 0; i < this.ensemble.length; i++) {
            acc[i] = this.scms[this.orderPosition[i]] + this.swms[this.orderPosition[i]];
            if (acc[i] != 0.0) {
                acc[i] = this.scms[this.orderPosition[i]] / acc[i];
            }
        }
        
	// Sort by accuracy in ascending order
        for (i = 1; i < this.ensemble.length; i++) {
            key_position = this.orderPosition[i];
            key_acc = acc[i];
            j = i - 1;
            while ( (j >=0) && (acc[j] < key_acc) ) {
                this.orderPosition[j+1] = this.orderPosition[j];
                acc[j+1] = acc[j];
                j--;
            }
            this.orderPosition[j+1] = key_position;
            acc[j+1] = key_acc;
        }
        
        correct = false; 
        maxAcc = 0; 
        minAcc = this.ensemble.length - 1; 
        lambda_d = 1.0; 
        for (i = 0; i < this.ensemble.length; i++) {
            if (correct) {
                pos = this.orderPosition[maxAcc];
                maxAcc++;
            } else {
                pos = this.orderPosition[minAcc];
                minAcc--;
            }
            
            if (this.pureBoostOption.isSet())
                k = lambda_d;
            else
                k = MiscUtils.poisson(lambda_d, this.classifierRandom);
            
            if (k > 0.0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[pos].trainOnInstance(weightedInst);
            }

	    // Increases or decreases lambda based on the prediction of instance
            if (this.ensemble[pos].correctlyClassifies(inst)) {
                this.scms[pos] += lambda_d;
                lambda_d *= (this.trainingWeightSeenByModel / (2 * this.scms[pos]));
                correct = true;
            } else {
                this.swms[pos] += lambda_d; 
                lambda_d *= (this.trainingWeightSeenByModel / (2 * this.swms[pos]));
                correct = false;
            }
        }
    }

    protected double getEnsembleMemberWeight(int i) {
        if ( (this.scms[i] > 0.0) && (this.swms[i] > 0.0) ) { 
            em = this.swms[i] / (this.scms[i] + this.swms[i]);
            if (em <= this.errorBoundOption.getValue()) {
                Bm = em / (1.0 - em);
                okay = true;
                return Math.log(1.0 / Bm); 
            } 

        }
        okay = false;
        return 0.0;
    }

    public double[] getVotesForInstance(Instance inst) {
        DoubleVector combinedVote = new DoubleVector(); 
        for (i = 0; i < this.ensemble.length; i++) {
            memberWeight = getEnsembleMemberWeight(i) + this.weightShiftOption.getValue(); 
            if (okay) {
                DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(inst));
                if (vote.sumOfValues() > 0.0) {
                    vote.normalize();
                    vote.scaleValues(memberWeight);
                    combinedVote.addValues(vote);
                } 
            } 
            else if (this.breakVotesOption.isSet()) {
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
