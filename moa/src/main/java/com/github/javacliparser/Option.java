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

/**
 * Interface representing an option or parameter. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $ 
 */
public interface Option extends Serializable {

    /**
     * Gets the name of this option
     *
     * @return the name of this option
     */
    public String getName();

    /**
     * Gets the Command Line Interface text of this option
     *
     * @return the Command Line Interface text
     */
    public char getCLIChar();

    /**
     * Gets the purpose of this option
     *
     * @return the purpose of this option
     */
    public String getPurpose();

    /**
     * Gets the Command Line Interface text
     *
     * @return the Command Line Interface text
     */
    public String getDefaultCLIString();

    /**
     * Sets value of this option via the Command Line Interface text
     *
     * @param s the Command Line Interface text
     */
    public void setValueViaCLIString(String s);

    /**
     * Gets the value of a Command Line Interface text as a string
     *
     * @return the string with the value of the Command Line Interface text
     */
    public String getValueAsCLIString();

    /**
     * Resets this option to the default value
     *
     */
    public void resetToDefault();

    /**
     * Gets the state of this option in human readable form
     *
     * @return the string with state of this option in human readable form
     */
    public String getStateString();

    /**
     * Gets a copy of this option
     *
     * @return the copy of this option
     */
    public Option copy();

    /**
     * Gets the GUI component to edit
     *
     * @return the component to edit
     */
    //public JComponent getEditComponent();
}
