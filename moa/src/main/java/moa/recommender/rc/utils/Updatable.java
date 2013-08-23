/*
 *    Updatable.java
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
