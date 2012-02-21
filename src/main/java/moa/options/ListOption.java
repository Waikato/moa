/*
 *    ListOption.java
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
