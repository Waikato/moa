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
 * Capability.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.capabilities;

/**
 * Class enumerating the different possible capabilities of objects in
 * MOA.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public enum Capability {
  VIEW_STANDARD, // Can be viewed in STANDARD mode
  VIEW_LITE, // Can be viewed in LITE mode
  VIEW_EXPERIMENTAL; // Can be viewed in EXPERIMENTAL mode

  @Override
  public String toString() {
    // Removes the "VIEW_" part of the name
    return name().substring(5);
  }

  /**
   * Creates an array of the string representation
   * of each value.
   *
   * @return  The array of strings.
   */
  public static String[] stringValues() {
    Capability[] values = values();
    String[] stringValues = new String[values.length];

    for (int i = 0; i < values.length; i++) {
      stringValues[i] = values[i].toString();
    }

    return stringValues;
  }

  public static Capability forShortName(String shortName) {
    return Enum.valueOf(Capability.class, "VIEW_" + shortName.toUpperCase());
  }
}
