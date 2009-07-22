/*
 *    AbstractOption.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.options;

import javax.swing.JComponent;

import moa.AbstractMOAObject;
import moa.gui.StringOptionEditComponent;

public abstract class AbstractOption extends AbstractMOAObject implements
		Option {

	public static final char[] illegalNameCharacters = new char[] { ' ', '-',
			'(', ')' };

	protected String name;

	protected char cliChar;

	protected String purpose;

	public static boolean nameIsLegal(String optionName) {
		for (char illegalChar : illegalNameCharacters) {
			if (optionName.indexOf(illegalChar) >= 0) {
				return false;
			}
		}
		return true;
	}

	public AbstractOption(String name, char cliChar, String purpose) {
		if (!nameIsLegal(name)) {
			throw new IllegalArgumentException("Illegal option name: " + name);
		}
		this.name = name;
		this.cliChar = cliChar;
		this.purpose = purpose;
	}

	public String getName() {
		return this.name;
	}

	public char getCLIChar() {
		return this.cliChar;
	}

	public String getPurpose() {
		return this.purpose;
	}

	public void resetToDefault() {
		setValueViaCLIString(getDefaultCLIString());
	}

	public String getStateString() {
		return getValueAsCLIString();
	}

	@Override
	public Option copy() {
		return (Option) super.copy();
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	public JComponent getEditComponent() {
		return new StringOptionEditComponent(this);
	}

}
