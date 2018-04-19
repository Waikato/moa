/*
 *    ADOB.java
 *    Copyright (C) 2014 Santos, Goncalves, Barros
 *    @author Silas G. T. C. Santos (sgtcs@cin.ufpe.br)
 *            Paulo M. Goncalves Jr. (paulomgj@gmail.com)
 *            Roberto S. M. Barros (roberto@cin.ufpe.br)
 *    @version $Version: 1 $
 *
 *    Evolved from OzaBoost.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @version $Revision: 7 $
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

package moa.classifiers.meta;

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;

/**
 * Adaptable Diversity-based Online Boosting (ADOB) is a modified version
 * of the online boosting, as proposed by Oza and Russell, which is aimed
 * at speeding up the experts recovery after concept drifts.
 *
 * published as:
 *     Silas G. T. C. Santos, Paulo M. Goncalves Jr., Geyson D. S. Silva,
 *     and Roberto S. M. Barros:
 *     Speeding Up Recovery from Concept Drifts.
 *     In book: Machine Learning and Knowledge Discovery in Databases,
 *     ECML/PKDD 2014, Part III, LNCS 8726, pp. 179-194. 09/2014.
 *     DOI: 10.1007/978-3-662-44845-8_12
 */

public class ADOB extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Adaptable Diversity-based Online Boosting (ADOB)";
    }

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class,
            "drift.SingleClassifierDrift -l trees.HoeffdingTree -d ADWINChangeDetector");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models to boost.", 10, 1, Integer.MAX_VALUE);

    public FlagOption pureBoostOption = new FlagOption("pureBoost", 'p',
            "Boost with weights only; no poisson.");

    protected Classifier[] ensemble;
    
    protected int[] orderPosition;

    protected double[] scms;

    protected double[] swms;

    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        this.orderPosition = new int[this.ensemble.length];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < this.ensemble.length; i++) {
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
        for ( int i=0; i<this.ensemble.length; i++ ) {
            acc[i] = this.scms[this.orderPosition[i]] + this.swms[this.orderPosition[i]];
            if ( acc[i] != 0.0 ) {
                acc[i] = this.scms[this.orderPosition[i]] / acc[i];
            }
        }
        
	// Sort by accuracy in ascending order
        double key_acc; int key_position, j;
        for ( int i=1; i<this.ensemble.length; i++ ) {
            key_position = this.orderPosition[i];
            key_acc = acc[i];
            j = i-1;
            while ( j>=0 && acc[j]<key_acc ) {
                this.orderPosition[j+1] = this.orderPosition[j];
                acc[j+1] = acc[j];
                j--;
            }
            this.orderPosition[j+1] = key_position;
            acc[j+1] = key_acc;
        }
        
        boolean correct=false; int pos;
        double lambda_d = 1.0; int maxAcc=0, minAcc=this.ensemble.length-1;
        for (int i = 0; i < this.ensemble.length; i++) {
            if ( correct ) {
                pos = this.orderPosition[maxAcc];
                maxAcc++;
            } else {
                pos = this.orderPosition[minAcc];
                minAcc--;
            }
            
            double k;
            if ( this.pureBoostOption.isSet() ) {
                k = lambda_d;
            } else {
                k = MiscUtils.poisson(lambda_d, this.classifierRandom);
            }
            
            if (k > 0.0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[pos].trainOnInstance(weightedInst);
            }

	    // Increases or decreases lambda based on the prediction of instance
            if (this.ensemble[pos].correctlyClassifies(inst)) {
                this.scms[pos] += lambda_d;
                lambda_d *= this.trainingWeightSeenByModel / (2 * this.scms[pos]);
                correct = true;
            } else {
                this.swms[pos] += lambda_d;
                lambda_d *= this.trainingWeightSeenByModel / (2 * this.swms[pos]);
                correct = false;
            }
        }
    }

    protected double getEnsembleMemberWeight(int i) {
        if ( this.scms[i]>0.0 && this.swms[i]>0.0 ) {
            double em = this.swms[i] / (this.scms[i] + this.swms[i]);
            if (em <= 0.5) {
                double Bm = em / (1.0 - em);
                return Math.log(1.0 / Bm);
            }
        }
        return 0.0;
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
