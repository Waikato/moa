/*
 *    RecommenderData.java
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
