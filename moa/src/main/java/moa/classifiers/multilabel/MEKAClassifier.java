/*
 *    MEKAClassifier.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jesse@tsc.uc3m.es)
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
package moa.classifiers.multilabel;

import java.util.Arrays;
import moa.core.Measurement;
import weka.classifiers.UpdateableClassifier;
import moa.classifiers.AbstractClassifier;
import moa.core.Measurement;
import com.github.javacliparser.IntOption;
import moa.options.WEKAClassOption;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;

import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Example;

/**
 * Wrapper for MEKA classifiers. 
 *
 * @author Jesse Read
 * @version $Revision: 1 $
 */
public class MEKAClassifier extends AbstractMultiLabelLearner implements MultiTargetRegressor {

	private static final long serialVersionUID = 1L;

    protected SamoaToWekaInstanceConverter instanceConverter;

    @Override
    public String getPurposeString() {
        return "Classifier from Meka";
    }
    
    public WEKAClassOption baseLearnerOption = new WEKAClassOption("baseLearner", 'l',
            "Classifier to train.", weka.classifiers.Classifier.class, "meka.classifiers.multilabel.incremental.BRUpdateable");

    protected Classifier classifier;

    protected int numberInstances;

    protected weka.core.Instances instancesBuffer;

    protected boolean isClassificationEnabled;

    protected boolean isBufferStoring;


	private int L = 0;

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
	public void trainOnInstanceImpl(MultiLabelInstance samoaInstance) {

		// Convert instance
		L = samoaInstance.numberOutputTargets();
		weka.core.Instance x = this.instanceConverter.wekaInstance(samoaInstance);
		x.dataset().setClassIndex(L);

		if (numberInstances == 0) {
			// This is the first instance -- Setup!
			this.instancesBuffer = new weka.core.Instances(x.dataset());
			if (classifier instanceof UpdateableClassifier) {
				try {
					System.err.println("Setting L="+L+" to MEKA classifier");
					instancesBuffer.setClassIndex(L);
					classifier.buildClassifier(instancesBuffer);
				} catch(Exception e) {
					System.err.println("[ERROR] Failed to build classifier");
					e.printStackTrace();
					System.exit(1);
				}
				this.isClassificationEnabled = true;
			} else {
				this.isBufferStoring = true;
				System.out.println("[ERROR] Please use an Updateable Classifier");
			}
		}
		numberInstances++;

		// Update classifier
		if (classifier instanceof UpdateableClassifier) {
			if (numberInstances > 0) {
				try {
					((UpdateableClassifier) classifier).updateClassifier(x);
				} catch(Exception e) {
					System.err.println("[ERROR] Failed to update classifier");
					e.printStackTrace();
					System.exit(1);
				}
			}
			else {
				System.out.println("[TO-DO] Non-Updateable MEKA classifiers should also work here!");
			}
		}
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance samoaInstance) {

		weka.core.Instance inst = this.instanceConverter.wekaInstance(samoaInstance);

		MultiLabelPrediction prediction=null;

		if (isClassificationEnabled == true){ 

			prediction = new MultiLabelPrediction(L);
			double votes[] = new double[L];
			try {
				votes = this.classifier.distributionForInstance(inst);
			} catch(Exception e) {
				System.err.println("[ERROR] Failed to get votes from multi-label classifier.");
				e.printStackTrace();
				System.exit(1);
			}

			//System.out.println("[votes] "+Arrays.toString(votes));
			for (int j = 0; j < L; j++) {
				int pos = (votes[j] >= 0.5) ? 1 : 0;
				prediction.setVotes(j, new double[]{1.-votes[j],votes[j]});
			}
			//System.out.println("[pred]  "+prediction);

		}
		else{
			//System.out.println("Classification not enableed!");
		}
		
		return prediction;
	}

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
		//Measurement[] m = new Measurement[0];
        //return m;
		return null;
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

    public void createWekaClassifier(String[] options) throws Exception {
        String classifierName = options[0];
        String[] newoptions = options.clone();
        newoptions[0] = "";
        this.classifier = weka.classifiers.AbstractClassifier.forName(classifierName, newoptions);
    }

}
