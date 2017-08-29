/*
 *    WEKAClassifier.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *    @author FracPete (fracpete at waikato dot ac dot nz)
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

import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import com.github.javacliparser.IntOption;
import moa.options.WEKAClassOption;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;

/**
 * Class for using a classifier from WEKA.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class WEKAClassifier
        extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    protected SamoaToWekaInstanceConverter instanceConverter;

    @Override
    public String getPurposeString() {
        return "Classifier from Weka";
    }
    
    public WEKAClassOption baseLearnerOption = new WEKAClassOption("baseLearner", 'l',
            "Classifier to train.", weka.classifiers.Classifier.class, "weka.classifiers.bayes.NaiveBayesUpdateable");

    public IntOption widthOption = new IntOption("width",
            'w', "Size of Window for training learner.", 0, 0, Integer.MAX_VALUE);

    public IntOption widthInitOption = new IntOption("widthInit",
            'i', "Size of first Window for training learner.", 1000, 0, Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
            'f',
            "How many instances between samples of the learning performance.",
            0, 0, Integer.MAX_VALUE);

    protected Classifier classifier;

    protected int numberInstances;

    protected weka.core.Instances instancesBuffer;

    protected boolean isClassificationEnabled;

    protected boolean isBufferStoring;

    @Override
    public void resetLearningImpl() {

        try {
            //System.out.println(baseLearnerOption.getValue());
            String[] options = weka.core.Utils.splitOptions(baseLearnerOption.getValueAsCLIString());
            createWekaClassifier(options);
        } catch (Exception e) {
            System.err.println("Creating a new classifier: " + e.getMessage());
        }
        numberInstances = 0;
        isClassificationEnabled = false;
        this.isBufferStoring = true;
        this.instanceConverter = new SamoaToWekaInstanceConverter();
    }

    @Override
    public void trainOnInstanceImpl(Instance samoaInstance) {
        weka.core.Instance inst = this.instanceConverter.wekaInstance(samoaInstance);
        try {
            if (numberInstances == 0) {
                this.instancesBuffer = new weka.core.Instances(inst.dataset());
                if (classifier instanceof UpdateableClassifier) {
                    classifier.buildClassifier(instancesBuffer);
                    this.isClassificationEnabled = true;
                } else {
                    this.isBufferStoring = true;
                }
            }
            numberInstances++;

            if (classifier instanceof UpdateableClassifier) {
                if (numberInstances > 0) {
                    ((UpdateableClassifier) classifier).updateClassifier(inst);
                }
            } else {
                if (numberInstances == widthInitOption.getValue()) {
                    //Build first time Classifier
                    buildClassifier();
                    isClassificationEnabled = true;
                    //Continue to store instances
                    if (sampleFrequencyOption.getValue() != 0) {
                        isBufferStoring = true;
                    }
                }
                if (widthOption.getValue() == 0) {
                    //Used from SingleClassifierDrift
                    if (isBufferStoring == true) {
                        instancesBuffer.add(inst);
                    }
                } else {
                    //Used form WekaClassifier without using SingleClassifierDrift
                    int numInstances = numberInstances % sampleFrequencyOption.getValue();
                    if (sampleFrequencyOption.getValue() == 0) {
                        numInstances = numberInstances;
                    }
                    if (numInstances == 0) {
                        //Begin to store instances
                        isBufferStoring = true;
                    }
                    if (isBufferStoring == true && numInstances <= widthOption.getValue()) {
                        //Store instances
                        instancesBuffer.add(inst);
                    }
                    if (numInstances == widthOption.getValue()) {
                        //Build Classifier
                        buildClassifier();
                        isClassificationEnabled = true;
                        this.instancesBuffer = new weka.core.Instances(inst.dataset());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Training: " + e.getMessage());
        }
    }

    public void buildClassifier() {
        try {
            if ((classifier instanceof UpdateableClassifier) == false) {
                Classifier auxclassifier = weka.classifiers.AbstractClassifier.makeCopy(classifier);
                auxclassifier.buildClassifier(instancesBuffer);
                classifier = auxclassifier;
                isBufferStoring = false;
            }
        } catch (Exception e) {
            System.err.println("Building WEKA Classifier: " + e.getMessage());
        }
    }

    @Override
    public double[] getVotesForInstance(Instance samoaInstance) {
        weka.core.Instance inst = this.instanceConverter.wekaInstance(samoaInstance);
        double[] votes = new double[inst.numClasses()];
        if (isClassificationEnabled == false) {
            for (int i = 0; i < inst.numClasses(); i++) {
                votes[i] = 1.0 / inst.numClasses();
            }
		} else {
			try {
				votes = this.classifier.distributionForInstance(inst);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		return votes;
	}

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        if (classifier != null) {
            out.append(classifier.toString());
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Measurement[] m = new Measurement[0];
        return m;
    }

    public void createWekaClassifier(String[] options) throws Exception {
        String classifierName = options[0];
        String[] newoptions = options.clone();
        newoptions[0] = "";
        this.classifier = weka.classifiers.AbstractClassifier.forName(classifierName, newoptions);
    }
}
