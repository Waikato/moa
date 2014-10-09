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
 * TestHelper.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */
package moa.test;

/**
 * A helper class specific to the moa project.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 4584 $
 * @param <I> the type of input data
 * @param <O> the type of output data
 */
public class TestHelper<I, O>
  extends AbstractTestHelper<I, O> {

  /**
   * Initializes the helper class.
   *
   * @param owner	the owning test case
   * @param dataDir	the data directory to use
   */
  public TestHelper(MoaTestCase owner, String dataDir) {
    super(owner, dataDir);
  }

  /**
   * Dummy, does nothing.
   *
   * @param filename	the filename to load (without path)
   * @return		always null
   */
  @Override
  public I load(String filename) {
    return null;
  }

  /**
   * Dummy, just write the string returned by the object's toString()
   * method to the file.
   *
   * @param data	the data to save
   * @param filename	the filename to save to (without path)
   * @return		always true
   */
  @Override
  public boolean save(O data, String filename) {
    return FileUtils.writeToFile(new TmpFile(filename).getAbsolutePath(), data.toString(), false);
  }
}
