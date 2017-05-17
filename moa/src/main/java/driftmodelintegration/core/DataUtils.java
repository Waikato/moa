package driftmodelintegration.core;

/*
 *  streams library
 *
 *  Copyright (C) 2011-2012 by Christian Bockermann, Hendrik Blom
 * 
 *  streams is a library, API and runtime environment for processing high
 *  volume data streams. It is composed of three submodules "stream-api",
 *  "stream-core" and "stream-runtime".
 *
 *  The streams library (and its submodules) is free software: you can 
 *  redistribute it and/or modify it under the terms of the 
 *  GNU Affero General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any 
 *  later version.
 *
 *  The stream.ai library (and its submodules) is distributed in the hope
 *  that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>
 * This class provides some static convenience methods for accessing data items.
 * In general, each data item represents a (key,value) mapping. Some keys may
 * refer to special ones, as determined by their string-value.
 * </p>
 * <p>
 * For example, keys starting with <code>_</code> are regarded as special keys
 * and will not be processed by learning algorithms. Likewise, keys starting
 * with <code>._</code> will be considered as <i>hidden</i> keys.
 * </p>
 * 
 * @author Christian Bockermann &lt;chris@jwall.org&gt;
 */
public class DataUtils {

	/**
	 * Returns the list of <i>non-special</i>, <i>non-hidden</i> keys of the
	 * given data item.
	 * 
	 * @param item
	 * @return
	 */
	public final static Set<String> getKeys(Data item) {
		Set<String> set = new LinkedHashSet<String>();
		for (String key : item.keySet()) {
			if (!isSpecial(key) && !isHidden(key))
				set.add(key);
		}
		return set;
	}

	/**
	 * Returns the set of special keys contained in the given data item. It will
	 * not return any hidden keys!
	 * 
	 * @param item
	 * @return
	 */
	public final static Set<String> getSpecialKeys(Data item) {
		Set<String> set = new LinkedHashSet<String>();
		for (String key : item.keySet())
			if (isSpecial(key) && !isHidden(key))
				set.add(key);
		return set;
	}

	/**
	 * Returns the given data item, with the specified key being hidden.
	 * 
	 * @param key
	 * @param item
	 * @return
	 * @deprecated
	 */
	public final static Data hide(String key, Data item) {
		if (item.containsKey(key)) {
			Serializable o = item.remove(key);
			item.put(Data.HIDDEN_PREFIX + key, o);
		}
		return item;
	}

	/**
	 * Returns the given data item, with the specified hidden key being
	 * un-hidden.
	 * 
	 * @param key
	 * @param item
	 * @return
	 * @deprecated
	 */
	public final static Data unhide(String key, Data item) {
		String hidden = Data.HIDDEN_PREFIX + key;
		if (item.containsKey(hidden)) {
			Serializable o = item.remove(hidden);
			item.put(key, o);
		}
		return item;
	}

	/**
	 * Checks if the given key is a special key. Annotations are special keys as
	 * well.
	 * 
	 * @param key
	 * @return
	 */
	public final static boolean isSpecial(String key) {
		return key.startsWith(Data.SPECIAL_PREFIX) || isAnnotation(key);
	}

	/**
	 * Checks if the given key is a hidden key.
	 * 
	 * @param key
	 * @return
	 * @deprecated
	 */
	public final static boolean isHidden(String key) {
		return key.startsWith(Data.HIDDEN_PREFIX);
	}

	public final static boolean isAnnotation(String key) {
		return key != null && key.startsWith(Data.ANNOTATION_PREFIX);
	}

	public final static boolean isFeature(String key) {
		return key != null && !isAnnotation(key) && !isHidden(key);
	}

	/**
	 * @param key
	 * @return
	 * @deprecated
	 */
	public final static String hide(String key) {
		if (key.startsWith(Data.HIDDEN_PREFIX))
			return key;
		return Data.HIDDEN_PREFIX + key;
	}

	public final static boolean isHiddenOrSpecial(String key) {
		return isHidden(key) || isSpecial(key);
	}
}