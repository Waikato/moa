/*
 *    FlagOption.java
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

import moa.gui.FlagOptionEditComponent;

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

    @Override
    public JComponent getEditComponent() {
        return new FlagOptionEditComponent(this);
    }
}
