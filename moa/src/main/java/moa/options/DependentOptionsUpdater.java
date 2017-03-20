/*
 *    DependentOptionsUpdater.java
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

import java.io.Serializable;
import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;

/**
 * This class handles the dependency between two options by updating the 
 * dependent option whenever the option it is depending on changes.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class DependentOptionsUpdater implements ChangeListener, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	protected String lastLearnerClassName;
	protected ClassOptionWithListenerOption learnerOption;
	protected EditableMultiChoiceOption variedParamNameOption;
	
	public DependentOptionsUpdater(
			ClassOptionWithListenerOption learnerOption,
			EditableMultiChoiceOption variedParamNameOption) 
	{
		this.learnerOption = learnerOption;
		this.variedParamNameOption = variedParamNameOption;
		
		this.learnerOption.setChangeListener(this);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		this.refreshVariedParamNameOption();
	}
	
	/**
	 * Refresh the provided choices of an EditableMultiChoiceOption every time
	 * a ClassOption (the learner) is changed. This method checks if the
	 * chosen learner actually changed, before updating the MultiChoiceOption.
	 * <br>
	 * Only Int and Float options of the selected learner are shown in the
	 * MultiChoiceOption, because only continuous parameters should be 
	 * variable.
	 * <br>
	 * If one of the learner's options is named "budget" or its name contains
	 * the word "budget", it is selected as the default option.
	 * 
	 * @param learnerOption
	 * @param variedParamNameOption
	 */
	public void refreshVariedParamNameOption()
	{
		OptionHandler learner = 
				(OptionHandler) learnerOption.getPreMaterializedObject();
		String currentLearner = learner.getClass().getSimpleName();
		
		// check if an update is actually needed
		if (lastLearnerClassName == null || 
			!lastLearnerClassName.equals(currentLearner)) 
		{
			lastLearnerClassName = currentLearner;
			
			Option[] options = learner.getOptions().getOptionArray();
			
			// filter for Int and Float Options
			options = Arrays.stream(options)
					.filter(x -> x instanceof IntOption || 
								 x instanceof FloatOption)
					.toArray(Option[]::new);
			
			String[] optionNames = new String[options.length];
			String[] optionDescriptions = new String[options.length];
			int defaultIndex = -1;
			
			// get option names and descriptions and look for default option
			for (int i = 0; i < options.length; i++) {
				optionNames[i] = options[i].getName();
				optionDescriptions[i] = options[i].getPurpose();
				
				if (optionNames[i].equals("budget") || 
					(optionNames[i].contains("budget") && defaultIndex < 0)) 
				{
					defaultIndex = i;
				}
			}
			
			// pass new options to the EditableMultiChoiceOption
			variedParamNameOption.setOptions(optionNames, optionDescriptions, 
					defaultIndex >= 0 ? defaultIndex : 0);
		}
	}
}
