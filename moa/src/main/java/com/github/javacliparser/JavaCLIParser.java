/*
 * Copyright 2007 University of Waikato.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */

package com.github.javacliparser;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Java Command Line Interface Parser.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class JavaCLIParser implements Serializable {

    public Object handler;
    
    public JavaCLIParser(Object c, String cliString) {
        this.handler = c;
    }
    
    private static final long serialVersionUID = 1L;

    /** Options to handle */
    protected Options options;

    /** Dictionary with option texts and objects */
    protected Map<String, Object> classOptionNamesToPreparedObjects;


    public String getPurposeString() {
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
    }

    /**
     * Gets the options of this class via reflection.
     *
     * @return an array of options
     */
    public Option[] discoverOptionsViaReflection() {
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
    }

    /**
     * Prepares the options of this class.
     * 
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     */
    public void prepareClassOptions() { //TaskMonitor monitor,
            //ObjectRepository repository) {
        this.classOptionNamesToPreparedObjects = null;
        Option[] optionArray = getOptions().getOptionArray();
        for (Option option : optionArray) {
            if (option instanceof ClassOption) {
                ClassOption classOption = (ClassOption) option;
               // monitor.setCurrentActivity("Materializing option "
               //         + classOption.getName() + "...", -1.0);
                Object optionObj = classOption.materializeObject(); //monitor,
                        //repository);
                //if (monitor.taskShouldAbort()) {
                //    return;
                //}
                if (optionObj instanceof Configurable) {
                 //   monitor.setCurrentActivity("Preparing option "
                 //           + classOption.getName() + "...", -1.0);
                    JavaCLIParser config = new JavaCLIParser(optionObj, "");
                    //((Configurable) optionObj).prepareForUse(); //monitor,
                            //repository);
                 //   if (monitor.taskShouldAbort()) {
                 //       return;
                 //   }
                }
                if (this.classOptionNamesToPreparedObjects == null) {
                    this.classOptionNamesToPreparedObjects = new HashMap<String, Object>();
                }
                this.classOptionNamesToPreparedObjects.put(option.getName(),
                        optionObj);
            }
        }
    }

}
