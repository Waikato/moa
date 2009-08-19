/*
 *    AbstractClassOption.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.options;

import java.io.File;

import javax.swing.JComponent;

import moa.core.ObjectRepository;
import moa.core.SerializeUtils;
import moa.tasks.Task;
import moa.tasks.TaskMonitor;

public abstract class AbstractClassOption extends AbstractOption {

	private static final long serialVersionUID = 1L;

	public static final String FILE_PREFIX_STRING = "file:";

	public static final String INMEM_PREFIX_STRING = "inmem:";

	protected Object currentValue;

	protected Class<?> requiredType;

	protected String defaultCLIString;

	protected String nullString;

	public AbstractClassOption(String name, char cliChar, String purpose,
			Class<?> requiredType, String defaultCLIString) {
		this(name, cliChar, purpose, requiredType, defaultCLIString, null);
	}

	public AbstractClassOption(String name, char cliChar, String purpose,
			Class<?> requiredType, String defaultCLIString, String nullString) {
		super(name, cliChar, purpose);
		this.requiredType = requiredType;
		this.defaultCLIString = defaultCLIString;
		this.nullString = nullString;
		resetToDefault();
	}

	public void setCurrentObject(Object obj) {
		if (((obj == null) && (this.nullString != null))
				|| this.requiredType.isInstance(obj)
				|| (obj instanceof String)
				|| (obj instanceof File)
				|| ((obj instanceof Task) && this.requiredType.isAssignableFrom(((Task) obj).getTaskResultType()))) {
			this.currentValue = obj;
		} else {
			throw new IllegalArgumentException("Object not of required type.");
		}
	}

	public Object getPreMaterializedObject() {
		return this.currentValue;
	}

	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	public String getNullString() {
		return this.nullString;
	}

	public Object materializeObject(TaskMonitor monitor,
			ObjectRepository repository) {
		if ((this.currentValue == null)
				|| this.requiredType.isInstance(this.currentValue)) {
			return this.currentValue;
		} else if (this.currentValue instanceof String) {
			if (repository != null) {
				Object inmemObj = repository
						.getObjectNamed((String) this.currentValue);
				if (inmemObj == null) {
					throw new RuntimeException("No object named "
							+ this.currentValue + " found in repository.");
				}
				return inmemObj;
			}
			throw new RuntimeException("No object repository available.");
		} else if (this.currentValue instanceof Task) {
			Task task = (Task) this.currentValue;
			Object result = task.doTask(monitor, repository);
			return result;
		} else if (this.currentValue instanceof File) {
			File inputFile = (File) this.currentValue;
			Object result = null;
			try {
				result = SerializeUtils.readFromFile(inputFile);
			} catch (Exception ex) {
				throw new RuntimeException("Problem loading "
						+ this.requiredType.getName() + " object from file '"
						+ inputFile.getName() + "':\n" + ex.getMessage(), ex);
			}
			return result;
		} else {
			throw new RuntimeException(
					"Could not materialize object of required type "
							+ this.requiredType.getName() + ", found "
							+ this.currentValue.getClass().getName()
							+ " instead.");
		}
	}

	public String getDefaultCLIString() {
		return this.defaultCLIString;
	}

	public static String classToCLIString(Class<?> aClass, Class<?> requiredType) {
		String className = aClass.getName();
		String packageName = requiredType.getPackage().getName();
		if (className.startsWith(packageName)) {
			// cut off package name
			className = className.substring(packageName.length() + 1, className
					.length());
		} else if (Task.class.isAssignableFrom(aClass)) {
			packageName = Task.class.getPackage().getName();
			if (className.startsWith(packageName)) {
				// cut off task package name
				className = className.substring(packageName.length() + 1,
						className.length());
			}
		}
		return className;
	}

	public abstract String getValueAsCLIString();

	public abstract void setValueViaCLIString(String s);

	public abstract JComponent getEditComponent();

	public static String stripPackagePrefix(String className, Class<?> expectedType) {
		if (className.startsWith(expectedType.getPackage().getName())) {
			return className.substring(expectedType.getPackage().getName().length() + 1);
		}
		return className;
	}
}
