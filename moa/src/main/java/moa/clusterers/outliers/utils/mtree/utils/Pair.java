/*
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

package moa.clusterers.outliers.utils.mtree.utils;

/**
 * A pair of objects of the same type.
 *
 * @param <T> The type of the objects.
 */
public class Pair<T> {
	
	/**
	 * The first object.
	 */
	public T first;
	
	
	/**
	 * The second object.
	 */
	public T second;
	
	/**
	 * Creates a pair of {@code null} objects.
	 */
	public Pair() {}
	
	/**
	 * Creates a pair with the objects specified in the arguments.
	 * @param first  The first object.
	 * @param second The second object.
	 */
	public Pair(T first, T second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Accesses an object by its index. The {@link #first} object has index
	 * {@code 0} and the {@link #second} object has index {@code 1}.
	 * @param index The index of the object to be accessed.
	 * @return The {@link #first} object if {@code index} is {@code 0}; the
	 *         {@link #second} object if {@code index} is {@code 1}.
	 * @throws IllegalArgumentException If {@code index} is neither {@code 0}
	 *         or {@code 1}.
	 */
	public T get(int index) throws IllegalArgumentException {
		switch(index) {
		case 0: return first;
		case 1: return second;
		default: throw new IllegalArgumentException();
		}
	}

}
