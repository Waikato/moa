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
    
    public void addUser(int userID, List<Integer> ratedItems, List<Double> ratings);
    public void removeUser(int userID);
    public void addItem(int itemID, List<Integer> ratingUsers, List<Double> ratings);
    public void removeItem(int itemID);
    public void setRating(int userID, int itemID, double rating);
    public void removeRating(int userID, int itemID);
    public SparseVector getRatingsUser(int userID); //TODO:Iterator version for this?
    public SparseVector getRatingsItem(int itemID); //TODO:Iterator version for this?
    public double getRating(int userID, int itemID);
    public int getNumItems();
    public int getNumUsers();
    public int getNumRatings();
    public double getAvgRatingUser(int userID);
    public double getAvgRatingItem(int itemID);
    public double getMinRating();
    public double getMaxRating();
    public Set<Integer> getUsers();
    public Set<Integer> getItems();
    public double getGlobalMean();
    public void attachUpdatable(Updatable obj);
    public void disableUpdates(boolean disable);
    public int countRatingsUser(int userID);
    public int countRatingsItem(int itemID);
    public Iterator<Rating> ratingIterator();
    public boolean userExists(int userID);
    public boolean itemExists(int itemID);
    public void clear();
    public void close();
}
