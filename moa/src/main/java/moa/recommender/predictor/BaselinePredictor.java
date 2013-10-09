/*
 *    BaselinePredictor.java
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
import moa.tasks.TaskMonitor;

/**
 * A naive algorithm which combines the global mean of all the existing 
 * ratings, the mean rating of the user and the mean rating of the item 
 * to make a prediction.
 * 
 */

public class BaselinePredictor extends AbstractOptionHandler implements moa.recommender.predictor.RatingPredictor {   
    protected moa.recommender.rc.predictor.impl.BaselinePredictor rp;
    
    public ClassOption dataOption = new ClassOption("data", 'd',
            "Data", moa.recommender.data.RecommenderData.class, "moa.recommender.data.MemRecommenderData");

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        moa.recommender.data.RecommenderData data = (moa.recommender.data.RecommenderData) getPreparedClassOption(this.dataOption);
        rp = new moa.recommender.rc.predictor.impl.BaselinePredictor(data.getData());
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
