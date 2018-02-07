/*
 *    ALUncertainty.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Indre Zliobaite (zliobaite at gmail dot com)
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
package moa.classifiers.active;

import java.util.LinkedList;
import java.util.List;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Utils;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;

/**
 * Active learning setting for evolving data streams.
 *
 * <p>Active learning focuses on learning an accurate model with as few labels
 * as possible. Streaming data poses additional challenges for active learning,
 * since the data distribution may change over time (concept drift) and
 * classifiers need to adapt. Conventional active learning strategies
 * concentrate on querying the most uncertain instances, which are typically
 * concentrated around the decision boundary. If changes do not occur close to
 * the boundary, they will be missed and classifiers will fail to adapt. This
 * class contains three active learning strategies for streaming data that
 * explicitly handle concept drift. They are based on fixed uncertainty,
 * dynamic allocation of labeling efforts over time and randomization of the
 * search space [ZBPH]. It also contains the Selective Sampling strategy, which
 * is adapted from [CGZ] and uses a variable labeling threshold.
 * </p>
 *
 * <p>[ZBPH] Indre Zliobaite, Albert Bifet, Bernhard Pfahringer, Geoff Holmes:
 * Active Learning with Evolving Streaming Data. ECML/PKDD (3) 2011: 597-612</p>
 *
 * <p>[CGZ] N. Cesa-Bianchi, C. Gentile, and L. Zaniboni. Worst-case analysis of
 * selective sampling for linear classification. J. Mach. Learn. Res. (7) 2006:
 * 1205-1230</p>.
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>-l : Classifier to train</li>
 *   <li>-d : Strategy to use: FixedUncertainty, VarUncertainty,
 *            RandVarUncertainty, SelSampling</li>
 *   <li>-b : Budget to use</li>
 *   <li>-u : Fixed threshold</li>
 *   <li>-s : Floating budget step</li>
 *   <li>-n : Number of instances at beginning without active learning</li>
 * </ul>
 *
 * <p>Structural changes to match active learning framework by Daniel Kottke.
 * </p>
 *
 * @author Indre Zliobaite (zliobaite at gmail dot com)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @author Daniel Kottke (daniel dot kottke at ovgu dot de) - adapted to AL framework
 * @version $Revision: 7 $
 */
