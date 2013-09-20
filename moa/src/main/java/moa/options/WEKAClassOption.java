/*
 *    WEKAClassOption.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author FracPete (fracpete at waikato dot ac dot nz)
 *
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
package moa.options;

import com.github.javacliparser.Option;
import weka.core.Utils;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

//import javax.swing.JComponent;

//import moa.gui.WEKAClassOptionEditComponent;
import moa.tasks.Task;

/**
 * WEKA class option. This option is used to access options in WEKA.
 * For example, WEKAClassifier uses it to set the base learner.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class WEKAClassOption extends AbstractClassOption {

	private static final long serialVersionUID = 1L;

	public WEKAClassOption(String name, char cliChar, String purpose,
			Class<?> requiredType, String defaultCLIString) {
		super(name, cliChar, purpose, requiredType, defaultCLIString);
	}

	public WEKAClassOption(String name, char cliChar, String purpose,
			Class<?> requiredType, String defaultCLIString, String nullString) {
		super(name, cliChar, purpose, requiredType, defaultCLIString, nullString);
	}

    @Override
	public String getValueAsCLIString() {
		if ((this.currentValue == null) && (this.nullString != null)) {
			return this.nullString;
		}
		
		String result = currentValue.getClass().getName();
		if (currentValue instanceof weka.core.OptionHandler)
			result += " " + Utils.joinOptions(((weka.core.OptionHandler) currentValue).getOptions());
		result = result.trim();
		
		return result;
	}

    @Override
	public void setValueViaCLIString(String s) {
		if ((this.nullString != null)
				&& ((s == null) || (s.length() == 0) || s
						.equals(this.nullString))) {
			this.currentValue = null;
		} else {
			try {
				this.currentValue = cliStringToObject(s, this.requiredType,	null);
			} catch (Exception e) {
				throw new IllegalArgumentException("Problems with option: " + getName(), e);
			}
		}
	}

	public static String objectToCLIString(Object obj, Class<?> requiredType) {
		if (obj == null) {
			return "";
		}
		if (obj instanceof File) {
			return (FILE_PREFIX_STRING + ((File) obj).getPath());
		}
		if (obj instanceof String) {
			return (INMEM_PREFIX_STRING + obj);
		}
		String className = classToCLIString(obj.getClass(), requiredType);
		if (obj instanceof weka.core.OptionHandler) {
			String subOptions = Utils.joinOptions(((weka.core.OptionHandler) obj).getOptions());
			if (subOptions.length() > 0) {
				return new String(className + " " + subOptions).trim();
			}
		}
		return className;
	}

	public static Object cliStringToObject(String cliString,
			Class<?> requiredType, Option[] externalOptions) throws Exception {
		if (cliString.startsWith(FILE_PREFIX_STRING)) {
			return new File(cliString.substring(FILE_PREFIX_STRING.length()));
		}
		if (cliString.startsWith(INMEM_PREFIX_STRING)) {
			return cliString.substring(INMEM_PREFIX_STRING.length());
		}
		cliString = cliString.trim();
		int firstSpaceIndex = cliString.indexOf(' ', 0);
		String className;
		String WEKAClassOptions;
		if (firstSpaceIndex > 0) {
			className = cliString.substring(0, firstSpaceIndex);
			WEKAClassOptions = cliString.substring(firstSpaceIndex + 1, cliString.length());
			WEKAClassOptions = WEKAClassOptions.trim();
		} else {
			className = cliString;
			WEKAClassOptions = "";
		}
		Class<?> classObject;
		try {
			classObject = Class.forName(className);
		} catch (Throwable t1) {
			try {
				// try prepending default package
				classObject = Class.forName(requiredType.getPackage().getName()	+ "." + className);
			} catch (Throwable t2) {
				try {
					// try prepending task package
					classObject = Class.forName(Task.class.getPackage().getName()	+ "." + className);
				} catch (Throwable t3) {
					throw new Exception("Class not found: " + className);
				}
			}
		}
		Object classInstance;
		try {
			classInstance = classObject.newInstance();
		} catch (Exception ex) {
			throw new Exception("Problem creating instance of class: "
					+ className, ex);
		}
		if (requiredType.isInstance(classInstance)
				|| ((classInstance instanceof Task) && requiredType.isAssignableFrom(((Task) classInstance).getTaskResultType()))) {
			Vector<String> options = new Vector<String>();
			if (externalOptions != null) {
				for (Option option : externalOptions) {
					options.add(option.getValueAsCLIString());
				}
			}
			else {
				String[] optionsTmp = Utils.splitOptions(cliString);
				for (int i = 1; i < optionsTmp.length; i++)
					options.add(optionsTmp[i]);
			}
			if (classInstance instanceof weka.core.OptionHandler) {
				try {
					((weka.core.OptionHandler) classInstance).setOptions(options.toArray(new String[options.size()]));
				} catch (Exception ex) {
					Enumeration enm = ((weka.core.OptionHandler) classInstance).listOptions();
					StringBuffer optionsText = new StringBuffer();
					while (enm.hasMoreElements()) {
						weka.core.Option option = (weka.core.Option) enm.nextElement();
						optionsText.append(option.synopsis() + '\n');
						optionsText.append(option.description() + "\n");
					}
					throw new Exception("Problem with options to '"
							+ className
							+ "'."
							+ "\n\nValid options for "
							+ className
							+ ":\n"
							+ optionsText.toString(), ex);
				}
		  }
		} else {
			throw new Exception("Class named '" + className
					+ "' is not an instance of " + requiredType.getName() + ".");
		}
		return classInstance;
	}

	//@Override
	//public JComponent getEditComponent() {
	//	return new WEKAClassOptionEditComponent(this);
	//}
}
