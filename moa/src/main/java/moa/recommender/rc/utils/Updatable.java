/*
 *    Updatable.java
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
package moa.recommender.rc.utils;

import java.util.List;

public interface Updatable {
    public void updateNewUser(int userID, List<Integer> ratedItems, List<Double> ratings);
    public void updateNewItem(int itemID, List<Integer> ratingUsers, List<Double> ratings);
    public void updateRemoveUser(int userID);
    public void updateRemoveItem(int itemID);
    public void updateSetRating(int userID, int itemID, double rating);
    public void updateRemoveRating(int userID, int itemID);
}
