package moa.gui;

import javax.swing.DefaultComboBoxModel;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.MultiChoiceOptionEditComponent;

import moa.options.EditableMultiChoiceOption;

public class EditableMultiChoiceOptionEditComponent extends MultiChoiceOptionEditComponent {

	private static final long serialVersionUID = 1L;

	public EditableMultiChoiceOptionEditComponent(Option option) {
		super(option);
		((EditableMultiChoiceOption) option).registerEditComponent(this);
	}
	
	public void refresh() {
		setModel(new DefaultComboBoxModel<String>(
				this.editedOption.getOptionLabels()));
        setSelectedIndex(this.editedOption.getChosenIndex());
	}
	
}
