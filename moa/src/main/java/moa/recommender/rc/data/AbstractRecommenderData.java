/*
 *    AbstractRecommenderData.java
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
		this.updatables = new ArrayList<>();
	}

	@Override
	public void disableUpdates(boolean disable) {
		this.disableUpdates = disable;
	}

	@Override
	public void addUser(int userID, List<Integer> ratedItems, List<Double> ratings) {
		Iterator<Updatable> it = updatables.iterator();
		while (it.hasNext()) {
			Updatable u = it.next();
			if (!disableUpdates)
				u.updateNewUser(userID, ratedItems, ratings);
		}
	}

	@Override
	public void removeUser(int userID) {
		Iterator<Updatable> it = updatables.iterator();
		while (it.hasNext()) {
			Updatable u = it.next();
			if (!disableUpdates)
				u.updateRemoveUser(userID);
		}
	}

	@Override
	public void addItem(int itemID, List<Integer> ratingUsers, List<Double> ratings) {
		Iterator<Updatable> it = updatables.iterator();
		while (it.hasNext()) {
			Updatable u = it.next();
			if (!disableUpdates)
				u.updateNewItem(itemID, ratingUsers, ratings);
		}
	}

	@Override
	public void removeItem(int itemID) {
		Iterator<Updatable> it = updatables.iterator();
		while (it.hasNext()) {
			Updatable u = it.next();
			if (!disableUpdates)
				u.updateRemoveItem(itemID);
		}
	}

	@Override
	public void setRating(int userID, int itemID, double rating) {
		Iterator<Updatable> it = updatables.iterator();
		while (it.hasNext()) {
			Updatable u = it.next();
			if (!disableUpdates) {
				u.updateSetRating(userID, itemID, rating);
			}

		}
	}

	@Override
	public void removeRating(int userID, int itemID) {
		Iterator<Updatable> it = updatables.iterator();
		while (it.hasNext()) {
			Updatable u = it.next();
			if (!disableUpdates)
				u.updateRemoveRating(userID, itemID);
		}
	}

	@Override
	public void attachUpdatable(Updatable obj) {
		updatables.add(obj);
	}

	@Override
	public void clear() {
		updatables.clear();
		disableUpdates = false;
	}

	@Override
	public void close() {

	}
}
