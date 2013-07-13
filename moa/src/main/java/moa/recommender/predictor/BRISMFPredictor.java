/*
 *    BRISMFPredictor.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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

package moa.recommender.predictor;

import java.util.List;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.recommender.data.RecommenderData;
import moa.tasks.TaskMonitor;

/**
 * Implementation of the algorithm described in Scalable 
 * Collaborative Filtering Approaches for Large Recommender
 * Systems (Gábor Takács, István Pilászy, Bottyán Németh, 
 * and Domonkos Tikk). A feature vector is learned for every 
 * user and item, so that the prediction of a rating is roughly
 * the dot product of the corresponding user and item vector. 
 * Stochastic gradient descent is used to train the model, 
 * minimizing its prediction error. Both Tikhonov regularization
 * and early stopping are used to reduce overfitting. The 
 * algorithm allows batch training (from scratch, using all
 * ratings available at the moment) as well as incremental,
 * by retraining only the affected user and item vectors when 
 * a new rating is inserted.
 * 
 * <p>Parameters:</p>
 * <ul>
 * <li> f: features - the number of features to be trained for each user and 
 *      item</li>
 * <li> r: learning rate - the learning rate used in the regularization</li>
 * <li> a: ratio - the regularization ratio to be used in the Tikhonov 
 *      regularization</li>
 * <li> i: iterations - the number of iterations to be used when retraining
 *      user and item features (online training). </li>
 * </lu>
 * 
 */

public class BRISMFPredictor extends AbstractOptionHandler implements RatingPredictor {

    protected moa.recommender.rc.predictor.impl.BRISMFPredictor rp;
    
    public IntOption featuresOption = new IntOption("features",
            'f',
            "How many features to use.",
            20, 0, Integer.MAX_VALUE);
    
    public FloatOption lRateOption = new FloatOption("lRate",
            'r', "lRate", 0.001);
    
    public FloatOption rFactorOption = new FloatOption("rFactor",
            'a', "rFactor", 0.01);
    
    public IntOption iterationsOption = new IntOption("iterations",
            'i',
            "How many iterations to use.",
            100, 0, Integer.MAX_VALUE);

    public ClassOption dataOption = new ClassOption("data", 'd',
            "Data", RecommenderData.class, "moa.recommender.data.DiskRecommenderData");
     
    
    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        RecommenderData data = (RecommenderData) getPreparedClassOption(this.dataOption);
        rp = new moa.recommender.rc.predictor.impl.BRISMFPredictor(featuresOption.getValue(), data.getData(), lRateOption.getValue(), rFactorOption.getValue(), false);
        rp.setNIterations(iterationsOption.getValue());
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append(rp.toString());
    }

    public double predictRating(Integer user, Integer item) {
        return rp.predictRating(user,item);
    }

    public moa.recommender.rc.data.RecommenderData getData() {
        return rp.getData();
    }

    @Override
    public double predictRating(int userID, int itemID) {
        return rp.predictRating(userID, itemID);
    }

    @Override
    public List<Double> predictRatings(int userID, List<Integer> itemIDS) {
        return rp.predictRatings(userID, itemIDS);
    }

    @Override
    public void train() {
        rp.train();
    }
    
}
