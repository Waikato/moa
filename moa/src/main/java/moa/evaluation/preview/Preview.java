/*
 *    Preview.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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
package moa.evaluation.preview;

import java.util.ArrayList;
import java.util.List;

import moa.AbstractMOAObject;

/**
 * Abstract class which is used to define the methods needed from a preview
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public abstract class Preview extends AbstractMOAObject{
	private static final long serialVersionUID = 1L;

	// TODO add methods to return a 2D double array
	public abstract int getMeasurementNameCount();

	public abstract String getMeasurementName(int measurementIndex);

	public abstract int numEntries();

	public abstract String entryToString(int entryIndex);

	public abstract Class<?> getTaskClass();

	public abstract double[] getEntryData(int entryIndex);
	
	public String[] getMeasurementNames() {
		int numNames = getMeasurementNameCount();
		String[] names = new String[numNames];
		for (int i = 0; i < numNames; i++) {
			names[i] = getMeasurementName(i);
		}
		return names;
	}

	public List<double[]> getData()
	{
		// create list to store all entries
		List<double[]> data = new ArrayList<>();
		// add all entries in the list above
        for (int entryIdx = 0; entryIdx < numEntries(); entryIdx++) {
            data.add(getEntryData(entryIdx));
        }
		
		return data;
	}
}
