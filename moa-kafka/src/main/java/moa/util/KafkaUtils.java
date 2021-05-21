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
 * KafkaUtils.java
 * Copyright (C) 2021 University of Waikato, Hamilton, NZ
 */

package moa.util;

import java.time.Duration;

/**
 * Static utilities for working with Kafka.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class KafkaUtils {

  // The longest wait time that can be specified to Kafka calls
  public static final Duration WAIT_AS_LONG_AS_POSSIBLE = Duration.ofMillis(Long.MAX_VALUE);

  /**
   * Generates a unique but consistent group ID string for
   * an object.
   */
  public static String uniqueGroupIDString(Object obj) {
    return Long.toHexString(System.currentTimeMillis()) + "-" + Integer.toHexString(obj.hashCode());
  }

  /**
   * Gets the Kafka broker to connect to.
   */
  public static String broker(String host, String port) {
    return host + ":" + port;
  }
}
