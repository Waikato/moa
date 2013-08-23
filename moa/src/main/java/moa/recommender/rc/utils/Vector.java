/*
 *    Vector.java
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

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

public abstract class Vector implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2440314068879207731L;
    abstract public int size();
    abstract public void set(int index, double val);
    abstract public void remove(int index);
    abstract public Double get(int index);
    abstract public Iterator<Pair<Integer, Double>> iterator();
    abstract public Set<Integer> getIdxs();
    
    public double dotProduct(Vector vec) {
        if (size() > vec.size()) return vec.dotProduct(this);
        Iterator<Pair<Integer, Double>> it = iterator();
        double ret = 0;
        
        while(it.hasNext()) {
            Pair<Integer, Double> ind = it.next();
            Double val1 = ind.getSecond();
            Double val2 = vec.get(ind.getFirst());
            if (val2 != null) ret += val1*val2;
        }
        
        return ret;
    }
    
    public double norm() {
        Iterator<Pair<Integer, Double>> it = iterator();
        double ret = 0;
        
        while(it.hasNext())
            ret += Math.pow(it.next().getSecond(), 2);
        
        return Math.sqrt(ret);
    }
    abstract public Vector copy();
}
