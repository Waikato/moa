/*
 *    ALTaskTextViewerPanel.java
 *    Original Work: Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *    Modified Work: Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
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

import moa.evaluation.MeasureCollection;

/**
 * A BudgetGraphCanvas is a live graph showing the performance per budget as a
 * scatter plot.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvas
 */
public class BudgetGraphCanvas extends AbstractGraphCanvas {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor. Initialises the class variables and panels.
	 */
    public BudgetGraphCanvas() { 	
    	super(new ParamGraphAxes(), new GraphScatter());
    }

    
    /**
     * Updates the graph.
     * @param measure0  list of measure collections, one for each budget
     * @param measure1  list of measure collections, one for each budget
     * @param mSelect   currently selected measure
     */
    public void setGraph(MeasureCollection[] measures, int mSelect) {
        this.measures = measures;
        this.measureSelected = mSelect;

        ((GraphScatter) this.curvePanel).setGraph(measures, mSelect);
        updateCanvas(false);
    }

}
