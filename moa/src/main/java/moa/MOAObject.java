/*
 *    MOAObject.java
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
package moa;

import java.io.Serializable;

/**
 * Interface implemented by classes in MOA, so that all are serializable,
 * can produce copies of their objects, and can measure its memory size.
 * They also give a string description. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface MOAObject extends Serializable {

    /**
     * Gets the memory size of this object.
     *
     * @return the memory size of this object
     */
    public int measureByteSize();

    /**
     * This method produces a copy of this object.
     *
     * @return a copy of this object
     */
    public MOAObject copy();

    /**
     * Returns a string representation of this object.
     * Used in <code>AbstractMOAObject.toString</code>
     * to give a string representation of the object.
     *
     * @param sb	the stringbuilder to add the description
     * @param indent	the number of characters to indent
     */
    public void getDescription(StringBuilder sb, int indent);
}
