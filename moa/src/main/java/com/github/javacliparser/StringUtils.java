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

import java.text.DecimalFormat;

/**
 * Class implementing some string utility methods.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class StringUtils {

    public static final String newline = System.getProperty("line.separator");

    public static String doubleToString(double value, int fractionDigits) {
        return doubleToString(value, 0, fractionDigits);
    }

    public static String doubleToString(double value, int minFractionDigits,
            int maxFractionDigits) {
        DecimalFormat numberFormat = new DecimalFormat();
        numberFormat.setMinimumFractionDigits(minFractionDigits);
        numberFormat.setMaximumFractionDigits(maxFractionDigits);
        return numberFormat.format(value);
    }

    public static void appendNewline(StringBuilder out) {
        out.append(newline);
    }

    public static void appendIndent(StringBuilder out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.append(' ');
        }
    }

    public static void appendIndented(StringBuilder out, int indent, String s) {
        appendIndent(out, indent);
        out.append(s);
    }

    public static void appendNewlineIndented(StringBuilder out, int indent,
            String s) {
        appendNewline(out);
        appendIndented(out, indent, s);
    }

    /*public static String secondsToDHMSString(double seconds) {
        if (seconds < 60) {
            return doubleToString(seconds, 2, 2) + 's';
        }
        long secs = (int) (seconds);
        long mins = secs / 60;
        long hours = mins / 60;
        long days = hours / 24;
        secs %= 60;
        mins %= 60;
        hours %= 24;
        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days);
            result.append('d');
        }
        if ((hours > 0) || (days > 0)) {
            result.append(hours);
            result.append('h');
        }
        if ((hours > 0) || (days > 0) || (mins > 0)) {
            result.append(mins);
            result.append('m');
        }
        result.append(secs);
        result.append('s');
        return result.toString();
    }*/
}
