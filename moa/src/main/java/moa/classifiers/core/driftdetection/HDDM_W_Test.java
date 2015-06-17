/*
 *    HDDM_W_Test.java
 *
 *    @author Isvani Frias-Blanco (ifriasb@udg.co.cu)
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
 *    
 *    
 */
package moa.classifiers.core.driftdetection;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/**
 * <p>Online drift detection method based on McDiarmid's bounds. 
 * HDDM<sub><i>W</i>-test</sub> uses the EWMA statistic as estimator.
 * It receives as input a stream of real values and returns the estimated status
 * of the stream: STABLE, WARNING or DRIFT.</p>
 *
 * <p>I. Frias-Blanco, J. del Campo-Avila, G. Ramos-Jimenez, R. Morales-Bueno, 
 * A. Ortiz-Diaz, and Y. Caballero-Mota, Online and non-parametric drift 
 * detection methods based on Hoeffding's bound, IEEE Transactions on Knowledge
 * and Data Engineering, 2014. DOI 10.1109/TKDE.2014.2345382.</p>
 *
 * <p>Parameters:</p> <ul> <li>-d : Confidence to the drift</li><li>-w : 
 * Confidence to the warning</li><li>-m : Controls how much weight is given to 
 * more recent data compared to older data. Smaller values mean less weight 
 * given to recent data</li><li>-t : Option to monitor error increments and 
 * decrements (two-sided) or only increments (one-sided)</li>
 * </ul>
 *
 * @author Isvani Frias-Blanco (ifriasb@udg.co.cu)
 *
 */

public class HDDM_W_Test extends AbstractChangeDetector {

    protected static final long serialVersionUID = 1L;
    public FloatOption driftConfidenceOption = new FloatOption("driftConfidence", 'd',
            "Confidence to the drift",
            0.001, 0, 1);
    public FloatOption warningConfidenceOption = new FloatOption("warningConfidence", 'w',
            "Confidence to the warning",
            0.005, 0, 1);
    public FloatOption lambdaOption = new FloatOption("lambda",
            'm', "Controls how much weight is given to more recent data compared to older data. Smaller values mean less weight given to recent data.",
            0.050, 0, 1);
    public MultiChoiceOption oneSidedTestOption = new MultiChoiceOption(
            "typeOfTest", 't',
            "Monitors error increments and decrements (two-sided) or only increments (one-sided)", new String[]{
        "One-sided", "Two-sided"}, new String[]{
        "One-sided", "Two-sided"},
            0);
    

    public static class SampleInfo {

        private static final long serialVersionUID = 1L;
        public double EWMA_Estimator;
        public double independentBoundedConditionSum;

        public SampleInfo() {
            this.EWMA_Estimator = -1.0;
        }
    }
    private static SampleInfo sample1_IncrMonitoring,
            sample2_IncrMonitoring,
            sample1_DecrMonitoring,
            sample2_DecrMonitoring,
            total;
    protected double incrCutPoint, decrCutPoint;
    protected double lambda;
    protected double warningConfidence;
    protected double driftConfidence;
    protected boolean oneSidedTest;
    protected int width;

