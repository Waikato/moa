/*
 *    OptionEditComponent.java
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
package com.github.javacliparser.gui;

import com.github.javacliparser.Option;

/**
 * Interface representing a component to edit an option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface OptionEditComponent {

	/**
	 * Gets the option of this component
	 *
	 * @return the option of this component
	 */
	Option getEditedOption();

	/**
	 * Sets the state of the component
	 *
	 * @param cliString the state of the component
	 */
	void setEditState(String cliString);

	/**
	 * This method applies the state
	 *
	 */
	void applyState();
}
