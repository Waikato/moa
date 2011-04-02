/*
 *    AbstractTabPanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author FracPete (fracpete at waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.gui;

/**
 * Abstract Tab Panel.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public abstract class AbstractTabPanel extends javax.swing.JPanel {

    /**
     * Returns the string to display as title of the tab.
     * 
     * @return the string to display as title of the tab
     */
    public abstract String getTabTitle();

    /**
     * Returns a short description (can be used as tool tip) of the tab, or contributor, etc.
     *
     * @return a short description of this tab panel.
     */
    public abstract String getDescription();
}
