/*
 *    SPegasos.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
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

/*
 *    SPegasos.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */
package moa.classifiers.functions;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import moa.core.StringUtils;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Utils;

/**
 * <!-- globalinfo-start --> Implements the stochastic variant of the Pegasos
 * (Primal Estimated sub-GrAdient SOlver for SVM) method of Shalev-Shwartz et
 * al. (2007). For more information, see<br/> <br/> S. Shalev-Shwartz, Y.
 * Singer, N. Srebro: Pegasos: Primal Estimated sub-GrAdient SOlver for SVM. In:
 * 24th International Conference on MachineLearning, 807-814, 2007.
 * <p/>
 * <!-- globalinfo-end -->
 * * 
<!-- technical-bibtex-start --> BibTeX:
 * <pre>
 * &#64;inproceedings{Shalev-Shwartz2007,
 *    author = {S. Shalev-Shwartz and Y. Singer and N. Srebro},
 *    booktitle = {24th International Conference on MachineLearning},
 *    pages = {807-814},
 *    title = {Pegasos: Primal Estimated sub-GrAdient SOlver for SVM},
 *    year = {2007}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 *
 */
public class SPegasos extends AbstractClassifier implements MultiClassClassifier {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -3732968666673530290L;

    @Override
    public String getPurposeString() {
        return "Stochastic variant of the Pegasos (Primal Estimated sub-GrAdient SOlver for SVM) method of Shalev-Shwartz et al. (2007).";
    }

    /**
     * The regularization parameter
     */
    protected double m_lambda = 0.0001;

    public FloatOption lambdaRegularizationOption = new FloatOption("lambdaRegularization",
            'l', "Lambda regularization parameter .",
            0.0001, 0.00, Integer.MAX_VALUE);

    protected static final int HINGE = 0;

    protected static final int LOGLOSS = 1;

    /**
     * The current loss function to minimize
     */
    protected int m_loss = HINGE;

    public MultiChoiceOption lossFunctionOption = new MultiChoiceOption(
            "lossFunction", 'o', "The loss function to use.", new String[]{
                "HINGE", "LOGLOSS"}, new String[]{
                "Hinge loss (SVM)",
                "Log loss (logistic regression)"}, 0);

    /**
     * Stores the weights (+ bias in the last element)
     */
    protected double[] m_weights;

    /**
     * Holds the current iteration number
     */
    protected double m_t;

    /**
     * Set the value of lambda to use
     *
     * @param lambda the value of lambda to use
     */
    public void setLambda(double lambda) {
        m_lambda = lambda;
    }

    /**
     * Get the current value of lambda
     *
     * @return the current value of lambda
     */
    public double getLambda() {
        return m_lambda;
    }

    /**
     * Set the loss function to use.
     *
     * @param function the loss function to use.
     */
    public void setLossFunction(int function) {
        m_loss = function;
    }

    /**
     * Get the current loss function.
     *
     * @return the current loss function.
     */
    public int getLossFunction() {
        return m_loss;
    }

    /**
     * Reset the classifier.
     */
    public void reset() {
        m_t = 2;
        m_weights = null;
    }

