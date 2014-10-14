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
 * DiffUtils.java
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */
package moa.test;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.InsertDelta;
import difflib.Patch;

/**
 * A helper class for generating diffs between two files, lists of strings.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 6358 $
 */
public class DiffUtils {

  /** the indicator for "changed". */
  public final static char INDICATOR_CHANGED = 'c';

  /** the indicator for "added". */
  public final static char INDICATOR_ADDED = 'a';

  /** the indicator for "deleted". */
  public final static char INDICATOR_DELETED = 'd';

  /** the indicator for "same". */
  public final static char INDICATOR_SAME = ' ';

  /** the source indicator. */
  public final static String SOURCE = "<";

  /** the destination indicator. */
  public final static String DESTINATION = ">";

  /** the separator for the unified output. */
  public final static String SEPARATOR_UNIFIED = "---";

  /** the separator for the side-by-side output on the command-line. */
  public final static String SEPARATOR_SIDEBYSIDE = " | ";

  /** the unified option. */
  public final static String OPTION_UNIFIED = "unified";

  /** the side-by-side option. */
  public final static String OPTION_SIDEBYSIDE = "side-by-side";

  /** the brief option. */
  public final static String OPTION_BRIEF = "brief";

  /** the number of array elements in side-by-side diff. */
  public final static int SIDEBYSIDE_SIZE = 5;

  /** the index for the first file in side-by-side diff. */
  public final static int SIDEBYSIDE_FIRST = 0;

  /** the index for the second file in side-by-side diff. */
  public final static int SIDEBYSIDE_SECOND = 1;

  /** the index for the indicator list in side-by-side diff. */
  public final static int SIDEBYSIDE_INDICATOR = 2;

  /** the index for the list of start positions of deltas in side-by-side diff. */
  public final static int SIDEBYSIDE_STARTPOS = 3;

  /** the index for the list of end positions of deltas in side-by-side diff. */
  public final static int SIDEBYSIDE_ENDPOS = 4;

