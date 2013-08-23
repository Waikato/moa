/*
 *    RatingPredictor.java
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

import java.io.Serializable;
import java.util.List;
import moa.recommender.rc.data.RecommenderData;

/**
 * Rating predicting algorithm. The core of any recommender system is its 
 * rating prediction algorithm. Its purpose is to estimate the rating
 * (a numeric score) that a certain user would give to a certain item,
 * based on previous ratings given of the user and the item.
 * 
 */
public interface RatingPredictor extends Serializable {
	public double predictRating(int userID, int itemID);
	public List<Double> predictRatings(int userID, List<Integer> itemIDS);
	public RecommenderData getData();
	public void train();
}
