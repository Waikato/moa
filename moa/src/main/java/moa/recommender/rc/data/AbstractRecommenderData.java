/*
 *    AbstractRecommenderData.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moa.recommender.rc.utils.Updatable;


public abstract class AbstractRecommenderData implements RecommenderData {
    
    
    /**
     * 
     */
    private static final long serialVersionUID = -5409390358073330733L;
    
    protected ArrayList<Updatable> updatables;
    protected boolean disableUpdates = false;
    
    public AbstractRecommenderData() {
        this.updatables = new ArrayList<Updatable>();
    }
    
    public void disableUpdates(boolean disable) {
        this.disableUpdates = disable;
    }
    
    public void addUser(int userID, List<Integer> ratedItems, List<Double> ratings) {
        Iterator<Updatable> it = updatables.iterator();
        while (it.hasNext()) {
            Updatable u = it.next();
            if (!disableUpdates)
                u.updateNewUser(userID, ratedItems, ratings);
        }
    }
    public void removeUser(int userID) {
        Iterator<Updatable> it = updatables.iterator();
        while (it.hasNext()) {
            Updatable u = it.next();
            if (!disableUpdates)
                u.updateRemoveUser(userID);
        }
    }
    public void addItem(int itemID, List<Integer> ratingUsers, List<Double> ratings) {
        Iterator<Updatable> it = updatables.iterator();
        while (it.hasNext()) {
            Updatable u = it.next();
            if (!disableUpdates)
                u.updateNewItem(itemID, ratingUsers, ratings);
        }
    }
    public void removeItem(int itemID) {
        Iterator<Updatable> it = updatables.iterator();
        while (it.hasNext()) {
            Updatable u = it.next();
            if (!disableUpdates)
                u.updateRemoveItem(itemID);
        }
    }
    public void setRating(int userID, int itemID, double rating) {
        Iterator<Updatable> it = updatables.iterator();
        while (it.hasNext()) {
            Updatable u = it.next();
            if (!disableUpdates) {
                u.updateSetRating(userID, itemID, rating);
            }
                
        }
    }
    public void removeRating(int userID, int itemID) {
        Iterator<Updatable> it = updatables.iterator();
        while (it.hasNext()) {
            Updatable u = it.next();
            if (!disableUpdates)
                u.updateRemoveRating(userID, itemID);
        }
    }

    public void attachUpdatable(Updatable obj) {
        updatables.add(obj);
    }
    public void clear() {
        updatables.clear();
        disableUpdates = false;
    }
    public void close() {
        
    }
}
