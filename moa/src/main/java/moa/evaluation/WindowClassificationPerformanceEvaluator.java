/*
 *    WindowClassificationPerformanceEvaluator.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
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
package moa.evaluation;

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;

import com.github.javacliparser.IntOption;

import moa.tasks.TaskMonitor;
import moa.core.Utils;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceData;
import com.yahoo.labs.samoa.instances.Prediction;

/**
 * Classification evaluator that updates evaluation results using a sliding
 * window.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class WindowClassificationPerformanceEvaluator extends AbstractOptionHandler
        implements LearningPerformanceEvaluator<Example<Instance>> {

    private static final long serialVersionUID = 1L;

    public IntOption widthOption = new IntOption("width",
            'w', "Size of Window", 1000);

    protected double TotalweightObserved = 0;

    protected Estimator weightObserved;

    protected Estimator weightCorrect;

    protected Estimator weightCorrectNoChangeClassifier;

    protected double lastSeenClass;

    protected Estimator[] columnKappa;

    protected Estimator[] rowKappa;

    protected Estimator[] classAccuracy;

    protected int numClasses;

    public class Estimator {

        protected double[] window;

        protected int posWindow;

        protected int lenWindow;

        protected int SizeWindow;

        protected double sum;

        public Estimator(int sizeWindow) {
            window = new double[sizeWindow];
            SizeWindow = sizeWindow;
            posWindow = 0;
            lenWindow = 0;
        }

        public void add(double value) {
            sum -= window[posWindow];
            sum += value;
            window[posWindow] = value;
            posWindow++;
            if (posWindow == SizeWindow) {
                posWindow = 0;
            }
            if (lenWindow < SizeWindow) {
                lenWindow++;
            }
        }

        public double total() {
            return sum;
        }

        public double length() {
            return lenWindow;
        }

    }

    /*   public void setWindowWidth(int w) {
     this.width = w;
     reset();
     }*/
    @Override
    public void reset() {
        reset(this.numClasses);
    }

    public void reset(int numClasses) {
        this.numClasses = numClasses;
        this.rowKappa = new Estimator[numClasses];
        this.columnKappa = new Estimator[numClasses];
        this.classAccuracy = new Estimator[numClasses];
        for (int i = 0; i < this.numClasses; i++) {
            this.rowKappa[i] = new Estimator(this.widthOption.getValue());
            this.columnKappa[i] = new Estimator(this.widthOption.getValue());
            this.classAccuracy[i] = new Estimator(this.widthOption.getValue());
        }
        this.weightCorrect = new Estimator(this.widthOption.getValue());
        this.weightCorrectNoChangeClassifier = new Estimator(this.widthOption.getValue());
        this.weightObserved = new Estimator(this.widthOption.getValue());
        this.TotalweightObserved = 0;
        this.lastSeenClass = 0;
    }

    @Override
    public void addResult(Example<Instance> example, double[] classVotes) {
        Instance inst = example.getData();
        double weight = inst.weight();
        if (inst.classIsMissing() == false){
            int trueClass = (int) inst.classValue();
            if (weight > 0.0) {
                if (TotalweightObserved == 0) {
                    reset(inst.dataset().numClasses());
                }
                this.TotalweightObserved += weight;
                this.weightObserved.add(weight);
                int predictedClass = Utils.maxIndex(classVotes);
                if (predictedClass == trueClass) {
                    this.weightCorrect.add(weight);
                } else {
                    this.weightCorrect.add(0);
                }
                //Add Kappa statistic information
                for (int i = 0; i < this.numClasses; i++) {
                    this.rowKappa[i].add(i == predictedClass ? weight : 0);
                    this.columnKappa[i].add(i == trueClass ? weight : 0);
                }
                if (this.lastSeenClass == trueClass) {
                    this.weightCorrectNoChangeClassifier.add(weight);
                } else {
                    this.weightCorrectNoChangeClassifier.add(0);
                }
                this.classAccuracy[trueClass].add(predictedClass == trueClass ? weight : 0.0);
                this.lastSeenClass = trueClass;
            }
        }
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {
        return new Measurement[]{
            new Measurement("classified instances",
            this.TotalweightObserved),
            new Measurement("classifications correct (percent)",
            getFractionCorrectlyClassified() * 100.0),
            new Measurement("Kappa Statistic (percent)",
            getKappaStatistic() * 100.0),
            new Measurement("Kappa Temporal Statistic (percent)",
            getKappaTemporalStatistic() * 100.0)
        };

    }

    public double getTotalWeightObserved() {
        return this.weightObserved.total();
    }

    public double getFractionCorrectlyClassified() {
        return this.weightObserved.total() > 0.0 ? (double) this.weightCorrect.total()
                / this.weightObserved.total() : 0.0;
    }

    public double getKappaStatistic() {
        if (this.weightObserved.total() > 0.0) {
            double p0 = this.weightCorrect.total() / this.weightObserved.total();
            double pc = 0;
            for (int i = 0; i < this.numClasses; i++) {
                pc += (this.rowKappa[i].total() / this.weightObserved.total())
                        * (this.columnKappa[i].total() / this.weightObserved.total());
            }
            return (p0 - pc) / (1 - pc);
        } else {
            return 0;
        }
    }

    public double getKappaTemporalStatistic() {
        if (this.weightObserved.total() > 0.0) {
            double p0 = this.weightCorrect.total() / this.weightObserved.total();
            double pc = this.weightCorrectNoChangeClassifier.total() / this.weightObserved.total();

            return (p0 - pc) / (1 - pc);
        } else {
            return 0;
        }
    }

    public double getFractionIncorrectlyClassified() {
        return 1.0 - getFractionCorrectlyClassified();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
    }

	@Override
	public void addResult(Example<Instance> testInst, Prediction prediction) {
		// TODO Auto-generated method stub
		
	}
    

}
