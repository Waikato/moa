/*
 *    EditableMultiChoiceOptionEditComponent.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
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
package moa.gui;

import javax.swing.DefaultComboBoxModel;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.MultiChoiceOptionEditComponent;

import moa.options.EditableMultiChoiceOption;

/**
 * EditComponent for the {@link EditableMultiChoiceOption} which allows for 
 * refreshing the shown contents.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class EditableMultiChoiceOptionEditComponent extends MultiChoiceOptionEditComponent {

	private static final long serialVersionUID = 1L;
	
	public EditableMultiChoiceOptionEditComponent(Option option) {
		super(option);
		
		// register the EditComponent with the corresponding 
		// EditableMultiChoiceOption, so that updates can be received
		((EditableMultiChoiceOption) option).registerEditComponent(this);
	}
	
	/**
	 * Refresh the shown contents.
	 */
	public void refresh() {
		setModel(new DefaultComboBoxModel<String>(
				this.editedOption.getOptionLabels()));
        setSelectedIndex(this.editedOption.getChosenIndex());
	}
	
}
