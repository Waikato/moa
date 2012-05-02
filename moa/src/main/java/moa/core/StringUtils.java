/*
 *    StringUtils.java
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
package moa.core;

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

    public static String secondsToDHMSString(double seconds) {
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
    }
}
