/*
 *    LimAttClassifier.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *    @author Eibe Frank (eibe{[at]}cs{[dot]}waikato{[dot]}ac{[dot]}nz)
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
package moa.classifiers.meta;

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.trees.LimAttHoeffdingTree;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Utils;

import java.math.BigInteger;
import java.util.Arrays;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;

import moa.core.Measurement;
import moa.options.ClassOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;

/**
 * Ensemble Combining Restricted Hoeffding Trees using Stacking.
 * It produces a classification model based on an
 * ensemble of restricted decision trees, where each tree is built from a
 * distinct subset of the attributes. The overall model is formed by
 * combining the log-odds of the predicted class probabilities of these trees
 * using sigmoid perceptrons, with one perceptron per class.
 * In contrast to the standard boosting approach,
 * which forms an ensemble classifier in a greedy fashion, building each tree in
 * sequence and assigning corresponding weights as a by-product, our
 * method generates each tree in parallel and combines them using perceptron
 * classifiers by adopting the stacking approach.
 *
 * For more information see,<br/>
 * <br/>
 * Albert Bifet, Eibe Frank, Geoffrey Holmes, Bernhard Pfahringer: Accurate
 * Ensembles for Data Streams: Combining Restricted Hoeffding Trees using Stacking.
 * Journal of Machine Learning Research - Proceedings Track 13: 225-240 (2010)
 *
<!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{BifetFHP10,
 * author    = {Albert Bifet and
 *              Eibe Frank and
 *              Geoffrey Holmes and
 *              Bernhard Pfahringer},
 * title     = {Accurate Ensembles for Data Streams: Combining Restricted
 *              Hoeffding Trees using Stacking},
 * journal   = {Journal of Machine Learning Research - Proceedings Track},
 * volume    = {13},
 * year      = {2010},
 * pages     = {225-240}
 * }
 * </pre>
 * <p/>
<!-- technical-bibtex-end -->
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @author Eibe Frank (eibe{[at]}cs{[dot]}waikato{[dot]}ac{[dot]}nz)
 * @version $Revision: 7 $
 */
