/*
 *    WEKAClassifier.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet
 *    @author FracPete (fracpete at waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import sizeof.agent.SizeOfAgent;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.Instances;

import moa.core.Measurement;
import moa.options.IntOption;
import moa.options.WEKAClassOption;

public class WEKAClassifier
  extends AbstractClassifier {

	private static final long serialVersionUID = 1L;

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

	protected Instances instancesBuffer;

	protected boolean isClassificationEnabled;

	protected boolean isRelearnEnabled;

	@Override
	public int measureByteSize() {
		int size = (int) SizeOfAgent.sizeOf(this);
		//size += classifier.measureByteSize();
		return size;
	}

	@Override
	public void resetLearningImpl() {

		try{
			//System.out.println(baseLearnerOption.getValue());
			String[] options = weka.core.Utils.splitOptions(baseLearnerOption.getValueAsCLIString());
			createWekaClassifier(options);
		}  catch (Exception e) {
			System.err.println("Creating a new classifier: "+e.getMessage());
		}
		numberInstances = 0;
		isClassificationEnabled = false;
		this.isRelearnEnabled = true;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		try {
			if (numberInstances == 0) {
				this.instancesBuffer = new Instances(inst.dataset());
				if (classifier instanceof UpdateableClassifier) {
					classifier.buildClassifier(instancesBuffer);
					this.isClassificationEnabled = true;
				} else {
					this.isRelearnEnabled = true;
				}
			}
			numberInstances++;

			if (classifier instanceof UpdateableClassifier) {
				if (numberInstances > 0 ) {
					((UpdateableClassifier) classifier).updateClassifier(inst);
				}
			} else {
				if (widthOption.getValue() == 0 ){
					if (isRelearnEnabled == true) {
						instancesBuffer.add(inst);
					}
				} else {
					if (isRelearnEnabled == true && (numberInstances % sampleFrequencyOption.getValue()) <= widthOption.getValue()) {
						instancesBuffer.add(inst);
					}

					if (sampleFrequencyOption.getValue() != 0 && (numberInstances % sampleFrequencyOption.getValue() == 0)) {
						isRelearnEnabled = true;
					}
					if (numberInstances == widthInitOption.getValue() ){ //|| numberInstances == 30 ||numberInstances == 5 ) {
						buildClassifier();
						isClassificationEnabled = true;
					}

					if (numberInstances == widthOption.getValue() ){ 
						buildClassifier();
						isClassificationEnabled = true;
						this.instancesBuffer = new Instances(inst.dataset());
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Training: "+e.getMessage());
		}
	}

	public void buildClassifier() {
		try {
			if ((classifier instanceof UpdateableClassifier) == false ) {
				Classifier auxclassifier= Classifier.makeCopy(classifier);
				auxclassifier.buildClassifier(instancesBuffer);
				classifier = auxclassifier;
				isRelearnEnabled = false;	
			}
		} catch (Exception e) {
			System.err.println("Building WEKA Classifier: "+e.getMessage());
		}
	}

	public double[] getVotesForInstance(Instance inst) {
		double[] votes= new double[inst.numClasses()];
		if (isClassificationEnabled == false ) {
			for ( int i=0;  i<inst.numClasses(); i++) {
				votes[i] = 1.0/inst.numClasses();
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
		this.classifier = Classifier.forName(classifierName, newoptions);
	}
}
