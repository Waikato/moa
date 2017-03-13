package moa.gui;

import javax.swing.DefaultComboBoxModel;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.MultiChoiceOptionEditComponent;

public class EditableMultiChoiceOptionEditComponent extends MultiChoiceOptionEditComponent {

	private static final long serialVersionUID = 1L;

	public EditableMultiChoiceOptionEditComponent(Option option) {
		super(option);
		
		System.out.println("EditableMultiChoiceOptionEditComponent created");
	}
	
	public void refresh() {
		setModel(new DefaultComboBoxModel<String>(this.editedOption.getOptionLabels()));
        setSelectedIndex(this.editedOption.getChosenIndex());
	}
	
}
