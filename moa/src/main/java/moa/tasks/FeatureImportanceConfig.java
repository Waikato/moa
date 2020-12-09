/*
 *    FeatureImportanceConfig.java
 *    Copyright (C) 2020 University of Waikato, Hamilton, New Zealand
 *    @author Yongheng Ma (2560653665@qq.com)
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
package moa.tasks;


import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instances;
import moa.capabilities.CapabilitiesHandler;
import moa.core.ObjectRepository;
import moa.learners.featureanalysis.ClassifierWithFeatureImportance;
import moa.options.ClassOption;

import javax.swing.*;
import java.util.Arrays;

/**
 * This class Provides GUI to user so that they can configure parameters for feature importance algorithm.
 * After user clicks Run button, this class executes task to compute scores of feature importance.
 */
public class FeatureImportanceConfig extends ClassificationMainTask implements CapabilitiesHandler {

    private static final long serialVersionUID = 1L;

    /**
     * This holds the current set of instances
     */
    protected Instances m_instances;

    /** Scores produced by feature importance algorithm. */
    protected double[][] scores;

    /** When scores of feature importance are NaNs, NaNs will be replaced by NaNSubstitute
     * shown in feature importance line graph.*/
    protected double m_NaNSubstitute=0.0;//default value is 0.0

    /** The default windowSize parameter for feature importance algorithm. */
    protected int m_windowSize=500;

    /** The default doNotNormalizeFeatureScore parameter for feature importance algorithm.*/
    protected boolean m_doNotNormalizeFeatureScore=false;

    /** Use progress bar to show the progress of computing scores of feature importance. */
    protected JProgressBar progressBar=new JProgressBar();

    public double getNaNSubstitute() {
        return m_NaNSubstitute;
    }

    public void setNaNSubstitute(double NaNSubstitute) {
        this.m_NaNSubstitute = NaNSubstitute;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public int getWindowSize() {
        return m_windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.m_windowSize = windowSize;
    }

    public boolean doNotNormalizeFeatureScore() {
        return m_doNotNormalizeFeatureScore;
    }

    public void setDoNotNormalizeFeatureScore(boolean doNotNormalizeFeatureScore) {
        this.m_doNotNormalizeFeatureScore = doNotNormalizeFeatureScore;
    }

    /**
     * Provides GUI to user so that they can configure parameters for feature importance algorithm.
     */
    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Classifier with feature importance Learner to train.", ClassifierWithFeatureImportance.class,
            "moa.learners.featureanalysis.ClassifierWithFeatureImportance");

    public FloatOption nanSubstitute = new FloatOption(
            "NaNSubstitute", 'u',
            "When scores of feature importance are NaN, NaN will be replaced by NaNSubstitute shown in line graph.", 0,
            Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);

    /**
     * After user clicks Run button, this method executes task to compute scores of feature importance and return.
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     * @return scores of features' importance
     */
    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ClassifierWithFeatureImportance cwfi = (ClassifierWithFeatureImportance) getPreparedClassOption(learnerOption);
        cwfi.resetLearningImpl();

        int windowSize = cwfi.windowSizeOption.getValue();
        setWindowSize(windowSize);

        boolean doNotNormalizeFeatureScore = cwfi.doNotNormalizeFeatureScoreOption.isSet();
        setDoNotNormalizeFeatureScore(doNotNormalizeFeatureScore);

        double nanSubstitute = this.nanSubstitute.getValue();
        setNaNSubstitute(nanSubstitute);

        int row = 0;
        int instanceSeen = 0;

        int numInstances = m_instances.numInstances();
        int rows = numInstances / windowSize; //neglect remainder
        int columns = m_instances.numAttributes() - 1;// There is no feature importance for class
        double[][] scores = new double[rows][columns];

        progressBar.setValue(0);
        progressBar.setMaximum(rows);

        if (m_instances != null) {

                for (int i = 0; i < numInstances; i++) {
                    instanceSeen++;

                    /** First train, then get scores. */
                    cwfi.trainOnInstance(m_instances.get(i));//train

                    if (instanceSeen % windowSize == 0) {
                        double[] currentScore = cwfi.getCurrentFeatureImportances();//get scores
                        for (int j = 0; j < columns; j++) {
                            scores[row][j] = currentScore[j];
                        }
                        row++;
                        progressBar.setValue(row);
                    }

                }

        }
        return scores;
    }

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    @Override
    public String getPurposeString() {
        return "Set parameters for feature importance learner to get scores of feature importance.";
    }

    public void setInstances(Instances instances) {
        this.m_instances = instances;
    }
}
