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
 * FileUtils.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */
package moa.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic file-handling stuff.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class FileUtils {
  
  /**
   * Returns the content of the given file, null in case of an error.
   *
   * @param file	the file to load
   * @return		the content/lines of the file
   */
  public static List<String> loadFromFile(File file) {
    return loadFromFile(file, null);
  }

  /**
   * Returns the content of the given file, null in case of an error.
   *
   * @param file	the file to load
   * @param encoding	the encoding to use, null to use default
   * @return		the content/lines of the file
   */
  public static List<String> loadFromFile(File file, String encoding) {
    List<String>	result;
    BufferedReader	reader;
    String		line;

    result = new ArrayList<String>();

    try {
      if ((encoding != null) && (encoding.length() > 0))
	reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath()), encoding));
      else
	reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath())));
      while ((line = reader.readLine()) != null)
        result.add(line);
      reader.close();
    }
    catch (Exception e) {
      result = null;
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Saves the content to the given file.
   *
   * @param content	the content to save
   * @param file	the file to save the content to
   * @return		true if successfully saved
   */
  public static boolean saveToFile(String[] content, File file) {
    List<String>	lines;
    int			i;

    lines = new ArrayList<String>();
    for (i = 0; i < content.length; i++)
      lines.add(content[i]);

    return FileUtils.saveToFile(lines, file);
  }

  /**
   * Saves the content to the given file.
   *
   * @param content	the content to save
   * @param file	the file to save the content to
   * @return		true if successfully saved
   */
  public static boolean saveToFile(List<String> content, File file) {
    return saveToFile(content, file, null);
  }

  /**
   * Saves the content to the given file.
   *
   * @param content	the content to save
   * @param file	the file to save the content to
   * @param encoding	the encoding to use, null for default
   * @return		true if successfully saved
   */
  public static boolean saveToFile(List<String> content, File file, String encoding) {
    boolean		result;
    BufferedWriter	writer;
    int			i;

    result = true;
    
    try {
      if ((encoding != null) && (encoding.length() > 0))
	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath()), encoding));
      else
	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath())));
      for (i = 0; i < content.size(); i++) {
        writer.write(content.get(i));
        writer.newLine();
      }
      writer.flush();
      writer.close();
    }
    catch (Exception e) {
      result = false;
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Writes the given object to the specified file. The object is always
   * appended.
   *
   * @param filename	the file to write to
   * @param obj		the object to write
   * @return		true if writing was successful
   */
  public static boolean writeToFile(String filename, Object obj) {
    return writeToFile(filename, obj, null);
  }

  /**
   * Writes the given object to the specified file. The object is always
   * appended.
   *
   * @param filename	the file to write to
   * @param obj		the object to write
   * @param encoding	the encoding to use, null for default
   * @return		true if writing was successful
   */
  public static boolean writeToFile(String filename, Object obj, String encoding) {
    return writeToFile(filename, obj, true, encoding);
  }

  /**
   * Writes the given object to the specified file. The message is either
   * appended or replaces the current content of the file.
   *
   * @param filename	the file to write to
   * @param obj		the object to write
   * @param append	whether to append the message or not
   * @return		true if writing was successful
   */
  public static boolean writeToFile(String filename, Object obj, boolean append) {
    return writeToFile(filename, obj, append, null);
  }

  /**
   * Writes the given object to the specified file. The message is either
   * appended or replaces the current content of the file.
   *
   * @param filename	the file to write to
   * @param obj		the object to write
   * @param append	whether to append the message or not
   * @param encoding	the encoding to use, null for default
   * @return		true if writing was successful
   */
  public static boolean writeToFile(String filename, Object obj, boolean append, String encoding) {
    boolean		result;
    BufferedWriter	writer;

    try {
      if ((encoding != null) && (encoding.length() > 0))
	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), encoding));
      else
	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append)));
      writer.write("" + obj);
      writer.newLine();
      writer.flush();
      writer.close();
      result = true;
    }
    catch (Exception e) {
      result = false;
    }

    return result;
  }

  /**
   * Copies the file/directory (recursively).
   *
   * @param sourceLocation	the source file/dir
   * @param targetLocation	the target file/dir
   * @return			if successfully copied
   * @throws IOException	if copying fails
   */
  public static boolean copy(File sourceLocation, File targetLocation) throws IOException {
    return copyOrMove(sourceLocation, targetLocation, false);
  }

  /**
   * Moves the file/directory (recursively).
   *
   * @param sourceLocation	the source file/dir
   * @param targetLocation	the target file/dir
   * @return			if successfully moved
   * @throws IOException	if moving fails
   */
  public static boolean move(File sourceLocation, File targetLocation) throws IOException {
    return copyOrMove(sourceLocation, targetLocation, true);
  }

  /**
   * Copies or moves files and directories (recursively).
   * If targetLocation does not exist, it will be created.
   * <p/>
   * Original code from <a href="http://www.java-tips.org/java-se-tips/java.io/how-to-copy-a-directory-from-one-location-to-another-loc.html" target="_blank">Java-Tips.org</a>.
   *
   * @param sourceLocation	the source file/dir
   * @param targetLocation	the target file/dir
   * @param move		if true then the source files/dirs get deleted
   * 				as soon as copying finished
   * @return			false if failed to delete when moving or failed to create target directory
   * @throws IOException	if copying/moving fails
   */
  public static boolean copyOrMove(File sourceLocation, File targetLocation, boolean move) throws IOException {
    String[] 		children;
    int 		i;
    InputStream 	in;
    OutputStream 	out;
    byte[] 		buf;
    int 		len;

    if (sourceLocation.isDirectory()) {
      if (!targetLocation.exists()) {
	if (!targetLocation.mkdir())
	  return false;
      }

      children = sourceLocation.list();
      for (i = 0; i < children.length; i++) {
        if (!copyOrMove(
            new File(sourceLocation.getAbsoluteFile(), children[i]),
            new File(targetLocation.getAbsoluteFile(), children[i]),
            move))
          return false;
      }

      if (move)
        return sourceLocation.delete();
      else
	return true;
    }
    else {
      in = new FileInputStream(sourceLocation.getAbsoluteFile());
      // do we need to append the filename?
      if (targetLocation.isDirectory())
        out = new FileOutputStream(targetLocation.getAbsolutePath() + File.separator + sourceLocation.getName());
      else
        out = new FileOutputStream(targetLocation.getAbsoluteFile());

      // Copy the content from instream to outstream
      buf = new byte[1024];
      while ((len = in.read(buf)) > 0)
        out.write(buf, 0, len);

      in.close();
      out.close();

      if (move)
        return sourceLocation.delete();
      else
	return true;
    }
  }
}
