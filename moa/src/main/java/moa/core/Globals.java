/*
 *    Globals.java
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

/**
 * Class for storing global information about current version of MOA.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class Globals {

    public static final String workbenchTitle = "{M}assive {O}nline {A}nalysis";

    public static final String versionString = " 21.07 July 2021";

    public static final String copyrightNotice = "(C) 2007-2021 University of Waikato, Hamilton, New Zealand";

    public static final String webAddress = "http://moa.cms.waikato.ac.nz/";

    public static String getWorkbenchInfoString() {
        StringBuilder result = new StringBuilder();
        result.append(workbenchTitle);
        StringUtils.appendNewline(result);
        result.append("Version: ");
        result.append(versionString);
        StringUtils.appendNewline(result);
        result.append("Copyright: ");
        result.append(copyrightNotice);
        StringUtils.appendNewline(result);
        result.append("Web: ");
        result.append(webAddress);
        return result.toString();
    }
}
