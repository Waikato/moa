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
 * Int option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class IntOption extends AbstractOption {

    private static final long serialVersionUID = 1L;

    protected int currentVal;

    protected int defaultVal;

    protected int minVal;

    protected int maxVal;

    public IntOption(String name, char cliChar, String purpose, int defaultVal) {
        this(name, cliChar, purpose, defaultVal, Integer.MIN_VALUE,
                Integer.MAX_VALUE);
    }

    public IntOption(String name, char cliChar, String purpose, int defaultVal,
            int minVal, int maxVal) {
        super(name, cliChar, purpose);
        this.defaultVal = defaultVal;
        this.minVal = minVal;
        this.maxVal = maxVal;
        resetToDefault();
    }

    public void setValue(int v) {
        if (v < this.minVal) {
            throw new IllegalArgumentException("Option " + getName()
                    + " cannot be less than " + this.minVal
                    + ", out of range: " + v);
        }
        if (v > this.maxVal) {
            throw new IllegalArgumentException("Option " + getName()
                    + " cannot be greater than " + this.maxVal
                    + ", out of range: " + v);
        }
        this.currentVal = v;
    }

    public int getValue() {
        return this.currentVal;
    }

    public int getMinValue() {
        return this.minVal;
    }

    public int getMaxValue() {
        return this.maxVal;
    }

    @Override
    public String getDefaultCLIString() {
        return intToCLIString(this.defaultVal);
    }

    @Override
    public String getValueAsCLIString() {
        return intToCLIString(this.currentVal);
    }

    @Override
    public void setValueViaCLIString(String s) {
        setValue(cliStringToInt(s));
    }

    public static int cliStringToInt(String s) {
        return Integer.parseInt(s.trim());
    }

    public static String intToCLIString(int i) {
        return Integer.toString(i);
    }

    //@Override
    //public JComponent getEditComponent() {
    //    return new IntOptionEditComponent(this);
    //}
}
