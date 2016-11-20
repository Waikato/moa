/*
 *    AdaGrad.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *    @author Henry Gouk (hgrg1{[at]}students{[dot]}waikato{[dot]}ac{[dot]}nz)
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
 *    AdaGrad.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.classifiers.functions;

import moa.classifiers.AbstractClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.classifiers.Regressor;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Utils;

/**
<!-- globalinfo-start -->
 * Implements the AdaGrad oneline optimiser for learning various linear models (binary class SVM, binary class logistic regression and linear regression). 
 * <p/>
<!-- globalinfo-end -->
 *
 */
public class AdaGrad extends AbstractClassifier implements Regressor{

    /** For serialization */
    private static final long serialVersionUID = -3732968666673530291L;

      @Override
    public String getPurposeString() {
        return "An online optimiser for learning various linear models (binary class SVM, binary class logistic regression and linear regression).";
    }

    /** The regularization parameter */
    protected double m_lambda = 0.0001;

    public FloatOption lambdaRegularizationOption = new FloatOption("lambdaRegularization",
            'l', "Lambda regularization parameter .",
            0.0001, 0.00, Integer.MAX_VALUE);

    /** The learning rate */
    protected double m_learningRate = 0.01;

    public FloatOption learningRateOption = new FloatOption("learningRate",
            'r', "Learning rate parameter.",
            0.01, 0.00, Integer.MAX_VALUE);

    /** The epsilon value */
    protected double m_epsilon = 1e-8;

    public FloatOption epsilonOption = new FloatOption("epsilon",
            'p', "epsilon parameter.",
            1e-8, 0.00, 1);

    /** Stores the weights (+ bias in the last element) */
    protected DoubleVector m_weights;
    protected DoubleVector m_gradients;
    protected DoubleVector m_velocity;
    protected double m_bias;
    protected double m_biasGradient;
    protected double m_biasVelocity;

    /** Holds the current iteration number */
    protected double m_t;

    protected static final int HINGE = 0;

    protected static final int LOGLOSS = 1;

    protected static final int SQUAREDLOSS = 2;

    /** The current loss function to minimize */
    protected int m_loss = HINGE;

    public MultiChoiceOption lossFunctionOption = new MultiChoiceOption(
            "lossFunction", 'o', "The loss function to use.", new String[]{
                "HINGE", "LOGLOSS", "SQUAREDLOSS"}, new String[]{
                "Hinge loss (SVM)",
                "Log loss (logistic regression)",
                "Squared loss (regression)"}, 0);

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
     * Set the learning rate.
     *
     * @param lr the learning rate to use.
     */
    public void setLearningRate(double lr) {
        m_learningRate = lr;
    }

    /**
     * Get the learning rate.
     *
     * @return the learning rate
     */
    public double getLearningRate() {
        return m_learningRate;
    }

    /**
     * Set the epsilon value.
     *
     * @param eps the epsilon value to use.
     */
    public void setEpsilon(double eps) {
        m_epsilon = eps;
    }

    /**
     * Get the epsilon value.
     *
     * @return the epsilon value
     */
    public double getEpsilon() {
        return m_epsilon;
    }

    /**
     * Reset the classifier.
     */
    public void reset() {
        m_t = 1;
        m_weights = null;
        m_gradients = null;
        m_velocity = null;
        m_bias = 0;
        m_biasGradient = 0;
        m_biasVelocity = 0;
    }

