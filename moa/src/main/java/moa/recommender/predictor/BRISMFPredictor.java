/*
 *    BRISMFPredictor.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.recommender.predictor;

import java.util.List;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
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
            "Data", RecommenderData.class, "moa.recommender.data.MemRecommenderData");
     
    
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
