/*
 *   ClassifierWithChangeDetector.java
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.core.Measurement;
import moa.options.ClassOption;
import weka.core.Utils;
/**
 *
 * @author Isvani Frías Blanco (ifriasb at hotmail dot com)
 */
public class ClassifierWithChangeDetector extends AbstractClassifier implements MultiClassClassifier{

    private static final long serialVersionUID = 1L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "bayes.NaiveBayes");

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'd',
            "Drift detection method to use.", ChangeDetector.class, "HDDM_A_Test");

    public Classifier classifier;

    public ChangeDetector driftDetectionMethod;

    public ChangeDetector getDriftDetectionMethod() {
        return driftDetectionMethod;
    }

    @Override
    public void resetLearningImpl() {
        this.classifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        this.classifier.resetLearning();
        this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy();
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        boolean correctlyClassifies = this.classifier.correctlyClassifies(inst);
        this.driftDetectionMethod.input(correctlyClassifies ? 0 : 1);
        this.classifier.trainOnInstance(inst);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.add(new Measurement("Error estimates", this.driftDetectionMethod.getEstimation()));
        Measurement[] modelMeasurements = ((AbstractClassifier) this.classifier).getModelMeasurements();
        if (modelMeasurements != null) {
            measurementList.addAll(Arrays.asList(modelMeasurements));
        }
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        ((AbstractClassifier) this.classifier).getModelDescription(out, indent);
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        double[] votes = this.classifier.getVotesForInstance(inst);
        double distSum = Utils.sum(votes);
        if (distSum * this.driftDetectionMethod.getEstimation() > 0.0) {
            Utils.normalize(votes, distSum * this.driftDetectionMethod.getEstimation()); //Adding weight
        }
        return votes;
    }

    public boolean getChange() {
        return this.driftDetectionMethod.getChange();
    }

    public boolean getWarningZone() {
        return this.driftDetectionMethod.getWarningZone();
    }

    public double getEstimation() {
        return this.driftDetectionMethod.getEstimation();
    }

    public double getDelay() {
        return this.driftDetectionMethod.getDelay();
    }

    public double[] getOutput() {
        return this.driftDetectionMethod.getOutput();
    }

    public ChangeDetector changeDetectorCopy() {
        return this.driftDetectionMethod.copy();
    }

}
