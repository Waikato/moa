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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * File option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class Options implements Serializable {//extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected List<Option> optionList = new LinkedList<Option>();

    public void addOption(Option opt) {
        if (getOption(opt.getName()) != null) {
            throw new IllegalArgumentException("Duplicate option name: "
                    + opt.getName());
        }
        if (getOption(opt.getCLIChar()) != null) {
            throw new IllegalArgumentException(
                    "Duplicate option command line character: "
                    + opt.getCLIChar());
        }
        this.optionList.add(opt);
    }

    public int numOptions() {
        return this.optionList.size();
    }

    public Option getOption(String optName) {
        for (Option option : this.optionList) {
            if (optName.equals(option.getName())) {
                return option;
            }
        }
        return null;
    }

    public Option getOption(char cliChar) {
        for (Option option : this.optionList) {
            if (option.getCLIChar() == cliChar) {
                return option;
            }
        }
        return null;
    }

    public Option[] getOptionArray() {
        return this.optionList.toArray(new Option[this.optionList.size()]);
    }

    public void removeOption(String optName) {
        removeOption(getOption(optName));
    }

    public void removeOption(Option opt) {
        this.optionList.remove(opt);
    }

    public void removeAllOptions() {
        this.optionList = new LinkedList<Option>();
    }

    public void resetToDefaults() {
        for (Option option : this.optionList) {
            option.resetToDefault();
        }
    }

    public void setViaCLIString(String cliString) {
        cliString = cliString.trim();
        while (cliString.length() > 0) {
            if (cliString.startsWith("-")) {
                boolean flagClusterFound = false;
                String optionString = null;
                int nextSpaceIndex = cliString.indexOf(' ', 1);
                Option opt;
                if (nextSpaceIndex > 0) {
                    optionString = cliString.substring(1, nextSpaceIndex);
                } else {
                    optionString = cliString.substring(1, cliString.length());
                    nextSpaceIndex = cliString.length() - 1;
                }
                if (optionString.length() == 1) {
                    opt = getOption(optionString.charAt(0));
                } else {
                    opt = getOption(optionString);
                    if (opt == null) {
                        // check for cluster of flags
                        flagClusterFound = true;
                        for (int i = 0; i < optionString.length(); i++) {
                            opt = getOption(optionString.charAt(i));
                            if (!(opt instanceof FlagOption)) {
                                flagClusterFound = false;
                                opt = null;
                                break;
                            }
                        }
                        if (flagClusterFound) {
                            for (int i = 0; i < optionString.length(); i++) {
                                opt = getOption(optionString.charAt(i));
                                opt.setValueViaCLIString("");
                            }
                            cliString = cliString.substring(nextSpaceIndex + 1,
                                    cliString.length());
                        }
                    }
                }
                if (!flagClusterFound) {
                    if (opt != null) {
                        String parameters = cliString.substring(
                                nextSpaceIndex + 1, cliString.length());
                        if (opt instanceof FlagOption) {
                            opt.setValueViaCLIString("");
                            cliString = parameters;
                        } else {
                            String[] paramSplit = splitParameterFromRemainingOptions(parameters);
                            opt.setValueViaCLIString(paramSplit[0]);
                            cliString = paramSplit[1];
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown option: -"
                                + optionString);
                    }
                }
            } else {
                throw new IllegalArgumentException("Expecting option, found: '"
                        + cliString + "'.");
            }
            cliString = cliString.trim();
        }
    }

    public String getAsCLIString() {
        StringBuilder commandLine = new StringBuilder();
        for (Option option : this.optionList) {
            String value = option.getValueAsCLIString();
            if ((value != null) && !value.equals(option.getDefaultCLIString())) {
                if (commandLine.length() > 0) {
                    commandLine.append(" ");
                }
                commandLine.append("-" + option.getCLIChar());
                if (value.length() > 0) {
                    if (value.indexOf(' ') < 0) {
                        commandLine.append(" " + value);
                    } else {
                        commandLine.append(" (" + value + ")");
                    }
                }
            }
        }
        return commandLine.toString();
    }

    public String getHelpString() {
        StringBuilder sb = new StringBuilder();
        getHelp(sb, 0);
        return sb.toString();
    }

    public void getHelp(StringBuilder sb, int indent) {
        if (optionList.size() > 0) {
            for (Option option : optionList) {
                StringUtils.appendIndent(sb, indent);
                sb.append('-');
                sb.append(option.getCLIChar());
                sb.append(' ');
                sb.append(option.getName());
                String defaultString = option.getDefaultCLIString();
                if (defaultString != null && defaultString.length() > 0) {
                    sb.append(" (default: ");
                    sb.append(defaultString);
                    sb.append(')');
                }
                StringUtils.appendNewline(sb);
                StringUtils.appendIndent(sb, indent);
                sb.append(option.getPurpose());
                StringUtils.appendNewline(sb);
            }
        } else {
            StringUtils.appendIndented(sb, indent, "No options.");
        }
    }

    /**
     * Internal method that splits a string into two parts - the parameter for
     * the current option, and the remaining options.
     *
     * @param cliString
     *            the command line string, beginning at an option parameter
     * @return an array of two strings - the first is the option paramter, the
     *         second is the remaining cli string
     */
    protected static String[] splitParameterFromRemainingOptions(
            String cliString) {
        String[] paramSplit = new String[2];
        cliString = cliString.trim();
        if (cliString.startsWith("\"") || cliString.startsWith("'")) {
            int endQuoteIndex = cliString.indexOf(cliString.charAt(0), 1);
            if (endQuoteIndex < 0) {
                throw new IllegalArgumentException(
                        "Quotes not terminated correctly.");
            }
            paramSplit[0] = cliString.substring(1, endQuoteIndex);
            paramSplit[1] = cliString.substring(endQuoteIndex + 1, cliString.length());
        } else if (cliString.startsWith("(")) {
            int bracketsOpen = 1;
            int currPos = 1;
            int nextCloseIndex = cliString.indexOf(")", currPos);
            int nextOpenIndex = cliString.indexOf("(", currPos);
            while (bracketsOpen != 0) {
                if (nextCloseIndex < 0) {
                    throw new IllegalArgumentException("Brackets do not match.");
                } else if ((nextOpenIndex < 0)
                        || (nextCloseIndex < nextOpenIndex)) {
                    bracketsOpen--;
                    currPos = nextCloseIndex + 1;
                    nextCloseIndex = cliString.indexOf(")", currPos);
                } else {
                    bracketsOpen++;
                    currPos = nextOpenIndex + 1;
                    nextOpenIndex = cliString.indexOf("(", currPos);
                }
            }
            paramSplit[0] = cliString.substring(1, currPos - 1);
            paramSplit[1] = cliString.substring(currPos, cliString.length());
        } else {
            int firstSpaceIndex = cliString.indexOf(" ", 0);
            if (firstSpaceIndex >= 0) {
                paramSplit[0] = cliString.substring(0, firstSpaceIndex);
                paramSplit[1] = cliString.substring(firstSpaceIndex + 1,
                        cliString.length());
            } else {
                paramSplit[0] = cliString;
                paramSplit[1] = "";
            }
        }
        return paramSplit;
    }

    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