    public HDDM_W_Test() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        super.resetLearning();
        this.total = new SampleInfo();
        this.sample1_DecrMonitoring = new SampleInfo();
        this.sample1_IncrMonitoring = new SampleInfo();
        this.sample2_DecrMonitoring = new SampleInfo();
        this.sample2_IncrMonitoring = new SampleInfo();
        this.incrCutPoint = Double.MAX_VALUE;
        this.decrCutPoint = Double.MIN_VALUE;
        this.lambda = this.lambdaOption.getValue();
        this.driftConfidence = this.driftConfidenceOption.getValue();
        this.warningConfidence = this.warningConfidenceOption.getValue();
        this.oneSidedTest = this.oneSidedTestOption.getChosenIndex() == 0;
        this.width = 0;
        this.delay = 0;
    }

    public void input(boolean prediction) {
        double value = prediction == false ? 1.0 : 0.0;
        input(value);
    }

    @Override
    public void input(double value) {
        double auxDecayRate = 1.0 - lambda;
        this.width++;
        if (total.EWMA_Estimator < 0) {
            total.EWMA_Estimator = value;
            total.independentBoundedConditionSum = 1;
        } else {
            total.EWMA_Estimator = lambda * value + auxDecayRate * total.EWMA_Estimator;
            total.independentBoundedConditionSum = lambda * lambda + auxDecayRate * auxDecayRate * total.independentBoundedConditionSum;
        }
        updateIncrStatistics(value, driftConfidence);
        if (monitorMeanIncr(value, driftConfidence)) {
            resetLearning();
            this.isChangeDetected = true;
            this.isWarningZone = false;
        } else if (monitorMeanIncr(value, warningConfidence)) {
            this.isChangeDetected = false;
            this.isWarningZone = true;
        } else {
            this.isChangeDetected = false;
            this.isWarningZone = false;
        }
        updateDecrStatistics(value, driftConfidence);
        if (!oneSidedTest && monitorMeanDecr(value, driftConfidence)) {
            resetLearning();
        }
        this.estimation = this.total.EWMA_Estimator;
    }

    public boolean detectMeanIncrement(SampleInfo sample1, SampleInfo sample2, double confidence) {
        if (sample1.EWMA_Estimator < 0 || sample2.EWMA_Estimator < 0) {
            return false;
        }
        double bound = Math.sqrt((sample1.independentBoundedConditionSum + sample2.independentBoundedConditionSum) * Math.log(1 / confidence) / 2);
        return sample2.EWMA_Estimator - sample1.EWMA_Estimator > bound;
    }
    
    void updateIncrStatistics(double valor, double confidence) {
            double auxDecay = 1.0 - lambda;
            double bound = Math.sqrt(total.independentBoundedConditionSum * Math.log(1.0 / driftConfidence) / 2);

            if (total.EWMA_Estimator + bound < incrCutPoint) {
                incrCutPoint = total.EWMA_Estimator + bound;
                sample1_IncrMonitoring.EWMA_Estimator = total.EWMA_Estimator;
                sample1_IncrMonitoring.independentBoundedConditionSum = total.independentBoundedConditionSum;
                sample2_IncrMonitoring = new SampleInfo();
                this.delay = 0;
            } else {
                this.delay++;
                if (sample2_IncrMonitoring.EWMA_Estimator < 0) {
                    sample2_IncrMonitoring.EWMA_Estimator = valor;
                    sample2_IncrMonitoring.independentBoundedConditionSum = 1;
                } else {
                    sample2_IncrMonitoring.EWMA_Estimator = lambda * valor + auxDecay * sample2_IncrMonitoring.EWMA_Estimator;
                    sample2_IncrMonitoring.independentBoundedConditionSum = lambda * lambda + auxDecay * auxDecay * sample2_IncrMonitoring.independentBoundedConditionSum;
                }
            }
        
    }

    protected boolean monitorMeanIncr(double valor, double confidence) {
        return detectMeanIncrement(sample1_IncrMonitoring, sample2_IncrMonitoring, confidence);
    }
    
    void updateDecrStatistics(double valor, double confidence) {
            double auxDecay = 1.0 - lambda;
            double epsilon = Math.sqrt(total.independentBoundedConditionSum * Math.log(1.0 / driftConfidence) / 2);

            if (total.EWMA_Estimator - epsilon > decrCutPoint) {
                decrCutPoint = total.EWMA_Estimator - epsilon;
                sample1_DecrMonitoring.EWMA_Estimator = total.EWMA_Estimator;
                sample1_DecrMonitoring.independentBoundedConditionSum = total.independentBoundedConditionSum;
                sample2_DecrMonitoring = new SampleInfo();
            } else {
                if (sample2_DecrMonitoring.EWMA_Estimator < 0) {
                    sample2_DecrMonitoring.EWMA_Estimator = valor;
                    sample2_DecrMonitoring.independentBoundedConditionSum = 1;
                } else {
                    sample2_DecrMonitoring.EWMA_Estimator = lambda * valor + auxDecay * sample2_DecrMonitoring.EWMA_Estimator;
                    sample2_DecrMonitoring.independentBoundedConditionSum = lambda * lambda + auxDecay * auxDecay * sample2_DecrMonitoring.independentBoundedConditionSum;
                }
            }
    }

    protected boolean monitorMeanDecr(double valor, double confidence) {
        return detectMeanIncrement(sample2_DecrMonitoring, sample1_DecrMonitoring, confidence);
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        resetLearning();
    }

    public void getDescription(StringBuilder sb, int indent) {
    }
}
