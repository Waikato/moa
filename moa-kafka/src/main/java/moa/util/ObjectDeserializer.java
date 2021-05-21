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
 * ObjectDeserializer.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.util;

import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Kafka deserialiser for Java objects. Uses Java's serialisation tools
 * internally.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class ObjectDeserializer<T>
  implements Deserializer<T>{

  @Override
  public T deserialize(String s, byte[] bytes) {
    // Bytes can be null; deserialise to null
    if (bytes == null)
      return null;

    try {
      ObjectInputStream streamDeserialiser = new ObjectInputStream(new ByteArrayInputStream(bytes));
      return (T) streamDeserialiser.readObject();
    } catch (IOException | ClassNotFoundException | ClassCastException e) {
      throw new RuntimeException("Failed to deserialise object from Kafka", e);
    }
  }
}
