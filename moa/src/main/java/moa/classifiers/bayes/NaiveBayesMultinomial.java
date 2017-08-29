/*
 *    NaiveBayesMultinomial.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
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

package moa.classifiers.bayes;

import java.util.Arrays;
import com.github.javacliparser.FloatOption;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.core.Utils;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * <!-- globalinfo-start --> Class for building and using a multinomial Naive
 * Bayes classifier. Performs text classic bayesian prediction while making
 * naive assumption that all inputs are independent. For more information
 * see,<br/> <br/> Andrew Mccallum, Kamal Nigam: A Comparison of Event Models
 * for Naive Bayes Text Classification. In: AAAI-98 Workshop on 'Learning for
 * Text Categorization', 1998.<br/> <br/> The core equation for this
 * classifier:<br/> <br/> P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)<br/>
 * <br/> where Ci is class i and D is a document.<br/> <br/> Incremental version
 * of the algorithm.
 * <p/>
 * <!-- globalinfo-end --> * <!-- technical-bibtex-start --> BibTeX:
 * <pre>
 * &#64;inproceedings{Mccallum1998,
 *    author = {Andrew Mccallum and Kamal Nigam},
 *    booktitle = {AAAI-98 Workshop on 'Learning for Text Categorization'},
 *    title = {A Comparison of Event Models for Naive Bayes Text Classification},
 *    year = {1998}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 */
public class NaiveBayesMultinomial extends AbstractClassifier  implements MultiClassClassifier {

    public FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection",
            'l', "Laplace correction factor.",
            1.0, 0.00, Integer.MAX_VALUE);

    /**
     * for serialization
     */
    private static final long serialVersionUID = -7204398796974263187L;

    @Override
    public String getPurposeString() {
        return "Multinomial Naive Bayes classifier: performs classic bayesian prediction while making naive assumption that all inputs are independent.";
    }

    /**
     * sum of weight_of_instance * word_count_of_instance for each class
     */
    protected double[] m_classTotals;

    /**
     * copy of header information for use in toString method
     */
    protected Instances m_headerInfo;

    /**
     * number of class values
     */
    protected int m_numClasses;

    /**
     * the probability of a class (i.e. Pr[H])
     */
    protected double[] m_probOfClass;

    /**
     * probability that a word (w) exists in a class (H) (i.e. Pr[w|H]) The
     * matrix is in the this format: m_wordTotalForClass[wordAttribute][class]
     */
    protected DoubleVector[] m_wordTotalForClass;

    protected boolean reset = false;

    @Override
    public void resetLearningImpl() {
        this.reset = true;
    }

    /**
     * Trains the classifier with the given instance.
     *
     * @param instance the new training instance to include in the model
     */
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.reset == true) {
            this.m_numClasses = inst.numClasses();
            double laplace = this.laplaceCorrectionOption.getValue();
            int numAttributes = inst.numAttributes();

            m_probOfClass = new double[m_numClasses];
            Arrays.fill(m_probOfClass, laplace);

            m_classTotals = new double[m_numClasses];
            Arrays.fill(m_classTotals, laplace * numAttributes);

            m_wordTotalForClass = new DoubleVector[m_numClasses];
            for (int i = 0; i< m_numClasses; i++) {
                //Arrays.fill(wordTotal, laplace);
                m_wordTotalForClass[i] = new DoubleVector();
            }
            this.reset = false;
        }
        // Update classifier
        int classIndex = inst.classIndex();
        int classValue = (int) inst.classValue();//value(classIndex);

        double w = inst.weight();
        m_probOfClass[classValue] += w;

        m_classTotals[classValue] += w * totalSize(inst);
        double total = m_classTotals[classValue];

        for (int i = 0; i < inst.numValues(); i++) {
            int index = inst.index(i);
            if (index != classIndex && !inst.isMissing(i)) {
                //m_wordTotalForClass[index][classValue] += w * inst.valueSparse(i);
                double laplaceCorrection = 0.0;
                if (m_wordTotalForClass[classValue].getValue(index)== 0) {
                    laplaceCorrection = this.laplaceCorrectionOption.getValue();
                }
                m_wordTotalForClass[classValue].addToValue(index, w * inst.valueSparse(i) + laplaceCorrection);
            }
        }
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     */
    @Override
    public double[] getVotesForInstance(Instance instance) {
        if (this.reset == true) {
            return new double[2];
        }
        double[] probOfClassGivenDoc = new double[m_numClasses];
        double totalSize = totalSize(instance);

        for (int i = 0; i < m_numClasses; i++) {
            probOfClassGivenDoc[i] = Math.log(m_probOfClass[i]) - totalSize * Math.log(m_classTotals[i]);
        }

        for (int i = 0; i < instance.numValues(); i++) {

            int index = instance.index(i);
            if (index == instance.classIndex() || instance.isMissing(i)) {
                continue;
            }

            double wordCount = instance.valueSparse(i);
            for (int c = 0; c < m_numClasses; c++) {
                double value = m_wordTotalForClass[c].getValue(index);
                probOfClassGivenDoc[c] += wordCount * Math.log(value == 0 ? this.laplaceCorrectionOption.getValue() : value );
            }
        }

        return Utils.logs2probs(probOfClassGivenDoc);
    }

    public double totalSize(Instance instance) {
        int classIndex = instance.classIndex();
        double total = 0.0;
        for (int i = 0; i < instance.numValues(); i++) {
            int index = instance.index(i);
            if (index == classIndex || instance.isMissing(i)) {
                continue;
            }
            double count = instance.valueSparse(i);
            if (count >= 0) {
                total += count;
            } else {
                //throw new Exception("Numeric attribute value is not >= 0. " + i + " " + index + " " +
                //		    instance.valueSparse(i) + " " + " " + instance);
            }
        }
        return total;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder result, int indent) {
        StringUtils.appendIndented(result, indent, "xxx MNB1 xxx\n\n");

        result.append("The independent probability of a class\n");
        result.append("--------------------------------------\n");

        for (int c = 0; c < m_numClasses; c++) {
            result.append(m_headerInfo.classAttribute().value(c)).append("\t").
                    append(Double.toString(m_probOfClass[c])).append("\n");
        }

        result.append("\nThe probability of a word given the class\n");
        result.append("-----------------------------------------\n\t");

        for (int c = 0; c < m_numClasses; c++) {
            result.append(m_headerInfo.classAttribute().value(c)).append("\t");
        }

        result.append("\n");

        for (int w = 0; w < m_headerInfo.numAttributes(); w++) {
            if (w == m_headerInfo.classIndex()) {
                continue;
            }
            result.append(m_headerInfo.attribute(w).name()).append("\t");
            for (int c = 0; c < m_numClasses; c++) {
                double value = m_wordTotalForClass[c].getValue(w);
                if (value == 0){
                    value = this.laplaceCorrectionOption.getValue();
                }
                result.append(value / m_classTotals[c]).append("\t");
            }
            result.append("\n");
        }
        StringUtils.appendNewline(result);
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
