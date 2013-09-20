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
 * List option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ListOption extends AbstractOption {

    private static final long serialVersionUID = 1L;

    protected Option[] currentList;

    protected Option expectedType;

    protected Option[] defaultList;

    protected char separatorChar;

    public ListOption(String name, char cliChar, String purpose,
            Option expectedType, Option[] defaultList, char separatorChar) {
        super(name, cliChar, purpose);
        this.expectedType = expectedType;
        this.defaultList = defaultList.clone();
        this.separatorChar = separatorChar;
        resetToDefault();
    }

    public void setList(Option[] optList) {
        Option[] newArray = new Option[optList.length];
        for (int i = 0; i < optList.length; i++) {
            newArray[i] = this.expectedType.copy();
            newArray[i].setValueViaCLIString(optList[i].getValueAsCLIString());
        }
        this.currentList = newArray;
    }

    public Option[] getList() {
        return this.currentList.clone();
    }

    @Override
    public String getDefaultCLIString() {
        return optionArrayToCLIString(this.defaultList, this.separatorChar);
    }

    @Override
    public String getValueAsCLIString() {
        return optionArrayToCLIString(this.currentList, this.separatorChar);
    }

    @Override
    public void setValueViaCLIString(String s) {
        this.currentList = cliStringToOptionArray(s, this.separatorChar,
                this.expectedType);
    }

    public static Option[] cliStringToOptionArray(String s, char separator,
            Option expectedType) {
	 if (s == null || s.length() < 1) {
             return new Option[0];
         }
        String[] subStrings = s.split(Character.toString(separator));
        Option[] options = new Option[subStrings.length];
        for (int i = 0; i < options.length; i++) {
            options[i] = expectedType.copy();
            options[i].setValueViaCLIString(subStrings[i]);
        }
        return options;
    }

    public static String optionArrayToCLIString(Option[] os, char separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < os.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(os[i].getValueAsCLIString());
        }
        return sb.toString();
    }
}
