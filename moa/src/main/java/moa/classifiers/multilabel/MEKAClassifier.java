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
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.meta.WEKAClassifier;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import weka.classifiers.UpdateableClassifier;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.SamoaToWekaInstanceConverter;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Example;

/**
 * Class for using a MEKA classifier. NOTE: This class only exists to adjust the
 * classIndex by +1 We can use the standard WEKAClassifier if we set -c L where,
 * L = the number of labels + 1 (Because MOA understands that L specified on the
 * command line is the (L-1)th index).
 *
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MEKAClassifier extends WEKAClassifier implements MultiLabelLearner, MultiTargetRegressor {

	private static final long serialVersionUID = 1L;
	protected int m_L = -1;

	@Override
	public void setModelContext(InstancesHeader raw_header) {
		//m_L = (m_L < 0 ? raw_header.classIndex() + 1 : m_L);
		m_L = (m_L < 0 ? raw_header.numOutputAttributes(): m_L);
		super.setModelContext(raw_header);

		weka.core.Instances D = null;
		SamoaToWekaInstanceConverter conv = new SamoaToWekaInstanceConverter();
		try {
			D = conv.wekaInstances(raw_header);
		} catch(Exception e) {
			System.err.println("FATAL ERROR: Failed to convert InstancesHeader to Instances");
			e.printStackTrace();
			System.exit(1);
		}
		D.setClassIndex(m_L);
		//System.out.println("L="+D.classIndex()+","+m_L);
		this.instancesBuffer = new weka.core.Instances(D);
		if (classifier instanceof UpdateableClassifier) {
			this.instancesBuffer.setClassIndex(m_L);
			// System.out.println(instancesBuffer.classIndex());
			// System.out.println(instancesBuffer.classAttribute().name());
			// System.out.println(instancesBuffer.classAttribute().isNumeric());
			//System.out.println("N="+instancesBuffer.numInstances()+"\nD:\n"+instancesBuffer);

			try {
				this.classifier.buildClassifier(instancesBuffer);
			} catch(Exception e) {
				System.err.println("FATAL ERROR: Failed to initialize Meka classifier!");
				e.printStackTrace();
				System.exit(1);
			}
			this.isClassificationEnabled = true;
		} else {
			System.err.println("Only suports UpdateableClassifiers for now.");
			System.exit(1);
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance instance) {
		trainOnInstanceImpl((MultiLabelInstance) instance);
	}


	@Override
	public void trainOnInstanceImpl(MultiLabelInstance samoaInstance) {
		weka.core.Instance inst = this.instanceConverter.wekaInstance(samoaInstance);
		//System.out.println(""+m_L);                   // <--  this is correct
		//System.out.println(""+inst.classIndex());     // <--- this one is wrong
		inst.dataset().setClassIndex(m_L);                      // <-- so, fix it!

		if (m_L < 0) {
			System.out.println("FATAL ERROR: setModelContext(..) has not been called yet!");
			m_L = samoaInstance.numOutputAttributes();//inst.classIndex() + 1;
			System.exit(1);
		}

		//System.out.println(inst.classIndex());
		try {
			//System.out.println("UPDATE   WITH instances of "+instancesBuffer.classIndex()+" labels :\n"+inst);
			((UpdateableClassifier) classifier).updateClassifier(inst);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public double[] getVotesForInstance(Instance samoaInstance) {
		weka.core.Instance inst = this.instanceConverter.wekaInstance(samoaInstance);
		double votes[] = null;
		try {
			votes = this.classifier.distributionForInstance(inst);
		} catch(Exception e) {
			System.err.println("FATAL ERROR: Failed to get votes");
			e.printStackTrace();
			System.exit(1);
		}
		return votes;
	}

	@Override
	public Prediction getPredictionForInstance(Example<Instance> example) {
		return getPredictionForInstance((MultiLabelInstance)example.getData());
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance instance) {

		double[] predictionArray = this.getVotesForInstance(instance);

		//System.out.println("y = "+Arrays.toString(predictionArray));

		Prediction prediction = new MultiLabelPrediction(predictionArray.length);
		for (int j = 0; j < predictionArray.length; j++){
			prediction.setVote(j, 1, predictionArray[j]);
			//prediction.setVote(j, 0, 1. - predictionArray[j]);
		}
		return prediction;
	}
}
