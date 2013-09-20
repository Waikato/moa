/**
 * [ClassOptionWithNamesEditComponent.java]
 * 
 * ClassOptionWithNames: Editing window
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Richard Kirkby
 * Data Management and Data Exploration Group, RWTH Aachen University
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

import moa.options.ClassOptionWithNames;
import com.github.javacliparser.Option;
import moa.gui.ClassOptionWithNamesSelectionPanel;

public class ClassOptionWithNamesEditComponent extends JPanel implements OptionEditComponent {

	private static final long serialVersionUID = 1L;

    protected ClassOptionWithNames editedOption;

    protected JTextField textField = new JTextField();

    protected JButton editButton = new JButton("Edit");

    /** listeners that listen to changes to the chosen option. */
    protected HashSet<ChangeListener> changeListeners = new HashSet<ChangeListener>();

    public ClassOptionWithNamesEditComponent(ClassOptionWithNames option) {
        this.editedOption = option;
        this.textField.setEditable(false);
        this.textField.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                notifyChangeListeners();
            }

            public void insertUpdate(DocumentEvent e) {
                notifyChangeListeners();
            }

            public void changedUpdate(DocumentEvent e) {
                notifyChangeListeners();
            }
        });
        
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
        setEditState(ClassOptionWithNamesSelectionPanel.showSelectClassDialog(this,
                "Editing option: " + this.editedOption.getName(),
                this.editedOption.getRequiredType(), this.textField.getText(),
                this.editedOption.getNullString(), this.editedOption.getClassNames()));
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
