/*
 *    ClassOptionEditComponent.java
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import moa.options.ClassOption;
import moa.options.Option;

public class ClassOptionEditComponent extends JPanel implements
		OptionEditComponent {

	private static final long serialVersionUID = 1L;

	protected ClassOption editedOption;

	protected JTextField textField = new JTextField();

	protected JButton editButton = new JButton("Edit");

	public ClassOptionEditComponent(ClassOption option) {
		this.editedOption = option;
		this.textField.setEditable(false);
		setLayout(new BorderLayout());
		add(this.textField, BorderLayout.CENTER);
		add(this.editButton, BorderLayout.EAST);
		this.editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				editObject();
			}
		});
		setEditState(this.editedOption.getValueAsCLIString());
	}

	public void applyState() {
		this.editedOption.setValueViaCLIString(this.textField.getText());
	}

	public Option getEditedOption() {
		return this.editedOption;
	}

	public void setEditState(String cliString) {
		this.textField.setText(cliString);
	}

	public void editObject() {
		setEditState(ClassOptionSelectionPanel.showSelectClassDialog(this,
				"Editing option: " + this.editedOption.getName(),
				this.editedOption.getRequiredType(), this.textField.getText(),
				this.editedOption.getNullString()));
	}

}
