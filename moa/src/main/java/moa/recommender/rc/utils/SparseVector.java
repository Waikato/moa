/*
 *    SparseVector.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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
