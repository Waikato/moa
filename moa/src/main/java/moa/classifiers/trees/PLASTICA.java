/*
 *    DriftDetectionMethodClassifier.java
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
package moa.classifiers.trees;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.trees.plastic_util.CustomADWINChangeDetector;
import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

import java.util.LinkedList;
import java.util.List;

/**
 * PLASTIC-A
 *
 * <p>Combination of PLASTIC and a drift detector at the root. Will grow a background tree after a change and
 * replace the current tree once the BG tree is more accurate.</p>
 *
 * <p>See details in:<br> Marco Heyden, Heitor Murilo Gomes, Edouard Fouché, Bernhard Pfahringer, Klemens Böhm:
 * Leveraging Plasticity in Incremental Decision Trees. ECML/PKDD (5) 2024: 38-54</p>
 *
 * @author Marco Heyden (marco dot heyden at kit dot edu)
 * @version $Revision: 1 $
 */
public class PLASTICA extends AbstractClassifier implements MultiClassClassifier,
                                                                                  CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "Classifier that grows a background tree when a change is detected in accuracy.";
    }
    
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.PLASTIC");

    public FloatOption confidenceOption = new FloatOption(
            "Confidence",
            'c',
            "Confidence at which the current learner will be replaced.",
            0.05, 0.0, 1.0);

    protected Classifier classifier;

    protected Classifier newclassifier;

    protected CustomADWINChangeDetector mainLearnerChangeDetector;
    protected CustomADWINChangeDetector bgLearnerChangeDetector;

    private double confidence;
    protected boolean newClassifierReset;

    @Override
    public void resetLearningImpl() {
        this.classifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        this.classifier.resetLearning();
        mainLearnerChangeDetector = new CustomADWINChangeDetector();
        bgLearnerChangeDetector = new CustomADWINChangeDetector();
        this.newClassifierReset = false;
        confidence = confidenceOption.getValue();
    }

    protected int changeDetected = 0;

    protected int warningDetected = 0;

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        //this.numberInstances++;
        int trueClass = (int) inst.classValue();
        boolean prediction;
        if (Utils.maxIndex(this.classifier.getVotesForInstance(inst)) == trueClass) {
            prediction = true;
        } else {
            prediction = false;
        }

        mainLearnerChangeDetector.input(prediction ? 0.0 : 1.0);

        if (this.newclassifier != null) {
            if (Utils.maxIndex(this.newclassifier.getVotesForInstance(inst)) == trueClass) {
                prediction = true;
            } else {
                prediction = false;
            }
            bgLearnerChangeDetector.input(prediction ? 0.0 : 1.0);
        }

        if (this.mainLearnerChangeDetector.getChange() && newclassifier == null) {
            makeNewClassifier();
        }
        else if (this.bgLearnerChangeDetector.getChange()) {
            makeNewClassifier();
        }

        if (mainLearnerChangeDetector.getWidth() > 200 && bgLearnerChangeDetector.getWidth() > 200) {
            double oldErrorRate = mainLearnerChangeDetector.getEstimation();
            double oldWS = mainLearnerChangeDetector.getWidth();
            double altErrorRate = bgLearnerChangeDetector.getEstimation();
            double altWS = bgLearnerChangeDetector.getWidth();

            double Bound = computeBound(oldErrorRate, oldWS, altWS);

            if (Bound < oldErrorRate - altErrorRate) {
                classifier = newclassifier;
                newclassifier = null;
                bgLearnerChangeDetector.resetLearning();
            }
            else if (Bound < altErrorRate - oldErrorRate) {
                // Erase alternate tree
                newclassifier = null;
                bgLearnerChangeDetector.resetLearning();
            }
        }

        this.classifier.trainOnInstance(inst);
        if (newclassifier != null)
            newclassifier.trainOnInstance(inst);
    }

    private double computeBound(double oldErrorRate, double oldWS, double altWS) {
        double fDelta = confidenceOption.getValue();
        double fN = 1.0 / altWS + 1.0 / oldWS;
        return Math.sqrt(2.0 * oldErrorRate * (1.0 - oldErrorRate) * Math.log(2.0 / fDelta) * fN);
    }

    public double[] getVotesForInstance(Instance inst) {
        return this.classifier.getVotesForInstance(inst);
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        ((AbstractClassifier) this.classifier).getModelDescription(out, indent);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.add(new Measurement("Change detected", this.changeDetected));
        measurementList.add(new Measurement("Warning detected", this.warningDetected));
        Measurement[] modelMeasurements = ((AbstractClassifier) this.classifier).getModelMeasurements();
        if (modelMeasurements != null) {
            for (Measurement measurement : modelMeasurements) {
                measurementList.add(measurement);
            }
        }
        this.changeDetected = 0;
        this.warningDetected = 0;
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == PLASTICA.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    private void makeNewClassifier() {
        newclassifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        bgLearnerChangeDetector.resetLearning();
    }
}
