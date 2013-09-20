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
 * Float option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class FloatOption extends AbstractOption {

    private static final long serialVersionUID = 1L;

    protected double currentVal;

    protected double defaultVal;

    protected double minVal;

    protected double maxVal;

    public FloatOption(String name, char cliChar, String purpose,
            double defaultVal) {
        this(name, cliChar, purpose, defaultVal, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
    }

    public FloatOption(String name, char cliChar, String purpose,
            double defaultVal, double minVal, double maxVal) {
        super(name, cliChar, purpose);
        this.defaultVal = defaultVal;
        this.minVal = minVal;
        this.maxVal = maxVal;
        resetToDefault();
    }

    public void setValue(double v) {
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

    public double getValue() {
        return this.currentVal;
    }

    public double getMinValue() {
        return this.minVal;
    }

    public double getMaxValue() {
        return this.maxVal;
    }

    @Override
    public String getDefaultCLIString() {
        return doubleToCLIString(this.defaultVal);
    }

    @Override
    public String getValueAsCLIString() {
        return doubleToCLIString(this.currentVal);
    }

    @Override
    public void setValueViaCLIString(String s) {
        setValue(cliStringToDouble(s));
    }

    public static double cliStringToDouble(String s) {
        return Double.parseDouble(s.trim());
    }

    public static String doubleToCLIString(double d) {
        return Double.toString(d);
    }

    //@Override
    //public JComponent getEditComponent() {
    //    return new FloatOptionEditComponent(this);
    //}
}
