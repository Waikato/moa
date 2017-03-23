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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	protected String lastLearnerCLIString;
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
		String currentLearnerCLIString = 
				this.learnerOption.getValueAsCLIString();
		
		// check if an update is actually needed
		if (this.lastLearnerCLIString == null || 
			!this.lastLearnerCLIString.equals(currentLearnerCLIString)) 
		{
			this.lastLearnerCLIString = currentLearnerCLIString;
			
			// create result lists
			List<String> optionNames = new ArrayList<String>();
			List<String> optionDescriptions = new ArrayList<String>();
			
			// recursively add options
			this.addRecursiveNumberOptions(
					this.learnerOption, optionNames, optionDescriptions, "");
			
			// get option names and descriptions and look for default option
			int defaultIndex = -1;
			for (int i = 0; i < optionNames.size(); i++) {
				if (optionNames.get(i).endsWith("/budget") || 
					(optionNames.get(i).contains("budget") && defaultIndex < 0)) 
				{
					defaultIndex = i;
				}
			}
			
			// pass new options to the EditableMultiChoiceOption
			this.variedParamNameOption.setOptions(
					optionNames.toArray(new String[0]), 
					optionDescriptions.toArray(new String[0]), 
					defaultIndex >= 0 ? defaultIndex : 0);
		}
	}
	
	private void addRecursiveNumberOptions(ClassOption option, 
			List<String> optionNames, List<String> optionDescriptions,
			String namePrefix) 
	{
		OptionHandler optionHandler = 
				(OptionHandler) option.getPreMaterializedObject();
		Option[] options = optionHandler.getOptions().getOptionArray();
		
		// filter for Int and Float Options
		Option[] numberOptions = Arrays.stream(options)
				.filter(x -> x instanceof IntOption || 
							 x instanceof FloatOption)
				.toArray(Option[]::new);
		
		// get names and descriptions
		String newNamePrefix = namePrefix + option.getName() + "/";
		String[] names = Arrays.stream(numberOptions)
				.map(x -> newNamePrefix + x.getName())
				.toArray(String[]::new);
		String[] descriptions = Arrays.stream(numberOptions)
				.map(x -> x.getPurpose())
				.toArray(String[]::new);
		
		// add to overall lists
		optionNames.addAll(Arrays.asList(names));
		optionDescriptions.addAll(Arrays.asList(descriptions));
		
		// filter for ClassOptions
		ClassOption[] classOptions = Arrays.stream(options)
				.filter(x -> x instanceof ClassOption)
				.toArray(ClassOption[]::new);
		
		// add number options of this class option to the overall list
		for (ClassOption classOption : classOptions) {
			this.addRecursiveNumberOptions(classOption, 
					optionNames, optionDescriptions, newNamePrefix);
		}
	}
}
