/*
 *    MultiChoiceOptionEditComponent.java
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
package moa.gui;

import javax.swing.JComboBox;

import moa.options.MultiChoiceOption;
import moa.options.Option;

public class MultiChoiceOptionEditComponent extends JComboBox implements
		OptionEditComponent {

	private static final long serialVersionUID = 1L;

	protected MultiChoiceOption editedOption;

	public MultiChoiceOptionEditComponent(MultiChoiceOption option) {
		super(option.getOptionLabels());
		this.editedOption = option;
		setSelectedIndex(option.getChosenIndex());
	}

	public void applyState() {
		this.editedOption.setChosenIndex(getSelectedIndex());
	}

	public Option getEditedOption() {
		return this.editedOption;
	}

	public void setEditState(String cliString) {
		MultiChoiceOption tempOpt = (MultiChoiceOption) this.editedOption
				.copy();
		tempOpt.setValueViaCLIString(cliString);
		setSelectedIndex(tempOpt.getChosenIndex());
	}

}
