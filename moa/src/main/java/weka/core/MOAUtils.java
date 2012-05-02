/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

/*
 * MOAUtils.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import moa.MOAObject;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;

/**
 * A helper class for MOA related classes.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MOAUtils {

  /**
   * Turns a commandline into an object (classname + optional options).
   * 
   * @param option the corresponding class option
   * @param commandline the commandline to turn into an object
   * @return the generated oblect
   */
  public static MOAObject fromCommandLine(ClassOption option, String commandline) {
  	return fromCommandLine(option.getRequiredType(), commandline);
  }

  /**
   * Turns a commandline into an object (classname + optional options).
   * 
   * @param requiredType the required class
   * @param commandline the commandline to turn into an object
   * @return the generated oblect
   */
  public static MOAObject fromCommandLine(Class requiredType, String commandline) {
  	MOAObject     result;
  	String[]			tmpOptions;
  	String				classname;
  	
  	try {
  		tmpOptions    = Utils.splitOptions(commandline);
  		classname     = tmpOptions[0];
  		tmpOptions[0] = "";
  		try {
  			result = (MOAObject) Class.forName(classname).newInstance();
  		}
  		catch (Exception e) {
  			// try to prepend package name
  			result = (MOAObject) Class.forName(requiredType.getPackage().getName() + "." + classname).newInstance();
  		}
  		if (result instanceof AbstractOptionHandler) {
  			((AbstractOptionHandler) result).getOptions().setViaCLIString(Utils.joinOptions(tmpOptions));
  			((AbstractOptionHandler) result).prepareForUse();
  		}
  	}
  	catch (Exception e) {
  		e.printStackTrace();
  		result = null;
  	}
  	
  	return result;
  }
  
  /**
   * Creates a MOA object from the specified class option.
   * 
   * @param option the option to build the object from
   * @return the created object
   */
  public static MOAObject fromOption(ClassOption option) {
  	return MOAUtils.fromCommandLine(option.getRequiredType(), option.getValueAsCLIString());
  }
  
  /**
   * Returs the commandline for the given object. If the object is not
   * derived from AbstractOptionHandler, then only the classname. Otherwise
   * the classname and the options are returned.
   * 
   * @param obj the object to generate the commandline for
   * @return the commandline
   */
  public static String toCommandLine(MOAObject obj) {
  	String result = obj.getClass().getName();
  	if (obj instanceof AbstractOptionHandler)
  		result += " " + ((AbstractOptionHandler) obj).getOptions().getAsCLIString();
  	return result.trim();
  }
}
