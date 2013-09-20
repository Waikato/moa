/**
 * [ClassOptionWithNames.java]
 * 
 * A variation of [ClassOption], which you can choose specific classes as its entries.
 * In the constructor, put a list of class names you want to add (String[] classNames) as an argument.
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Richard Kirkby
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.options;

import com.github.javacliparser.Options;
import com.github.javacliparser.Option;
import java.io.File;

import javax.swing.JComponent;

import com.github.javacliparser.gui.ClassOptionWithNamesEditComponent;
import moa.tasks.Task;

public class ClassOptionWithNames extends AbstractClassOption {

    private static final long serialVersionUID = 1L;
    private String[] names;

    public ClassOptionWithNames(String name, char cliChar, String purpose,
            Class<?> requiredType, String defaultCLIString, String[] classNames) {
        super(name, cliChar, purpose, requiredType, defaultCLIString);
        this.names = classNames;
    }

    public ClassOptionWithNames(String name, char cliChar, String purpose,
            Class<?> requiredType, String defaultCLIString, String nullString, String[] classNames) {
        super(name, cliChar, purpose, requiredType, defaultCLIString, nullString);
        this.names = classNames;
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
            } catch (Throwable t2) {
                try {
                    // try prepending task package
                    classObject = Class.forName(Task.class.getPackage().getName()
                            + "." + className);
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
                throw new Exception("Problem with options to '"
                        + className
                        + "'."
                        + "\n\nValid options for "
                        + className
                        + ":\n"
                        + ((OptionHandler) classInstance).getOptions().getHelpString(), ex);
            } finally {
                options.removeAllOptions(); // clean up listener refs
            }
        } else {
            throw new Exception("Class named '" + className
                    + "' is not an instance of " + requiredType.getName() + ".");
        }
        return classInstance;
    }

//    @Override
//    public JComponent getEditComponent() {
//        return new ClassOptionWithNamesEditComponent(this);
//    }
    
    public String[] getClassNames() {
    	return this.names;
    }
}
