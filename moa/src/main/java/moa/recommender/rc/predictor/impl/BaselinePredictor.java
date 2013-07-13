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

package moa.recommender.rc.predictor.impl;

import java.util.ArrayList;
import java.util.List;
import moa.recommender.rc.data.RecommenderData;
import moa.recommender.rc.predictor.RatingPredictor;

public class BaselinePredictor implements RatingPredictor {
    /**
     * 
     */
    private static final long serialVersionUID = 8444152568941483368L;
    protected RecommenderData data;

    public BaselinePredictor(RecommenderData data) {
        this.data = data;
    }

    @Override
    public double predictRating(int userID, int itemID) {
        ArrayList<Integer> itm = new ArrayList<Integer>();
        itm.add(itemID);
        return predictRatings(userID, itm).get(0);
    }

    @Override
    public List<Double> predictRatings(int userID, List<Integer> itemIDS) {
        ArrayList<Double> ret = new ArrayList<Double>(itemIDS.size());
        double avg = data.getAvgRatingUser(userID) - data.getGlobalMean();
        for (int i = 0; i < itemIDS.size(); ++i) {
            int itemID = itemIDS.get(i);
            double rat = avg + data.getAvgRatingItem(itemID);
            rat = Math.min(Math.max(rat, data.getMinRating()),
                    data.getMaxRating());
            ret.add(rat);
        }
        return ret;
    }

    @Override
    public RecommenderData getData() {
        return data;
    }

    @Override
    public void train() {
    }
}
