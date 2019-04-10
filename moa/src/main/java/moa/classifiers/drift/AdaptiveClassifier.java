/*
 *    AdaptiveClassifier.java
 *
 *    @author Isvani Frías-Blanco
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
package moa.classifiers.drift;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import moa.options.ClassOption;

/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 *<p>See details in:<br /> Frías-Blanco, I., Verdecia-Cabrera, A., Ortiz-Díaz, A., & Carvalho, A. (2016, April). 
 * Fast adaptive stacking of ensembles.  * In Proceedings of the 31st Annual ACM Symposium on Applied Computing 
 * (pp. 929-934). ACM.</p>
 */
 
public class AdaptiveClassifier extends AbstractClassifier implements MultiClassClassifier{

    private static final long serialVersionUID = 1L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", ClassifierWithChangeDetector.class, "ClassifierWithChangeDetector");

    public Classifier mainClassifier,
            alternativeClassifier;

    protected int driftStatus;

    protected int changeDetected = 0;

    protected int warningDetected = 0;

    public static final int DDM_INCONTROL_LEVEL = 0;

    public static final int DDM_WARNING_LEVEL = 1;

    public static final int DDM_OUTCONTROL_LEVEL = 2;

    @Override
    public void resetLearningImpl() {
        this.mainClassifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        this.alternativeClassifier = null;
        this.trainingWeightSeenByModel = 0.0;
    }

    public boolean isMoreAccurate(Classifier alt, Classifier main) {
        if (alt == null) {
            return false;
        }
        if (!(alt instanceof ClassifierWithChangeDetector && main instanceof ClassifierWithChangeDetector)) {
            return false;
        }
        ClassifierWithChangeDetector aClassifier = (ClassifierWithChangeDetector) alt,
                mClassifier = (ClassifierWithChangeDetector) main;
        boolean flag = false;
        double errorAlt = aClassifier.getEstimation(),
                errorMain = mClassifier.getEstimation();
        double nAlt = aClassifier.getDelay(),
                nMain = mClassifier.getDelay();
        double m = 1.0 / nAlt + 1.0 / nMain,
                delta = 0.01,
                bound = Math.sqrt(m * Math.log(2.0 / delta) / 2.0);

        if (errorMain > errorAlt + bound) {
            flag = true;
        }

        return flag;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.mainClassifier.trainOnInstance(inst);
        if (this.alternativeClassifier != null) {
            this.alternativeClassifier.trainOnInstance(inst);
        }
        // estimating the drift level
        if (((ClassifierWithChangeDetector) this.mainClassifier).getChange()) {
            this.driftStatus = DDM_OUTCONTROL_LEVEL;
        } else if (((ClassifierWithChangeDetector) this.mainClassifier).getWarningZone()) {
            this.driftStatus = DDM_WARNING_LEVEL;
        } else {
            this.driftStatus = DDM_INCONTROL_LEVEL;
        }
        /* new 
        if (((ClassifierWithChangeDetector) this.alternativeClassifier).getChange()) {
            this.driftStatus = DDM_OUTCONTROL_LEVEL;
        }

        if (isMoreAccurate(this.alternativeClassifier, this.mainClassifier)) {
            this.mainClassifier = this.alternativeClassifier;
            this.alternativeClassifier = null;
        }
        /* end new */
        switch (driftStatus) {
            case DDM_WARNING_LEVEL:
                if (this.alternativeClassifier == null) {
                    this.alternativeClassifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
                    this.alternativeClassifier.resetLearning();
                    this.alternativeClassifier.trainOnInstance(inst);
                }
                break;

            case DDM_OUTCONTROL_LEVEL:
                if (this.alternativeClassifier == null) {
                    this.alternativeClassifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
                    this.alternativeClassifier.resetLearning();
                    this.alternativeClassifier.trainOnInstance(inst);
                }
                this.mainClassifier = this.alternativeClassifier;
                this.alternativeClassifier = null;
                break;
            case DDM_INCONTROL_LEVEL:
                this.alternativeClassifier = null;
                break;
            default:
        }
    }

    protected boolean isSignificantlyGreaterThan(double error1, int n1, double error2, int n2) {
        if (n1 == 0 || n2 == 0) {
            return false;
        }
        double m = 1.0 / n1 + 1.0 / n2,
                size = 0.05;
        return error1 - error2 > Math.sqrt(m / 2 * Math.log(1.0 / size));
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return this.mainClassifier.getModelMeasurements();
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        ((AbstractClassifier) this.mainClassifier).getModelDescription(out, indent);
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.alternativeClassifier == null) {
            return this.mainClassifier.getVotesForInstance(inst);
        } else {
            double combinedVote[] = new double[inst.numClasses()];
            double mainVotes[] = this.mainClassifier.getVotesForInstance(inst);
            double alternativeVotes[] = this.alternativeClassifier.getVotesForInstance(inst);
            for (int i = 0; i < combinedVote.length; i++) {
                combinedVote[i] = (i < mainVotes.length ? mainVotes[i] : 0) + (i < alternativeVotes.length ? alternativeVotes[i] : 0);
            }
            return combinedVote;
        }
    }
     public double getEstimation() {
        return ((ClassifierWithChangeDetector)this.mainClassifier).driftDetectionMethod.getEstimation();
    }
}
