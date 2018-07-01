/*
 *    FileOptionEditComponent.java
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

import com.github.javacliparser.FileOption;
import com.github.javacliparser.Option;
import moa.gui.FileExtensionFilter;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * An OptionEditComponent that lets the user edit a file option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class FileOptionEditComponent extends JPanel implements
        OptionEditComponent {

    private static final long serialVersionUID = 1L;

    protected FileOption editedOption;

    protected JTextField textField = new JTextField();

    protected JButton browseButton = new JButton("Browse");

    public FileOptionEditComponent(Option opt) {
        FileOption option = (FileOption) opt;
        this.editedOption = option;
        setLayout(new BorderLayout());
        add(this.textField, BorderLayout.CENTER);
        add(this.browseButton, BorderLayout.EAST);
        this.browseButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                browseForFile();
            }
        });
        setEditState(this.editedOption.getValueAsCLIString());
    }

    @Override
    public void applyState() {
        this.editedOption.setValueViaCLIString(this.textField.getText().length() > 0 ? this.textField.getText() : null);
    }

    @Override
    public Option getEditedOption() {
        return this.editedOption;
    }

    @Override
    public void setEditState(String cliString) {
        this.textField.setText(cliString);
    }

    public void browseForFile() {
        BaseFileChooser fileChooser = new BaseFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        String extension = this.editedOption.getDefaultFileExtension();
        if (extension != null) {
            fileChooser.addChoosableFileFilter(new FileExtensionFilter(
                    extension));
        }
        fileChooser.setSelectedFile(new File(this.textField.getText()));
        if (this.editedOption.isOutputFile()) {
            if (fileChooser.showSaveDialog(this.browseButton) == BaseFileChooser.APPROVE_OPTION) {
                File chosenFile = fileChooser.getSelectedFile();
                String fileName = chosenFile.getPath();
                if (!chosenFile.exists()) {
                    if ((extension != null) && !fileName.endsWith(extension)) {
                        fileName = fileName + "." + extension;
                    }
                }
                this.textField.setText(fileName);
            }
        } else {
            if (fileChooser.showOpenDialog(this.browseButton) == BaseFileChooser.APPROVE_OPTION) {
                this.textField.setText(fileChooser.getSelectedFile().getPath());
            }
        }
    }
}
