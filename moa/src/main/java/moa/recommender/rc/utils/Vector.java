/*
 *    Vector.java
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
