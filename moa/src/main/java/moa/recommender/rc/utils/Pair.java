/*
 *    Pair.java
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
