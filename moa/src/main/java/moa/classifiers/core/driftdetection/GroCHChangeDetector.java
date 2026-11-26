/*
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

package moa.classifiers.core.driftdetection;

import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * Adapter around MOA {@link ChangeDetector} implementations that normalizes
 * warning-edge detection and isolates dual-detector behavior (e.g., {@link ADDM})
 * from meta-classifiers such as {@code GroCH}.
 */
public class GroCHChangeDetector extends AbstractChangeDetector {

    private static final long serialVersionUID = 2461734596321887617L;

    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'x',
            "Change detector for drifts and its parameters", ChangeDetector.class, "ADDM -a 1.0E-5 -p 1.0E-4");

    private ChangeDetector driftDetectionMethod;
    private boolean isWarningDetected;

    public GroCHChangeDetector() {
        this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy();
    }

    public GroCHChangeDetector(ClassOption changeDetectorOption, ChangeDetector changeDetector) {
        this.driftDetectionMethodOption = changeDetectorOption;
        this.driftDetectionMethod = changeDetector;
    }

    /**
     * Returns {@code true} when the wrapped detector maintains independent
     * warning and drift sub-detectors (currently {@link ADDM}).
     */
    public boolean isDualDetector() {
        return this.driftDetectionMethod instanceof ADDM;
    }

    @Override
    public void input(double inputValue) {
        boolean wasWarningZone = this.driftDetectionMethod.getWarningZone();
        this.isWarningDetected = false;
        this.driftDetectionMethod.input(inputValue);

        if (isDualDetector()) {
            this.isWarningDetected = ((ADDM) this.driftDetectionMethod).getWarning();
        } else if (!wasWarningZone && this.driftDetectionMethod.getWarningZone()) {
            this.isWarningDetected = true;
        }
    }

    @Override
    public void resetLearning() {
        this.driftDetectionMethod.resetLearning();
    }

    public void resetWarningDetector() {
        if (isDualDetector()) {
            ((ADDM) this.driftDetectionMethod).resetWarningDetector();
        }
    }

    public void resetDriftDetector() {
        if (isDualDetector()) {
            ((ADDM) this.driftDetectionMethod).resetDriftDetector();
        }
    }

    public void resetAfterDrift(ChangeDetector freshDetector) {
        if (isDualDetector()) {
            ((ADDM) this.driftDetectionMethod).resetDriftDetector();
        } else {
            this.driftDetectionMethod = freshDetector;
        }
    }

    public void setDriftDetector(ChangeDetector changeDetector) {
        this.driftDetectionMethod = changeDetector;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        this.driftDetectionMethod.getDescription(sb, indent);
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.driftDetectionMethod.prepareForUse();
    }

    @Override
    public boolean getWarningZone() {
        return this.driftDetectionMethod.getWarningZone();
    }

    public boolean getWarning() {
        return this.isWarningDetected;
    }

    @Override
    public boolean getChange() {
        return this.driftDetectionMethod.getChange();
    }

    @Override
    public double getEstimation() {
        return this.driftDetectionMethod.getEstimation();
    }

    @Override
    public double getDelay() {
        return this.driftDetectionMethod.getDelay();
    }

    @Override
    public double[] getOutput() {
        return this.driftDetectionMethod.getOutput();
    }

    @Override
    public ChangeDetector copy() {
        return new GroCHChangeDetector(this.driftDetectionMethodOption, this.driftDetectionMethod.copy());
    }
}
