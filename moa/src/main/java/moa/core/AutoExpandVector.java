/*
 *    AutoExpandVector.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.core;

import java.util.ArrayList;
import java.util.Collection;

import moa.AbstractMOAObject;
import moa.MOAObject;

/**
 * Vector with the capability of automatic expansion.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class AutoExpandVector<T> extends ArrayList<T> implements MOAObject {

    private static final long serialVersionUID = 1L;

    public AutoExpandVector() {
        super(0);
    }
    
    public AutoExpandVector(int size) {
        super(size);
    }

    @Override
    public void add(int pos, T obj) {
        if (pos > size()) {
            while (pos > size()) {
                add(null);
            }
            trimToSize();
        }
        super.add(pos, obj);
    }

    @Override
    public T get(int pos) {
        return ((pos >= 0) && (pos < size())) ? super.get(pos) : null;
    }

    @Override
    public T set(int pos, T obj) {
        if (pos >= size()) {
            add(pos, obj);
            return null;
        }
        return super.set(pos, obj);
    }

    @Override
    public boolean add(T arg0) {
        boolean result = super.add(arg0);
        trimToSize();
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        boolean result = super.addAll(arg0);
        trimToSize();
        return result;
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends T> arg1) {
        boolean result = super.addAll(arg0, arg1);
        trimToSize();
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        trimToSize();
    }

    @Override
    public T remove(int arg0) {
        T result = super.remove(arg0);
        trimToSize();
        return result;
    }

    @Override
    public boolean remove(Object arg0) {
        boolean result = super.remove(arg0);
        trimToSize();
        return result;
    }

    @Override
    protected void removeRange(int arg0, int arg1) {
        super.removeRange(arg0, arg1);
        trimToSize();
    }

    @Override
    public MOAObject copy() {
        return AbstractMOAObject.copy(this);
    }

    @Override
    public int measureByteSize() {
        return AbstractMOAObject.measureByteSize(this);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