public class LimAttClassifier extends AbstractClassifier implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "Ensemble Combining Restricted Hoeffding Trees using Stacking";
    }    
    
    /*
     * Class that generates all combinations of n elements, taken
     * r at a time. The algorithm is described by
     *
     * Kenneth H. Rosen, Discrete Mathematics and Its Applications,
     * 2nd edition (NY: McGraw-Hill, 1991), pp. 284-286.
     *
     *  @author Michael Gilleland (megilleland@yahoo.com)
     */
    public class CombinationGenerator {

        private int[] a;

        private int n;

        private int r;

        private BigInteger numLeft;

        private BigInteger total;
        //------------
        // Constructor
        //------------

        public CombinationGenerator(int n, int r) {
            if (r > n) {
                throw new IllegalArgumentException();
            }
            if (n < 1) {
                throw new IllegalArgumentException();
            }
            this.n = n;
            this.r = r;
            a = new int[r];
            BigInteger nFact = getFactorial(n);
            BigInteger rFact = getFactorial(r);
            BigInteger nminusrFact = getFactorial(n - r);
            total = nFact.divide(rFact.multiply(nminusrFact));
            reset();
        }
        //------
        // Reset
        //------

        public void reset() {
            for (int i = 0; i < a.length; i++) {
                a[i] = i;
            }
            numLeft = new BigInteger(total.toString());
        }
        //------------------------------------------------
        // Return number of combinations not yet generated
        //------------------------------------------------

        public BigInteger getNumLeft() {
            return numLeft;
        }
        //-----------------------------
        // Are there more combinations?
        //-----------------------------

        public boolean hasMore() {
            return numLeft.compareTo(BigInteger.ZERO) == 1;
        }
        //------------------------------------
        // Return total number of combinations
        //------------------------------------

        public BigInteger getTotal() {
            return total;
        }
        //------------------
        // Compute factorial
        //------------------

        private BigInteger getFactorial(int n) {
            BigInteger fact = BigInteger.ONE;
            for (int i = n; i > 1; i--) {
                fact = fact.multiply(new BigInteger(Integer.toString(i)));
            }
            return fact;
        }
        //--------------------------------------------------------
        // Generate next combination (algorithm from Rosen p. 286)
        //--------------------------------------------------------

        public int[] getNext() {
            if (numLeft.equals(total)) {
                numLeft = numLeft.subtract(BigInteger.ONE);
                int[] b = new int[a.length];
                for (int k = 0; k < a.length; k++) {
                    b[k] = a[k];
                }
                return b;
            }
            int i = r - 1;
            while (a[i] == n - r + i) {
                i--;
            }
            a[i] = a[i] + 1;
            for (int j = i + 1; j < r; j++) {
                a[j] = a[i] + j - i;
            }
            numLeft = numLeft.subtract(BigInteger.ONE);
            int[] b = new int[a.length];
            for (int k = 0; k < a.length; k++) {
                b[k] = a[k];
            }
            return b;
        }
    }

    private static final long serialVersionUID = 1L;

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.LimAttHoeffdingTree");

    public IntOption numAttributesOption = new IntOption("numAttributes", 'n',
            "The number of attributes to use per model.", 1, 1, Integer.MAX_VALUE);

    public FloatOption weightShrinkOption = new FloatOption("weightShrink", 'w',
            "The number to multiply the weight misclassified counts.", 0.5, 0.0, Float.MAX_VALUE);

    public FloatOption deltaAdwinOption = new FloatOption("deltaAdwin", 'a',
            "Delta of Adwin change detection", 0.002, 0.0, 1.0);

    public FloatOption oddsOffsetOption = new FloatOption("oddsOffset", 'o',
            "Offset for odds to avoid probabilities that are zero.", 0.001, 0.0, Float.MAX_VALUE);

    public FlagOption pruneOption = new FlagOption("prune", 'x',
            "Enable pruning.");

    public FlagOption bigTreesOption = new FlagOption("bigTrees", 'b',
            "Use m-n attributes on the trees.");

    public IntOption numEnsemblePruningOption = new IntOption("numEnsemblePruning", 'm',
            "The pruned number of classifiers to use to predict.", 10, 1, Integer.MAX_VALUE);

    public FlagOption adwinReplaceWorstClassifierOption = new FlagOption("adwinReplaceWorstClassifier", 'z',
            "When one Adwin detects change, replace worst classifier.");

    protected Classifier[] ensemble;

    protected ADWIN[] ADError;

    protected int numberOfChangesDetected;

    protected int[][] matrixCodes;

    protected boolean initMatrixCodes = false;

    protected boolean initClassifiers = false;

    protected int numberAttributes = 1;

    protected int numInstances = 0;

    @Override
    public void resetLearningImpl() {
        this.initClassifiers = true;
        this.reset = true;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        int numClasses = inst.numClasses();
        //Init Ensemble
        if (this.initClassifiers == true) {
            numberAttributes = numAttributesOption.getValue();
            if (bigTreesOption.isSet()) {
                numberAttributes = inst.numAttributes() - 1 - numAttributesOption.getValue();
            }
            CombinationGenerator x = new CombinationGenerator(inst.numAttributes() - 1, this.numberAttributes);
            int numberClassifiers = x.getTotal().intValue();
            this.ensemble = new Classifier[numberClassifiers];
            Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
            baseLearner.resetLearning();
            for (int i = 0; i < this.ensemble.length; i++) {
                this.ensemble[i] = baseLearner.copy();
            }
            this.ADError = new ADWIN[this.ensemble.length];
            for (int i = 0; i < this.ensemble.length; i++) {
                this.ADError[i] = new ADWIN((double) this.deltaAdwinOption.getValue());
            }
            this.numberOfChangesDetected = 0;
            //Prepare combinations
            int i = 0;
            if (baseLearner instanceof LimAttHoeffdingTree) {
                while (x.hasMore()) {
                    ((LimAttHoeffdingTree) this.ensemble[i]).setlistAttributes(x.getNext());
                    i++;
                }
            }

            this.initClassifiers = false;
        }

        boolean Change = false;
        Instance weightedInst = (Instance) inst.copy();

        //Train Perceptron
        double[][] votes = new double[this.ensemble.length + 1][numClasses];
        for (int i = 0; i < this.ensemble.length; i++) {
            double[] v = new double[numClasses];
            for (int j = 0; j < v.length; j++) {
                v[j] = (double) this.oddsOffsetOption.getValue();
            }
            double[] vt = this.ensemble[i].getVotesForInstance(inst);
            double sum = Utils.sum(vt);
            if (!Double.isNaN(sum) && (sum > 0)) {
                for (int j = 0; j < vt.length; j++) {
                    vt[j] /= sum;
                }
            } else {
                // Just in case the base learner returns NaN
                for (int k = 0; k < vt.length; k++) {
                    vt[k] = 0.0;
                }
            }
            sum = numClasses * (double) this.oddsOffsetOption.getValue();
            for (int j = 0; j < vt.length; j++) {
                v[j] += vt[j];
                sum += vt[j];
            }
            for (int j = 0; j < vt.length; j++) {
                votes[i][j] = Math.log(v[j] / (sum - v[j]));
            }
        }

        if (adwinReplaceWorstClassifierOption.isSet() == false) {
            //Train ensemble of classifiers
            for (int i = 0; i < this.ensemble.length; i++) {
                boolean correctlyClassifies = this.ensemble[i].correctlyClassifies(weightedInst);
                double ErrEstim = this.ADError[i].getEstimation();
                if (this.ADError[i].setInput(correctlyClassifies ? 0 : 1)) {
                    numInstances = initialNumInstancesOption.getValue();
                    if (this.ADError[i].getEstimation() > ErrEstim) {
                        Change = true;
                        //Replace classifier if ADWIN has detected change
                        numberOfChangesDetected++;
                        this.ensemble[i].resetLearning();
                        this.ADError[i] = new ADWIN((double) this.deltaAdwinOption.getValue());
                        for (int ii = 0; ii < inst.numClasses(); ii++) {
                            weightAttribute[ii][i] = 0.0;// 0.2 * Math.random() - 0.1;
                        }
                    }
                }
            }
        } else {
            //Train ensemble of classifiers
            for (int i = 0; i < this.ensemble.length; i++) {
                boolean correctlyClassifies = this.ensemble[i].correctlyClassifies(weightedInst);
                double ErrEstim = this.ADError[i].getEstimation();
                if (this.ADError[i].setInput(correctlyClassifies ? 0 : 1)) {
                    if (this.ADError[i].getEstimation() > ErrEstim) {
                        Change = true;
                    }
                }
            }
            //Replace classifier with higher error if ADWIN has detected change
            if (Change) {
                numberOfChangesDetected++;
                double max = 0.0;
                int imax = -1;
                for (int i = 0; i < this.ensemble.length; i++) {
                    if (max < this.ADError[i].getEstimation()) {
                        max = this.ADError[i].getEstimation();
                        imax = i;
                    }
                }
                if (imax != -1) {
                    this.ensemble[imax].resetLearning();
                    this.ADError[imax] = new ADWIN((double) this.deltaAdwinOption.getValue());
                    for (int ii = 0; ii < inst.numClasses(); ii++) {
                        weightAttribute[ii][imax] = 0.0;
                    }
                }
            }
        }

        trainOnInstanceImplPerceptron(inst.numClasses(), (int) inst.classValue(), votes);

        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i].trainOnInstance(inst);
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.initClassifiers == true) {
            return new double[0];
        }
        int numClasses = inst.numClasses();

        int sizeEnsemble = this.ensemble.length;
        if (pruneOption.isSet()) {
            sizeEnsemble = this.numEnsemblePruningOption.getValue();
        }

        double[][] votes = new double[sizeEnsemble + 1][numClasses];
        int[] bestClassifiers = new int[sizeEnsemble];
        if (pruneOption.isSet()) {
            //Check for the best classifiers
            double[] weight = new double[this.ensemble.length];
            for (int i = 0; i < numClasses; i++) {
                for (int j = 0; j < this.ensemble.length; j++) {
                    weight[j] += weightAttribute[i][j];
                }
            }
            Arrays.sort(weight);
            double cutValue = weight[this.ensemble.length - sizeEnsemble]; //reverse order
            int ii = 0;
            for (int j = 0; j < this.ensemble.length; j++) {
                if (weight[j] >= cutValue && ii < sizeEnsemble) {
                    bestClassifiers[ii] = j;
                    ii++;
                }
            }
        } else { //Not pruning: all classifiers
            for (int ii = 0; ii < sizeEnsemble; ii++) {
                bestClassifiers[ii] = ii;
            }
        }
        for (int ii = 0; ii < sizeEnsemble; ii++) {
            int i = bestClassifiers[ii];
            double[] v = new double[numClasses];
            for (int j = 0; j < v.length; j++) {
                v[j] = (double) this.oddsOffsetOption.getValue();
            }
            double[] vt = this.ensemble[i].getVotesForInstance(inst);
            double sum = Utils.sum(vt);
            if (!Double.isNaN(sum) && (sum > 0)) {
                for (int j = 0; j < vt.length; j++) {
                    vt[j] /= sum;
                }
            } else {
                // Just in case the base learner returns NaN
                for (int k = 0; k < vt.length; k++) {
                    vt[k] = 0.0;
                }
            }
            sum = numClasses * (double) this.oddsOffsetOption.getValue();
            for (int j = 0; j < vt.length; j++) {
                v[j] += vt[j];
                sum += vt[j];
            }
            for (int j = 0; j < vt.length; j++) {
                votes[ii][j] = Math.log(v[j] / (sum - v[j]));
                //                    votes[i][j] = vt[j];
            }
        }
        return getVotesForInstancePerceptron(votes, bestClassifiers, inst.numClasses());
    }

    @Override
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
                    this.ensemble != null ? this.ensemble.length : 0),
                    new Measurement("change detections", this.numberOfChangesDetected)
                };
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return this.ensemble.clone();
    }

    //Perceptron
    public FloatOption learningRatioOption = new FloatOption("learningRatio", 'r', "Learning ratio", 1);

    public FloatOption penaltyFactorOption = new FloatOption("lambda", 'p', "Lambda", 0.0);

    public IntOption initialNumInstancesOption = new IntOption("initialNumInstances", 'i', "initialNumInstances", 10);

    protected double[][] weightAttribute;

    protected boolean reset;

    public void trainOnInstanceImplPerceptron(int numClasses, int actualClass, double[][] votes) {

        //Init Perceptron
        if (this.reset == true) {
            this.reset = false;
            this.weightAttribute = new double[numClasses][votes.length];
            for (int i = 0; i < numClasses; i++) {
                for (int j = 0; j < votes.length - 1; j++) {
                    weightAttribute[i][j] = 1.0 / (votes.length - 1.0);
                }
            }
            numInstances = initialNumInstancesOption.getValue();
        }

        // Weight decay
        double learningRatio = learningRatioOption.getValue() * 2.0 / (numInstances + (votes.length - 1) + 2.0);
        double lambda = penaltyFactorOption.getValue();
        numInstances++;

        double[] preds = new double[numClasses];

        for (int i = 0; i < numClasses; i++) {
            preds[i] = prediction(votes, i);
        }
        for (int i = 0; i < numClasses; i++) {
            double actual = (i == actualClass) ? 1.0 : 0.0;
            double delta = (actual - preds[i]) * preds[i] * (1 - preds[i]);
            for (int j = 0; j < this.ensemble.length; j++) {
                this.weightAttribute[i][j] += learningRatio * (delta * votes[j][i] - lambda * this.weightAttribute[i][j]);
            }
            this.weightAttribute[i][this.ensemble.length] += learningRatio * delta;
        }
    }

    public double predictionPruning(double[][] votes, int[] bestClassifiers, int classVal) {
        double sum = 0.0;
        for (int i = 0; i < votes.length - 1; i++) {
            sum += (double) weightAttribute[classVal][bestClassifiers[i]] * votes[i][classVal];
        }
        sum += weightAttribute[classVal][votes.length - 1];
        return 1.0 / (1.0 + Math.exp(-sum));
    }

    public double prediction(double[][] votes, int classVal) {
        double sum = 0.0;
        for (int i = 0; i < votes.length - 1; i++) {
            sum += (double) weightAttribute[classVal][i] * votes[i][classVal];
        }
        sum += weightAttribute[classVal][votes.length - 1];
        return 1.0 / (1.0 + Math.exp(-sum));
    }

    public double[] getVotesForInstancePerceptron(double[][] votesEnsemble, int[] bestClassifiers, int numClasses) {
        double[] votes = new double[numClasses];
        if (this.reset == false) {
            for (int i = 0; i < votes.length; i++) {
                votes[i] = predictionPruning(votesEnsemble, bestClassifiers, i);
            }
        }
        return votes;

    }
}
