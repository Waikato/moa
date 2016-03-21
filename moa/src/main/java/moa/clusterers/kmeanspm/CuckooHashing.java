/*
 *    CuckooHashing.java
 *    Copyright (C) 2015 TU Dortmund University, Germany
 *    @author Jan Stallmann (jan.stallmann@tu-dortmund.de)
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
package moa.clusterers.kmeanspm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Provides a hash table based on Cuckoo Hashing.
 *
 * Citation: Rasmus Pagh, Flemming Friche Rodler:
 * Cuckoo Hashing.
 * ESA 2001: 121-133
 *
 * @param <T>
 *            class of stored elements
 */
public class CuckooHashing<T> {

	public static final int DEFAULT_STASH_SIZE = 10;
	public static final int DEFAULT_NUM_TABLES = 2;

	private final int startHashSize;
	private final int maxStashSize;
	private final int startNumTables;
	private Random random;

	private int hashSize;
	private List<DietzfelbingerHash> hashfunctions;
	private List<Entry<T>> elements;
	private int numElements;
	private List<List<Entry<T>>> tables;
	private int numTables;
	private List<Entry<T>> stash;
	private int stashSize;

	/**
	 * Creates a new hash table based on Cuckoo Hashing.
	 *
	 * @param startHashSize
	 *            size of the hash function at the beginning (must be smaller
	 *            than 31)
	 * @param maxStashSize
	 *            maximum size of the stash
	 * @param startNumTables
	 *            number of tables for Cuckoo hashing
	 * @param random
	 *            instance to generate a stream of pseudorandom numbers
	 */
	public CuckooHashing(int startHashSize, int maxStashSize, int startNumTables,
			Random random) {
		assert(startHashSize < 31);
		this.startHashSize = startHashSize;
		this.maxStashSize = maxStashSize;
		this.startNumTables = startNumTables;
		this.random = random;

		this.hashSize = startHashSize;
		int sizeTables = 1 << startHashSize;
		this.hashfunctions = new ArrayList<DietzfelbingerHash>(startNumTables);
		this.elements = new ArrayList<Entry<T>>(sizeTables);
		this.numElements = 0;
		this.tables = new ArrayList<List<Entry<T>>>(startNumTables);
		this.numTables = startNumTables;
		this.stash = new ArrayList<Entry<T>>();
		this.stashSize = 0;

		for (int i = 0; i < startNumTables; i++) {
			this.hashfunctions.add(new DietzfelbingerHash(startHashSize, random));
			List<Entry<T>> table = new ArrayList<Entry<T>>(sizeTables);
			for (int j = 0; j < sizeTables; j++) {
				table.add(null);
			}
			this.tables.add(table);
		}
	}

	/**
	 * Creates a new hash table based on Cuckoo Hashing.
	 *
	 * @param hashSize
	 *            size of the hash function
	 * @param random
	 *            instance to generate a stream of pseudorandom numbers
	 */
	public CuckooHashing(int hashSize, Random random) {
		this(hashSize, DEFAULT_STASH_SIZE, DEFAULT_NUM_TABLES, random);
	}

	/**
	 * Adds an element to the hash table.
	 *
	 * @param key
	 *            key value of the element
	 * @param element
	 *            value of the element
	 */
	public void put(long key, T element) {
		Entry<T> entry = new Entry<T>(key, element);
		elements.add(entry);
		this.numElements++;
		fileElement(entry, true);
	}

	/**
	 * Adds an element to one of the tables for Cuckoo Hashing.
	 *
	 * @param currentElement
	 *            entry to add in a table
	 * @param rehash
	 *            if <code>true</code> the hash table will be rebuild when the
	 *            stash became too big.
	 */
	private void fileElement(Entry<T> currentElement, boolean rehash) {
		int maxFailures = Math.max((int) Math.log(this.numElements),
				this.numTables * 2);
		int currentTable = 0;
		for (int i = 0; i < maxFailures; i++) {
			int hash = this.hashfunctions.get(currentTable).hash(
					currentElement.getKey());
			currentElement = this.tables.get(currentTable).set(hash,
					currentElement);
			if (currentElement == null) {
				break;
			}
			currentTable = (currentTable + 1) % this.numTables;
		}
		if (currentElement != null) {
			this.stash.add(currentElement);
			this.stashSize++;
		}
		while (rehash && this.stashSize > this.maxStashSize) {
			reset();
			if (this.stashSize > this.maxStashSize) {
				increaseAndReset();
			}
		}
	}

