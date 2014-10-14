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

/**
 * Utils.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */
package moa.test;

import java.lang.reflect.Array;

/**
 * Class with useful methods.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class Utils {

  /**
   * Returns the basic class of an array class (handles multi-dimensional
   * arrays).
   * @param c        the array to inspect
   * @return         the class of the innermost elements
   */
  public static Class getArrayClass(Class c) {
    if (c.getComponentType().isArray())
      return getArrayClass(c.getComponentType());
    else
      return c.getComponentType();
  }

  /**
   * Returns the dimensions of the given array. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   *
   * @param array       the array to determine the dimensions for
   * @return            the dimensions of the array
   */
  public static int getArrayDimensions(Class array) {
    if (array.getComponentType().isArray())
      return 1 + getArrayDimensions(array.getComponentType());
    else
      return 1;
  }

  /**
   * Returns the dimensions of the given array. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   *
   * @param array       the array to determine the dimensions for
   * @return            the dimensions of the array
   */
  public static int getArrayDimensions(Object array) {
    return getArrayDimensions(array.getClass());
  }

  /**
   * Returns the given Array in a string representation. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   *
   * @param array       the array to return in a string representation
   * @param outputClass	whether to output the class name instead of calling
   * 			the object's "toString()" method
   * @return            the array as string
   */
  public static String arrayToString(Object array, boolean outputClass) {
    StringBuilder	result;
    int			dimensions;
    int			i;
    Object		obj;

    result     = new StringBuilder();
    dimensions = getArrayDimensions(array);

    if (dimensions == 0) {
      result.append("null");
    }
    else if (dimensions == 1) {
      for (i = 0; i < Array.getLength(array); i++) {
	if (i > 0)
	  result.append(",");
	if (Array.get(array, i) == null) {
	  result.append("null");
	}
	else {
	  obj = Array.get(array, i);
	  if (outputClass) {
	    if (obj instanceof Class)
	      result.append(((Class) obj).getName());
	    else
	      result.append(obj.getClass().getName());
	  }
	  else {
	    result.append(obj.toString());
	  }
	}
      }
    }
    else {
      for (i = 0; i < Array.getLength(array); i++) {
	if (i > 0)
	  result.append(",");
	result.append("[" + arrayToString(Array.get(array, i)) + "]");
      }
    }

    return result.toString();
  }

  /**
   * Returns the given Array in a string representation. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   *
   * @param array       the array to return in a string representation
   * @return            the array as string
   */
  public static String arrayToString(Object array) {
    return arrayToString(array, false);
  }

}
