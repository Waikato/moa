/*
 *    MajorityLabelset.java
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
import java.util.HashMap;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;

/**
 * Majority Labelset classifier. Each labelset combination of relevances, e.g.
 * [0,0,1,1,0,0], is treated as a single class value.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MajorityLabelset extends AbstractMultiLabelLearner implements MultiTargetRegressor {
    //AbstractClassifier {

    private static final long serialVersionUID = 1L;

    private int m_L = -1;

    private double maxValue = 0.0;

    private double predictionArray[] = null;

    private HashMap<String, Double> classFreqs = new HashMap<String, Double>();

    // raw instance to bit string (i.e. from binary representation)
    private static final String toBitString(Instance ins, int c) {
        StringBuilder sb = new StringBuilder(c);
        for (int i = 0; i < c; i++) {
            sb.append((int) Math.round(ins.value(i)));
        }
        return sb.toString();
    }

    protected void updateCount(Instance x, int L) {

        String y = toBitString(x, L);

        if (classFreqs.containsKey(y)) {
            double freq = classFreqs.get(y) + x.weight();
            classFreqs.put(y, freq);
            if (freq >= maxValue) {
                maxValue = freq;
                this.predictionArray = new double[L];
                for (int j = 0; j < L; j++) {
                    this.predictionArray[j] = x.value(j);
                }
            }
        } else {
            classFreqs.put(y, x.weight());
        }
    }

    @Override
    public void setModelContext(InstancesHeader raw_header) {
        //set the multilabel model context
        this.modelContext = raw_header;
        m_L = raw_header.numOutputAttributes(); //raw_header.classIndex() + 1;
        predictionArray = new double[m_L];
    }

    @Override
    public void resetLearningImpl() {
    }

    @Override
    public void trainOnInstanceImpl(MultiLabelInstance x) {
        updateCount(x, m_L);
    }

    @Override
    //public double[] getVotesForInstance(Instance x) {
    public Prediction getPredictionForInstance(MultiLabelInstance x){
        
		System.out.println("-------- start MC vote   ---------------");
        int L = x.numOutputAttributes(); //x.classIndex() + 1;
        if (m_L != L) {
            System.err.println("set L = " + L);
            m_L = L;
            predictionArray = new double[m_L];
        }

		System.out.println("y = "+Arrays.toString(predictionArray));
		System.out.println("-------- end MC vote   -----------------");
        
       Prediction prediction = new MultiLabelPrediction(predictionArray.length);
       for (int j = 0; j < predictionArray.length; j++){
            prediction.setVote(j, 1, predictionArray[j]);
        }
		
        return prediction;
        //System.out.println("getVotesForInstance(): "+x.classIndex());
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{};
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

}
