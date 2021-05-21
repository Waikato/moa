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
 * ObjectSerializer.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.util;

import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Kafka serialiser for Java objects. Uses Java's serialisation tools
 * internally.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class ObjectSerializer<T>
  implements Serializer<T> {

  @Override
  public byte[] serialize(String topic, T data) {
    // Null serialises to null
    if (data == null)
      return null;

    try {
      ByteArrayOutputStream streamSerialiser = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(streamSerialiser);
      objectStream.writeObject(data);
      objectStream.flush();
      return streamSerialiser.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialise instance for Kafka", e);
    }
  }
}
