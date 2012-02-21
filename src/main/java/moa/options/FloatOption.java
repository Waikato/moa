/*
 *    FloatOption.java
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

import javax.swing.JComponent;

import moa.gui.FloatOptionEditComponent;

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

    @Override
    public JComponent getEditComponent() {
        return new FloatOptionEditComponent(this);
    }
}
