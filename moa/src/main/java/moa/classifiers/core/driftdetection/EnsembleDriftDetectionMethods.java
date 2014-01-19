/*
 *    DDM.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Manuel Baena (mbaena@lcc.uma.es)
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
 */
package moa.classifiers.core.driftdetection;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.MultiChoiceOption;
import com.github.javacliparser.Option;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.options.OptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Ensemble Drift detection method
 *
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version $Revision: 7 $
 */
public class EnsembleDriftDetectionMethods extends AbstractChangeDetector {

    private static final long serialVersionUID = -3518369648142099719L;

    //private static final int DDM_MINNUMINST = 30;
    public IntOption minNumInstancesOption = new IntOption(
            "minNumInstances",
            'n',
            "The minimum number of instances before permitting detecting change.",
            30, 0, Integer.MAX_VALUE);

    public ListOption changeDetectorsOption = new ListOption("changeDetectors", 'c',
            "Change Detectors to use.", new ClassOption("driftDetectionMethod", 'd',
            "Drift detection method to use.", ChangeDetector.class, "DDM"),
            new Option[0], ',');

    public MultiChoiceOption predictionOption = new MultiChoiceOption(
            "prediction", 'l', "Prediction to use.", new String[]{
                "max", "min", "majority"}, new String[]{
                "Maximum",
                "Minimum",
                "Majority"}, 0);

    public EnsembleDriftDetectionMethods() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        //if (preds == null) {
             preds = new Boolean[this.changeDetectorsOption.getList().length];
        //}
        for (int i = 0; i < preds.length; i++) {
            preds[i] = false;
        }
    }
        
    protected ChangeDetector[] cds;

    protected Boolean[] preds;

    @Override
    public void input(double prediction) {
        for (int i = 0; i < cds.length; i++) {
            cds[i].input(prediction);
            if (cds[i].getChange()) {
                preds[i] = true;
            }
        }
        int typePrediction = this.predictionOption.getChosenIndex();
                   int numberDetections = 0;
            for (int i = 0; i < cds.length; i++) {
                if (preds[i] == true) {
                    numberDetections++;
                }
            }
        if (typePrediction == 0) { 
            //Choose Max
            this.isChangeDetected = (numberDetections == cds.length);
        } else if (typePrediction == 1) {
            //Choose Min
             this.isChangeDetected = (numberDetections > 0);
        } else if (typePrediction == 2) {
            //Choose Avg
            this.isChangeDetected = (numberDetections > cds.length/2) ;
        }
        if (this.isChangeDetected == true) {
            this.resetLearning();
        }
    }

    //public double[] getOutput() {
    //    double[] res = {this.isChangeDetected ? 1 : 0, this.isWarningZone ? 1 : 0, this.delay, this.estimation};
    //    return res;
    //}
    
    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // TODO Auto-generated method stub
        Option[] changeDetectorOptions = this.changeDetectorsOption.getList();
        cds = new ChangeDetector[changeDetectorOptions.length];
        preds = new Boolean[changeDetectorOptions.length];
        for (int i = 0; i < cds.length; i++) {
            //monitor.setCurrentActivity("Materializing change detector " + (i + 1)
            //        + "...", -1.0);
            cds[i] = ((ChangeDetector) ((ClassOption) changeDetectorOptions[i]).materializeObject(monitor, repository)).copy();
            if (monitor.taskShouldAbort()) {
                return;
            }
            if (cds[i] instanceof OptionHandler) {
                monitor.setCurrentActivity("Preparing change detector " + (i + 1)
                        + "...", -1.0);
                ((OptionHandler) cds[i]).prepareForUse(monitor, repository);
                if (monitor.taskShouldAbort()) {
                    return;
                }
            }
            preds[i] = false;
        }
    }
}