/*
 *    MultiChoiceOption.java
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
package moa.options;

import javax.swing.JComponent;

import moa.gui.MultiChoiceOptionEditComponent;

/**
 * Multi choice option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MultiChoiceOption extends AbstractOption {

    private static final long serialVersionUID = 1L;

    protected String[] optionLabels;

    protected String[] optionDescriptions;

    protected int defaultOptionIndex;

    protected int chosenOptionIndex;

    public MultiChoiceOption(String name, char cliChar, String purpose,
            String[] optionLabels, String[] optionDescriptions,
            int defaultOptionIndex) {
        super(name, cliChar, purpose);
        if (optionLabels.length != optionDescriptions.length) {
            throw new IllegalArgumentException("Labels/descriptions mismatch.");
        }
        this.optionLabels = optionLabels.clone();
        this.optionDescriptions = optionDescriptions.clone();
        this.defaultOptionIndex = defaultOptionIndex;
        resetToDefault();
    }

    @Override
    public String getDefaultCLIString() {
        return this.optionLabels[this.defaultOptionIndex];
    }

    @Override
    public String getValueAsCLIString() {
        return getChosenLabel();
    }

    @Override
    public void setValueViaCLIString(String s) {
        try {
            setChosenIndex(Integer.parseInt(s.trim()));
        } catch (NumberFormatException nfe) {
            setChosenLabel(s);
        }
    }

    public void setChosenLabel(String label) {
        label = label.trim();
        for (int i = 0; i < this.optionLabels.length; i++) {
            if (this.optionLabels[i].equals(label)) {
                this.chosenOptionIndex = i;
                return;
            }
        }
        throw new IllegalArgumentException("Label not recognised: " + label);
    }

    public void setChosenIndex(int index) {
        if ((index < 0) || (index >= this.optionLabels.length)) {
            throw new IndexOutOfBoundsException();
        }
        this.chosenOptionIndex = index;
    }

    public String[] getOptionLabels() {
        return this.optionLabels.clone();
    }

    public String getChosenLabel() {
        return this.optionLabels[this.chosenOptionIndex];
    }

    public int getChosenIndex() {
        return this.chosenOptionIndex;
    }

    @Override
    public JComponent getEditComponent() {
        return new MultiChoiceOptionEditComponent(this);
    }
}
