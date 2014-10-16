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

import java.util.Arrays;
import com.yahoo.labs.samoa.instances.Range;

/**
 * Range option.
 *
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version $Revision: 7 $
 */
public class RangeOption extends StringOption {

    private static final long serialVersionUID = 1L;

	public RangeOption(String name, char cliChar, String purpose, String defaultValue) {
        super(name, cliChar, purpose, defaultValue);
    }

	/*
	 * This class will be like StringOption, but expect a string of numbers like Weka's Range
	 * e.g., 1,2,5-9,end will return something like [0,1,4,5,6,7,8,-1] which we will use later indicate e.g. multiple class attributes
	 */

	public void setRange(int indices[]) {
        this.currentVal = Arrays.toString(indices); 								// "[1,2,3]"
		this.currentVal = this.currentVal.substring(1,this.currentVal.length()-1);	// "1,2,3"
    }
    
	public Range getRange() {	
        return new Range(this.getValue());
    }

    /*public int[] getRange() {
		Range r = new Range(this.getValue());
        return r.getSelection();
    }*/

}

