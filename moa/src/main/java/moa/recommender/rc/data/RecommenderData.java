/*
 *    RecommenderData.java
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

package moa.recommender.rc.data;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import moa.recommender.rc.utils.Rating;
import moa.recommender.rc.utils.SparseVector;
import moa.recommender.rc.utils.Updatable;

public interface RecommenderData extends Serializable {

	void addUser(int userID, List<Integer> ratedItems, List<Double> ratings);

	void removeUser(int userID);

	void addItem(int itemID, List<Integer> ratingUsers, List<Double> ratings);

	void removeItem(int itemID);

	void setRating(int userID, int itemID, double rating);

	void removeRating(int userID, int itemID);

	SparseVector getRatingsUser(int userID); // TODO:Iterator version for this?

	SparseVector getRatingsItem(int itemID); // TODO:Iterator version for this?

	double getRating(int userID, int itemID);

	int getNumItems();

	int getNumUsers();

	int getNumRatings();

	double getAvgRatingUser(int userID);

	double getAvgRatingItem(int itemID);

	double getMinRating();

	double getMaxRating();

	Set<Integer> getUsers();

	Set<Integer> getItems();

	double getGlobalMean();

	void attachUpdatable(Updatable obj);

	void disableUpdates(boolean disable);

	int countRatingsUser(int userID);

	int countRatingsItem(int itemID);

	Iterator<Rating> ratingIterator();

	boolean userExists(int userID);

	boolean itemExists(int itemID);

	void clear();

	void close();
}
