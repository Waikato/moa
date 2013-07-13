/*
 *    RatingPredictor.java
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
