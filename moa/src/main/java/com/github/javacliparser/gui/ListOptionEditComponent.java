/*
 *    StringOptionEditComponent.java
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

import com.github.javacliparser.Option;

import javax.swing.*;

/**
 * An OptionEditComponent that lets the user edit a list option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ListOptionEditComponent extends JTextField implements
        OptionEditComponent {

    protected StringOptionEditComponent component;

    private static final long serialVersionUID = 1L;

    public ListOptionEditComponent(Option option) {
        component = new StringOptionEditComponent(option);
        setText(component.getText());
    }

    @Override
    public Option getEditedOption() {
        return component.getEditedOption();
    }

    @Override
    public void setEditState(String cliString) {
        component.setText(cliString);
        setText(component.getText());
    }

    @Override
    public void applyState() {
        getEditedOption().setValueViaCLIString(getText().length() > 0 ? getText() : null);
        setText(component.getText());
    }
}

