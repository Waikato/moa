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

/**
 * Abstract option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractOption implements Option {

    /** Array of characters not valid to use in option names. */
    public static final char[] illegalNameCharacters = new char[]{' ', '-',
        '(', ')'};

    /** Name of this option. */
    protected String name;

    /** Command line interface text of this option. */
    protected char cliChar;

    /** Text of the purpose of this option. */
    protected String purpose;

    /**
     * Gets whether the name is valid or not.
     *
     * @param optionName the name of the option
     * @return true if the name that not contain any illegal character
     */
    public static boolean nameIsLegal(String optionName) {
        for (char illegalChar : illegalNameCharacters) {
            if (optionName.indexOf(illegalChar) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new instance of an abstract option given its class name,
     * command line interface text and its purpose.
     *
     * @param name the name of this option
     * @param cliChar the command line interface text
     * @param purpose the text describing the purpose of this option
     */
    public AbstractOption(String name, char cliChar, String purpose) {
        if (!nameIsLegal(name)) {
            throw new IllegalArgumentException("Illegal option name: " + name);
        }
        this.name = name;
        this.cliChar = cliChar;
        this.purpose = purpose;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public char getCLIChar() {
        return this.cliChar;
    }

    @Override
    public String getPurpose() {
        return this.purpose;
    }

    @Override
    public void resetToDefault() {
        setValueViaCLIString(getDefaultCLIString());
    }

    @Override
    public String getStateString() {
        return getValueAsCLIString();
    }

   
    @Override
    public Option copy() {
        try {
            return (Option) SerializeUtils.copyObject(this);
        } catch (Exception e) {
            throw new RuntimeException("Object copy failed.", e);
        }
    }
    
    //@Override
    //public Option copy() {
    //    return (Option) super.copy();
    //}

    
    //@Override
    //public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    //}

    //@Override
    //public JComponent getEditComponent() {
    //    return new StringOptionEditComponent(this);
    //}
}
