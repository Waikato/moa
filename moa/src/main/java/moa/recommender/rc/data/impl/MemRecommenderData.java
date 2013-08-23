/*
 *    MemRecommenderData.java
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

package moa.recommender.rc.data.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import moa.recommender.rc.data.AbstractRecommenderData;
import moa.recommender.rc.utils.Rating;
import moa.recommender.rc.utils.SparseVector;

public class MemRecommenderData extends AbstractRecommenderData {

    private static final long serialVersionUID = 2844235954903772074L;

    class EntityStats implements Serializable {
        private static final long serialVersionUID = -8933750377510577120L;
        public double sum = 0;
        public double num = 0;
    }
    
    protected Map<Integer, Map<Integer, Double>> ratingsUser;
    protected Map<Integer, Map<Integer, Double>> ratingsItem;
    protected Map<Integer, EntityStats> usersStats;
    protected Map<Integer, EntityStats> itemsStats;
    
    protected int nItems = 0;
    protected int nUsers = 0;
    protected double sumRatings = 0;
    protected int nRatings = 0;
    protected double minRating = 0;
    protected double maxRating = 0;
    
    protected class RatingIterator implements Iterator<Rating> {
        private int currentUser = -1;
        private Iterator<Integer> userIt = null;
        private Iterator<Entry<Integer, Double>> ratsIt = null;
        private boolean calculated = false;
        private boolean result = true;
        
        RatingIterator() throws Exception {
        }
        
        @Override
        public boolean hasNext() {
            if (calculated)
                return result;
            
            calculated = true;
            result = false;
            if (ratsIt == null) {
                if (!ratingsUser.isEmpty()) {
                    userIt = ratingsUser.keySet().iterator();
                    if (userIt.hasNext()) {
                        Integer first = userIt.next();
                        currentUser = first;
                        ratsIt = ratingsUser.get(first).entrySet().iterator();
                        if (ratsIt.hasNext()) {
                            result = true;
                        }
                    }
                }
            }
            else {
                if (ratsIt.hasNext()) {
                    result = true;
                }
                else if (userIt.hasNext()) {
                    Integer first = userIt.next();
                    currentUser = first;
                    ratsIt = ratingsUser.get(first).entrySet().iterator();
                    if (ratsIt.hasNext()) {
                        result = true;
                    }
                }
            }
            return result;
        }

        @Override
        public Rating next() {
            if (!calculated)
                hasNext();
            calculated = false;
            Entry<Integer, Double> pair = ratsIt.next();
            
            return new Rating(currentUser, pair.getKey(), pair.getValue());
        }

        @Override
        public void remove() {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    public MemRecommenderData() {
        super();
        ratingsItem = new HashMap<Integer, Map<Integer, Double>>();
        ratingsUser = new HashMap<Integer, Map<Integer, Double>>();
        usersStats = new HashMap<Integer, EntityStats>();
        itemsStats = new HashMap<Integer, EntityStats>();
    }
    
    @Override
    public void addUser(int userID, List<Integer> ratedItems, List<Double> ratings) {
        super.addUser(userID, ratedItems, ratings);
        
        ratingsUser.put(userID, new HashMap<Integer, Double>());
        usersStats.put(userID, new EntityStats());
        
        int n = ratedItems.size();
        
        for (int i = 0; i < n; ++i)
            auxSetRating(userID, ratedItems.get(i), ratings.get(i));
    }

    //FIXME: have to update item stats!!!
    @Override
    public void removeUser(int userID) {
        super.removeUser(userID);
        ratingsUser.remove(userID);
        usersStats.remove(userID);
    }

    @Override
    public void addItem(int itemID, List<Integer> ratingUsers, List<Double> ratings) {
        super.addItem(itemID, ratingUsers, ratings);
        
        ratingsItem.put(itemID, new HashMap<Integer, Double>());
        itemsStats.put(itemID, new EntityStats());
        int n = ratingUsers.size();
        for (int i = 0; i < n; ++i)
            auxSetRating(ratingUsers.get(i), itemID, ratings.get(i));
    }

    //FIXME: have to update user stats!!!
    @Override
    public void removeItem(int itemID) {
        super.removeItem(itemID);
        ratingsItem.remove(itemID);
        itemsStats.remove(itemID);
    }

    private void auxSetRating(int userID, int itemID, double rating) {
        if (nRatings == 0) {
            minRating = rating;
            maxRating = rating;
        }
        else {
            minRating = Math.min(minRating, rating);
            maxRating = Math.max(maxRating, rating);
        }
        
        EntityStats userStats = usersStats.get(userID);
        EntityStats itemStats = itemsStats.get(itemID);
        if (userStats == null) {
            ++nUsers;
            ratingsUser.put(userID, new HashMap<Integer, Double>());
            userStats = new EntityStats();
            usersStats.put(userID, userStats);
        }
        
        if (itemStats == null) {
            ++nItems;
            ratingsItem.put(itemID, new HashMap<Integer, Double>());
            itemStats = new EntityStats();
            itemsStats.put(itemID, itemStats);
        }
        
        Map<Integer, Double> ratUser = ratingsUser.get(userID);
        Map<Integer, Double> ratItem = ratingsItem.get(itemID);
        Double rat = ratUser.get(itemID);
        if (rat != null) {
            sumRatings -= rat;
            userStats.sum -= rat;
            userStats.num--;
            itemStats.sum -= rat;
            itemStats.num--;
            --nRatings;
        }
        
        userStats.sum += rating;
        userStats.num++;
        itemStats.sum += rating;
        itemStats.num++;
        sumRatings += rating;
        ++nRatings;
        ratUser.put(itemID, rating);
        ratItem.put(userID, rating);
    }
    
    @Override
    public void setRating(int userID, int itemID, double rating) {
        super.setRating(userID, itemID, rating);
        auxSetRating(userID, itemID, rating);
    }
    
    @Override
    public void removeRating(int userID, int itemID) {
        super.removeRating(userID, itemID);
        
        Map<Integer, Double> ratUser = ratingsUser.get(userID);
        Map<Integer, Double> ratItem = ratingsItem.get(itemID);
        Double rat = ratUser.get(itemID);
        EntityStats userStats = usersStats.get(userID);
        EntityStats itemStats = itemsStats.get(itemID);
        if (rat != null) {
            sumRatings -= rat;
            --nRatings;
            userStats.sum -= rat;
            userStats.num--;
            itemStats.sum -= rat;
            itemStats.num--;
            ratUser.remove(itemID);
            ratItem.remove(userID);
        }
    }

    @Override
    public SparseVector getRatingsUser(int userID) {
        Map<Integer, Double> ratUser = ratingsUser.get(userID);
        return new SparseVector(ratUser);
    }
    
    @Override
    public double getRating(int userID, int itemID) {
        Map<Integer, Double> ratUser = ratingsUser.get(userID);
        return (ratUser.get(itemID) != null ? ratUser.get(itemID) : 0);
    }

    @Override
    public int getNumItems() {
        return nItems;
    }

    @Override
    public int getNumUsers() {
        return nUsers;
    }
    
    @Override
    public double getAvgRatingUser(int userID) {
        EntityStats stats = usersStats.get(userID);
        double sum = (stats != null ? stats.sum : 0);
        double num = (stats != null ? stats.num : 0);
        double mean = (nRatings > 0 ? sumRatings/(double)nRatings : (minRating + maxRating)/2.0);
        return (mean*25 + sum)/(25 + num);
    }

    @Override
    public double getAvgRatingItem(int itemID) {
        EntityStats stats = itemsStats.get(itemID);
        double sum = (stats != null ? stats.sum : 0);
        double num = (stats != null ? stats.num : 0);
        double mean = (nRatings > 0 ? sumRatings/(double)nRatings : (minRating + maxRating)/2.0);
        return (mean*25 + sum)/(25 + num);
    }
    
    @Override
    public double getMinRating() {
        return minRating;
    }

    @Override
    public double getMaxRating() {
        return maxRating;
    }

    @Override
    public Set<Integer> getUsers() {
        return usersStats.keySet();
    }

    @Override
    public SparseVector getRatingsItem(int itemID) {
        Map<Integer, Double> ratItem = ratingsItem.get(itemID);
        return new SparseVector(ratItem);
    }

    @Override
    public Set<Integer> getItems() {
        return itemsStats.keySet();
    }

    @Override
    public double getGlobalMean() {
        return (nRatings > 0 ? sumRatings/(double)nRatings : (minRating + maxRating)/2.0);
    }

    @Override
    public int countRatingsUser(int userID) {
        EntityStats stats = usersStats.get(userID);
        return (stats != null ? (int)stats.num : 0);
    }

    @Override
    public int countRatingsItem(int itemID) {
        EntityStats stats = itemsStats.get(itemID);
        return (stats != null ? (int)stats.num : 0);
    }

    @Override
    public Iterator<Rating> ratingIterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumRatings() {
        return nRatings;
    }
    
    @Override
    public boolean userExists(int userID) {
        return usersStats.containsKey(userID);
    }
    @Override
    public boolean itemExists(int itemID) {
        return itemsStats.containsKey(itemID);
    }

    @Override
    public void clear() {
        usersStats.clear();
        itemsStats.clear();
        minRating = maxRating = nItems = nUsers = 0;
        sumRatings = nRatings = 0;
        ratingsUser.clear();
        ratingsItem.clear();
    }
}