    protected static double dotProd(Instance inst1, DoubleVector weights, int classIndex) {
        double result = 0;

        int n1 = inst1.numValues();
        int n2 = weights.numValues();

        for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
            int ind1 = inst1.index(p1);
            int ind2 = p2;
            if (ind1 == ind2) {
                if (ind1 != classIndex && !inst1.isMissingSparse(p1)) {
                    result += inst1.valueSparse(p1) * weights.getValue(p2);
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

    @Override
    public void resetLearningImpl() {
        reset();
        setLambda(this.lambdaRegularizationOption.getValue());
        setLearningRate(this.learningRateOption.getValue());
        setEpsilon(this.epsilonOption.getValue());
        setLossFunction(this.lossFunctionOption.getChosenIndex());
    }

    /**
     * Trains the classifier with the given instance.
     *
     * @param instance    the new training instance to include in the model
     */
    @Override
    public void trainOnInstanceImpl(Instance instance) {

        if (m_weights == null) {
            m_weights = new DoubleVector();
            m_gradients = new DoubleVector();
            m_velocity = new DoubleVector();

            //Allocate the weights
            m_weights.setValue(instance.numAttributes(), 0);
        }

        if (instance.classIsMissing()) {
            return;
        }
   
        double z = dotProd(instance, m_weights, instance.classIndex()) + m_bias;

        double y;
        double yhat;
        double dldz;

        if (instance.classAttribute().isNominal()) {
            y = (instance.classValue() == 0) ? 0 : 1;
            
            if (m_loss == LOGLOSS) {
                yhat = 1.0 / (1.0 + Math.exp(-z));
                dldz = (yhat - y) * (yhat * (1.0 - yhat));
            }
            else {
                y = y * 2 - 1;
                yhat = z > 0.0 ? 1.0 : -1.0;
                
                if(y * z < 1.0)
                {
                    dldz = -y;
                }
                else
                {
                    dldz = 0;
                }
            }
        }
        else {
            y = instance.classValue();
            yhat = z;
            dldz = z - y;
        }


        for (int i = 0; i < m_weights.numValues(); i++) {
            //L2 Weight decay
            m_gradients.setValue(i, (m_lambda / m_t) * m_weights.getValue(i));
        }

        int n = instance.numValues();

        for(int i = 0; i < n; i++)
        {
            //Loss function gradient (sans regularisation)
            m_gradients.addToValue(instance.index(i), instance.valueSparse(i) * dldz);
        }

        //Weight update for the bias
        m_biasGradient = dldz;
        m_biasVelocity += m_biasGradient * m_biasGradient;
        m_bias -= (m_learningRate / Math.sqrt(m_biasVelocity)) * m_biasGradient;

        for(int i = 0; i < m_weights.numValues(); i++) {
            //Weight update
            double g = m_gradients.getValue(i);
            m_velocity.addToValue(i, g * g);
            m_weights.addToValue(i, -(m_learningRate / Math.sqrt(m_velocity.getValue(i))) * g);
        }

        m_t += 1.0;
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param instance    the instance to be classified
     * @return        predicted class probability distribution
     */
    @Override
    public double[] getVotesForInstance(Instance inst) {

        if (m_weights == null) {
            return new double[inst.numClasses()];
        }
        double[] result = (inst.classAttribute().isNominal())
                ? new double[2]
                : new double[1];


        double wx = dotProd(inst, m_weights, inst.classIndex());// * m_wScale;
        double z = (wx + m_bias);

        if (inst.classAttribute().isNumeric()) {
            result[0] = z;
            return result;
        }

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
    public String toString() {
        if (m_weights == null) {
            return "AdaGrad: No model built yet.\n";
        }
        StringBuffer buff = new StringBuffer();
        buff.append("Loss function: ");
        if (m_loss == HINGE) {
            buff.append("Hinge loss (SVM)\n\n");
        } else if (m_loss == LOGLOSS) {
            buff.append("Log loss (logistic regression)\n\n");
        } else {
            buff.append("Squared loss (linear regression)\n\n");
        }

        // buff.append(m_data.classAttribute().name() + " = \n\n");
        int printed = 0;

        for (int i = 0; i < m_weights.numValues(); i++) {
            // if (i != m_data.classIndex()) {
            if (printed > 0) {
                buff.append(" + ");
            } else {
                buff.append("   ");
            }

            buff.append(Utils.doubleToString(m_weights.getValue(i), 12, 4) + " "
                    // + m_data.attribute(i).name()
                    + "\n");

            printed++;
            //}
        }

        if (m_bias > 0) {
            buff.append(" + " + Utils.doubleToString(m_bias, 12, 4));
        } else {
            buff.append(" - " + Utils.doubleToString(-m_bias, 12, 4));
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
