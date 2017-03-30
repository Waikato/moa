/*
 *    ParamGraphCanvas.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de),
 *            Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.gui.visualization;

import java.awt.Color;

import moa.evaluation.MeasureCollection;

/**
 * ParamGraphCanvas is an implementation of AbstractGraphCanvas showing the
 * relation between the actual relative number of acquired labels (also called
 * budget) and the measures.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de), 
 *         Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphCanvas
 */
public class BudgetGraphCanvas extends AbstractGraphCanvas {
	// TODO: maybe it is possible to extend ParamGraphCanvas

	private static final long serialVersionUID = 1L;

	public BudgetGraphCanvas(AbstractGraphAxes ax, AbstractGraphPlot g) {
		super(ax, g);
		// TODO Auto-generated constructor stub
	}
	
	public void setGraph(
			MeasureCollection[] measures, int mSelect, Color[] colors) 
	{
		// TODO use rel. number of acq. labels as x-value for each measurement
    }

	@Override
	public void setSize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPreferredSize() {
		// TODO Auto-generated method stub

	}

	@Override
	public double getMinXValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxXValue() {
		// TODO Auto-generated method stub
		return 0;
	}

}
