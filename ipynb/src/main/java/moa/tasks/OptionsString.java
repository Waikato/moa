/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * OptionsString.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.tasks;


import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;

/**
 * This class get input string of learner, stream and evaluator then process them
 * the output will be name of learner, stream, or evaluator besides their options
 *
 * @author Truong To (todinhtruong at gmail dot com)
 **/
public class OptionsString {

  private String classShortName = "";

  private String classFullName = "";

  private String classOptionsString = "";

  private ArrayList<Pair<String, String>> outputClassObjectOptions = new ArrayList();

  private ArrayList<Pair<String, String>> outputClassOptions = new ArrayList();

  private String inputString = "";

  public OptionsString(String str) {
    this.inputString = str;
    int i = this.inputString.indexOf(" ");
    if (i > 0) {
      this.classOptionsString = this.inputString.substring(i + 1);
      this.classFullName = this.inputString.substring(0, i);
    } else {
      this.classFullName = this.inputString;
    }
  }

  public String getInputString() {
    return this.inputString;
  }

  public String getClassShortName() {
    return this.classShortName;
  }

  public String getClassFullName() {
    return this.classFullName;
  }

  public void addOptionsStringToCell(JSONCell cell) {
    cell.addNewLineToCell(this.getClassShortName() + " " + this.getClassShortName().substring(0, 4).toLowerCase() +
      " = new " + this.getClassShortName() + "();", 5);
    if (!this.classOptionsString.equals(""))
      cell.addNewLineToCell(this.getClassShortName().substring(0, 4).toLowerCase() +
	".getOptions().setViaCLIString(\\\"" + this.classOptionsString + "\\\");", 5);
  }

  /**
   * Separates out options from command strings
   */
  public void createOptionsList() {
    int j;
    int i;
    String tempClassOptionsString = this.classOptionsString;
    while (tempClassOptionsString.length() > 0) {
      char cliChar = ' ';
      String optionValue = "";
      String str = "";
      tempClassOptionsString = tempClassOptionsString.trim();

      i = tempClassOptionsString.indexOf("-");
      if (i >= 0) {
	cliChar = tempClassOptionsString.charAt(i + 1);
	tempClassOptionsString = tempClassOptionsString.substring(i + 2).trim();
	if (tempClassOptionsString.length() == 0) {
	  optionValue = "true";
	  Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
	  this.outputClassOptions.add(optionPair);
	} else {
	  if (tempClassOptionsString.charAt(0) == '-') {
	    optionValue = "true";
	    Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
	    this.outputClassOptions.add(optionPair);
	  } else if (tempClassOptionsString.charAt(0) == '(') {
	    int openBracket = 0;
	    int closeBracket = 0;
	    StringBuffer temp = new StringBuffer("");
	    for (int k = 0; k < tempClassOptionsString.length(); k++) {
	      char cTemp = tempClassOptionsString.charAt(k);
	      temp.append(cTemp);
	      switch (cTemp) {
		case '(': {
		  openBracket += 1;
		  break;
		}
		case ')': {
		  closeBracket += 1;
		  if (closeBracket == openBracket) {
		    tempClassOptionsString = tempClassOptionsString.substring(k + 1).trim();
		    optionValue = temp.toString().trim();
		    optionValue = optionValue.substring(1, optionValue.length() - 1);
		    Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
		    this.outputClassObjectOptions.add(optionPair);
		    OptionsString subObject = new OptionsString(optionValue);
		  }
		  break;
		}
	      }
	    }


	  } else {
	    j = tempClassOptionsString.indexOf(" ");
	    if (j > 0) {
	      optionValue = tempClassOptionsString.substring(0, j);
	      tempClassOptionsString = tempClassOptionsString.substring(j + 1).trim();
	      Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
	      this.outputClassOptions.add(optionPair);
	    } else {
	      optionValue = tempClassOptionsString;
	      tempClassOptionsString = "";
	      Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
	      this.outputClassOptions.add(optionPair);
	    }
	  }
	}

      }
    }

    i = this.classFullName.lastIndexOf('.');
    if (i > 0) {
      this.classShortName = this.classFullName.substring(i + 1);
    } else
      this.classShortName = this.classFullName;
  }
}
