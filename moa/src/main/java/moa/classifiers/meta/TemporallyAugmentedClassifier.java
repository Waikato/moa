/*
 *    TemporallyAugmentedClassifier.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
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
package moa.classifiers.meta;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.yahoo.labs.samoa.instances.*;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

/**
 * Include labels of previous instances into the training data
 *
 * <p>This enables a classifier to exploit potentially present auto-correlation
 * </p>
 *
 * <p>Parameters:</p> <ul> <li>-l : Classiﬁer to train</li> <li>-n : The number
 * of old labels to include</li> </ul>
 *
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision: 1 $
 */
public class TemporallyAugmentedClassifier extends AbstractClassifier implements MultiClassClassifier {

    @Override
    public String getPurposeString() {
        return "Add some old labels to every instance";
    }
    private static final long serialVersionUID = 1L;
    
    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");
    
    public IntOption numOldLabelsOption = new IntOption("numOldLabels", 'n',
            "The number of old labels to add to each example.", 1, 0, Integer.MAX_VALUE);
    
    protected Classifier baseLearner;
    
    protected double[] oldLabels;
    
    protected Instances header;

    public FlagOption labelDelayOption = new FlagOption("labelDelay", 'd',
        "Labels arrive with Delay. Use predictions instead of true Labels.");
    
    @Override
    public void resetLearningImpl() {
        this.baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        this.oldLabels = new double[this.numOldLabelsOption.getValue()];
        this.header = null;
        baseLearner.resetLearning();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        this.baseLearner.trainOnInstance(extendWithOldLabels(instance));
        if (this.labelDelayOption.isSet() == false) {
            // Use true old Labels to add attributes to instances
            addOldLabel(instance.classValue());
        }
    }

    public void addOldLabel(double newPrediction) {
        int numLabels = this.oldLabels.length;
        if (numLabels > 0) {
            for (int i = 1; i < numLabels; i++) {
                this.oldLabels[i - 1] = this.oldLabels[i];
            }
            this.oldLabels[ numLabels - 1] = newPrediction;
        }
    }

    public void initHeader(Instances dataset) {
        int numLabels = this.numOldLabelsOption.getValue();
        Attribute target = dataset.classAttribute();

        List<String> possibleValues = new ArrayList<String>();
        int n = target.numValues();
        for (int i = 0; i < n; i++) {
            possibleValues.add(target.value(i));
        }

        ArrayList<Attribute> attrs = new ArrayList<Attribute>(numLabels + dataset.numAttributes());
        for (int i = 0; i < numLabels; i++) {
            attrs.add(new Attribute(target.name() + "_" + i, possibleValues));
        }
        for (int i = 0; i < dataset.numAttributes(); i++) {
            Attribute attr = dataset.attribute(i);
            Attribute newAttribute = null;
            if (attr.isNominal() == true) {
                newAttribute = new Attribute(attr.name(), attr.getAttributeValues());
            }
            if (attr.isNumeric() == true) {
                newAttribute = new Attribute(attr.name());
            }
            if (newAttribute != null) {
                attrs.add(newAttribute);
            }
        }
        this.header = new Instances("extended_" + dataset.getRelationName(), attrs, 0);
        this.header.setClassIndex(numLabels + dataset.classIndex());
    }

    public Instance extendWithOldLabels(Instance instance) {
        if (this.header == null) {
            initHeader(instance.dataset());
            this.baseLearner.setModelContext(new InstancesHeader(this.header));
        }
        int numLabels = this.oldLabels.length;
        if (numLabels == 0) {
            return instance;
        }
        double[] x = instance.toDoubleArray();
        double[] x2 = Arrays.copyOfRange(this.oldLabels, 0, numLabels + x.length);
        System.arraycopy(x, 0, x2, numLabels, x.length);
        Instance extendedInstance = new DenseInstance(instance.weight(), x2);
        extendedInstance.setDataset(this.header);
        //System.out.println( extendedInstance);
        return extendedInstance;
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        double[] prediction = this.baseLearner.getVotesForInstance(extendWithOldLabels(instance));
        if (this.labelDelayOption.isSet() == true) {
            // Use predicted Labels to add attributes to instances
            addOldLabel(Utils.maxIndex(prediction));
        }
        return prediction;
    }

    @Override
    public boolean isRandomizable() {
        return false; // ??? this.baseLearner.isRandomizable;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList<Measurement>();
        Measurement[] modelMeasurements = ((AbstractClassifier) this.baseLearner).getModelMeasurements();
        if (modelMeasurements != null) {
            for (Measurement measurement : modelMeasurements) {
                measurementList.add(measurement);
            }
        }
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // TODO Auto-generated method stub
    }

    public String toString() {
        return "TemporallyAugmentedClassifier using " + this.numOldLabelsOption.getValue() + " labels\n" + this.baseLearner;
    }
}
