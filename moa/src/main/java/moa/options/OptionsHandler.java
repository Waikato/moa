package moa.options;


import java.util.HashMap;
import com.github.javacliparser.JavaCLIParser;
import com.github.javacliparser.Option;
import moa.core.ObjectRepository;
import moa.tasks.NullMonitor;
import moa.tasks.TaskMonitor;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author abifet
 */
public class OptionsHandler extends JavaCLIParser {

    //public Object handler;
    
    public OptionsHandler(Object c, String cliString) {
        super(c,cliString);
        //this.handler = c;
        //this.prepareForUse();
        /*int firstSpaceIndex = cliString.indexOf(' ', 0);
        String classOptions;
        String className;
        if (firstSpaceIndex > 0) {
            className = cliString.substring(0, firstSpaceIndex);
            classOptions = cliString.substring(firstSpaceIndex + 1, cliString.length());
            classOptions = classOptions.trim();
        } else {
            className = cliString;
            classOptions = "";
        }*/
        //options.setViaCLIString(cliString);
    }
    
    
    //private static final long serialVersionUID = 1L;

    /** Options to handle */
    //protected Options options;

    /** Dictionary with option texts and objects */
    //protected Map<String, Object> classOptionNamesToPreparedObjects;


    /*public String getPurposeString() {
        return "Anonymous object: purpose undocumented.";
    }

    public Options getOptions() {
        if (this.options == null) {
            this.options = new Options();
            Option[] myOptions = discoverOptionsViaReflection();
            for (Option option : myOptions) {
                this.options.addOption(option);
            }
        }
        return this.options;
    }*/

    public void prepareForUse() {
        prepareForUse(new NullMonitor(), null);
    }

    public void prepareForUse(TaskMonitor monitor, ObjectRepository repository) {
        prepareClassOptions(monitor, repository);
        //prepareForUseImpl(monitor, repository);
    }

    /**
     * This method describes the implementation of how to prepare this object for use.
     * All classes that extends this class have to implement <code>prepareForUseImpl</code>
     * and not <code>prepareForUse</code> since
     * <code>prepareForUse</code> calls <code>prepareForUseImpl</code>.
     *
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     */
    //protected abstract void prepareForUseImpl(TaskMonitor monitor,
      //      ObjectRepository repository);

    /* public String getCLICreationString(Class<?> expectedType) {
        return ClassOption.stripPackagePrefix(this.getClass().getName(),
                expectedType)
                + " " + getOptions().getAsCLIString();
    }*/


    /**
     * Gets the options of this class via reflection.
     *
     * @return an array of options
     */
    /*public Option[] discoverOptionsViaReflection() {
        //Class<? extends AbstractOptionHandler> c = this.getClass();
        Class c = this.handler.getClass();
        Field[] fields = c.getFields();
        List<Option> optList = new LinkedList<Option>();
        for (Field field : fields) {
            String fName = field.getName();
            Class<?> fType = field.getType();
            if (fType.getName().endsWith("Option")) {
                if (Option.class.isAssignableFrom(fType)) {
                    Option oVal = null;
                    try {
                        field.setAccessible(true);
                        oVal = (Option) field.get(this.handler);
                    } catch (IllegalAccessException ignored) {
                        // cannot access this field
                    }
                    if (oVal != null) {
                        optList.add(oVal);
                    }
                }
            }
        }
        return optList.toArray(new Option[optList.size()]);
    }*/
    
    /**
     * Prepares the options of this class.
     * 
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     */
    public void prepareClassOptions(TaskMonitor monitor,
            ObjectRepository repository) {
        this.classOptionNamesToPreparedObjects = null;
        Option[] optionArray = getOptions().getOptionArray();
        for (Option option : optionArray) {
            if (option instanceof ClassOption) {
                ClassOption classOption = (ClassOption) option;
                monitor.setCurrentActivity("Materializing option "
                        + classOption.getName() + "...", -1.0);
                Object optionObj = classOption.materializeObject(monitor,
                        repository);
                if (monitor.taskShouldAbort()) {
                    return;
                }
                if (optionObj instanceof OptionHandler) {
                    monitor.setCurrentActivity("Preparing option "
                            + classOption.getName() + "...", -1.0);
                    ((OptionHandler) optionObj).prepareForUse(monitor,
                            repository);
                    if (monitor.taskShouldAbort()) {
                        return;
                    }
                }
                if (this.classOptionNamesToPreparedObjects == null) {
                    this.classOptionNamesToPreparedObjects = new HashMap<String, Object>();
                }
                this.classOptionNamesToPreparedObjects.put(option.getName(),
                        optionObj);
            }
        }
    }

    /**
     *  Gets a prepared option of this class.
     *
     * @param opt the class option to get
     * @return an option stored in the dictionary
     */
    public Object getPreparedClassOption(ClassOption opt) {
        if (this.classOptionNamesToPreparedObjects == null) {
                    this.prepareForUse();
                }
        return this.classOptionNamesToPreparedObjects.get(opt.getName());
    }

    //@Override
    //public void getDescription(StringBuilder sb, int i) {
    //    throw new UnsupportedOperationException("Not supported yet.");
    //}
    
}