  /**
   * Container object for a side-by-side diff.
   *
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 6358 $
   */
  public static class SideBySideDiff
    implements Serializable, Cloneable {

    /** for serialization. */
    private static final long serialVersionUID = -6775907286936991130L;

    /** the diff information. */
    protected List[] m_Diff;

    /**
     * Initializes the container with an empty diff.
     */
    public SideBySideDiff() {
      this(
	  new List[]{
	      new ArrayList(),
	      new ArrayList(),
	      new ArrayList(),
	      new ArrayList(),
	      new ArrayList()});
    }

    /**
     * Initializes the container.
     */
    public SideBySideDiff(List[] diff) {
      if (diff.length != SIDEBYSIDE_SIZE)
	throw new IllegalArgumentException("Expected array with length " + SIDEBYSIDE_SIZE + " but got: " + diff.length);
      m_Diff = diff.clone();
    }

    /**
     * Return the diff of the left/first list/file.
     *
     * @return		the diff
     */
    public List getLeft() {
      return m_Diff[SIDEBYSIDE_FIRST];
    }

    /**
     * Return the diff of the right/second list/file.
     *
     * @return		the diff
     */
    public List getRight() {
      return m_Diff[SIDEBYSIDE_SECOND];
    }

    /**
     * Returns the indicator list.
     *
     * @return		the indicator
     */
    public List getIndicator() {
      return m_Diff[SIDEBYSIDE_INDICATOR];
    }

    /**
     * Return the list with starting positions of the deltas.
     *
     * @return		the list with start positions
     */
    public List getStartPos() {
      return m_Diff[SIDEBYSIDE_STARTPOS];
    }

    /**
     * Returns whether there are any differences between the two files/lists.
     *
     * @return		true if there are differences
     */
    public boolean hasDifferences() {
      return (m_Diff[SIDEBYSIDE_STARTPOS].size() > 0);
    }

    /**
     * Returns the number of differences.
     *
     * @return		the number of differences
     */
    public int differences() {
      return m_Diff[SIDEBYSIDE_STARTPOS].size();
    }

    /**
     * Returns the closest patch delta index for the given line number.
     *
     * @param line	the 0-based line number
     * @return		the index of the delta, -1 if no delta matched
     */
    public int lineToDelta(int line) {
      int	result;
      int	i;
      List	start;
      List	end;

      result = -1;

      if (!hasDifferences())
	return result;

      start = m_Diff[SIDEBYSIDE_STARTPOS];
      end   = m_Diff[SIDEBYSIDE_ENDPOS];
      for (i = 0; i < start.size(); i++) {
	if ((((Integer) start.get(i)) >= line) && (line <= ((Integer) end.get(i)))) {
	  result = i;
	  break;
	}
      }

      return result;
    }

    /**
     * Returns the line number (start or end) of the given delta.
     *
     * @param delta	the 0-based delta index
     * @return		the line number, -1 if failed to determine
     */
    public int deltaToLine(int delta, boolean start) {
      int	result;

      result = -1;

      if ((delta >= 0) && (delta < m_Diff[SIDEBYSIDE_STARTPOS].size())) {
	if (start)
	  result = (Integer) m_Diff[SIDEBYSIDE_STARTPOS].get(delta);
	else
	  result = (Integer) m_Diff[SIDEBYSIDE_ENDPOS].get(delta);
      }

      return result;
    }

    /**
     * Checks whether there is a next delta after the current line.
     *
     * @param line	the current line number (0-based)
     * @return		the next delta index, -1 if none available
     */
    public boolean hasNextDelta(int line) {
      return (getNextDelta(line) != -1);
    }

    /**
     * Returns the next delta after the current line.
     *
     * @param line	the current line number (0-based)
     * @return		the next delta index, -1 if none available
     */
    public int getNextDelta(int line) {
      int	result;
      int	delta;

      result = -1;
      delta  = lineToDelta(line);
      if (delta == -1)
	return result;

      delta++;
      if (delta < m_Diff[SIDEBYSIDE_STARTPOS].size())
	result = delta;

      return result;
    }

    /**
     * Checks whether there is a previous delta after the current line.
     *
     * @param line	the current line number (0-based)
     * @return		the previous delta index, -1 if none available
     */
    public boolean hasPreviousDelta(int line) {
      return (getPreviousDelta(line) != -1);
    }

    /**
     * Returns the previous delta after the current line.
     *
     * @param line	the current line number (0-based)
     * @return		the previous delta index, -1 if none available
     */
    public int getPreviousDelta(int line) {
      int	result;
      int	delta;

      result = -1;
      delta  = lineToDelta(line);
      if (delta == -1)
	return result;

      delta--;
      if (delta >= 0)
	result = delta;

      return result;
    }

    /**
     * Returns a clone if itself.
     *
     * @return		the clone
     */
    @Override
    public SideBySideDiff clone() {
      return new SideBySideDiff(m_Diff);
    }

    /**
     * Generates a string representation of the diff information.
     *
     * @param separator	the separator between the columns
     * @return		the string representation
     */
    public String toString(String separator) {
      StringBuilder	result;
      int		i;

      result = new StringBuilder();

      for (i = 0; i < m_Diff[SIDEBYSIDE_FIRST].size(); i++) {
	result.append(m_Diff[SIDEBYSIDE_INDICATOR].get(i));
	result.append(separator);
	result.append(m_Diff[SIDEBYSIDE_FIRST].get(i));
	result.append(separator);
	result.append(m_Diff[SIDEBYSIDE_SECOND].get(i));
	result.append("\n");
      }

      return result.toString();
    }

    /**
     * Generates a string representation of the diff information.
     *
     * @return		the string representation
     */
    @Override
    public String toString() {
      return toString(SEPARATOR_SIDEBYSIDE);
    }
  }

  /**
   * A helper class for the side-by-side diff.
   *
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 6358 $
   */
  public static class Filler
    implements Serializable {

    /** for serialization. */
    private static final long serialVersionUID = 3295616348711569065L;

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if the object is a {@link Filler} instance
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == null)
	return false;
      else if (obj instanceof Filler)
	return true;
      else
	return false;
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     *
     * @return  a hash code value for this object.
     */
    @Override
    public int hashCode() {
      return toString().hashCode();
    }

