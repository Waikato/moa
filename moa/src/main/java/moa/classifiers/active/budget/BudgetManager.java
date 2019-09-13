/*
 *    BudgetManager.java
 *    Copyright (C) 2016 Otto von Guericke University, Magdeburg, Germany
 *    @author Daniel Kottke (daniel dot kottke at ovgu dot de)
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

package moa.classifiers.active.budget;

/**
 * Budget Manager Interface to make AL Classifiers select the most beneficial
 * instances. A Budget Manager is defined in that way that it defines a
 * threshold internally and tests if the internal value is above or equal that
 * threshold.
 * 
 * This Budget Manager assumes that a new instance is processed each time the
 * method 'isAbove' is called and that the number of acquired instances equals
 * the number of returned 'true's.
 *
 * @author Daniel Kottke (daniel dot kottke at ovgu dot de)
 * @version $Revision: 1 $
 */
public interface BudgetManager {

	/**
	 * Returns true if the given value is above an internal threshold and the 
	 * label should be acquired.
	 */
	public boolean isAbove(double value);

	/**
	 * Returns the number of labels that have been chosen for acquisition since
	 * the last report.
	 */
	public int getLastLabelAcqReport();

	/**
	 * Resets the budget manager.
	 */
	public void resetLearning();
}
