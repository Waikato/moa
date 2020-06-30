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
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;

import moa.tasks.meta.ALPrequentialEvaluationTask;

/**
 * This class handles the dependency between two options by updating the 
 * dependent option whenever the option it is depending on changes.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class DependentOptionsUpdater implements ChangeListener, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	protected String lastEvalTaskCLIString;
	protected ClassOptionWithListenerOption evalTaskOption;
	protected EditableMultiChoiceOption variedParamNameOption;
	
	public DependentOptionsUpdater(
			ClassOptionWithListenerOption learnerOption,
			EditableMultiChoiceOption variedParamNameOption) 
	{
		this.evalTaskOption = learnerOption;
		this.variedParamNameOption = variedParamNameOption;
		
		this.evalTaskOption.setChangeListener(this);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		this.refreshVariedParamNameOption();
	}
	
	/**
	 * Refresh the provided choices of an EditableMultiChoiceOption every time
	 * a ClassOption (the prequential evaluation task) is changed. This method 
	 * checks if the chosen task actually changed, before updating the 
	 * MultiChoiceOption.
	 * <br>
	 * The method looks for available options in the learner option of the
	 * prequential evaluation task and recursively in all of its ClassOptions.
	 * <br>
	 * Only Int and Float options are shown in the MultiChoiceOption, because 
	 * only numeric parameters should be variable.
	 * <br>
	 * If one of the options is named "budget" or its name contains the word 
	 * "budget", it is selected as the default option.
	 */
	public void refreshVariedParamNameOption()
	{
		String currentEvalTaskCLIString = 
				this.evalTaskOption.getValueAsCLIString();
		
		// check if an update is actually needed
		if (this.lastEvalTaskCLIString == null || 
			!this.lastEvalTaskCLIString.equals(currentEvalTaskCLIString)) 
		{
			this.lastEvalTaskCLIString = currentEvalTaskCLIString;
			
			// create result lists
			List<String> optionNames = new ArrayList<String>();
			List<String> optionDescriptions = new ArrayList<String>();
			
			// recursively add options
			ALPrequentialEvaluationTask evalTask = 
					(ALPrequentialEvaluationTask) 
					this.evalTaskOption.getPreMaterializedObject();
			this.addRecursiveNumberOptions(
					evalTask.learnerOption, optionNames, optionDescriptions, "");
			
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
		OptionHandler optionHandler = (OptionHandler) option.getPreMaterializedObject();
		Option[]      options       = optionHandler.getOptions().getOptionArray();
		
		// filter for Int and Float Options
		List<Option> numberOptions = new ArrayList<Option>(options.length);
		for (Option o : options) {
			if (o instanceof IntOption || o instanceof FloatOption) {
				numberOptions.add(o);
			}
		}
		
		// get names and descriptions
		String       newNamePrefix = namePrefix + option.getName() + "/";
		List<String> names         = new ArrayList<String>(numberOptions.size());
		List<String> descriptions  = new ArrayList<String>(numberOptions.size());
		for (Option o : numberOptions) {
			names.add(newNamePrefix + o.getName());
			descriptions.add(o.getPurpose());
		}
		
		// add to overall lists
		optionNames.addAll(names);
		optionDescriptions.addAll(descriptions);
		
		// filter for ClassOptions
		List<ClassOption> classOptions = new ArrayList<ClassOption>(options.length);
		for (Option o : options) {
			if (o instanceof ClassOption) {
				classOptions.add((ClassOption) o);
			}
		}
		
		// add number options of this class option to the overall list
		for (ClassOption classOption : classOptions) {
			this.addRecursiveNumberOptions(classOption, 
					optionNames, optionDescriptions, newNamePrefix);
		}
	}
	
	/**
	 * Resolve the name of the varied parameter and return the corresponding option.
	 * The varied parameter name has the format "learner/suboptions.../numberOption".
	 * If no matching parameter can be found, <code>null</code> is returned.
	 * 
	 * @param learner the learner object that has the varied option
	 * @param variedParamName name of the (nested) varied parameter
	 * @return varied option
	 */
	public static Option getVariedOption(OptionHandler learner, String variedParamName) {
		// split nested option string
		String[] singleOptions = variedParamName.split("/");
		
		// check if first level is "learner", which has already been resolved
		int startIndex = 0;
		if (singleOptions.length > 0 && singleOptions[0].equals("learner/")) {
			startIndex = 1;
		}
		
		// iteratively create objects and get next options for each level
		Option learnerVariedParamOption = null;
		OptionHandler currentOptionHandler = learner;
		for (int i = startIndex; i < singleOptions.length; i++) {
			for (Option opt : currentOptionHandler.getOptions().getOptionArray()) {
				if (opt.getName().equals(singleOptions[i])) {
					if (opt instanceof ClassOption) {
						currentOptionHandler = (OptionHandler) 
								((ClassOption) opt).getPreMaterializedObject();
					}
					else {
						learnerVariedParamOption = opt;
					}
					break;
				}
			}
		}
		
		return learnerVariedParamOption;
	}
}
