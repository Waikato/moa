/*
 *    DenseVector.java
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DenseVector extends Vector {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -6077169543484777829L;
	private ArrayList<Double> list;
    
    public DenseVector() {
        list = new ArrayList<Double>();
    }
    
    public DenseVector(ArrayList<Double> list) {
        this.list = list;
    }
    
    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void set(int index, double val) {
        while (index < list.size())
            list.add(0.0);
        list.set(index, val);
    }

    @Override
    public void remove(int index) {
        list.remove(index);
    }

    @Override
    public Double get(int index) {
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    @Override
    public Set<Integer> getIdxs() {
        HashSet<Integer> keys = new HashSet<Integer>();
        for (int i = 0; i < list.size(); ++i)
            keys.add(i);
        return keys;
    }

    @Override
    public Vector copy() {
        return new DenseVector(new ArrayList<Double>(list));
    }
    
    public class DenseVectorIterator implements Iterator<Pair<Integer, Double>> {
        private int index = 0;
        
        @Override
        public boolean hasNext() {
            return index < DenseVector.this.list.size();
        }

        @Override
        public Pair<Integer, Double> next() {
            return new Pair<Integer, Double>(index, DenseVector.this.list.get(index++));
        }

        @Override
        public void remove() {
            list.remove(index);
        }
    }
    
    @Override
    public Iterator<Pair<Integer, Double>> iterator() {
        return new DenseVectorIterator();
    }
  
}