	/**
	 * Adds a new table and rebuild the hash table.
	 *
	 */
	private void increaseAndReset() {
		if (this.hashSize < 30) {
			this.hashSize += 1;
			this.hashfunctions.clear();
			for (List<Entry<T>> table : this.tables) {
				this.hashfunctions.add(new DietzfelbingerHash(this.hashSize,
						this.random));
				((ArrayList<Entry<T>>) table)
						.ensureCapacity(1 << this.hashSize);
			}
		} else {
			this.hashfunctions.add(new DietzfelbingerHash(this.hashSize,
					this.random));
			this.tables.add(new ArrayList<Entry<T>>(1 << this.hashSize));
			this.numTables++;
		}
		reset();
	}

	/**
	 * Gets an element of the hash table.
	 *
	 * @param key
	 *            key value of the element
	 */
	public T get(long key) {
		for (int i = 0; i < this.numTables; i++) {
			Entry<T> entry = this.tables.get(i).get(
					this.hashfunctions.get(i).hash(key));
			if (entry != null && entry.getKey() == key) {
				return entry.getValue();
			}
		}
		for (Entry<T> entry : this.stash) {
			if (entry.getKey() == key) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Rebuilds the hash table.
	 *
	 */
	private void reset() {
		for (DietzfelbingerHash hashfunction : this.hashfunctions) {
			hashfunction.nextHashFunction();
		}

		int sizeTables = 1 << this.hashSize;
		for (List<Entry<T>> table : this.tables) {
			table.clear();
			for (int j = 0; j < sizeTables; j++) {
				table.add(null);
			}
		}

		this.stash.clear();
		this.stashSize = 0;

		for (Entry<T> entry : this.elements) {
			fileElement(entry, false);
		}
	}

	/**
	 * Removes all of the elements from this hash table. The hash table will be
	 * empty after this call returns.
	 *
	 */
	public void clear() {
		this.hashSize = this.startHashSize;
		this.numTables = this.startNumTables;
		this.numElements = 0;

		this.hashfunctions.clear();
		this.tables.clear();
		int sizeTables = 1 << this.startHashSize;
		for (int i = 0; i < this.startNumTables; i++) {
			this.hashfunctions.add(new DietzfelbingerHash(this.startHashSize,
					this.random));
			List<Entry<T>> table = new ArrayList<Entry<T>>(sizeTables);
			for (int j = 0; j < sizeTables; j++) {
				table.add(null);
			}
			this.tables.add(table);
		}

		this.stash.clear();
		this.stashSize = 0;
	}

	/**
	 * Returns the number of elements in the hash table.
	 *
	 * @return the number of elements in the hash table
	 */
	public int size() {
		return this.numElements;
	}

	/**
	 * Returns <code>true</code> if this hash table contains no elements.
	 *
	 * @return <code>true</code> if this hash table contains no elements
	 */
	public boolean isEmpty() {
		return this.numElements == 0;
	}

	/**
	 * Class to save an element in the hash table.
	 *
	 * @param <V>
	 *            class of stored elements
	 */
	private class Entry<V> {

		private final long key;
		private final V value;

		/**
		 * Creates a new entry for an element.
		 *
		 * @param key
		 *            key of element
		 * @param value
		 *            value of element
		 */
		public Entry(long key, V value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * Gets the key of the element.
		 *
		 * @return key of the element
		 */
		public long getKey() {
			return this.key;
		}

		/**
		 * Gets the value of the element.
		 *
		 * @return value of the element
		 */
		public V getValue() {
			return this.value;
		}

	}

}
