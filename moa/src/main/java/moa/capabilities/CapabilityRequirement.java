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
 * CapabilityRequirement.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.capabilities;

import java.util.function.Predicate;

/**
 * Represents a requirement that a set of capabilities must meet.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class CapabilityRequirement
{

  /**
   * The capabilities to assume a class has if it does not implement
   * the CapabilitiesHandler interface.
   */
  public static final ImmutableCapabilities NON_HANDLER_CAPABILITIES = new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);

  /** The function to use to test if the requirement is met. */
  private Predicate<Capabilities> m_Test;

  /**
   * Creates a capabilities requirement with the given predicate
   * as its method of checking if the requirement is met.
   *
   * @param check	The test for adherence.
   */
  public CapabilityRequirement(Predicate<Capabilities> check) {
    // Can't create a null test
    if (check == null)
      throw new IllegalArgumentException("Capability requirement cannot be null");

    m_Test = check;
  }

  /**
   * Tests if the requirement is met by the given set of capabilities.
   *
   * @param capabilities	The set of capabilities to test.
   * @return			True if the requirement is met,
   * 				false if not.
   */
  public boolean isMetBy(Capabilities capabilities) {
    return m_Test.test(capabilities);
  }

  /**
   * Tests if the requirement is met by the given capabilities handler.
   *
   * @param handler	The handler to test.
   * @return		True if the handler meets the requirements,
   * 			false if not.
   */
  public boolean isMetBy(CapabilitiesHandler handler) {
    return isMetBy(handler.getCapabilities());
  }

  /**
   * Tests if the requirement is met by the given class.
   *
   * @param klass	The class to test.
   * @return		True if the class meets the requirements,
   * 			false if not.
   */
  public boolean isMetBy(Class<?> klass) {
    // Classes which aren't capabilities handlers have an assumed
    // set of capabilities
    if (!CapabilitiesHandler.class.isAssignableFrom(klass))
      return isMetBy(NON_HANDLER_CAPABILITIES);

    // Attempt to instantiate an instance of the class
    CapabilitiesHandler instance;
    try {
       instance = (CapabilitiesHandler) klass.newInstance();
    }
    catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Couldn't instantiate CapabilitiesHandler " +
	klass.getSimpleName(),
	e);
    }

    // Test the instance
    return isMetBy(instance);
  }

  /**
   * Creates a requirement which is the logical OR of this and the given
   * requirement.
   *
   * @param other	The other operand to the OR.
   * @return		The resulting requirement.
   */
  public CapabilityRequirement or(CapabilityRequirement other) {
    return new CapabilityRequirement(this.m_Test.or(other.m_Test));
  }

  /**
   * Creates a requirement that a given set of capabilities have all of the
   * specified capabilities.
   *
   * @param capabilities	The capabilities that a tested set must have.
   * @return			The requirement.
   */
  public static CapabilityRequirement hasAll(Capability... capabilities) {
    return new CapabilityRequirement(c -> {
      for (Capability capability : capabilities) {
        if (!c.hasCapability(capability))
          return false;
      }
      return true;
    });
  }

  /**
   * Creates a requirement that a given set of capabilities have at least on
   * of the specified capabilities.
   *
   * @param capabilities	The capabilities that a tested set must have
   *                            at least one of.
   * @return			The requirement.
   */
  public static CapabilityRequirement hasAny(Capability... capabilities) {
    return new CapabilityRequirement(c -> {
      for (Capability capability : capabilities) {
	if (c.hasCapability(capability))
	  return true;
      }
      return false;
    });
  }

  /**
   * Creates a requirement that a given set of capabilities must have the
   * given capability.
   *
   * @param capability	The capability a tested set must have.
   * @return		The requirement.
   */
  public static CapabilityRequirement has(Capability capability) {
    return new CapabilityRequirement(c -> c.hasCapability(capability));
  }
}
