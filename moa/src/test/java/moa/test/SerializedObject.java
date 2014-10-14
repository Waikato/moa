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
 *    SerializedObject.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for storing an object in serialized form in memory. It can be used 
 * to make deep copies of objects, and also allows compression to conserve
 * memory.
 *
 * @author Richard Kirkby (rbk1@cs.waikato.ac.nz)
 * @version $Revision: 4584 $ 
 */
public class SerializedObject
  implements Serializable {

  /** for serialization. */
  private static final long serialVersionUID = 6635502953928860434L;

  /** The array storing the object. */
  private byte[] m_storedObjectArray;

  /** Whether or not the object is compressed. */
  private boolean m_isCompressed;
  
  /**
   * Creates a new serialized object (without compression).
   *
   * @param o 		the object to store
   * @throws Exception 	if the object couldn't be serialized
   */ 
  public SerializedObject(Serializable o) throws Exception {
    this(o, false);
  }

  /**
   * Creates a new serialized object.
   *
   * @param o 		the object to store
   * @param compress 	whether or not to use compression
   * @throws Exception 	if the object couldn't be serialized
   */ 
  public SerializedObject(Serializable o, boolean compress) throws Exception {
    ByteArrayOutputStream 	ostream;
    ObjectOutputStream		p;

    ostream = new ByteArrayOutputStream();
    if (!compress)
      p = new ObjectOutputStream(new BufferedOutputStream(ostream));
    else
      p = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(ostream)));
    p.writeObject(o);
    p.flush();
    p.close();
    m_storedObjectArray = ostream.toByteArray();
    m_isCompressed = compress;
  }

  /**
   * Checks to see whether this object is equal to another.
   *
   * @param o the object to compare to
   * @return whether or not the objects are equal
   */
  @Override
  public boolean equals(Object o) {
    byte[] 	compareArray;
    int		i;
    
    if (o == null)
      return false;
    
    if (!o.getClass().equals(this.getClass())) 
      return false;
    
    compareArray = ((SerializedObject) o).m_storedObjectArray;
    
    if (compareArray.length != m_storedObjectArray.length) 
      return false;
    
    for (i = 0; i < compareArray.length; i++) {
      if (compareArray[i] != m_storedObjectArray[i]) 
	return false;
    }

    return true;
  }

  /**
   * Returns a hashcode for this object.
   *
   * @return the hashcode
   */
  @Override
  public int hashCode() {
    return m_storedObjectArray.length;
  }

  /**
   * Returns a serialized object.
   *
   * @return the restored object
   */ 
  public Object getObject() {
    ByteArrayInputStream 	istream;
    ObjectInputStream 		p;
    Object 			result;
    
    result = null;
    
    try {
      istream = new ByteArrayInputStream(m_storedObjectArray);
      if (!m_isCompressed)
	p = new ObjectInputStream(new BufferedInputStream(istream));
      else 
	p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(istream)));
      result = p.readObject();
      istream.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }

    return result;
  }
  
  /**
   * Returns the size of bytes stored.
   * 
   * @return		the number of bytes
   */
  public int size() {
    return m_storedObjectArray.length;
  }
  
  /**
   * Writes the object to the given file.
   * 
   * @param filename	the file to write to
   * @param o		the object to write
   * @return		true if successfully written
   */
  public static boolean write(String filename, Serializable o) {
    return write(new File(filename), o);
  }
  
  /**
   * Writes the object to the given file.
   * 
   * @param file	the file to write to
   * @param o		the object to write
   * @return		true if successfully written
   */
  public static boolean write(File file, Serializable o) {
    boolean		result;
    ObjectOutputStream 	oo;
    
    try {
      oo = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
      oo.writeObject(o);
      oo.close();
      result = true;
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }
    
    return result;
  }
  
  /**
   * Reads the object from the given file.
   * 
   * @param filename	the file to read from
   * @return		if successful the object, otherwise null
   */
  public static Object read(String filename) {
    return read(new File(filename));
  }
  
  /**
   * Reads the object from the given file.
   * 
   * @param file	the file to read from
   * @return		if successful the object, otherwise null
   */
  public static Object read(File file) {
    Object		result;
    ObjectInputStream	oi;
    
    try {
      oi     = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
      result = oi.readObject();
      oi.close();
    }
    catch (Exception ex) {
      result = null;
    }
    
    return result;
  }
}
