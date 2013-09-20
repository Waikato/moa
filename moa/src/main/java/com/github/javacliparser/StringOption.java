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
 * String option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class StringOption extends AbstractOption {

    private static final long serialVersionUID = 1L;

    protected String currentVal;

    protected String defaultVal;

    public StringOption(String name, char cliChar, String purpose,
            String defaultVal) {
        super(name, cliChar, purpose);
        this.defaultVal = defaultVal;
        resetToDefault();
    }

    public void setValue(String v) {
        this.currentVal = v;
    }

    public String getValue() {
        return this.currentVal;
    }

    @Override
    public String getDefaultCLIString() {
        return this.defaultVal;
    }

    @Override
    public String getValueAsCLIString() {
        return this.currentVal;
    }

    @Override
    public void setValueViaCLIString(String s) {
        setValue(s);
    }
}