public class ALUncertainty extends AbstractClassifier implements ALClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Active learning classifier for evolving data streams based on uncertainty";
    }

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "drift.SingleClassifierDrift");

    public MultiChoiceOption activeLearningStrategyOption = new MultiChoiceOption(
            "activeLearningStrategy", 'd', "Active Learning Strategy to use.",
            new String[]{
                    "FixedUncertainty",
                    "VarUncertainty",
                    "RandVarUncertainty",
                    "SelSampling"},
            new String[]{
                    "Fixed uncertainty strategy",
                    "Uncertainty strategy with variable threshold",
                    "Uncertainty strategy with randomized variable threshold",
                    "Selective Sampling"}, 
            0);

    public FloatOption budgetOption = new FloatOption("budget",
            'b', "Budget to use.",
            0.1, 0.0, 1.0);

    public FloatOption fixedThresholdOption = new FloatOption("fixedThreshold",
            'u', "Fixed threshold.",
            0.9, 0.00, 1.00);

    public FloatOption stepOption = new FloatOption("step",
            's', "Floating budget step.",
            0.01, 0.00, 1.00);

    public FloatOption numInstancesInitOption = new FloatOption("numInstancesInit",
            'n', "Number of instances at beginning without active learning.",
            0.0, 0.00, Integer.MAX_VALUE);

    public Classifier classifier;

    public int lastLabelAcq = 0;
    
    public int costLabeling;

    public int iterationControl;

    public double newThreshold;

    public double maxPosterior;

    public double accuracyBaseLearner;

    private double outPosterior;

    private double getMaxPosterior(double[] incomingPrediction) {
        if (incomingPrediction.length > 1) {
            DoubleVector vote = new DoubleVector(incomingPrediction);
            if (vote.sumOfValues() > 0.0) {
                vote.normalize();
            }
            incomingPrediction = vote.getArrayRef();
            outPosterior = (incomingPrediction[Utils.maxIndex(incomingPrediction)]);
        } else {
            outPosterior = 0;
        }
        return outPosterior;
    }

    private void labelFixed(double incomingPosterior, Instance inst) {
        if (incomingPosterior < this.fixedThresholdOption.getValue()) {
            this.classifier.trainOnInstance(inst);
            this.costLabeling++;
            this.lastLabelAcq += 1;
        }
    }

    private void labelVar(double incomingPosterior, Instance inst) {
        if (incomingPosterior < this.newThreshold) {
            this.classifier.trainOnInstance(inst);
            this.costLabeling++;
            this.lastLabelAcq += 1;
            this.newThreshold *= (1 - this.stepOption.getValue());
        } else {
            this.newThreshold *= (1 + this.stepOption.getValue());
        }
    }

    private void labelSelSampling(double incomingPosterior, Instance inst) {
        double p = Math.abs(incomingPosterior - 1.0 / (inst.numClasses()));
        double budget = this.budgetOption.getValue() / (this.budgetOption.getValue() + p);
        if (this.classifierRandom.nextDouble() < budget) {
            this.classifier.trainOnInstance(inst);
            this.costLabeling++;
            this.lastLabelAcq += 1;
        }
    }

    @Override
    public void resetLearningImpl() {
        this.classifier = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        this.classifier.resetLearning();
        this.costLabeling = 0;
        this.iterationControl = 0;
        this.newThreshold = 1.0;
        this.accuracyBaseLearner = 0;
        this.lastLabelAcq = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {

        this.iterationControl++;

        double costNow;

        if (this.iterationControl <= this.numInstancesInitOption.getValue()) {
            costNow = 0;
            //Use all instances at the beginning
            this.classifier.trainOnInstance(inst);
            this.costLabeling++;
            return;
        } else {
            costNow = (this.costLabeling - this.numInstancesInitOption.getValue()) / ((double) this.iterationControl - this.numInstancesInitOption.getValue());
        }

        if (costNow < this.budgetOption.getValue()) { //allow to label
            switch (this.activeLearningStrategyOption.getChosenIndex()) {
                case 0: //fixed
                    maxPosterior = getMaxPosterior(this.classifier.getVotesForInstance(inst));
                    labelFixed(maxPosterior, inst);
                    break;
                case 1: //variable
                    maxPosterior = getMaxPosterior(this.classifier.getVotesForInstance(inst));
                    labelVar(maxPosterior, inst);
                    break;
                case 2: //randomized
                    maxPosterior = getMaxPosterior(this.classifier.getVotesForInstance(inst));
                    maxPosterior = maxPosterior / (this.classifierRandom.nextGaussian() + 1.0);
                    labelVar(maxPosterior, inst);
                    break;
                case 3: //selective-sampling
                    maxPosterior = getMaxPosterior(this.classifier.getVotesForInstance(inst));
                    labelSelSampling(maxPosterior, inst);
                    break;
            }
        }
    }

    @Override
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
        measurementList.add(new Measurement("labeling cost", this.costLabeling));
        measurementList.add(new Measurement("newThreshold", this.newThreshold));
        measurementList.add(new Measurement("maxPosterior", this.maxPosterior));
        measurementList.add(new Measurement("accuracyBaseLearner (percent)", 100 * this.accuracyBaseLearner / this.costLabeling));
        Measurement[] modelMeasurements = ((AbstractClassifier) this.classifier).getModelMeasurements();
        if (modelMeasurements != null) {
            for (Measurement measurement : modelMeasurements) {
                measurementList.add(measurement);
            }
        }
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }

	@Override
	public int getLastLabelAcqReport() {
		int help = this.lastLabelAcq;
		this.lastLabelAcq = 0;
		return help; 
	}
	
	@Override
	public void setModelContext(InstancesHeader ih) {
		super.setModelContext(ih);
		classifier.setModelContext(ih);
	}
}
