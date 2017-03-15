/*
 *    EditableMultiChoiceOption.java
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
package moa.options;

import com.github.javacliparser.MultiChoiceOption;

import moa.gui.EditableMultiChoiceOptionEditComponent;

/**
 * MultiChoiceOption that can have changing options.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class EditableMultiChoiceOption extends MultiChoiceOption {

	private static final long serialVersionUID = 1L;
	private static final String NO_CHOICES = "--";
	private static final String NO_CHOICES_DESCRIPTION = 
			"No choices available.";
	
	/**
	 * The corresponding UI component
	 */
	protected EditableMultiChoiceOptionEditComponent editComponent;

	public EditableMultiChoiceOption(String name, char cliChar, 
			String purpose, String[] optionLabels,
			String[] optionDescriptions, int defaultOptionIndex) 
	{
		super(name, cliChar, purpose, optionLabels, optionDescriptions, 
				defaultOptionIndex);
		
		// set initial options and refresh edit component
		this.setOptions(optionLabels, optionDescriptions, defaultOptionIndex);
	}
	
	/**
	 * Register the corresponding UI component, so that it can be refreshed
	 * when options have changed.
	 * 
	 * @param editComponent
	 */
	public void registerEditComponent(
			EditableMultiChoiceOptionEditComponent editComponent) 
	{
		this.editComponent = editComponent;
	}
	
	/**
	 * Set new options for this MultiChoiceOption and refresh the edit 
	 * component.
	 * 
	 * @param labels
	 * @param descriptions
	 * @param defaultIndex
	 */
	public void setOptions(
			String[] labels, String[] descriptions, int defaultIndex) 
	{
		if (labels.length != descriptions.length) {
            throw new IllegalArgumentException("Labels/descriptions mismatch.");
        }
		
		if (labels.length > 0) {
			this.optionLabels = labels.clone();
			this.optionDescriptions = descriptions.clone();
			this.defaultOptionIndex = defaultIndex;
		}
		else {
			// use placeholders for empty list of choices
			this.optionLabels = new String[]{NO_CHOICES};
			this.optionDescriptions = new String[]{NO_CHOICES_DESCRIPTION};
			this.defaultOptionIndex = 0;
		}
		
		// reset to default value
        resetToDefault();
        
        // refresh the edit component
        if (this.editComponent != null) {
        	this.editComponent.refresh();
        }
	}
}
