/*
 *    IntOption.java
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

import moa.gui.IntOptionEditComponent;

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

    @Override
    public JComponent getEditComponent() {
        return new IntOptionEditComponent(this);
    }
}
