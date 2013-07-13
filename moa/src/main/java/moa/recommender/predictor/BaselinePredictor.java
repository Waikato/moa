/*
 *    BaselinePredictor.java
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
            "Data", moa.recommender.data.RecommenderData.class, "moa.recommender.data.DiskRecommenderData");

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
