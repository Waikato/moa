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
 * ImmutableCapabilities.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.capabilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Set of capabilities that cannot be modified after creation.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public final class ImmutableCapabilities extends Capabilities {

  /**
   * Creates an immutable set of capabilities.
   *
   * @param capabilities  The final set of capabilities the object will contain.
   */
  public ImmutableCapabilities(Capability... capabilities) {
    m_Capabilities.addAll(Arrays.asList(capabilities));
  }

  @Override
  public final void addCapabilities(Capability... capabilities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void addCapabilities(Collection<Capability> capabilities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addCapabilities(Capabilities other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void addCapability(Capability capability) {
    throw new UnsupportedOperationException();
  }

  /**
   * Static class containing a lookup of immutable capabilities objects. This allows
   * CapabilitiesHandlers that have identical sets of capabilities to share a single
   * unique set, rather than having one each.
   */
  static class StaticLookup {

    /** The set of unique immutable capabilities objects. */
    private static Map<ImmutableCapabilities, ImmutableCapabilities> m_UniqueSet = new HashMap<>();

    /** The mapping from handlers to their canonical set of capabilities. */
    private static Map<CapabilitiesHandler, ImmutableCapabilities> m_Lookup = new WeakHashMap<>();

    /**
     * Whether the given capabilities handler has defined an
     * immutable set of capabilities.
     *
     * @param owner   The handler in question.
     * @return        True if it has specified its capabilities,
     *                false if not.
     */
    public static boolean isDefined(CapabilitiesHandler owner) {
      // Can't check a null owner
      if (owner == null)
        throw new IllegalArgumentException("Null is not a valid capabilities handler.");

      return m_Lookup.containsKey(owner);
    }

    /**
     * Defines the given handler as having the given immutable set of capabilities.
     *
     * @param owner         The handler.
     * @param capabilities  The set of capabilities.
     */
    public static void define(CapabilitiesHandler owner,
                              ImmutableCapabilities capabilities) {
      // Can't redefine a handler's capabilities
      if (isDefined(owner))
        throw new IllegalArgumentException("Illegal attempt to redefine capabilities.");

      // If given null, interpret as the empty set
      if (capabilities == null)
        capabilities = new ImmutableCapabilities();

      // Substitute the canonical object, or make canonical
      if (m_UniqueSet.containsKey(capabilities))
        capabilities = m_UniqueSet.get(capabilities);
      else
        m_UniqueSet.put(capabilities, capabilities);

      // Save the mapping from owner to capabilities
      m_Lookup.put(owner, capabilities);
    }

    /**
     * Get the stored capabilities of the given handler.
     *
     * @param owner   The capabilities handler.
     * @return        The capabilities of that handler.
     */
    public static ImmutableCapabilities get(CapabilitiesHandler owner) {
      // Owner must be defined before being accessed
      if (!isDefined(owner))
        throw new IllegalArgumentException("Illegal attempt to get capabilities for undefined owner.");

      return m_Lookup.get(owner);
    }
  }

}
