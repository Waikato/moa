/*
 *    RingBuffer.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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
package moa.classifiers.active.budget;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a generic ring buffer
 * which acts like a queue with a maximum length
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @param <E> the type of instance which should be stored
 * @version $Revision: 1 $
 */
public class RingBuffer<E> {

	// the list to store data 
	List<E> buffer;
	// the index which points to the start of the ring buffer
	int start;
	// the maximum size of the ring buffer
	int capacity;

	/**
	 * constructor
	 * @param length the maximum size for the ring buffer
	 */
	public RingBuffer(int capacity) {
		ArrayList<E> l = new ArrayList<>();
		l.ensureCapacity(capacity);
		buffer = l;
		start = 0;
		this.capacity = capacity;
	}

	/**
	 * converts the index relative to the start position to the index used for the internal buffer
	 * @param index the index relative to the start position
	 * @return index used for internal buffer
	 */
	private int normalizeIndex(int index) {
		int newIndex = ((start + index) % capacity);
		return newIndex;
	}

	/**
	 * get an element of the ring buffer
	 * @param index the position relative to the start
	 * @return the corresponding element
	 */
	public E get(int index) {
		return buffer.get(normalizeIndex(index));
	}
	
	/**
	 * add an element to the end of the rinfbuffer
	 * overwrites the current start element if the
	 * ring buffer is full
	 * @param element the element to add
	 */
	public void add(E element) {
		if (size() < capacity) {
			buffer.add(element);
		} else {
			int index = size();
			int normalizedIndex = normalizeIndex(index);
			start = normalizeIndex(index + 1);
			buffer.set(normalizedIndex, element);
		}
	}
	
	/**
	 * get the current size of the ring buffer
	 * @return the size of the ring buffer
	 */
	public int size() {
		return buffer.size();
	}
	
	/**
	 * converts this ring buffer to an ordinary list
	 * @return a list with the elements in the right order
	 */
	public List<E> toList() {
		ArrayList<E> l = new ArrayList<>();
		for(int i = 0; i < size(); ++i)
		{
			l.add(get(i));
		}
		return l;
	}

	@Override
	public String toString() {
		int s = size();
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int i = 0; i < size(); ++i)
		{
			sb.append(get(i));
			if(i < s - 1)
			{
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}
}

