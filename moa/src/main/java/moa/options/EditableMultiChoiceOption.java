/*
 *    ALPrequentialEvaluationTask.java
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
 * Multi choice option that can have changing options.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class EditableMultiChoiceOption extends MultiChoiceOption {

	private static final long serialVersionUID = 1L;
	
	protected EditableMultiChoiceOptionEditComponent editComponent;

	public EditableMultiChoiceOption(String name, char cliChar, 
			String purpose, String[] optionLabels,
			String[] optionDescriptions, int defaultOptionIndex) 
	{
		super(name, cliChar, purpose, optionLabels, optionDescriptions, 
				defaultOptionIndex);
	}
	
	public void registerEditComponent(
			EditableMultiChoiceOptionEditComponent editComponent) 
	{
		this.editComponent = editComponent;
	}
	
	/**
	 * Set new options for this MultiChoiceOption.
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
        this.optionLabels = labels.clone();
        this.optionDescriptions = descriptions.clone();
        this.defaultOptionIndex = defaultIndex;
        resetToDefault();
        
        if (this.editComponent != null) {
        	this.editComponent.refresh();
        }
	}
}
