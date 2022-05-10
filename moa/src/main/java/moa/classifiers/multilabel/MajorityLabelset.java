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
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetRegressor;
import moa.core.StringUtils;

/**
 * Majority Labelset classifier. Each labelset combination of relevances, e.g.
 * [0,0,1,1,0,0], is treated as a single class value.
 * 
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 1 $
 */
public class MajorityLabelset extends AbstractMultiLabelLearner implements MultiLabelLearner {
    //AbstractClassifier {

    private static final long serialVersionUID = 1L;

	@Override
    public String getPurposeString() {
        return "Majority labelset classifier: always predicts the labelvector most frequently seen so far.";
    }

    private double maxValue = -1.0;

    private MultiLabelPrediction majorityLabelset = null;

    private HashMap<String, Double> vectorCounts = new HashMap<String, Double>();

    @Override
    public void resetLearningImpl() {
		this.majorityLabelset = null;
    }

    @Override
    public void trainOnInstanceImpl(MultiLabelInstance x) {
		int L = x.numberOutputTargets();

        MultiLabelPrediction y = new MultiLabelPrediction(L);
		for(int j=0; j<L;j++)
    		y.setVotes(j,new double[]{1- x.classValue(j), x.classValue(j)});

        double freq = x.weight();
		if (this.vectorCounts.containsKey(y.toString())) {
            freq += this.vectorCounts.get(y.toString());
        }
        this.vectorCounts.put(y.toString(), (Double)freq);
        if (freq > this.maxValue) {
            this.maxValue = freq;
            this.majorityLabelset = y;
        }
        //System.out.println("---"+this.majorityLabelset);
    }

    @Override
    public Prediction getPredictionForInstance(MultiLabelInstance x){

		if (this.majorityLabelset == null)  {
			int L = x.numberOutputTargets();
			return new MultiLabelPrediction(L);
		}

		return this.majorityLabelset;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        StringUtils.appendIndented(out, indent, "");
        out.append(this.majorityLabelset.toString());
        StringUtils.appendNewline(out);

    }

}
