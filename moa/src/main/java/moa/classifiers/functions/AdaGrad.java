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
package moa.classifiers.functions;

import moa.core.DoubleVector;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Utils;

/**
<!-- globalinfo-start -->
 * Implements the AdaGrad oneline optimiser for learning various linear models (binary class SVM, binary class logistic regression and linear regression). For more information, see<br/> <br/> Duchi, J., Hazan, E., &amp; Singer, Y. (2011). Adaptive subgradient methods for online learning and stochastic optimization. Journal of Machine Learning Research, 12 (Jul), 2121-2159.
 * <p/>
 * <!-- globalinfo-end -->
 * *
<!-- technical-bibtex-start --> BibTeX:
 * <pre>
 * &#64;inproceedings{duchi2011,
 *    author = {Duchi, John and Hazan, Elad and Singer, Yoram},
 *    booktitle = {Journal of Machine Learning Research},
 *    pages = {2121--2159},
 *    volume={12},
 *    number={Jul},
 *    title = {Adaptive subgradient methods for online learning and stochastic optimization},
 *    year = {2011}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 *
 */
public class AdaGrad extends SGD{

    /** For serialization */
    private static final long serialVersionUID = -3732968666673530291L;

    @Override
    public String getPurposeString() {
        return "An online optimiser for learning various linear models (binary class SVM, binary class logistic regression and linear regression).";
    }
 
    /** The epsilon value */
    protected double m_epsilon = 1e-8;

    public FloatOption epsilonOption = new FloatOption("epsilon",
            'p', "epsilon parameter.",
            1e-8);

    /** Stores the weights (+ bias in the last element) */
    protected DoubleVector m_velocity;
    protected double m_biasVelocity;

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

    public AdaGrad() {
        lambdaRegularizationOption = new FloatOption(
                lambdaRegularizationOption.getName(),
                lambdaRegularizationOption.getCLIChar(),
                lambdaRegularizationOption.getPurpose(),
                0.0);

        learningRateOption = new FloatOption(
                learningRateOption.getName(),
                learningRateOption.getCLIChar(),
                learningRateOption.getPurpose(),
                0.01);
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
            m_velocity = new DoubleVector();
            m_bias = 0;

            m_weights.setValue(instance.numAttributes(), 0);
            m_velocity.setValue(instance.numAttributes(), 0);
        }

        if (instance.classIsMissing()) {
            return;
        }
   
        double z = dotProd(instance, m_weights, instance.classIndex()) + m_bias;

        double y;
        double dldz;

        if (instance.classAttribute().isNominal()) {
            y = (instance.classValue() == 0) ? 0 : 1;
            
            if (m_loss == LOGLOSS) {
                double yhat = 1.0 / (1.0 + Math.exp(-z));
                dldz = (yhat - y);
            }
            else {
                y = y * 2 - 1;
                
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
            dldz = z - y;
        }

        int n = instance.numValues();
        DoubleVector gradients = new DoubleVector();
        gradients.setValue(instance.numAttributes(), 0);

        for(int i = 0; i < n; i++)
        {
            int idx = instance.index(i);
            gradients.setValue(idx, instance.valueSparse(i) * dldz + (m_lambda / (m_t + m_epsilon)) * m_weights.getValue(idx));
        }

        //Weight update for the bias
        double biasGradient = dldz;
        m_biasVelocity += biasGradient * biasGradient;
        m_bias -= (m_learningRate / (Math.sqrt(m_biasVelocity) + m_epsilon)) * biasGradient;

        for(int i = 0; i < m_weights.numValues(); i++) {
            //Weight update
            double g = gradients.getValue(i);
            m_velocity.addToValue(i, g * g);
            m_weights.addToValue(i, -(m_learningRate / (Math.sqrt(m_velocity.getValue(i)) + m_epsilon)) * g);
        }

        m_t += 1.0;
    }

    @Override
    protected String getModelName() {
        return "AdaGrad";
    }
}
