/*
 *    Pair.java
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
import java.io.Serializable;

public class Pair<T extends Comparable<T>, U extends Comparable<U>> implements Serializable, Comparable<Pair<T, U>> {
    /**
	 * 
	 */
	private static final long serialVersionUID = -1048781440947783998L;
	private T first;
    private U second;
    
    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }
    
    public U getSecond() {
        return second;
    }
    
    public void setSecond(U second) {
        this.second = second;
    }
    
    public void setFirst(T first) {
        this.first = first;
    }
    
    @Override
    public int compareTo(Pair<T, U> o) {
        int cmp = second.compareTo(o.second);
        if (cmp == 0) return first.compareTo(o.first);
        return cmp;
    }
}