    /**
     * Returns an empty string.
     *
     * @return		empty string
     */
    @Override
    public String toString() {
      return "";
    }
  }

  /**
   * Loads the file. If file points to a directory, an empty vector is
   * returned instead.
   *
   * @param file	the file to load
   * @return		the content of the file, empty vector if directory
   */
  protected static List<String> loadFromFile(File file) {
    if (file.isDirectory())
      return new ArrayList<String>();
    else
      return FileUtils.loadFromFile(file);
  }

  /**
   * Returns whether the two files differ.
   *
   * @param file1	the first text file
   * @param file2	the second text file
   * @return		true if different
   */
  public static boolean isDifferent(File file1, File file2) {
    return isDifferent(loadFromFile(file1), loadFromFile(file2));
  }

  /**
   * Returns whether the two lists differ.
   *
   * @param file1	the first list
   * @param file2	the second list
   * @return		true if different
   */
  public static boolean isDifferent(String[] list1, String[] list2) {
    return isDifferent(Arrays.asList(list1), Arrays.asList(list2));
  }

  /**
   * Returns whether the two lists differ.
   *
   * @param file1	the first text file
   * @param file2	the second text file
   * @return		the side-by-side diff (first file: index 0, second file: index 1, indicator: )
   */
  public static boolean isDifferent(List<String> list1, List<String> list2) {
    boolean	result;
    Patch	patch;

    patch  = difflib.DiffUtils.diff(list1, list2);
    result = (patch.getDeltas().size() > 0);

    return result;
  }

  /**
   * Assembles the lines into a string.
   *
   * @param ind		the indicator string
   * @param lines	the underlying lines
   * @return		the generated string
   */
  protected static String toString(String ind, List lines) {
    StringBuilder	result;

    result = new StringBuilder();

    for (Object line: lines)
      result.append(ind + " " + line + "\n");

    return result.toString();
  }

  /**
   * Generates a unified diff for the two files.
   *
   * @param file1	the first text file
   * @param file2	the second text file
   * @return		the unified diff
   */
  public static String unified(File file1, File file2) {
    return unified(loadFromFile(file1), loadFromFile(file2));
  }

  /**
   * Creates a range string.
   *
   * @param chunk	the chunk to create the range string for
   * @return		the range string
   */
  protected static String createRange(Chunk chunk) {
    if (chunk.size() == 1)
      return (chunk.getPosition() + 1) + "";
    else
      return (chunk.getPosition() + 1) + "," + (chunk.getPosition() + chunk.size());
  }

  /**
   * Generates a unified diff for the two lists.
   *
   * @param file1	the first list
   * @param file2	the second list
   * @return		the unified diff
   */
  public static String unified(List<String> list1, List<String> list2) {
    StringBuilder	result;
    Patch		patch;

    patch  = difflib.DiffUtils.diff(list1, list2);
    result = new StringBuilder();
    for (Delta delta: patch.getDeltas()) {
      if (delta instanceof InsertDelta) {
	result.append(delta.getOriginal().getPosition() + "a" + createRange(delta.getRevised()) + "\n");
	result.append(toString(DESTINATION, delta.getRevised().getLines()));
      }
      else if (delta instanceof DeleteDelta) {
	result.append(createRange(delta.getOriginal()) + "d" + delta.getRevised().getPosition() + "\n");
	result.append(toString(SOURCE, delta.getOriginal().getLines()));
      }
      else {
	result.append(createRange(delta.getOriginal()) + "c");
	result.append(createRange(delta.getRevised()) + "\n");
	result.append(toString(SOURCE, delta.getOriginal().getLines()));
	result.append(SEPARATOR_UNIFIED + "\n");
	result.append(toString(DESTINATION, delta.getRevised().getLines()));
      }
    }

    return result.toString();
  }

  /**
   * Generates a side-by-side diff for the two files.
   *
   * @param file1	the first text file
   * @param file2	the second text file
   * @return		the side-by-side diff
   */
  public static SideBySideDiff sideBySide(File file1, File file2) {
    return sideBySide(loadFromFile(file1), loadFromFile(file2));
  }

  /**
   * Generates a side-by-side diff for the two lists.
   *
   * @param file1	the first list
   * @param file2	the second list
   * @return		the side-by-side diff
   */
  public static SideBySideDiff sideBySide(String[] list1, String[] list2) {
    return sideBySide(Arrays.asList(list1), Arrays.asList(list2));
  }

  /**
   * Adds the specified contents of the source list to the destination list.
   *
   * @param from	the starting index
   * @param to		the ending index (included)
   * @param source	the source list
   * @param dest	the destination of the data
   */
  protected static void addToList(int from, int to, List<String> source, List dest) {
    for (int i = from; i <= to; i++)
      dest.add(source.get(i));
  }

  /**
   * Adds the object to the destination list.
   *
   * @param from	the starting index
   * @param to		the ending index (included)
   * @param dest	the destination of the data
   */
  protected static void addToList(int from, int to, Object obj, List dest) {
    for (int i = from; i <= to; i++)
      dest.add(obj);
  }

  /**
   * Generates a side-by-side diff for the two lists.
   *
   * @param file1	the first text file
   * @param file2	the second text file
   * @return		the side-by-side diff
   */
  public static SideBySideDiff sideBySide(List<String> list1, List<String> list2) {
    List[]	result;
    Patch	patch;
    int		from;
    int		to;
    int		sizeDiff;

    result                       = new List[SIDEBYSIDE_SIZE];
    result[SIDEBYSIDE_FIRST]     = new ArrayList();
    result[SIDEBYSIDE_SECOND]    = new ArrayList();
    result[SIDEBYSIDE_INDICATOR] = new ArrayList();
    result[SIDEBYSIDE_STARTPOS]  = new ArrayList();
    result[SIDEBYSIDE_ENDPOS]    = new ArrayList();
    patch                        = difflib.DiffUtils.diff(list1, list2);

    // the same?
    if (patch.getDeltas().size() == 0) {
      result[SIDEBYSIDE_FIRST].addAll(list1);
      result[SIDEBYSIDE_SECOND].addAll(list2);
      addToList(0, list1.size() - 1, INDICATOR_SAME, result[SIDEBYSIDE_INDICATOR]);
      return new SideBySideDiff(result);
    }

    to = 0;
    for (Delta delta: patch.getDeltas()) {
      from = to;

      // common content
      to = delta.getOriginal().getPosition() - 1;
      addToList(from, to, list1,          result[SIDEBYSIDE_FIRST]);
      addToList(from, to, list1,          result[SIDEBYSIDE_SECOND]);
      addToList(from, to, INDICATOR_SAME, result[SIDEBYSIDE_INDICATOR]);

      // start pos of delta
      result[SIDEBYSIDE_STARTPOS].add(result[SIDEBYSIDE_FIRST].size());

      if (delta instanceof InsertDelta) {
	// added content
	addToList(0, delta.getRevised().size() - 1, new Filler(), result[SIDEBYSIDE_FIRST]);
	result[SIDEBYSIDE_SECOND].addAll(delta.getRevised().getLines());
	addToList(0, delta.getRevised().size() - 1, INDICATOR_ADDED, result[SIDEBYSIDE_INDICATOR]);
	to = delta.getOriginal().getPosition() + delta.getOriginal().size();
      }
      else if (delta instanceof DeleteDelta) {
	// deleted content
	result[SIDEBYSIDE_FIRST].addAll(delta.getOriginal().getLines());
	addToList(0, delta.getOriginal().size() - 1, new Filler(), result[SIDEBYSIDE_SECOND]);
	addToList(0, delta.getOriginal().size() - 1, INDICATOR_DELETED, result[SIDEBYSIDE_INDICATOR]);
	to = delta.getOriginal().getPosition() + delta.getOriginal().size();
      }
      else {
	// changed content
	result[SIDEBYSIDE_FIRST].addAll(delta.getOriginal().getLines());
	result[SIDEBYSIDE_SECOND].addAll(delta.getRevised().getLines());
	addToList(1, Math.max(delta.getOriginal().size(), delta.getRevised().size()), INDICATOR_CHANGED, result[SIDEBYSIDE_INDICATOR]);
	// filler necessary?
	sizeDiff = delta.getRevised().size() - delta.getOriginal().size();
	if (sizeDiff > 0)
	  addToList(1, sizeDiff, new Filler(), result[SIDEBYSIDE_FIRST]);
	if (sizeDiff < 0)
	  addToList(1, -sizeDiff, new Filler(), result[SIDEBYSIDE_SECOND]);
	to = delta.getOriginal().getPosition() + delta.getOriginal().size();
      }

      // end pos of delta
      result[SIDEBYSIDE_ENDPOS].add(result[SIDEBYSIDE_FIRST].size());
    }

    // trailing common content?
    if (to < list1.size()) {
      addToList(to, list1.size() - 1, list1,          result[SIDEBYSIDE_FIRST]);
      addToList(to, list1.size() - 1, list1,          result[SIDEBYSIDE_SECOND]);
      addToList(to, list1.size() - 1, INDICATOR_SAME, result[SIDEBYSIDE_INDICATOR]);
    }

    return new SideBySideDiff(result);
  }

  /**
   * Usage: DiffUtils &lt;unified|side-by-side|brief&gt; &lt;file1&gt; &lt;file2&gt;
   *
   * @param args	the files to compare
   * @throws Exception	if comparison fails
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 3) {
      File file1 = new File(args[1]);
      if (file1.isDirectory()) {
	System.err.println("File '" + file1 + "' is a directory!");
	return;
      }
      if (!file1.exists()) {
	System.err.println("File '" + file1 + "' does not exist!");
	return;
      }
      File file2 = new File(args[2]);
      if (file2.isDirectory()) {
	System.err.println("File '" + file2 + "' is a directory!");
	return;
      }
      if (!file2.exists()) {
	System.err.println("File '" + file2 + "' does not exist!");
	return;
      }
      if (args[0].equals(OPTION_UNIFIED)) {
	System.out.println(unified(file1, file2));
      }
      else if (args[0].equals(OPTION_SIDEBYSIDE)) {
	System.out.println(sideBySide(file1, file2));
      }
      else if (args[0].equals(OPTION_BRIEF)) {
	if (isDifferent(file1, file2))
	  System.out.println("Files " + file1 + " and " + file2 + " differ");
      }
      else {
	System.err.println("Only '" + OPTION_UNIFIED + "', '" + OPTION_SIDEBYSIDE + "' and '" + OPTION_BRIEF + "' are available as options!");
	return;
      }
    }
    else {
      System.err.println("\nUsage: " + DiffUtils.class.getName() + " <" + OPTION_UNIFIED + "|" + OPTION_SIDEBYSIDE + "|" + OPTION_BRIEF + "> <file1> <file2>\n");
      return;
    }
  }
}
