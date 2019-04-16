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
 * CapabilitiesHandler.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.capabilities;

/**
 * Interface marking classes as being able to specify the capabilities
 * they can handle.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public interface CapabilitiesHandler {

  /**
   * Gets the capabilities of the object. Should be overridden if
   * the object's capabilities can change.
   *
   * @return  The capabilities of the object.
   */
  default Capabilities getCapabilities() {
    // On first access, register with the immutable capabilities lookup
    if (!ImmutableCapabilities.StaticLookup.isDefined(this))
      ImmutableCapabilities.StaticLookup.define(this, defineImmutableCapabilities());

    // Get our capabilities from the lookup
    return ImmutableCapabilities.StaticLookup.get(this);
  }

  /**
   * Defines the set of capabilities the object has. Should be overridden
   * if the object's capabilities do not change.
   *
   * @return  The capabilities of the object.
   */
  default ImmutableCapabilities defineImmutableCapabilities() {
    return CapabilityRequirement.NON_HANDLER_CAPABILITIES;
  }
}
