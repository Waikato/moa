/*
 *    DenseVector.java
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
