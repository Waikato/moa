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
 * Flag option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class FlagOption extends AbstractOption {

    private static final long serialVersionUID = 1L;

    protected boolean isSet = false;

    public FlagOption(String name, char cliChar, String purpose) {
        super(name, cliChar, purpose);
    }

    public void setValue(boolean v) {
        this.isSet = v;
    }

    public void set() {
        setValue(true);
    }

    public void unset() {
        setValue(false);
    }

    public boolean isSet() {
        return this.isSet;
    }

    @Override
    public String getDefaultCLIString() {
        return null;
    }

    @Override
    public String getValueAsCLIString() {
        return this.isSet ? "" : null;
    }

    @Override
    public void setValueViaCLIString(String s) {
        this.isSet = (s != null);
    }

    @Override
    public String getStateString() {
        return this.isSet ? "true" : "false";
    }

    //@Override
    //public JComponent getEditComponent() {
    //    return new FlagOptionEditComponent(this);
    //}
}
