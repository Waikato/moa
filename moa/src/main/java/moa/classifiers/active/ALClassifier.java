/*
 *    ALClassifier.java
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
package moa.classifiers.active;

import moa.classifiers.Classifier;

/**
 * Active Learning Classifier Interface to make AL Classifiers selectable in AL tasks.
 *
 * @author Daniel Kottke (daniel dot kottke at ovgu dot de)
 * @version $Revision: 1 $
 */
public interface ALClassifier extends Classifier {

   /**
     * Returns true if the previously chosen instance was added to the training set 
     * of the active learner.
     *
     */
	public int getLastLabelAcqReport();
}
