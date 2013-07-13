/*
 *    SparseVector.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SparseVector extends Vector {
    /**
     * 
     */
    private static final long serialVersionUID = 1971022389328939125L;
    private Map<Integer, Double> map;
    
    public SparseVector() {
        this.map = new HashMap<Integer, Double>();
    }
    
    public SparseVector(Map<Integer, Double> map) {
        if (map == null) this.map = new HashMap<Integer, Double>();
        else this.map = map;
    }
    
    public int size() {
        return map.size();
    }
    
    public void set(int index, double val) {
        map.put(index, val);
    }

    public void remove(int index) {
        map.remove(index);
    }
    
    public Set<Integer> getIdxs() {
        return map.keySet();
    }
    
    public SparseVector copy() {
        return new SparseVector(new HashMap<Integer, Double>(this.map));
    }

    @Override
    public Double get(int index) {
        return map.get(index);
    }
    
    public class SparseVectorIterator implements Iterator<Pair<Integer, Double>> {
        private Iterator<Integer> it = SparseVector.this.map.keySet().iterator();
        
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Pair<Integer, Double> next() {
            Integer idx = it.next();
            return new Pair<Integer, Double>(idx, SparseVector.this.map.get(idx));
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    @Override
    public Iterator<Pair<Integer, Double>> iterator() {
        return new SparseVectorIterator();
    }
}