    protected static double dotProd(Instance inst1, double[] weights, int classIndex) {
        double result = 0;

        int n1 = inst1.numValues();
        int n2 = weights.length - 1;

        for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
            int ind1 = inst1.index(p1);
            int ind2 = p2;
            if (ind1 == ind2) {
                if (ind1 != classIndex && !inst1.isMissingSparse(p1)) {
                    result += inst1.valueSparse(p1) * weights[p2];
                }
                p1++;
                p2++;
            } else if (ind1 > ind2) {
                p2++;
            } else {
                p1++;
            }
        }
        return (result);
    }

    protected double dloss(double z) {
        if (m_loss == HINGE) {
            return (z < 1) ? 1 : 0;
        }

        // log loss
        if (z < 0) {
            return 1.0 / (Math.exp(z) + 1.0);
        } else {
            double t = Math.exp(-z);
            return t / (t + 1);
        }
    }

    @Override
    public void resetLearningImpl() {
        reset();
        setLambda(this.lambdaRegularizationOption.getValue());
        setLossFunction(this.lossFunctionOption.getChosenIndex());
    }

    /**
     * Trains the classifier with the given instance.
     *
     * @param instance the new training instance to include in the model
     */
    @Override
    public void trainOnInstanceImpl(Instance instance) {

        if (m_weights == null) {
            m_weights = new double[instance.numAttributes() + 1];
        }
        if (!instance.classIsMissing()) {

            double learningRate = 1.0 / (m_lambda * m_t);
            //double scale = 1.0 - learningRate * m_lambda;
            double scale = 1.0 - 1.0 / m_t;
            double y = (instance.classValue() == 0) ? -1 : 1;
            double wx = dotProd(instance, m_weights, instance.classIndex());
            double z = y * (wx + m_weights[m_weights.length - 1]);

            for (int j = 0; j < m_weights.length - 1; j++) {
                if (j != instance.classIndex()) {
                    m_weights[j] *= scale;
                }
            }

            if (m_loss == LOGLOSS || (z < 1)) {
                double loss = dloss(z);
                int n1 = instance.numValues();
                for (int p1 = 0; p1 < n1; p1++) {
                    int indS = instance.index(p1);
                    if (indS != instance.classIndex() && !instance.isMissingSparse(p1)) {
                        double m = learningRate * loss * (instance.valueSparse(p1) * y);
                        m_weights[indS] += m;
                    }
                }

                // update the bias
                m_weights[m_weights.length - 1] += learningRate * loss * y;
            }

            double norm = 0;
            for (int k = 0; k < m_weights.length - 1; k++) {
                if (k != instance.classIndex()) {
                    norm += (m_weights[k] * m_weights[k]);
                }
            }

            double scale2 = Math.min(1.0, (1.0 / (m_lambda * norm)));
            if (scale2 < 1.0) {
                scale2 = Math.sqrt(scale2);
                for (int j = 0; j < m_weights.length - 1; j++) {
                    if (j != instance.classIndex()) {
                        m_weights[j] *= scale2;
                    }
                }
            }
            m_t++;
        }
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param inst the instance to be classified
     * @return predicted class probability distribution
     */
    @Override
    public double[] getVotesForInstance(Instance inst) {

        if (m_weights == null) {
            return new double[inst.numAttributes() + 1];
        }

        double[] result = new double[2];

        double wx = dotProd(inst, m_weights, inst.classIndex());// * m_wScale;
        double z = (wx + m_weights[m_weights.length - 1]);
        //System.out.print("" + z + ": ");
        // System.out.println(1.0 / (1.0 + Math.exp(-z)));
        if (z <= 0) {
            //  z = 0;
            if (m_loss == LOGLOSS) {
                result[0] = 1.0 / (1.0 + Math.exp(z));
                result[1] = 1.0 - result[0];
            } else {
                result[0] = 1;
            }
        } else {
            if (m_loss == LOGLOSS) {
                result[1] = 1.0 / (1.0 + Math.exp(-z));
                result[0] = 1.0 - result[1];
            } else {
                result[1] = 1;
            }
        }
        return result;
    }

    @Override
    public void getModelDescription(StringBuilder result, int indent) {
        StringUtils.appendIndented(result, indent, toString());
        StringUtils.appendNewline(result);
    }

    /**
     * Prints out the classifier.
     *
     * @return a description of the classifier as a string
     */
    @Override
    public String toString() {
        if (m_weights == null) {
            return "SPegasos: No model built yet.\n";
        }
        StringBuffer buff = new StringBuffer();
        buff.append("Loss function: ");
        if (m_loss == HINGE) {
            buff.append("Hinge loss (SVM)\n\n");
        } else {
            buff.append("Log loss (logistic regression)\n\n");
        }
        int printed = 0;

        for (int i = 0; i < m_weights.length - 1; i++) {
            //   if (i != m_data.classIndex()) {
            if (printed > 0) {
                buff.append(" + ");
            } else {
                buff.append("   ");
            }

            buff.append(Utils.doubleToString(m_weights[i], 12, 4) + " "
                    //+ m_data.attribute(i).name()
                    + "\n");

            printed++;
        }
        //}

        if (m_weights[m_weights.length - 1] > 0) {
            buff.append(" + " + Utils.doubleToString(m_weights[m_weights.length - 1], 12, 4));
        } else {
            buff.append(" - " + Utils.doubleToString(-m_weights[m_weights.length - 1], 12, 4));
        }

        return buff.toString();
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
