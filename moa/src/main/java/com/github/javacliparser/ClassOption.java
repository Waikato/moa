/*
 *    ClassOption.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package com.github.javacliparser;

import java.io.File;
//import moa.options.OptionHandler;
//import moa.tasks.Task;

/**
 * Class option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ClassOption extends AbstractClassOption {

    private static final long serialVersionUID = 1L;

    public ClassOption(String name, char cliChar, String purpose,
            Class<?> requiredType, String defaultCLIString) {
        super(name, cliChar, purpose, requiredType, defaultCLIString);
    }

    public ClassOption(String name, char cliChar, String purpose,
            Class<?> requiredType, String defaultCLIString, String nullString) {
        super(name, cliChar, purpose, requiredType, defaultCLIString, nullString);
    }

    @Override
    public String getValueAsCLIString() {
        if ((this.currentValue == null) && (this.nullString != null)) {
            return this.nullString;
        }
        return objectToCLIString(this.currentValue, this.requiredType);
    }

    @Override
    public void setValueViaCLIString(String s) {
        if ((this.nullString != null)
                && ((s == null) || (s.length() == 0) || s.equals(this.nullString))) {
            this.currentValue = null;
        } else {
            try {
                this.currentValue = cliStringToObject(s, this.requiredType,
                        null);
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
        if (obj instanceof Configurable) {
            //String subOptions = ((Configurable) obj).getOptions().getAsCLIString();
            //Add cli parser
            JavaCLIParser config = new JavaCLIParser(obj, "");
            String subOptions = config.getOptions().getAsCLIString();
            if (subOptions.length() > 0) {
                return (className + " " + subOptions);
            }
        }
        return className;
    }

    public static Object createObject(String cliString,
            Class<?> requiredType) throws Exception {
        return cliStringToObject(cliString, requiredType, null);
    }
    
        
   public static Object createObject(String[] args,
            Class<?> requiredType) throws Exception {
            // build a single string by concatenating cli options
            StringBuilder cliString = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                cliString.append(" ").append(args[i]);
            }
            return cliStringToObject(cliString.toString(), requiredType, null);
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
        String classOptions;
        if (firstSpaceIndex > 0) {
            className = cliString.substring(0, firstSpaceIndex);
            classOptions = cliString.substring(firstSpaceIndex + 1, cliString.length());
            classOptions = classOptions.trim();
        } else {
            className = cliString;
            classOptions = "";
        }
        Class<?> classObject;
        try {
            classObject = Class.forName(className);
        } catch (Throwable t1) {
            try {
                // try prepending default package
                classObject = Class.forName(requiredType.getPackage().getName()
                        + "." + className);
            /*} catch (Throwable t2) {
                try {
                    // try prepending task package
                    classObject = Class.forName(Task.class.getPackage().getName()
                            + "." + className);
                */} catch (Throwable t3) {
                    throw new Exception("Class not found: " + className);
                //}
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
 //               || ((classInstance instanceof Task) && requiredType.isAssignableFrom(((Task) classInstance).getTaskResultType()))
                ) {
            Options options = new Options();
            JavaCLIParser config = null;
            if (externalOptions != null) {
                for (Option option : externalOptions) {
                    options.addOption(option);
                }
            }
            if (classInstance instanceof Configurable) {
                 config = new JavaCLIParser(classInstance, "");
                 Option[] objectOptions = config.getOptions().getOptionArray();
                //Option[] objectOptions = ((Configurable) classInstance).getOptions().getOptionArray();
                for (Option option : objectOptions) {
                    options.addOption(option);
                }
            }
            try {
                options.setViaCLIString(classOptions);
            } catch (Exception ex) {
                throw new Exception("Problem with options to '"
                        + className
                        + "'."
                        + "\n\nValid options for "
                        + className
                        + ":\n"
                        + config == null ? "": config.getOptions().getHelpString(), ex);
            } finally {
                options.removeAllOptions(); // clean up listener refs
            }
        } else {
            throw new Exception("Class named '" + className
                    + "' is not an instance of " + requiredType.getName() + ".");
        }
        return classInstance;
    }

    //@Override
    //public JComponent getEditComponent() {
    //    return new ClassOptionEditComponent(this);
    //}
}
