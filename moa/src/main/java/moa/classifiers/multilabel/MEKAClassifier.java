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

import moa.classifiers.meta.WEKAClassifier;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import weka.classifiers.UpdateableClassifier;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.classifiers.MultiLabelLearner;

/**
 * Class for using a MEKA classifier. NOTE: This class only exists to adjust the
 * classIndex by +1 We can use the standard WEKAClassifier if we set -c L where,
 * L = the number of labels + 1 (Because MOA understands that L specified on the
 * command line is the (L-1)th index).
 *
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MEKAClassifier extends WEKAClassifier implements MultiLabelLearner {

    private static final long serialVersionUID = 1L;
    protected int m_L = -1;

    @Override
    public void setModelContext(InstancesHeader raw_header) {
        //m_L = (m_L < 0 ? raw_header.classIndex() + 1 : m_L);
        m_L = (m_L < 0 ? raw_header.numOutputAttributes(): m_L);
        super.setModelContext(raw_header);
    }

    @Override
    public void trainOnInstanceImpl(MultiLabelInstance samoaInstance) {
        weka.core.Instance inst = this.instanceConverter.wekaInstance(samoaInstance);
        if (m_L < 0) {
            m_L = samoaInstance.numOutputAttributes();//inst.classIndex() + 1;
        }

        try {
            if (numberInstances < 1) { // INIT 
                weka.core.Instances D = inst.dataset();
                D.setClassIndex(m_L);
                this.instancesBuffer = new weka.core.Instances(D);
                if (classifier instanceof UpdateableClassifier) {
                    this.instancesBuffer.setClassIndex(m_L);
                    this.classifier.buildClassifier(instancesBuffer);
                    this.isClassificationEnabled = true;
                } else {
                    System.err.println("Only suports UpdateableClassifiers for now.");
                    System.exit(1);
                }
            } else { // UPDATE
                ((UpdateableClassifier) classifier).updateClassifier(inst);
            }
            numberInstances++;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public Prediction getPredictionForInstance(MultiLabelInstance instance) {
        
       double[] predictionArray = this.getVotesForInstance(instance);
       Prediction prediction = new MultiLabelPrediction(predictionArray.length);
       for (int j = 1; j < predictionArray.length; j++){
            prediction.setVote(j, 1, predictionArray[j]);
        }
        return prediction;
    }
}
