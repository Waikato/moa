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
package moa.options;

import java.io.File;
import java.util.Arrays;

import com.github.javacliparser.Option;
import com.github.javacliparser.Options;
import moa.tasks.Task;

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
        if (obj instanceof OptionHandler) {
            String subOptions = ((OptionHandler) obj).getOptions().getAsCLIString();
            if (subOptions.length() > 0) {
                return (className + " " + subOptions);
            }
        }
        return className;
    }

    public static Class<?> classForName(
          String className,
          String[] additionalPackagesToSearch
    ) throws Exception {
        try {
            return Class.forName(className);
        } catch (Throwable ignored1) {
            for (String packageName: additionalPackagesToSearch) {
                try {
                    return Class.forName(packageName + "." + className);
                } catch (Throwable ignored2) {}
            }
        }

        throw new Exception("Class not found: " + className);
    }

    public static Object cliStringToObject(
          String cliString,
          Class<?> requiredType,
          Option[] externalOptions,
          String... additionalPackagesToSearch
    ) throws Exception {
        if (cliString.startsWith(FILE_PREFIX_STRING)) {
            return new File(cliString.substring(FILE_PREFIX_STRING.length()));
        }
        if (cliString.startsWith(INMEM_PREFIX_STRING)) {
            return cliString.substring(INMEM_PREFIX_STRING.length());
        }

        cliString = cliString.trim();
        String className;
        String classOptions;

        int firstSpaceIndex = cliString.indexOf(' ', 0);
        if (firstSpaceIndex > 0) {
            className = cliString.substring(0, firstSpaceIndex);
            classOptions = cliString.substring(firstSpaceIndex + 1);
            classOptions = classOptions.trim();
        } else {
            className = cliString;
            classOptions = "";
        }

        // Add the required type's package and the tasks package into the search space
        additionalPackagesToSearch = Arrays.copyOf(additionalPackagesToSearch, additionalPackagesToSearch.length + 2);
        additionalPackagesToSearch[additionalPackagesToSearch.length - 2] = requiredType.getPackage().getName();
        additionalPackagesToSearch[additionalPackagesToSearch.length - 1] = Task.class.getPackage().getName();

        Class<?> classObject = classForName(className, additionalPackagesToSearch);

        Object classInstance;
        try {
            classInstance = classObject.newInstance();
        } catch (Exception ex) {
            throw new Exception("Problem creating instance of class: " + className, ex);
        }

        // classInstance must either be a value of the required type, or a task
        // which results in a value assignable to the required type
        if (!requiredType.isInstance(classInstance)) {
            if (!(classInstance instanceof Task) || !requiredType.isAssignableFrom(((Task) classInstance).getTaskResultType())) {
                throw new Exception(
                      "Class named '" + className + "' is not an instance of " + requiredType.getName() + "."
                );
            }
        }

        Options options = new Options();

        if (externalOptions != null) {
            for (Option option : externalOptions) {
                options.addOption(option);
            }
        }

        if (classInstance instanceof OptionHandler) {
            Option[] objectOptions = ((OptionHandler) classInstance).getOptions().getOptionArray();

            for (Option option : objectOptions) {
                options.addOption(option);
            }
        }

        try {
            options.setViaCLIString(classOptions);
        } catch (Exception ex) {
            throw new Exception(
                  "Problem with options to '"
                  + className
                  + "'."
                  + "\n\nValid options for "
                  + className
                  + ":\n"
                  + ((OptionHandler) classInstance).getOptions().getHelpString()
                  , ex);
        } finally {
            options.removeAllOptions(); // clean up listener refs
        }

        return classInstance;
    }

    //@Override
    //public JComponent getEditComponent() {
    //    return new ClassOptionEditComponent(this);
    //}
}
