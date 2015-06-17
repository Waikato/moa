/*
 *    HDDM_A_Test.java
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
 * <p>Online drift detection method based on Hoeffding's bounds. 
 * HDDM<sub><i>A</i>-test</sub> uses the average as estimator.
 * It receives as input a stream of real values and returns the estimated status
 * of the stream: STABLE, WARNING or DRIFT.</p>
 *
 * <p>I. Frias-Blanco, J. del Campo-Avila, G. Ramos-Jimenez, R. Morales-Bueno, 
 * A. Ortiz-Diaz, and Y. Caballero-Mota, Online and non-parametric drift 
 * detection methods based on Hoeffding's bound, IEEE Transactions on Knowledge
 * and Data Engineering, 2014. DOI 10.1109/TKDE.2014.2345382.</p>
 *
 * <p>Parameters:</p> <ul> <li>-d : Confidence to the drift</li><li>-w : 
 * Confidence to the warning</li><li>-t : Option to monitor error increments and
 * decrements (two-sided) or only increments (one-sided)</li>
 * </ul>
 *
 * @author Isvani Frias-Blanco (ifriasb@udg.co.cu)
 *
 */

public class HDDM_A_Test extends AbstractChangeDetector {
    
    public FloatOption driftConfidenceOption = new FloatOption("driftConfidence", 'd',
            "Confidence to the drift",
            0.001, 0, 1);
    public FloatOption warningConfidenceOption = new FloatOption("warningConfidence", 'w',
            "Confidence to the warning",
            0.005, 0, 1);
    public MultiChoiceOption oneSidedTestOption = new MultiChoiceOption(
            "typeOfTest", 't',
            "Monitors error increments and decrements (two-sided) or only increments (one-sided)", new String[]{
        "One-sided", "Two-sided"}, new String[]{
        "One-sided", "Two-sided"},
            1);

    protected int n_min = 0;
    protected double c_min = 0;
    protected int total_n = 0;
    protected double total_c = 0;
    protected int n_max = 0;
    protected double c_max = 0;
    protected double cEstimacion = 0;
    protected int nEstimacion = 0;

    public HDDM_A_Test() {
        resetLearning();
    }

    @Override
    public void input(double value) {
        total_n++;
        total_c += value;
        if (n_min == 0) {
            n_min = total_n;
            c_min = total_c;
        }
        if (n_max == 0) {
            n_max = total_n;
            c_max = total_c;
        }

        double cota = Math.sqrt(1.0 / (2 * n_min) * Math.log(1.0 / driftConfidenceOption.getValue())),
                cota1 = Math.sqrt(1.0 / (2 * total_n) * Math.log(1.0 / driftConfidenceOption.getValue()));
        if (c_min / n_min + cota >= total_c / total_n + cota1) {
            c_min = total_c;
            n_min = total_n;
        }

        cota = Math.sqrt(1.0 / (2 * n_max) * Math.log(1.0 / driftConfidenceOption.getValue()));
        if (c_max / n_max - cota <= total_c / total_n - cota1) {
            c_max = total_c;
            n_max = total_n;
        }
        if (meanIncr(c_min, n_min, total_c, total_n, driftConfidenceOption.getValue())) {
            nEstimacion = total_n - n_min;
            cEstimacion = total_c - c_min;
            n_min = n_max = total_n = 0;
            c_min = c_max = total_c = 0;
            this.isChangeDetected = true;
            this.isWarningZone = false;
        } else if (meanIncr(c_min, n_min, total_c, total_n, warningConfidenceOption.getValue())) {
            this.isChangeDetected = false;
            this.isWarningZone = true;
        } else {
            this.isChangeDetected = false;
            this.isWarningZone = false;
        }
        if (this.oneSidedTestOption.getChosenIndex() == 1 
                && meanDecr(c_max, n_max, total_c, total_n)) {
            nEstimacion = total_n - n_max;
            cEstimacion = total_c - c_max;
            n_min = n_max = total_n = 0;
            c_min = c_max = total_c = 0;
        }
        updateEstimations();
    }

    private boolean meanIncr(double c_min, int n_min, double total_c, int total_n, double confianzaCambio) {
        if (n_min == total_n) {
            return false;
        }
        double m = (double) (total_n - n_min) / n_min * (1.0 / total_n);
        double cota = Math.sqrt(m / 2 * Math.log(2.0 / confianzaCambio));
        return total_c / total_n - c_min / n_min >= cota;
    }

    private boolean meanDecr(double c_max, int n_max, double total_c, int total_n) {
        if (n_max == total_n) {
            return false;
        }
        double m = (double) (total_n - n_max) / n_max * (1.0 / total_n);
        double cota = Math.sqrt(m / 2 * Math.log(2.0 / driftConfidenceOption.getValue()));
        return c_max / n_max - total_c / total_n >= cota;
    }

    @Override
    public void resetLearning() {
        super.resetLearning();
        n_min = 0;
        c_min = 0;
        total_n = 0;
        total_c = 0;
        n_max = 0;
        c_max = 0;
        cEstimacion = 0;
        nEstimacion = 0;
    }

    protected void updateEstimations() {
        if (this.total_n >= this.nEstimacion) {
            this.cEstimacion = this.nEstimacion = 0;
            this.estimation = this.total_c / this.total_n;
            this.delay = this.total_n;
        } else {
            this.estimation = this.cEstimacion / this.nEstimacion;
            this.delay = this.nEstimacion;
        }
    }

    @Override
    public double getEstimation() {
        return this.estimation;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        resetLearning();
    }
}
