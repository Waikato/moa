/*
 *    Option.java
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

import moa.MOAObject;

/**
 * Interface representing an option or parameter. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $ 
 */
public interface Option extends MOAObject {

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
    public JComponent getEditComponent();
}
