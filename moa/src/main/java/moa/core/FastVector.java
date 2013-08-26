/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    FastVector.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */
package moa.core;

import java.util.ArrayList;

/**
 * Simple extension of ArrayList. Exists for legacy reasons.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class FastVector<E> extends ArrayList<E> {

    /**
     * Adds an element to this vector. Increases its capacity if its not large
     * enough.
     *
     * @param element the element to add
     */
    public final void addElement(E element) {
        add(element);
    }

    /**
     * Returns the element at the given position.
     *
     * @param index the element's index
     * @return the element with the given index
     */
    public final E elementAt(int index) {
        return get(index);
    }

    /**
     * Deletes an element from this vector.
     *
     * @param index the index of the element to be deleted
     */
    public final void removeElementAt(int index) {
        remove(index);
    }
}
