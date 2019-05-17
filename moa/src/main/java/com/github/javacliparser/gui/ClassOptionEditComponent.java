/*
 *    ClassOptionEditComponent.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package com.github.javacliparser.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import moa.options.ClassOption;
import com.github.javacliparser.Option;
import moa.gui.ClassOptionSelectionPanel;

/**
 * An OptionEditComponent that lets the user edit a class option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ClassOptionEditComponent extends JPanel implements
        OptionEditComponent {

    private static final long serialVersionUID = 1L;

    protected ClassOption editedOption;

    protected JTextField textField = new JTextField();

    protected JButton editButton = new JButton("Edit");

    /**
     * Flag that says the text field is in the middle of an update operation.
     * This is to prevent two change notifications from going out when the
     * update is implemented as a remove followed by an insert.
     */
    protected boolean midUpdate = false;

    /** listeners that listen to changes to the chosen option. */
    protected HashSet<ChangeListener> changeListeners = new HashSet<ChangeListener>();

    public ClassOptionEditComponent(Option opt) {
        ClassOption option = (ClassOption) opt;
        this.editedOption = option;
        this.textField.setEditable(false);
        this.textField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!midUpdate)
                    notifyChangeListeners();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyChangeListeners();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyChangeListeners();
            }
        });
        setLayout(new BorderLayout());
        add(this.textField, BorderLayout.CENTER);
        add(this.editButton, BorderLayout.EAST);
        this.editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                editObject();
            }
        });
        setEditState(this.editedOption.getValueAsCLIString());
    }

    @Override
    public void applyState() {
        this.editedOption.setValueViaCLIString(this.textField.getText());
    }

    @Override
    public Option getEditedOption() {
        return this.editedOption;
    }

    @Override
    public void setEditState(String cliString) {
        if (cliString.length() > 0) midUpdate = true;
        this.textField.setText(cliString);
        midUpdate = false;
    }

    public void editObject() {
        setEditState(ClassOptionSelectionPanel.showSelectClassDialog(this,
                "Editing option: " + this.editedOption.getName(),
                this.editedOption.getRequiredType(), this.textField.getText(),
                this.editedOption.getNullString()));
    }

    /**
     * Adds the listener to the internal set of listeners. Gets notified when
     * the option string changes.
     *
     * @param l the listener to add
     */
    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    /**
     * Removes the listener from the internal set of listeners.
     *
     * @param l the listener to remove
     */
    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(l);
    }

    /**
     * Notifies all registered change listeners that the options have changed.
     */
    protected void notifyChangeListeners() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : changeListeners) {
            l.stateChanged(e);
        }
    }
}
