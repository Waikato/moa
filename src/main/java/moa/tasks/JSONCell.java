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
 * JSONCell.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.tasks;

/**
 * Class for creating a IPYNB file.
 *
 * @author Truong To (todinhtruong at gmail dot com)
 */
public class JSONCell {
  private String cell_type = "code";  //1
  private int execution_count = 1;    //2
  private String metadata = "";      //3
  private String outputs = "";       //4
  private String source = "";       //5
  private String result = "";

  public JSONCell() {

  }

  public JSONCell(String str) {
    this.cell_type = str;
  }

  /**
   * Adds a string to cell in a new separate line
   * @param st the string to be added
   * @param mode
   */
  public void addNewLineToCell(String st, int mode) {
    switch (mode) {
      case 1:
	if (cell_type != "")
	  this.cell_type = this.cell_type + ",\n";
	this.cell_type = this.cell_type + "\"" + st + "\\n\"";
	break;

      case 2:
	this.execution_count = Integer.valueOf(st);
	break;

      case 3:
	if (metadata != "")
	  this.metadata = this.metadata + ",\n";
	this.metadata = this.metadata + "\"" + st + "\\n\"";
	break;

      case 4:
	if (outputs != "")
	  this.outputs = this.outputs + ",\n";
	this.outputs = this.outputs + "\"" + st + "\\n\"";
	break;

      case 5:
	if (source != "")
	  this.source = this.source + ",\n";
	this.source = this.source + "\"" + st + "\\n\"";
	break;
    }
  }

  /**
   * Adds a string to cell in a current line at the last position before the return (\n) character
   * @param st the string to be added
   * @param mode
   */
  void addToCell(String st, int mode) {
    StringBuffer newString;
    switch (mode) {
      case 1:
	newString = new StringBuffer(this.cell_type);
	newString.insert(this.cell_type.length() - 3, st);
	this.cell_type = newString.toString();
	break;

      case 2:
	this.execution_count = Integer.valueOf(st);
	break;

      case 3:
	newString = new StringBuffer(this.metadata);
	newString.insert(this.metadata.length() - 3, st);
	this.metadata = newString.toString();
	break;

      case 4:
	newString = new StringBuffer(this.outputs);
	newString.insert(this.outputs.length() - 3, st);
	this.outputs = newString.toString();
	break;

      case 5:
	newString = new StringBuffer(this.source);
	newString.insert(this.source.length() - 3, st);
	this.source = newString.toString();
	break;
    }
  }

  /**
   * Creates a cell with the right format
   */
  public void createCell() {
    this.result = this.result + "\"" + "cell_type" + "\"" + ": " + "\"" + this.cell_type + "\",\n";
    if (!this.cell_type.equals("markdown"))
      this.result = this.result + "\"" + "execution_count" + "\"" + ": " + this.execution_count + ",\n";
    this.result = this.result + "\"" + "metadata" + "\"" + ": " + "{" + this.metadata + "},\n";
    if (!this.cell_type.equals("markdown"))
      this.result = this.result + "\"" + "outputs" + "\"" + ": " + "[" + this.outputs + "],\n";
    this.result = this.result + "\"" + "source" + "\"" + ": " + "[" + this.source + "]\n";
    this.result = "{\n" + this.result + "}";
  }

  public String getCell() {
    return this.result;
  }
}

