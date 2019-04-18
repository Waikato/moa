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
 * Capabilities.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.capabilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Container class representing the set of capabilities an object
 * has.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class Capabilities {

  /** The set of capabilities. */
  protected Set<Capability> m_Capabilities = new HashSet<>();

  /** Creates a capabilities object with no capabilities. */
  public Capabilities() {
  }

  /**
   * Creates a capabilities object with the given capabilities.
   *
   * @param capabilities  The capabilities to initially have.
   */
  public Capabilities(Capability... capabilities) {
    addCapabilities(capabilities);
  }

  /**
   * Augments this capabilities object with the given capabilities.
   *
   * @param capabilities  The capabilities to add.
   */
  public void addCapabilities(Capability... capabilities) {
    for (Capability capability : capabilities) {
      addCapability(capability);
    }
  }

  /**
   * Augments this capabilities object with the given capabilities.
   *
   * @param capabilities  The capabilities to add.
   */
  public void addCapabilities(Collection<Capability> capabilities) {
    m_Capabilities.addAll(capabilities);
  }

  /**
   * Augments this capabilities object with the given capabilities.
   *
   * @param other  The capabilities to add.
   */
  public void addCapabilities(Capabilities other) {
    for (Capability capability : other.m_Capabilities) {
      addCapability(capability);
    }
  }

  /**
   * Augments this capabilities object with the given capability.
   *
   * @param capability  The capability to add.
   */
  public void addCapability(Capability capability) {
    m_Capabilities.add(capability);
  }

  /**
   * Returns whether this capabilities object contains the given capability.
   *
   * @return  True if this capabilities object contains the given capability,
   *          false if not.
   */
  public boolean hasCapability(Capability capability) {
    return m_Capabilities.contains(capability);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Capabilities)
      return equals((Capabilities) other);
    else if (other instanceof Capability)
      return equals((Capability) other);
    else
      return false;
  }

  public boolean equals(Capabilities other) {
    return m_Capabilities.equals(other.m_Capabilities);
  }

  public boolean equals(Capability other) {
    return m_Capabilities.size() == 1 && m_Capabilities.contains(other);
  }
}
