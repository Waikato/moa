/*
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
package com.yahoo.labs.samoa.instances;

import java.io.Serializable;

public class Range implements Serializable {

    //Only works for ranges "start-end"
    private int start = 0;
    private int end = 0;
    private int upperLimit = 0;
    private final String rangeText;

    public Range(String range) {
        this.rangeText = range;
        this.setRange(range); //needs upperLimit
    }

    /**
     * Sets the range from a string representation.
     *
     * @param range the start and end string
     *
     */
    public void setRange(String range) {
        String single = range.trim();
        int hyphenIndex = range.indexOf('-');

        if (hyphenIndex > 0) {
            this.start = rangeSingle(range.substring(0, hyphenIndex));
            this.end = rangeSingle(range.substring(hyphenIndex + 1));
        } else {
            int number = rangeSingle(range);
            if (number >= 0) { // first n attributes
                this.start = 0;
                this.end = number;
            } else { // last n attributes
                this.start = this.upperLimit + number > 0 ? this.upperLimit + number : 0;
                this.end = this.upperLimit - 1;
            }
        }
    }

    /**
     * Translates a single string selection into it's internal 0-based
     * equivalent.
     *
     * @param singleSelection the string representing the selection (eg: 1 first last)
     * @return the number corresponding to the selected value
     */
    protected /*@pure@*/ int rangeSingle(/*@non_null@*/String singleSelection) {

        String single = singleSelection.trim();
        if (single.toLowerCase().equals("first")) {
            return 0;
        }
        if (single.toLowerCase().equals("last") || single.toLowerCase().equals("-1")) {
            return -1;
        }
        int index = Integer.parseInt(single);
        if (index >= 1) { //Non for negatives
            index--;
        }
        return index;
    }

    boolean isInRange(int value) {
        boolean ret = false;
        if (value >= start && value <= end) {
            ret = true;
        }
        return ret;
    }

    int getSelectionLength() {
        return end - start + 1;
    }

    public void setUpper(int attributeNumber) {
        this.upperLimit = attributeNumber;
        this.setRange(this.rangeText);
    }

    //JD
    public int getStart() {
        return start;
    }

    //JD

    public int getEnd() {
        return end;
    }

}
