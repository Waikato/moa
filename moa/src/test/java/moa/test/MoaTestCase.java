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
 * MoaTestCase.java
 * Copyright (C) 2010-2013 University of Waikato, Hamilton, New Zealand
 */
package moa.test;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Ancestor for all test cases.
 * <p/>
 * Any regression test can be skipped as follows: <br/>
 *   <code>-Dmoa.test.noregression=true</code>
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8349 $
 */
public class MoaTestCase
  extends TestCase {

  /** property indicating whether tests should be run in headless mode. */
  public final static String PROPERTY_HEADLESS = "moa.test.headless";

  /** property indicating whether regression tests should not be executed. */
  public final static String PROPERTY_NOREGRESSION = "moa.test.noregression";

  /** whether to execute any regression test. */
  protected boolean m_NoRegressionTest;
  
  /** the helper class for regression. */
  protected Regression m_Regression;

  /** the test class to use. */
  protected AbstractTestHelper m_TestHelper;

  /** whether to run tests in headless mode. */
  protected boolean m_Headless;

  /**
   * Constructs the test case. Called by subclasses.
   *
   * @param name 	the name of the test
   */
  public MoaTestCase(String name) {
    super(name);
  }

  /**
   * Tries to load the class based on the test class's name.
   *
   * @return		the class that is being tested or null if none could
   * 			be determined
   */
  protected Class getTestedClass() {
    Class	result;

    result = null;

    if (getClass().getName().endsWith("Test")) {
      try {
	result = Class.forName(getClass().getName().replaceAll("Test$", ""));
      }
      catch (Exception e) {
	result = null;
      }
    }

    return result;
  }
  
  /**
   * Returns whether the test can be executed in a headless environment. If not
   * then the test gets skipped.
   * 
   * @return		true if OK to run in headless mode
   */
  protected boolean canHandleHeadless() {
    return true;
  }
  
  /**
   * Called by JUnit before each test method.
   *
   * @throws Exception if an error occurs.
   */
  @Override
  protected void setUp() throws Exception {
    Class	cls;

    super.setUp();
    
    cls = getTestedClass();
    if (cls != null)
      m_Regression = new Regression(cls);

    m_TestHelper       = newTestHelper();
    m_Headless         = Boolean.getBoolean(PROPERTY_HEADLESS);
    m_NoRegressionTest = Boolean.getBoolean(PROPERTY_NOREGRESSION);
  }

  /**
   * Override to run the test and assert its state. Checks whether the test
   * or test-method is platform-specific.
   * 
   * @throws Throwable if any exception is thrown
   */
  @Override
  protected void runTest() throws Throwable {
    boolean		proceed;
    
    proceed = true;
    
    if (m_Headless && !canHandleHeadless())
      proceed = false;
    
    if (proceed)
      super.runTest();
    else
      System.out.println("Skipped");
  }

  /**
   * Called by JUnit after each test method.
   *
   * @throws Exception	if tear-down fails
   */
  @Override
  protected void tearDown() throws Exception {
    m_Regression = null;

    super.tearDown();
  }

  /**
   * Returns the test helper class to use.
   *
   * @return		the helper class instance
   */
  protected AbstractTestHelper newTestHelper() {
    return new TestHelper(this, "");
  }

  /**
   * Tries to obtain an instance of the given class.
   *
   * @param cls		the class to obtain an instance from
   * @param intf	the required interface that the class must implement
   * @param fail	if true, errors/exceptions will result in a test fail
   */
  protected Object getInstance(Class cls, Class intf, boolean fail) {
    Object		result;
    Constructor		constr;

    if (!intf.isAssignableFrom(cls))
      return null;

    // default constructor?
    constr = null;
    try {
      constr = cls.getConstructor(new Class[0]);
    }
    catch (NoSuchMethodException e) {
      if (fail)
	fail("No default constructor, requires custom test method: " + cls.getName());
      return null;
    }

    // create instance
    result = null;
    try {
      result = constr.newInstance(new Object[0]);
    }
    catch (Exception e) {
      if (fail)
	fail("Failed to instantiate object using default constructor: " + cls.getName());
      return null;
    }

    return result;
  }
  
  /**
   * Creates a deep copy of the given object (must be serializable!). Returns
   * null in case of an error.
   *
   * @param o		the object to copy
   * @return		the deep copy
   */
  protected Object deepCopy(Object o) {
    Object		result;
    SerializedObject	so;

    try {
      so     = new SerializedObject((Serializable) o);
      result = so.getObject();
    }
    catch (Exception e) {
      System.err.println("Failed to serialize " + o.getClass().getName() + ":");
      e.printStackTrace();
      result = null;
    }

    return result;
  }

  /**
   * Rounds a double and converts it into String.
   *
   * @param value 		the double value
   * @param afterDecimalPoint 	the (maximum) number of digits permitted
   * 				after the decimal point
   * @return 			the double as a formatted string
   */
  public static String doubleToString(double value, int afterDecimalPoint) {
    StringBuilder 	builder;
    double 		temp;
    int 		dotPosition;
    int 		currentPos;
    long 		precisionValue;
    char		separator;

    temp = value * Math.pow(10.0, afterDecimalPoint);
    if (Math.abs(temp) < Long.MAX_VALUE) {
      precisionValue = 	(temp > 0) ? (long)(temp + 0.5)
	  : -(long)(Math.abs(temp) + 0.5);
      if (precisionValue == 0)
	builder = new StringBuilder(String.valueOf(0));
      else
	builder = new StringBuilder(String.valueOf(precisionValue));

      if (afterDecimalPoint == 0)
	return builder.toString();

      separator   = '.';
      dotPosition = builder.length() - afterDecimalPoint;
      while (((precisionValue < 0) && (dotPosition < 1)) || (dotPosition < 0)) {
	if (precisionValue < 0)
	  builder.insert(1, '0');
	else
	  builder.insert(0, '0');
	dotPosition++;
      }

      builder.insert(dotPosition, separator);

      if ((precisionValue < 0) && (builder.charAt(1) == separator))
	builder.insert(1, '0');
      else if (builder.charAt(0) == separator)
	builder.insert(0, '0');

      currentPos = builder.length() - 1;
      while ((currentPos > dotPosition) && (builder.charAt(currentPos) == '0'))
	builder.setCharAt(currentPos--, ' ');

      if (builder.charAt(currentPos) == separator)
	builder.setCharAt(currentPos, ' ');

      return builder.toString().trim();
    }
    return new String("" + value);
  }

  /**
   * Performs a serializable test on the given class.
   *
   * @param cls		the class to test
   */
  protected void performSerializableTest(Class cls) {
    Object		obj;

    obj = getInstance(cls, Serializable.class, true);
    if (obj == null)
      return;
    
    assertNotNull("Serialization failed", deepCopy(obj));
  }

  /**
   * For classes (with default constructor) that are serializable, are tested
   * whether they are truly serializable.
   */
  public void testSerializable() {
    if (m_Regression != null)
      performSerializableTest(m_Regression.getRegressionClass());
  }

  /**
   * Runs the specified suite. Used for running the test from commandline.
   *
   * @param suite	the suite to run
   */
  public static void runTest(Test suite) {
    TestRunner.run(suite);
  }
}
