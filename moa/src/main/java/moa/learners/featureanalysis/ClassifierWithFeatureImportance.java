/*
 *    FeatureScore.java
 *    Copyright (C) 2020 University of Waikato, Hamilton, New Zealand
 *    @author Heitor Murilo Gomes (hgomes at waikato dot ac dot nz)
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
package moa.learners.featureanalysis;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.meta.AdaptiveRandomForest;
import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Classifier with Feature Importance
 *
 * <p>This meta algorithm serves the purpose of executing a classifier also capable of outputting feature importances.
 * Classifiers implementing the moa.streams.FeatureImportance interface </p>
 *
 * <p>Hyperparameters:</p> <ul>
 * <li>-l : Learner implementing FeatureImportance interface. </li>
 * <li>-n : Whether to normalize or not the feature importances. </li>
 * <li>-w : How often to verify and output </li>
 * <li>-o : Maximum number of features to include in the output file. </li>
 * <li>-c : Debug file</li>
 * <li>-d : Whether to output the results to the debug file or not.
 * Useful to analyze the </li>
 * </ul>
 *
 * @author Heitor Murilo Gomes (heitor dot gomes at waikato dot ac dot nz)
 * @version $Revision: 1 $
 */
public class ClassifierWithFeatureImportance extends AbstractClassifier
        implements MultiClassClassifier {
    @Override
    public String getPurposeString() {
        return "Only produces feature scores for tree-based algorithms.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption featureImportanceLearnerOption = new ClassOption("featureImportanceLearner", 'l',
            "Learner used to build the model from which the feature importances are extracted",
            FeatureImportanceClassifier.class, "moa.learners.featureanalysis.FeatureImportanceHoeffdingTree");

    public FlagOption doNotNormalizeFeatureScoreOption = new FlagOption("doNotNormalizeFeatureScore", 'n',
            "If set the feature importances will not be normalized");

    public IntOption windowSizeOption = new IntOption("windowSize", 'w',
            "The amount of instances seen before inspecting the feature scores again.", 500, 1, Integer.MAX_VALUE);

    public IntOption maxFeaturesDebugOption = new IntOption("maxFeaturesDebug", 'o',
            "The maximum number of features to show in the debug.", 100, 1, Integer.MAX_VALUE);

    public FileOption debugFileOption = new FileOption("debugFile", 'c',
            "File to append the feature scores.", "debug.csv", "csv", true);

    public FlagOption doNotOutputResultsToFileOption = new FlagOption("doNotOutputResultsToFile", 'd',
            "To evaluate the resources usage without writing the feature importance to a file.");

    protected PrintStream debugStream;

    protected long instancesSeen = 0;
    protected FeatureImportanceClassifier featureImportanceClassifierLearner;

    protected double mean = -1.0;
    protected double median = -1.0;
    protected double max = -1.0;
    protected double min = Double.POSITIVE_INFINITY;
    protected double sum = 0.0;

    protected void createDebugOutputFile() {
        if(this.doNotOutputResultsToFileOption.isSet() == false) {
            File dumpFile = this.debugFileOption.getFile();
            if (dumpFile != null) {
                try {
                    if (dumpFile.exists()) {
                        this.debugStream = new PrintStream(new FileOutputStream(dumpFile, true), true);
                    } else {
                        this.debugStream = new PrintStream(new FileOutputStream(dumpFile), true);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Unable to open immediate result file: " + dumpFile, ex);
                }
            }
        }
    }

    @Override
    public void resetLearningImpl() {
        this.instancesSeen = 0;
        this.featureImportanceClassifierLearner = null;
        this.featureImportanceClassifierLearner = (FeatureImportanceClassifier) getPreparedClassOption(this.featureImportanceLearnerOption);
        this.featureImportanceClassifierLearner.resetLearning();
        this.createDebugOutputFile();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        if (this.instancesSeen == 0 && this.debugStream != null) {
            // Debug info
            this.debugStream.println(this.describe());
            this.debugStream.print("instancesSeen,median,mean,max,min,sum");
            for (int i = 0; i < instance.numAttributes() - 1 && i < this.maxFeaturesDebugOption.getValue(); ++i) {
                this.debugStream.print(",top" + i);
            }
            for (int i = 0; i < instance.numAttributes() - 1 && i < this.maxFeaturesDebugOption.getValue(); ++i) {
                this.debugStream.print(",score(top" + i + ")");
            }
            for (int i = 0; i < instance.numAttributes() - 1 && i < this.maxFeaturesDebugOption.getValue(); ++i) {
                this.debugStream.print(",score(att" + i + "-" + instance.attribute(i).name() + ")");
            }
            this.debugStream.println();
        }

        ++this.instancesSeen;
        this.featureImportanceClassifierLearner.trainOnInstance(instance);

        if (this.instancesSeen % this.windowSizeOption.getValue() == 0) {
            // Output information
            double[] currentScore = this.featureImportanceClassifierLearner.getFeatureImportances(!this.doNotNormalizeFeatureScoreOption.isSet());
            int[] allTopK = this.featureImportanceClassifierLearner.getTopKFeatures(instance.numAttributes() - 1, !this.doNotNormalizeFeatureScoreOption.isSet());

            this.median = allTopK.length % 2 == 0
                    ? (currentScore[allTopK[allTopK.length / 2]] + currentScore[allTopK[allTopK.length / 2 - 1]]) / 2.0
                    : currentScore[allTopK[allTopK.length / 2]];
            this.mean = Utils.mean(currentScore);
            this.max = currentScore[Utils.maxIndex(currentScore)];
            this.min = currentScore[Utils.minIndex(currentScore)];
            this.sum = Utils.sum(currentScore);

            if(this.debugStream != null) {
                this.debugStream.print(this.instancesSeen + "," + median + "," + mean + "," + max + "," + min + "," + sum);

                for (int i = 0; i < allTopK.length && i < this.maxFeaturesDebugOption.getValue(); ++i) {
                    this.debugStream.print("," + allTopK[i] + "-" + instance.attribute(allTopK[i]).name());
                }
                for (int i = 0; i < currentScore.length && i < this.maxFeaturesDebugOption.getValue(); ++i) {
                    this.debugStream.print("," + currentScore[allTopK[i]]);
                }
                for (int i = 0; i < currentScore.length && i < this.maxFeaturesDebugOption.getValue(); ++i) {
                    this.debugStream.print("," + currentScore[i]);
                }
                this.debugStream.println();
            }
        }
    }

    public double[] getCurrentFeatureImportances() {
        return this.featureImportanceClassifierLearner.getFeatureImportances(!this.doNotNormalizeFeatureScoreOption.isSet());
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        return this.featureImportanceClassifierLearner.getVotesForInstance(instance);
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public void getModelDescription(StringBuilder arg0, int arg1) {
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }


    /**
     * Describe the feature importance method used. This is added to the first line of the output file.
     * @return description of the model used
     */
    protected String describe() {
        StringBuilder description = new StringBuilder();
        String name = this.featureImportanceClassifierLearner.getClass().getName();

        description.append(name.substring(name.lastIndexOf('.') + 1));
        description.append("_norm");
        description.append(this.doNotNormalizeFeatureScoreOption.isSet() ? "NO" : "YES");
        // If it is an instance of HoeffdingTree...
        if (this.featureImportanceClassifierLearner instanceof FeatureImportanceHoeffdingTree) {
            description.append("_gp");
            description.append(((FeatureImportanceHoeffdingTree) this.featureImportanceClassifierLearner).treeLearner.gracePeriodOption.getValue());
            description.append("_sc");
            description.append(((FeatureImportanceHoeffdingTree) this.featureImportanceClassifierLearner).treeLearner.splitConfidenceOption.getValue());
            description.append("_");
            description.append(((FeatureImportanceHoeffdingTree) this.featureImportanceClassifierLearner).treeLearner.splitCriterionOption.getValueAsCLIString());
            description.append("_");
            description.append(((FeatureImportanceHoeffdingTree) this.featureImportanceClassifierLearner).featureImportanceOption.getChosenLabel());
        }
        // If this is an ensemble...
        if (this.featureImportanceClassifierLearner.getSubClassifiers() != null) {
            description.append("_s");
            description.append(this.featureImportanceClassifierLearner.getSubClassifiers().length);
            if (this.featureImportanceClassifierLearner instanceof FeatureImportanceHoeffdingTreeEnsemble) {
                // If this is an instance of AdaptiveRandomForest...
                if (((FeatureImportanceHoeffdingTreeEnsemble) this.featureImportanceClassifierLearner).ensemble instanceof AdaptiveRandomForest) {
                    AdaptiveRandomForest arf = (AdaptiveRandomForest) ((FeatureImportanceHoeffdingTreeEnsemble) this.featureImportanceClassifierLearner).ensemble;
                    description.append("_m");
                    description.append(arf.mFeaturesModeOption.getChosenLabel());
                    description.append("_");
                    description.append(arf.mFeaturesPerTreeSizeOption.getValue());
                    description.append("_lambda");
                    description.append(arf.lambdaOption.getValue());

                    description.append("_");
                    description.append(arf.treeLearnerOption.getValueAsCLIString());
                }
            }
        }
        return description.toString();
    }
}
