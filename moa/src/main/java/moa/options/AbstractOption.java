/*
 *    AbstractOption.java
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

import moa.AbstractMOAObject;
import moa.gui.StringOptionEditComponent;

/**
 * Abstract option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractOption extends AbstractMOAObject implements
        Option {

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
        return (Option) super.copy();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    public JComponent getEditComponent() {
        return new StringOptionEditComponent(this);
    }
}
