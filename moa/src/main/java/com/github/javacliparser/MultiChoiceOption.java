/*
 * Copyright 2007 University of Waikato.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */

package com.github.javacliparser;

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

    //@Override
    //public JComponent getEditComponent() {
    //    return new MultiChoiceOptionEditComponent(this);
    //}
}
